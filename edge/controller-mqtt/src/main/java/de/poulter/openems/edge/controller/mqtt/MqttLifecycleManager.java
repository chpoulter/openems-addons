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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MqttLifecycleManager {

    private static final Logger LOGGER = Logger.getLogger(MqttLifecycleManager.class.getName());

    private final IMqttClient client;
    private final MqttConnectionOptions connectionOptions;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    private Consumer<IMqttClient> onConnected;

    public MqttLifecycleManager(
        String serverURI,
        String clientId,
        MqttConnectionOptions connectionOptions,
        Consumer<IMqttClient> onConnected
    ) {
        this.onConnected = onConnected;
        
        try {
            this.client = new MqttClient(serverURI, clientId);
            this.client.setCallback(new XXX());

        } catch (MqttException e) {
            throw new RuntimeException("Failed to initialize MQTT client", e);
        }
        this.connectionOptions = connectionOptions;
    }

    public void start() {
        isShuttingDown.set(false);
        CompletableFuture.runAsync(() -> attemptConnection());
    }
    
    private void attemptConnection() {
        if (client.isConnected() || isShuttingDown.get()) {
            return;
        }

        LOGGER.info("Attempting to connect to MQTT broker...");
        CompletableFuture.runAsync(() -> {
            try {
                client.connect(connectionOptions);
                LOGGER.info("Successfully connected to the MQTT broker.");
                if (onConnected != null) {
                    onConnected.accept(client);
                }
            } catch (MqttException e) {
                LOGGER.log(Level.WARNING, "Connection attempt failed. Retrying in 5 seconds...", e);
                if (!isShuttingDown.get()) {
                    CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> attemptConnection());
                }
            }
        });
    }


    public void shutdown() {
        isShuttingDown.set(true);
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
            LOGGER.info("MQTT client successfully shut down.");
        } catch (MqttException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while shutting down MQTT client", e);
        }
    }
    
    
    
    
    
    
    
    private class XXX implements MqttCallback {
        @Override
        public void disconnected(MqttDisconnectResponse disconnectResponse) {
            if (isShuttingDown.get()) {
                return;
            }
            
            String reason = disconnectResponse != null ? disconnectResponse.getReasonString() : "Unknown";
            LOGGER.warning("Disconnected from MQTT broker: " + reason);
            
            if (!connectionOptions.isAutomaticReconnect() && !client.isConnected()) {
                start();
            }
        }

        @Override
        public void mqttErrorOccurred(MqttException exception) {
            if (isShuttingDown.get()) {
                return;
            }
            
            LOGGER.log(Level.SEVERE, "An MQTT error occurred: " + exception.getMessage(), exception);
            
            if (!client.isConnected()) {
                triggerSafeReconnect();
            }
        }

        private void triggerSafeReconnect() {
            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                if (!client.isConnected() && !isShuttingDown.get()) {
                    LOGGER.info("Triggering recovery reconnect attempt due to error...");
                    start();
                }
            });
        }
    
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            LOGGER.info("Connect complete. Reconnect: " + reconnect + ", Server URI: " + serverURI);
        }
    
        @Override
        public void authPacketArrived(int reasonCode, MqttProperties properties) {
            LOGGER.info("Auth packet arrived with reason code: " + reasonCode);
        }
    
        @Override
        public void deliveryComplete(IMqttToken token) {
            LOGGER.info("Delivery complete, token: " + token);
        }
    
        @Override
        public void messageArrived(String topic, MqttMessage message) {
            LOGGER.info("Message arrived on topic " + topic + ".");
        }

    }
}