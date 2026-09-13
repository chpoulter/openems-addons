package de.poulter.openems.edge.controller.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.mqttv5.client.IMqttClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
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

import de.poulter.openems.lib.utils.ObjectDumper;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.jsonrpc.type.UpdateComponentConfig.Request;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.Controller;

///daten/Projekte/openems/openems/io.openems.edge.controller.api.mqtt/src/io/openems/edge/controller/api/mqtt/

@Designate(ocd = Config.class, factory = true)
@Component(
    name = "Controller.Api.WritableMQTT",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class MqttImpl extends AbstractOpenemsComponent implements Mqtt, Controller, OpenemsComponent {

    private static final Logger log = LoggerFactory.getLogger(MqttImpl.class);

    @Reference
    private ConfigurationAdmin cm;

    @Reference
    private ComponentManager componentManager;

    private MqttLifecycleManager mqttLifecycleManager;
    private Config config;

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

        this.config = config;

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
                mqttLifecycleManager = new MqttLifecycleManager(
                    config.uri(),
                    config.clientId(),
                    options,
                    (IMqttClient mqttClient) -> onConnect(mqttClient),
                    (IMqttClient mqttClient, String topic, MqttMessage message) -> onMessage(mqttClient, topic, message)
                );
                mqttLifecycleManager.start();

            } catch (Exception ex) {
                log.error("Could not initialize mqtt client", ex);

                throw new OpenemsException("Could not initialize mqtt client.", ex);
            }
        }
    }

    private void onConnect(IMqttClient mqttClient) {
        String topicName = config.topicPrefix() + "/edge/" + config.edgeId() + "/command/#";
        log.info("Subscribing to mqtt topic " + topicName);

        try {
            IMqttToken token = mqttClient.subscribe(topicName, 0);

            token.waitForCompletion(15000);

            log.info("Mqtt subscription completed: " + token.isComplete());

        } catch (Exception ex) {
            log.error("Could not subscribe to mqtt channels.", ex);
        }
    }

    private void onMessage(IMqttClient mqttClient, String topic, MqttMessage message) {

        try {
            log.info("Received message on topic: " + topic);
            log.info(ObjectDumper.objectToJsonString(message));

            String[] parts = topic.split("/");
            if (parts.length < 6) return;

            String componentId = parts[4];
            String channelId = parts[5];
            String propertyId = parts[5];
            String payloadValue = new String(message.getPayload());

            if (propertyId.startsWith("_Property")) {
                log.info("Setting " + payloadValue + " on component " + componentId + ", property: " + propertyId + ".");

                String key = propertyId.substring(9);

                Property property = new Property(key, payloadValue);

                List<Property> properties = new ArrayList<>();
                properties.add(property);

                User user = null;
                Request request = new Request(componentId, properties);

                log.info("Running update configuration request: " + ObjectDumper.objectToJsonString(request));
                componentManager.handleUpdateComponentConfigRequest(user, request);

            } else {
                log.info("Setting " + payloadValue + " on component " + componentId + ", channel: " + channelId + ".");

                WriteChannel<?> channel = (WriteChannel<?>) this.componentManager.getComponent(componentId).channel(channelId);

                log.info("Updating channel: " + channel.address().toString());
                channel.setNextWriteValueFromObject(payloadValue);
            }

        } catch (Exception ex) {
            log.error("Could not update value from topic " + topic + ".", ex);
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

        config = null;
    }

    @Override
    public void run() throws OpenemsNamedException {
    }
}
