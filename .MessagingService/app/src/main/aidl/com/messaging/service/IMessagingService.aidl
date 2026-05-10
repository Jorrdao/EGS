// IMessagingService.aidl
// Other applications must declare the BIND_MESSAGING permission to use this.
package com.messaging.service;

import com.messaging.service.IMessagingCallback;

/**
 * Public IPC interface exposed to other Android applications.
 * Online methods use JSON payloads (via the cloud REST API).
 * Offline methods dispatch via BLE GATT mesh.
 */
interface IMessagingService {

    // ── Online ────────────────────────────────────────────────────────────────

    /** Send a message online. Returns a generated message_id. */
    String sendMessageOnline(String chatId, String recipientId, String content);

    /** Fetch chat history as a JSON string (array of message objects). */
    String getChatHistory(String chatId, int limit, int offset);

    // ── Offline (BLE) ─────────────────────────────────────────────────────────

    /** Broadcast this device's presence (lat/lng) via BLE advertising. */
    void broadcastPresence(double lat, double lng);

    /**
     * Send a mesh message over BLE.
     * @param destinationId  Final recipient user_id (may require hops)
     * @param encryptedPayload  AES-256-GCM ciphertext (Base64)
     * @param ttl  Maximum hop count
     */
    void sendMeshMessage(String destinationId, String encryptedPayload, int ttl);

    /** Request marketplace cache sync from nearby BLE devices. */
    void requestMarketplaceSync();

    // ── Service control ───────────────────────────────────────────────────────

    /** Register a callback to receive incoming messages and KPI updates. */
    void registerCallback(IMessagingCallback callback);
    void unregisterCallback(IMessagingCallback callback);

    /** Returns a JSON snapshot of current KPI counters. */
    String getKpiSnapshot();

    /** Returns true if the BLE GATT server is running. */
    boolean isBleRunning();

    /** Returns true if the embedded HTTP server is running. */
    boolean isHttpServerRunning();

    /** Returns the port the embedded HTTP server is listening on. */
    int getHttpServerPort();

    /** Returns true if the online API is reachable. */
    boolean isOnlineAvailable();
}
