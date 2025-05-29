package com.andeptrai.iot.config;

import com.andeptrai.iot.websocket.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
   private final WebHander webHander;

    public WebSocketConfig(WebHander webHander) {
        this.webHander = webHander;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webHander, "/ws-device").setAllowedOrigins("*");
    }
}

