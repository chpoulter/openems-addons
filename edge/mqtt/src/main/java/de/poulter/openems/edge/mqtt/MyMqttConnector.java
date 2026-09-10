package de.poulter.openems.edge.mqtt;

import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;



import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.paho.mqttv5.client.IMqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.controller.api.mqtt.MqttConnector;

public class MyMqttConnector extends MqttConnector {

    private static final Logger log = LoggerFactory.getLogger(MyMqttConnector.class);
    
    private static final long INITIAL_RECONNECT_DELAY_SECONDS = 5;
    private static final long MAX_RECONNECT_DELAY_SECONDS = 300;
    private static final double RECONNECT_DELAY_MULTIPLIER = 1.5;
    
    private volatile ScheduledFuture<?> reconnectFuture = null;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger reconnectionAttempt = new AtomicInteger(0);    
    
    private IMqttClient mqttClient = null;
    private final Config config;

    private Consumer<IMqttClient> connectedConsumer;
    
    public MyMqttConnector(
        Config config,
        Consumer<IMqttClient> connectedConsumer
    ) {
        this.config = config;
        this.connectedConsumer = connectedConsumer;
    }
    
    public synchronized CompletableFuture<IMqttClient> connect(
        String serverUri, String clientId, String username,
        String password, String certPem, String privateKeyPem, String trustStorePem
    ) throws IllegalArgumentException, MqttException {
        return this.connect(serverUri, clientId, username, password, certPem, privateKeyPem, trustStorePem, null);
    }
    
    
    
    
    
    
    
    
    
    public synchronized void scheduleReconnect() {
        if (this.reconnectFuture != null && !this.reconnectFuture.isDone()) {
            this.reconnectFuture.cancel(false);
        }

        this.attemptConnect();
    }

    private void attemptConnect() {
        if (this.mqttClient != null && this.mqttClient.isConnected()) {
            return; // Already connected
        }

        try {
            connect(
                config.uri(),
                config.clientId(),
                config.username(), config.password(),
                //this.config.certPem(), this.config.privateKeyPem(), this.config.trustStorePem()
                null, null, null

            ).thenAccept(client -> {
                log.info("Connected to MQTT Broker [" + config.uri() + "]");

                mqttClient = client;
                reconnectionAttempt.set(0);
                
                connectedConsumer.accept(mqttClient);

            }).exceptionally(ex -> {
                log.error("Failed to connect to MQTT broker: " + ex.getMessage(), ex);
                this.scheduleNextAttempt(); // Schedule the next attempt with an increased delay.
                return null;
            });

        } catch (Exception ex) {
            log.error("Error attempting to connect to MQTT broker", ex);
            this.scheduleNextAttempt();
        }
    }

    private void scheduleNextAttempt() {
        long delay = this.calculateNextDelay();
        // Ensure the executor service is not shut down
        if (!this.scheduledExecutorService.isShutdown()) {
            this.reconnectFuture = this.scheduledExecutorService.schedule(this::attemptConnect, delay, TimeUnit.SECONDS);
        }
    }

    private long calculateNextDelay() {
        long delay = (long) (INITIAL_RECONNECT_DELAY_SECONDS
                * Math.pow(RECONNECT_DELAY_MULTIPLIER, this.reconnectionAttempt.getAndIncrement()));
        delay = Math.min(delay, MAX_RECONNECT_DELAY_SECONDS); // Ensure delay does not exceed maximum
        return delay;
    }
    
    public void shutdown() {
        shutdownAndAwaitTermination(this.scheduledExecutorService, 0);
        
        if (mqttClient != null) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                mqttClient = null;
            } catch (MqttException ex) {
                log.warn("Unable to close connection to MQTT broker: ", ex);
            }
        }
    }
}
