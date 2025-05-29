package com.andeptrai.iot.websocket;


import com.andeptrai.iot.model.Lichsu;
import com.andeptrai.iot.model.Iot;
import com.andeptrai.iot.model.dulieu;
import com.andeptrai.iot.repository.Data_repo;
import com.andeptrai.iot.repository.Lichsu_repo;
import com.andeptrai.iot.repository.iot_repo;
import com.andeptrai.iot.service.Data_ser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Component
public class WebHander extends TextWebSocketHandler {

    private final iot_repo iotRepo;
    private final Data_ser dataSer;
    private final Lichsu_repo lichsuRepo;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Float>> pendingRequests = new ConcurrentHashMap<>();


    public WebHander(iot_repo iotRepo, Data_ser dataSer, Lichsu_repo lichsuRepo) {
        this.iotRepo = iotRepo;
        this.dataSer = dataSer;
        this.lichsuRepo = lichsuRepo;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(message.getPayload());
            
            String deviceId = node.get("Mac").asText();
            float Doam = (float) node.get("Doam").asDouble();

            Optional<Iot> kiemtra= iotRepo.findBymacId(deviceId);
            if (kiemtra.isEmpty()) {
                return;
            }
            // Ghi nhớ phiên
            sessions.put(deviceId, session);
            
            // Lưu dữ liệu cảm biến
            dulieu data = new dulieu();
            data.setMac(deviceId);
            data.setDoam(Doam);
            data.setTime(LocalDateTime.now());
            dataSer.saveData(data);

            // Tự động tưới 
            Optional<Iot> device = iotRepo.findBymacId(deviceId);
            if (device != null  && device.get().getWater() != 0) {
                String command = Doam < device.get().getDo_am()? "ON" : "OFF";
                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage("{\"command\":\"" + command + "\"}"));
                }
                // Lưu lịch sử tưới
                Lichsu history = new Lichsu();
                history.setMac(deviceId);
                history.setTimestamp(LocalDateTime.now());
                history.setAction(command);
                history.setAuto(device.get().getWater());
                history.setUsername(device.get().getUsername());
                lichsuRepo.save(history);
            }
             if (pendingRequests.containsKey(deviceId)) {
                pendingRequests.get(deviceId).complete(Doam);
                pendingRequests.remove(deviceId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean sendCommandToDevice(String deviceId, String command) {
        WebSocketSession session = sessions.get(deviceId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage("{\"command\":\"" + command + "\"}"));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
    
    
    public Float xemDoam(String Mac) throws Exception {
        WebSocketSession session = sessions.get(Mac);
        if (session == null || !session.isOpen()) {
            throw new Exception("ko thay thiet bi");
        }

        CompletableFuture<Float> future = new CompletableFuture<>();
        pendingRequests.put(Mac, future);

        // Gửi lệnh tới IoT
        session.sendMessage(new TextMessage("{\"command\":\"GET_DO_AN\"}"));

        // Chờ IoT trả dữ liệu trong 5 giây
        return future.get(5, TimeUnit.SECONDS);
    }
}
