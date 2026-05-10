// IMessagingCallback.aidl
package com.messaging.service;

/**
 * Callback pushed back to the caller application.
 * Implement this in the client app and register via IMessagingService.
 */
oneway interface IMessagingCallback {

    /** Called when a new message arrives (online or BLE). */
    void onMessageReceived(String messageJson);

    /** Called when a nearby device is discovered via BLE scan. */
    void onPeerDiscovered(String userId, double lat, double lng);

    /** KPI metric update (name + value pair). */
    void onKpiUpdate(String metricName, long value);

    /** Service status change event. */
    void onStatusChanged(String status);
}
