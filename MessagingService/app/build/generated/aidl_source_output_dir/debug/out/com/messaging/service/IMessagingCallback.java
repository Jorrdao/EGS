/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.messaging.service;
/**
 * Callback pushed back to the caller application.
 * Implement this in the client app and register via IMessagingService.
 */
public interface IMessagingCallback extends android.os.IInterface
{
  /** Default implementation for IMessagingCallback. */
  public static class Default implements com.messaging.service.IMessagingCallback
  {
    /** Called when a new message arrives (online or BLE). */
    @Override public void onMessageReceived(java.lang.String messageJson) throws android.os.RemoteException
    {
    }
    /** Called when a nearby device is discovered via BLE scan. */
    @Override public void onPeerDiscovered(java.lang.String userId, double lat, double lng) throws android.os.RemoteException
    {
    }
    /** KPI metric update (name + value pair). */
    @Override public void onKpiUpdate(java.lang.String metricName, long value) throws android.os.RemoteException
    {
    }
    /** Service status change event. */
    @Override public void onStatusChanged(java.lang.String status) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.messaging.service.IMessagingCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.messaging.service.IMessagingCallback interface,
     * generating a proxy if needed.
     */
    public static com.messaging.service.IMessagingCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.messaging.service.IMessagingCallback))) {
        return ((com.messaging.service.IMessagingCallback)iin);
      }
      return new com.messaging.service.IMessagingCallback.Stub.Proxy(obj);
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
        case TRANSACTION_onMessageReceived:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.onMessageReceived(_arg0);
          break;
        }
        case TRANSACTION_onPeerDiscovered:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          double _arg1;
          _arg1 = data.readDouble();
          double _arg2;
          _arg2 = data.readDouble();
          this.onPeerDiscovered(_arg0, _arg1, _arg2);
          break;
        }
        case TRANSACTION_onKpiUpdate:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          long _arg1;
          _arg1 = data.readLong();
          this.onKpiUpdate(_arg0, _arg1);
          break;
        }
        case TRANSACTION_onStatusChanged:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.onStatusChanged(_arg0);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.messaging.service.IMessagingCallback
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
      /** Called when a new message arrives (online or BLE). */
      @Override public void onMessageReceived(java.lang.String messageJson) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(messageJson);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onMessageReceived, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Called when a nearby device is discovered via BLE scan. */
      @Override public void onPeerDiscovered(java.lang.String userId, double lat, double lng) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(userId);
          _data.writeDouble(lat);
          _data.writeDouble(lng);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onPeerDiscovered, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** KPI metric update (name + value pair). */
      @Override public void onKpiUpdate(java.lang.String metricName, long value) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(metricName);
          _data.writeLong(value);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onKpiUpdate, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      /** Service status change event. */
      @Override public void onStatusChanged(java.lang.String status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(status);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onStatusChanged, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onMessageReceived = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_onPeerDiscovered = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onKpiUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onStatusChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
  }
  public static final java.lang.String DESCRIPTOR = "com.messaging.service.IMessagingCallback";
  /** Called when a new message arrives (online or BLE). */
  public void onMessageReceived(java.lang.String messageJson) throws android.os.RemoteException;
  /** Called when a nearby device is discovered via BLE scan. */
  public void onPeerDiscovered(java.lang.String userId, double lat, double lng) throws android.os.RemoteException;
  /** KPI metric update (name + value pair). */
  public void onKpiUpdate(java.lang.String metricName, long value) throws android.os.RemoteException;
  /** Service status change event. */
  public void onStatusChanged(java.lang.String status) throws android.os.RemoteException;
}
