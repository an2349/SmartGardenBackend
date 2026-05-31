package com.smartgardenmini.config;

import com.smartgardenmini.websocket.LegacyWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket config giữ lại để tương thích ngược với ESP32 chạy firmware cũ.
 * Các thiết bị ESP32 cũ dùng WebSocket vẫn có thể kết nối được.
 * Thiết bị mới nên dùng MQTT.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final LegacyWebSocketHandler legacyHandler;

    public WebSocketConfig(LegacyWebSocketHandler legacyHandler) {
        this.legacyHandler = legacyHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(legacyHandler, "/ws/device", "/ws-device")
                .setAllowedOrigins("*");
    }
}