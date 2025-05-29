package com.andeptrai.iot.websocket;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class test1 extends TextWebSocketHandler {
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("🔌 Kết nối WebSocket mới: " + session.getId());
        session.sendMessage(new TextMessage("Xin chào từ server WebSocket!"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("📩 Nhận tin nhắn: " + message.getPayload());
        session.sendMessage(new TextMessage("Server nhận: " + message.getPayload()));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("❌ Lỗi WebSocket: " + exception.getMessage());
    }
}
