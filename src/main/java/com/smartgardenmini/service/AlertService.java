package com.smartgardenmini.service;

import com.smartgardenmini.model.AlertRule;
import com.smartgardenmini.model.Iot;
import com.smartgardenmini.model.SensorData;
import com.smartgardenmini.repository.AlertRuleRepository;
import com.smartgardenmini.repository.IotRepository;
import com.smartgardenmini.repository.SensorDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private final AlertRuleRepository alertRuleRepo;
    private final IotRepository iotRepo;
    private final SensorDataRepository sensorDataRepo;

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.chat-id:}")
    private String chatId;

    private final Map<String, LocalDateTime> lastWarned = new ConcurrentHashMap<>();

    public AlertService(AlertRuleRepository alertRuleRepo, IotRepository iotRepo,
                        SensorDataRepository sensorDataRepo) {
        this.alertRuleRepo = alertRuleRepo;
        this.iotRepo = iotRepo;
        this.sensorDataRepo = sensorDataRepo;
    }

    public void sendTelegram(String message) {
        if (botToken == null || botToken.isEmpty() || chatId == null || chatId.isEmpty()) {
            log.debug("Telegram chưa cấu hình, bỏ qua: {}", message);
            return;
        }
        try {
            String urlStr = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String body = "chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8)
                    + "&text=" + URLEncoder.encode(message, StandardCharsets.UTF_8)
                    + "&parse_mode=HTML";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                log.info("Telegram OK: {}", message.substring(0, Math.min(50, message.length())));
            } else {
                log.warn("Telegram HTTP {}", code);
            }
        } catch (Exception e) {
            log.error("Lỗi gửi Telegram", e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void checkAlertRules() {
        List<AlertRule> rules = alertRuleRepo.findAll();
        for (AlertRule rule : rules) {
            if (!rule.isEnabled()) continue;
            try {
                checkRule(rule);
            } catch (Exception e) {
                log.error("Lỗi rule {}", rule.getId(), e);
            }
        }
    }

    private void checkRule(AlertRule rule) {
        String macId = rule.getMacId();
        Iot device = iotRepo.findBymacId(macId).orElse(null);
        if (device == null) return;

        List<SensorData> data = sensorDataRepo.findByMacOrderByTimeDesc(macId);
        if (data.isEmpty()) return;

        SensorData last = data.get(0);
        float doam = last.getDoam();
        LocalDateTime now = LocalDateTime.now();

        if (doam < rule.getMinHumidity()) {
            String key = macId + "_LOW";
            if (canWarn(key, 30)) {
                sendTelegram("🌱 <b>Độ ẩm thấp!</b>\n"
                        + device.getName() + ": " + String.format("%.1f", doam) + "%\n"
                        + "Ngưỡng: " + rule.getMinHumidity() + "%");
            }
        } else if (doam > rule.getMaxHumidity()) {
            String key = macId + "_HIGH";
            if (canWarn(key, 30)) {
                sendTelegram("💧 <b>Độ ẩm cao!</b>\n"
                        + device.getName() + ": " + String.format("%.1f", doam) + "%\n"
                        + "Ngưỡng: " + rule.getMaxHumidity() + "%");
            }
        }

        if (rule.isNotifyOnDisconnect() && !device.isOnline()) {
            String key = macId + "_OFF";
            if (canWarn(key, 360)) {
                sendTelegram("⚠️ <b>Thiết bị offline!</b>\n" + device.getName());
            }
        }
    }

    private boolean canWarn(String key, int intervalMin) {
        LocalDateTime last = lastWarned.get(key);
        if (last == null || last.isBefore(LocalDateTime.now().minusMinutes(intervalMin))) {
            lastWarned.put(key, LocalDateTime.now());
            return true;
        }
        return false;
    }

    public void notifyAutoWater(String macId, String action, String username) {
        Iot device = iotRepo.findBymacId(macId).orElse(null);
        if (device == null) return;

        AlertRule rule = alertRuleRepo.findByMacIdAndUsername(macId, username).orElse(null);
        if (rule == null || !rule.isNotifyOnAutoWater()) return;

        String emoji = "ON".equals(action) ? "💦" : "✅";
        sendTelegram(emoji + " <b>Tưới tự động</b>\n"
                + device.getName() + " → " + action);
    }
}