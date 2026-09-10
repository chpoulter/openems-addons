package de.poulter.openems.edge.controller.mqtt;

import org.eclipse.paho.mqttv5.client.IMqttClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import io.openems.common.function.TriConsumer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttLifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(MqttLifecycleManager.class);

    private final AtomicBoolean isShuttingDown;
    private final MqttConnectionOptions connectionOptions;
    private final Consumer<IMqttClient> onConnected;
    private final TriConsumer<IMqttClient, String, MqttMessage> onMessage;

    private final IMqttClient client;

    public MqttLifecycleManager(
        String serverURI,
        String clientId,
        MqttConnectionOptions connectionOptions,
        Consumer<IMqttClient> onConnected,
        TriConsumer<IMqttClient, String, MqttMessage> onMessage
    ) throws MqttException {
        this.onConnected = onConnected;
        this.onMessage = onMessage;
        this.connectionOptions = connectionOptions;

        client = new MqttClient(serverURI, clientId);
        client.setCallback(new MqttLifecycleManagerCallback());

        isShuttingDown = new AtomicBoolean(false);
    }

    public void start() {
        log.info("Starting to attempt mqtt connections.");

        isShuttingDown.set(false);
        CompletableFuture.runAsync(() -> attemptConnection());
    }

    private void attemptConnection() {
        if (client.isConnected() || isShuttingDown.get()) {
            return;
        }

        log.info("Attempting to connect to MQTT broker.");

        CompletableFuture.runAsync(() -> {
            try {
                client.connect(connectionOptions);
                log.info("Successfully connected to the MQTT broker: " + client.isConnected());

            } catch (MqttException ex) {
                log.warn("Connection attempt failed. Retrying in 5 seconds.", ex);

                if (!isShuttingDown.get()) {
                    CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> attemptConnection());
                }
            }
        });
    }


    public void shutdown() {
        log.info("Shutting mqtt down.");

        isShuttingDown.set(true);
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
            log.info("Mqtt client successfully shut down.");

        } catch (MqttException ex) {
            log.error("Error occurred while shutting down MQTT client.", ex);
        }
    }

    private class MqttLifecycleManagerCallback implements MqttCallback {

        @Override
        public void disconnected(MqttDisconnectResponse disconnectResponse) {
            if (isShuttingDown.get()) {
                return;
            }

            String reason = disconnectResponse != null ? disconnectResponse.getReasonString() : "Unknown";
            log.warn("Disconnected from MQTT broker: " + reason);

            if (!connectionOptions.isAutomaticReconnect() && !client.isConnected()) {
                start();
            }
        }

        @Override
        public void mqttErrorOccurred(MqttException exception) {
            if (isShuttingDown.get()) {
                return;
            }

            log.error("An MQTT error occurred.", exception);

            if (!client.isConnected()) {
                triggerSafeReconnect();
            }
        }

        private void triggerSafeReconnect() {
            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {

                if (!client.isConnected() && !isShuttingDown.get()) {
                    log.info("Triggering recovery reconnect attempt due to error.");
                    start();
                }
            });
        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            log.info("Connect complete. Reconnect: " + reconnect + ", Server URI: " + serverURI);

            if (onConnected != null) {
                onConnected.accept(client);
            }
        }

        @Override
        public void authPacketArrived(int reasonCode, MqttProperties properties) {
            log.info("Auth packet arrived with reason code: " + reasonCode);
        }

        @Override
        public void deliveryComplete(IMqttToken token) {
            log.info("Delivery complete, token: " + token);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            log.info("Message arrived on topic " + topic + ".");

            if (onMessage != null) {
                onMessage.accept(client, topic, message);
            }
        }

    }
}