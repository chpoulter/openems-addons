package de.poulter.openems.edge.controller.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.mqttv5.client.IMqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

// /daten/Projekte/openems/openems/io.openems.edge.controller.api.mqtt/src/io/openems/edge/controller/api/mqtt/

@Designate(ocd = Config.class, factory = true)
@Component(
    name = "Controller.Api.WritableMQTT",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class MqttImpl extends AbstractOpenemsComponent implements Mqtt, Controller, OpenemsComponent {

    private static final Logger log = LoggerFactory.getLogger(MqttImpl.class);

    private MqttLifecycleManager mqttLifecycleManager;
    
    @Reference
    private ConfigurationAdmin cm;

    @Reference
    private ComponentManager componentManager;

    public MqttImpl() {
        super(
            OpenemsComponent.ChannelId.values(),
            Controller.ChannelId.values(),
            Mqtt.ChannelId.values()
        );
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsException {
        log.info("Mqtt.activate");

        super.activate(context, config.id(), config.alias(), config.enabled());

        if (this.isEnabled()) {
            MqttConnectionOptions options = new MqttConnectionOptions();
            options.setUserName(config.username());
            if (config.password() != null && !config.password().isBlank()) {
                options.setPassword(config.password().getBytes(StandardCharsets.UTF_8));
            }
            options.setAutomaticReconnect(true);
            options.setCleanStart(true);
            options.setConnectionTimeout(10);

//            if (certPem != null && !certPem.isBlank() //
//                    && privateKeyPem != null && !privateKeyPem.isBlank() //
//                    && trustStorePem != null && !trustStorePem.isBlank()) {
//                options.setSocketFactory(createSslSocketFactory(certPem, privateKeyPem, trustStorePem));
//            }
            
            
            try {
                mqttLifecycleManager = new MqttLifecycleManager(config.uri(), config.clientId(), options, (IMqttClient mqttClient) -> {
                    String topicName = config.topicPrefix() + "/" + "edge/" + config.clientId() + "/channel/+/+";
                    log.info("Subscribing to " + topicName);

                    try {
                        mqttClient.subscribe(topicName, 1, (topic, msg) -> this.handleIncomingMessage(topic, msg));
                    } catch (MqttException ex) {
                        log.error("Could not subscribe to mqtt channels", ex);
                    }
                });
                mqttLifecycleManager.start();

            } catch (MqttException ex) {
                log.error("Could not initialize mqtt client", ex);

                throw new OpenemsException("Could not initialize mqtt client.", ex);
            }
        }
    }

    
    @Override
    @Deactivate
    protected void deactivate() {
        log.info("Mqtt.deactivate");

        super.deactivate();

        if (mqttLifecycleManager != null) {
            mqttLifecycleManager.shutdown();
            mqttLifecycleManager = null;
        }


    }

    private void handleIncomingMessage(String topic, MqttMessage message) {
        log.info("Message on " + topic);
        log.info(toJson(message));

//        try {
//            String[] parts = topic.split("/");
//            if (parts.length < 5) return;
//
//            String componentId = parts[3];
//            String channelId = parts[4];
//            String payloadValue = new String(message.getPayload());
//
//            // Safely fetch component channel and apply write value
//            WriteChannel<?> channel = (WriteChannel<?>) this.componentManager
//                .getComponent(componentId)
//                .channel(channelId);
//
//            //channel.setNextWriteValueFromObject(payloadValue);
//
//        } catch (Exception e) {
//            log.error("Error", e);
//        }
    }



    
    
    
    
    
    
    
    
    

    @Override
    public void run() throws OpenemsNamedException {
        // TODO Auto-generated method stub
        
    }
    
    
    
    
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    public static String toJson(MqttMessage message) {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("payload", new String(message.getPayload(), StandardCharsets.UTF_8));
        jsonMap.put("qos", message.getQos());
        jsonMap.put("retained", message.isRetained());
        jsonMap.put("duplicate", message.isDuplicate());

        if (message.getProperties() != null) {
            jsonMap.put("properties", message.getProperties());
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(jsonMap);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to convert MqttMessage to JSON", e);
        }
    }

    
    private static final Gson GSON = new com.google.gson.Gson();

    public static String toJson2(MqttMessage message) {

        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("payload", new String(message.getPayload(), StandardCharsets.UTF_8));
        jsonMap.put("qos", message.getQos());
        jsonMap.put("retained", message.isRetained());
        jsonMap.put("duplicate", message.isDuplicate());

        if (message.getProperties() != null) {
            jsonMap.put("properties", message.getProperties());
        }

        return GSON.toJson(jsonMap);
    }
}
