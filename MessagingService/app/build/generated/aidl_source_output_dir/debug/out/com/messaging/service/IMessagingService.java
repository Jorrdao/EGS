/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.messaging.service;
/**
 * Public IPC interface exposed to other Android applications.
 * Online methods use JSON payloads (via the cloud REST API).
 * Offline methods dispatch via BLE GATT mesh.
 */
public interface IMessagingService extends android.os.IInterface
{
  /** Default implementation for IMessagingService. */
  public static class Default implements com.messaging.service.IMessagingService
  {
    // ── Online ────────────────────────────────────────────────────────────────
    /** Send a message online. Returns a generated message_id. */
    @Override public java.lang.String sendMessageOnline(java.lang.String chatId, java.lang.String recipientId, java.lang.String content) throws android.os.RemoteException
    {
      return null;
    }
    /** Fetch chat history as a JSON string (array of message objects). */
    @Override public java.lang.String getChatHistory(java.lang.String chatId, int limit, int offset) throws android.os.RemoteException
    {
      return null;
    }
    // ── Offline (BLE) ─────────────────────────────────────────────────────────
    /** Broadcast this device's presence (lat/lng) via BLE advertising. */
    @Override public void broadcastPresence(double lat, double lng) throws android.os.RemoteException
    {
    }
    /**
     * Send a mesh message over BLE.
     * @param destinationId  Final recipient user_id (may require hops)
     * @param encryptedPayload  AES-256-GCM ciphertext (Base64)
     * @param ttl  Maximum hop count
     */
    @Override public void sendMeshMessage(java.lang.String destinationId, java.lang.String encryptedPayload, int ttl) throws android.os.RemoteException
    {
    }
    /** Request marketplace cache sync from nearby BLE devices. */
    @Override public void requestMarketplaceSync() throws android.os.RemoteException
    {
    }
    // ── Service control ───────────────────────────────────────────────────────
    /** Register a callback to receive incoming messages and KPI updates. */
    @Override public void registerCallback(com.messaging.service.IMessagingCallback callback) throws android.os.RemoteException
    {
    }
    @Override public void unregisterCallback(com.messaging.service.IMessagingCallback callback) throws android.os.RemoteException
    {
    }
    /** Returns a JSON snapshot of current KPI counters. */
    @Override public java.lang.String getKpiSnapshot() throws android.os.RemoteException
    {
      return null;
    }
    /** Returns true if the BLE GATT server is running. */
    @Override public boolean isBleRunning() throws android.os.RemoteException
    {
      return false;
    }
    /** Returns true if the embedded HTTP server is running. */
    @Override public boolean isHttpServerRunning() throws android.os.RemoteException
    {
      return false;
    }
    /** Returns the port the embedded HTTP server is listening on. */
    @Override public int getHttpServerPort() throws android.os.RemoteException
    {
      return 0;
    }
    /** Returns true if the online API is reachable. */
    @Override public boolean isOnlineAvailable() throws android.os.RemoteException
    {
      return false;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.messaging.service.IMessagingService
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.messaging.service.IMessagingService interface,
     * generating a proxy if needed.
     */
    public static com.messaging.service.IMessagingService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.messaging.service.IMessagingService))) {
        return ((com.messaging.service.IMessagingService)iin);
      }
      return new com.messaging.service.IMessagingService.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_sendMessageOnline:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          java.lang.String _result = this.sendMessageOnline(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_getChatHistory:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          int _arg1;
          _arg1 = data.readInt();
          int _arg2;
          _arg2 = data.readInt();
          java.lang.String _result = this.getChatHistory(_arg0, _arg1, _arg2);
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_broadcastPresence:
        {
          double _arg0;
          _arg0 = data.readDouble();
          double _arg1;
          _arg1 = data.readDouble();
          this.broadcastPresence(_arg0, _arg1);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_sendMeshMessage:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          int _arg2;
          _arg2 = data.readInt();
          this.sendMeshMessage(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_requestMarketplaceSync:
        {
          this.requestMarketplaceSync();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_registerCallback:
        {
          com.messaging.service.IMessagingCallback _arg0;
          _arg0 = com.messaging.service.IMessagingCallback.Stub.asInterface(data.readStrongBinder());
          this.registerCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_unregisterCallback:
        {
          com.messaging.service.IMessagingCallback _arg0;
          _arg0 = com.messaging.service.IMessagingCallback.Stub.asInterface(data.readStrongBinder());
          this.unregisterCallback(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_getKpiSnapshot:
        {
          java.lang.String _result = this.getKpiSnapshot();
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        case TRANSACTION_isBleRunning:
        {
          boolean _result = this.isBleRunning();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_isHttpServerRunning:
        {
          boolean _result = this.isHttpServerRunning();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        case TRANSACTION_getHttpServerPort:
        {
          int _result = this.getHttpServerPort();
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        case TRANSACTION_isOnlineAvailable:
        {
          boolean _result = this.isOnlineAvailable();
          reply.writeNoException();
          reply.writeInt(((_result)?(1):(0)));
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.messaging.service.IMessagingService
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      // ── Online ────────────────────────────────────────────────────────────────
      /** Send a message online. Returns a generated message_id. */
      @Override public java.lang.String sendMessageOnline(java.lang.String chatId, java.lang.String recipientId, java.lang.String content) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(chatId);
          _data.writeString(recipientId);
          _data.writeString(content);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendMessageOnline, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Fetch chat history as a JSON string (array of message objects). */
      @Override public java.lang.String getChatHistory(java.lang.String chatId, int limit, int offset) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(chatId);
          _data.writeInt(limit);
          _data.writeInt(offset);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getChatHistory, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      // ── Offline (BLE) ─────────────────────────────────────────────────────────
      /** Broadcast this device's presence (lat/lng) via BLE advertising. */
      @Override public void broadcastPresence(double lat, double lng) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeDouble(lat);
          _data.writeDouble(lng);
          boolean _status = mRemote.transact(Stub.TRANSACTION_broadcastPresence, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /**
       * Send a mesh message over BLE.
       * @param destinationId  Final recipient user_id (may require hops)
       * @param encryptedPayload  AES-256-GCM ciphertext (Base64)
       * @param ttl  Maximum hop count
       */
      @Override public void sendMeshMessage(java.lang.String destinationId, java.lang.String encryptedPayload, int ttl) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(destinationId);
          _data.writeString(encryptedPayload);
          _data.writeInt(ttl);
          boolean _status = mRemote.transact(Stub.TRANSACTION_sendMeshMessage, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Request marketplace cache sync from nearby BLE devices. */
      @Override public void requestMarketplaceSync() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_requestMarketplaceSync, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      // ── Service control ───────────────────────────────────────────────────────
      /** Register a callback to receive incoming messages and KPI updates. */
      @Override public void registerCallback(com.messaging.service.IMessagingCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerCallback, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void unregisterCallback(com.messaging.service.IMessagingCallback callback) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(callback);
          boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterCallback, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Returns a JSON snapshot of current KPI counters. */
      @Override public java.lang.String getKpiSnapshot() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getKpiSnapshot, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Returns true if the BLE GATT server is running. */
      @Override public boolean isBleRunning() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isBleRunning, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Returns true if the embedded HTTP server is running. */
      @Override public boolean isHttpServerRunning() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isHttpServerRunning, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Returns the port the embedded HTTP server is listening on. */
      @Override public int getHttpServerPort() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_getHttpServerPort, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
      /** Returns true if the online API is reachable. */
      @Override public boolean isOnlineAvailable() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        boolean _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_isOnlineAvailable, _data, _reply, 0);
          _reply.readException();
          _result = (0!=_reply.readInt());
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_sendMessageOnline = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_getChatHistory = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_broadcastPresence = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_sendMeshMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_requestMarketplaceSync = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_registerCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
    static final int TRANSACTION_unregisterCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
    static final int TRANSACTION_getKpiSnapshot = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
    static final int TRANSACTION_isBleRunning = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
    static final int TRANSACTION_isHttpServerRunning = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
    static final int TRANSACTION_getHttpServerPort = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
    static final int TRANSACTION_isOnlineAvailable = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
  }
  public static final java.lang.String DESCRIPTOR = "com.messaging.service.IMessagingService";
  // ── Online ────────────────────────────────────────────────────────────────
  /** Send a message online. Returns a generated message_id. */
  public java.lang.String sendMessageOnline(java.lang.String chatId, java.lang.String recipientId, java.lang.String content) throws android.os.RemoteException;
  /** Fetch chat history as a JSON string (array of message objects). */
  public java.lang.String getChatHistory(java.lang.String chatId, int limit, int offset) throws android.os.RemoteException;
  // ── Offline (BLE) ─────────────────────────────────────────────────────────
  /** Broadcast this device's presence (lat/lng) via BLE advertising. */
  public void broadcastPresence(double lat, double lng) throws android.os.RemoteException;
  /**
   * Send a mesh message over BLE.
   * @param destinationId  Final recipient user_id (may require hops)
   * @param encryptedPayload  AES-256-GCM ciphertext (Base64)
   * @param ttl  Maximum hop count
   */
  public void sendMeshMessage(java.lang.String destinationId, java.lang.String encryptedPayload, int ttl) throws android.os.RemoteException;
  /** Request marketplace cache sync from nearby BLE devices. */
  public void requestMarketplaceSync() throws android.os.RemoteException;
  // ── Service control ───────────────────────────────────────────────────────
  /** Register a callback to receive incoming messages and KPI updates. */
  public void registerCallback(com.messaging.service.IMessagingCallback callback) throws android.os.RemoteException;
  public void unregisterCallback(com.messaging.service.IMessagingCallback callback) throws android.os.RemoteException;
  /** Returns a JSON snapshot of current KPI counters. */
  public java.lang.String getKpiSnapshot() throws android.os.RemoteException;
  /** Returns true if the BLE GATT server is running. */
  public boolean isBleRunning() throws android.os.RemoteException;
  /** Returns true if the embedded HTTP server is running. */
  public boolean isHttpServerRunning() throws android.os.RemoteException;
  /** Returns the port the embedded HTTP server is listening on. */
  public int getHttpServerPort() throws android.os.RemoteException;
  /** Returns true if the online API is reachable. */
  public boolean isOnlineAvailable() throws android.os.RemoteException;
}
