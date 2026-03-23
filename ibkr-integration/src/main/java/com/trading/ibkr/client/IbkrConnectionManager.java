package com.trading.ibkr.client;

import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.trading.ibkr.config.IbkrProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the connection lifecycle to IBKR TWS/Gateway.
 *
 * <p>Handles:
 * <ul>
 *   <li>Initial connection</li>
 *   <li>Reconnection on disconnect</li>
 *   <li>Connection state tracking</li>
 *   <li>Message reader thread management</li>
 * </ul>
 */
@Slf4j
@Component
public class IbkrConnectionManager {

    private final IbkrProperties properties;
    private final IbkrWrapper wrapper;
    private final EClientSocket clientSocket;
    private final EJavaSignal signal;

    @Getter
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;

    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private Thread readerThread;

    public IbkrConnectionManager(IbkrProperties properties) {
        this.properties = properties;
        this.signal = new EJavaSignal();
        this.wrapper = new IbkrWrapper();
        this.clientSocket = new EClientSocket(wrapper, signal);
    }

    @PostConstruct
    public void init() {
        log.info("Initializing IBKR Connection Manager");
        wrapper.setConnectionManager(this);
        connect().subscribe(
            success -> log.info("Initial connection established: {}", success),
            error -> log.error("Initial connection failed: {}", error.getMessage())
        );
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down IBKR Connection Manager");
        state = ConnectionState.SHUTTING_DOWN;
        disconnect();
    }

    /**
     * Initiates connection to IBKR Gateway.
     *
     * @return Mono emitting true on success, error on failure
     */
    public Mono<Boolean> connect() {
        if (state == ConnectionState.CONNECTED) {
            log.debug("Already connected");
            return Mono.just(true);
        }

        if (!connecting.compareAndSet(false, true)) {
            log.debug("Connection already in progress");
            return Mono.fromCallable(() -> waitForConnection())
                    .subscribeOn(Schedulers.boundedElastic());
        }

        return Mono.fromCallable(() -> {
                    state = ConnectionState.CONNECTING;
                    log.info("Connecting to IBKR Gateway at {}:{}", properties.getHost(), properties.getPort());

                    CompletableFuture<Boolean> connectionFuture = new CompletableFuture<>();
                    wrapper.setConnectionFuture(connectionFuture);

                    clientSocket.eConnect(properties.getHost(), properties.getPort(), properties.getClientId());
                    startReaderThread();

                    return connectionFuture.get(properties.getConnectionTimeoutSeconds(), TimeUnit.SECONDS);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(connected -> {
                    if (connected) {
                        state = ConnectionState.CONNECTED;
                        reconnectAttempts.set(0);
                        log.info("Successfully connected to IBKR Gateway");
                    } else {
                        state = ConnectionState.DISCONNECTED;
                        log.error("Connection to IBKR Gateway failed");
                    }
                    connecting.set(false);
                })
                .doOnError(error -> {
                    state = ConnectionState.DISCONNECTED;
                    connecting.set(false);
                    log.error("Connection error: {}", error.getMessage());
                });
    }

    /**
     * Disconnects from IBKR Gateway.
     */
    public void disconnect() {
        if (state == ConnectionState.DISCONNECTED) {
            return;
        }

        state = ConnectionState.DISCONNECTING;
        log.info("Disconnecting from IBKR Gateway");

        if (readerThread != null && readerThread.isAlive()) {
            readerThread.interrupt();
        }

        if (clientSocket.isConnected()) {
            clientSocket.eDisconnect();
        }

        state = ConnectionState.DISCONNECTED;
        log.info("Disconnected from IBKR Gateway");
    }

    /**
     * Returns the client socket for making API calls.
     */
    public EClientSocket getClientSocket() {
        if (state != ConnectionState.CONNECTED) {
            throw new IllegalStateException("Not connected to IBKR Gateway. Current state: " + state);
        }
        return clientSocket;
    }

    /**
     * Returns the wrapper for accessing callbacks and data.
     */
    public IbkrWrapper getWrapper() {
        return wrapper;
    }

    /**
     * Checks if currently connected.
     */
    public boolean isConnected() {
        return state == ConnectionState.CONNECTED && clientSocket.isConnected();
    }

    /**
     * Waits for connection to be established.
     *
     * @return true if connected, false otherwise
     */
    private boolean waitForConnection() throws InterruptedException {
        int attempts = 0;
        while (state == ConnectionState.CONNECTING && attempts < properties.getConnectionTimeoutSeconds() * 2) {
            Thread.sleep(500);
            attempts++;
        }
        return state == ConnectionState.CONNECTED;
    }

    private void startReaderThread() {
        readerThread = new Thread(() -> {
            EReader reader = new EReader(clientSocket, signal);
            reader.start();

            while (state != ConnectionState.SHUTTING_DOWN && state != ConnectionState.DISCONNECTED) {
                signal.waitForSignal();
                try {
                    reader.processMsgs();
                } catch (Exception e) {
                    if (state != ConnectionState.SHUTTING_DOWN) {
                        log.error("Error processing messages: {}", e.getMessage());
                    }
                }
            }
        }, "IBKR-Message-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Scheduled task to check connection health and reconnect if necessary.
     */
    @Scheduled(fixedDelay = 30000)
    public void healthCheck() {
        if (state == ConnectionState.SHUTTING_DOWN) {
            return;
        }

        if (!isConnected() && properties.isAutoReconnect()) {
            int attempts = reconnectAttempts.incrementAndGet();
            if (properties.getMaxReconnectAttempts() == 0 || attempts <= properties.getMaxReconnectAttempts()) {
                log.warn("Connection lost. Attempting reconnection {}/{}...",
                        attempts, properties.getMaxReconnectAttempts() == 0 ? "∞" : properties.getMaxReconnectAttempts());

                try {
                    Thread.sleep(properties.getReconnectDelaySeconds() * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                connect().subscribe(
                    success -> {
                        if (success) {
                            reconnectAttempts.set(0);
                            log.info("Reconnection successful");
                        }
                    },
                    error -> log.error("Reconnection failed: {}", error.getMessage())
                );
            } else {
                log.error("Max reconnection attempts ({}) reached. Manual intervention required.",
                        properties.getMaxReconnectAttempts());
            }
        }
    }

    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        SHUTTING_DOWN
    }
}
