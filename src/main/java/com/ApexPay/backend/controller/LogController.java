package com.ApexPay.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ApexPay OS - System Diagnostic Log Broadcaster
 * Handles real-time streaming of system events to the Angular Terminal.
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    // CopyOnWriteArrayList is used for thread-safety during broadcast
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Endpoint for the Angular LogService to subscribe to.
     * Keeps the HTTP connection open for continuous data streaming.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs() {
        System.out.println(">> TERMINAL_HANDSHAKE_INITIATED: New Admin Connected.");

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        this.emitters.add(emitter);

        emitter.onCompletion(() -> {
            System.out.println("<< TERMINAL_CONNECTION_CLOSED");
            this.emitters.remove(emitter);
        });

        // Send an immediate "Heartbeat" to confirm the connection is alive
        try {
            Map<String, String> bootLog = new HashMap<>();
            bootLog.put("category", "SECURITY");
            bootLog.put("event", "REMOTE_TERMINAL_LINK_ESTABLISHED");
            bootLog.put("status", "SUCCESS");
            bootLog.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            emitter.send(bootLog, MediaType.APPLICATION_JSON);
        } catch (IOException e) {
            this.emitters.remove(emitter);
        }

        return emitter;
    }
    /**
     * Broadcasts a new system event to all connected administrators.
     * Call this from your PayrollService or AI Oracle Logic.
     */
    public void broadcastLog(String category, String event, String status) {
        Map<String, String> logPayload = new HashMap<>();
        logPayload.put("category", category);
        logPayload.put("event", event);
        logPayload.put("status", status);
        logPayload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        for (SseEmitter emitter : emitters) {
            try {
                // Pushes the data packet to the Angular LogService
                emitter.send(logPayload, MediaType.APPLICATION_JSON);
            } catch (IOException e) {
                // If a client is disconnected, remove them from the list
                emitters.remove(emitter);
            }
        }
    }
}