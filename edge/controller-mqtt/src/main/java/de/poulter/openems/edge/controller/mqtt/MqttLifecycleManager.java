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

    private static final Logger log = Logger.getLogger(MqttLifecycleManager.class.getName());

    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    private final MqttConnectionOptions connectionOptions;
    private final Consumer<IMqttClient> onConnected;

    private final IMqttClient client;

    public MqttLifecycleManager(
        String serverURI,
        String clientId,
        MqttConnectionOptions connectionOptions,
        Consumer<IMqttClient> onConnected
    ) throws MqttException {
        this.onConnected = onConnected;
        this.connectionOptions = connectionOptions;

        this.client = new MqttClient(serverURI, clientId);
        this.client.setCallback(new MqttLifecycleManagerCallback());
    }

    public void start() {
        isShuttingDown.set(false);
        CompletableFuture.runAsync(() -> attemptConnection());
    }
    
    private void attemptConnection() {
        if (client.isConnected() || isShuttingDown.get()) {
            return;
        }

        log.info("Attempting to connect to MQTT broker...");
        CompletableFuture.runAsync(() -> {
            try {
                client.connect(connectionOptions);
                log.info("Successfully connected to the MQTT broker.");
                if (onConnected != null) {
                    onConnected.accept(client);
                }
            } catch (MqttException e) {
                log.log(Level.WARNING, "Connection attempt failed. Retrying in 5 seconds...", e);
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
            log.info("MQTT client successfully shut down.");
        } catch (MqttException e) {
            log.log(Level.SEVERE, "Error occurred while shutting down MQTT client", e);
        }
    }
    
    
    
    
    
    
    
    private class MqttLifecycleManagerCallback implements MqttCallback {

        @Override
        public void disconnected(MqttDisconnectResponse disconnectResponse) {
            if (isShuttingDown.get()) {
                return;
            }

            String reason = disconnectResponse != null ? disconnectResponse.getReasonString() : "Unknown";
            log.warning("Disconnected from MQTT broker: " + reason);

            if (!connectionOptions.isAutomaticReconnect() && !client.isConnected()) {
                start();
            }
        }

        @Override
        public void mqttErrorOccurred(MqttException exception) {
            if (isShuttingDown.get()) {
                return;
            }
            
            log.log(Level.SEVERE, "An MQTT error occurred: " + exception.getMessage(), exception);
            
            if (!client.isConnected()) {
                triggerSafeReconnect();
            }
        }

        private void triggerSafeReconnect() {
            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                if (!client.isConnected() && !isShuttingDown.get()) {
                    log.info("Triggering recovery reconnect attempt due to error...");
                    start();
                }
            });
        }
    
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            log.info("Connect complete. Reconnect: " + reconnect + ", Server URI: " + serverURI);
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
        }

    }
}