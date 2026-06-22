package com.smartgardenmini.config;

import com.smartgardenmini.service.MqttHandlerService;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.Mqttv5ClientManager;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaderMapper;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.topic.telemetry}")
    private String telemetryTopic;

    @Value("${mqtt.topic.status}")
    private String statusTopic;

    @Value("${mqtt.topic.ack}")
    private String ackTopic;

    @Bean
    public Mqttv5ClientManager mqttClientManager() {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setServerURIs(new String[]{brokerUrl});
        // Cho phep anonymous neu username/password trong
        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
        }
        if (password != null && !password.isEmpty()) {
            options.setPassword(password.getBytes());
        }
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);
        options.setSessionExpiryInterval(3600L);
        return new Mqttv5ClientManager(options, clientId);
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    public Mqttv5PahoMessageDrivenChannelAdapter mqttInbound() {
        Mqttv5PahoMessageDrivenChannelAdapter adapter =
                new Mqttv5PahoMessageDrivenChannelAdapter(
                        mqttClientManager(), "_backend_in_" + System.currentTimeMillis(),
                        telemetryTopic, statusTopic, ackTopic);

        MqttHeaderMapper headerMapper = new MqttHeaderMapper();
        headerMapper.setOutboundHeaderNames("mqtt_topic", "mqtt_receivedRetained");
        adapter.setHeaderMapper(headerMapper);

        adapter.setOutputChannel(mqttInputChannel());
        adapter.setQos(1);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttMessageHandler(MqttHandlerService handler) {
        return message -> {
            try {
                String topic = (String) message.getHeaders().get("mqtt_topic");
                String payload = new String((byte[]) message.getPayload());
                handler.handleMessage(topic, payload);
            } catch (Exception e) {
                log.error("Lỗi xử lý MQTT message", e);
            }
        };
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler mqttOutbound() {
        Mqttv5PahoMessageHandler handler = new Mqttv5PahoMessageHandler(mqttClientManager());
        handler.setAsync(true);
        handler.setDefaultRetained(false);
        handler.setDefaultQos(1);
        return handler;
    }
}