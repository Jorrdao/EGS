package com.messaging.service.offline.repository;

import android.bluetooth.BluetoothAdapter;
import com.messaging.service.db.MessageDao;
import com.messaging.service.kpi.KpiTracker;
import com.messaging.service.offline.ble.BleGattCentralClient;
import com.messaging.service.offline.ble.BleScanner;
import com.messaging.service.offline.crypto.MessageEncryption;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class OfflineMessagingRepository_Factory implements Factory<OfflineMessagingRepository> {
  private final Provider<BleScanner> bleScannerProvider;

  private final Provider<BleGattCentralClient> gattClientProvider;

  private final Provider<MessageEncryption> encryptionProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<KpiTracker> kpiTrackerProvider;

  private final Provider<BluetoothAdapter> bluetoothAdapterProvider;

  public OfflineMessagingRepository_Factory(Provider<BleScanner> bleScannerProvider,
      Provider<BleGattCentralClient> gattClientProvider,
      Provider<MessageEncryption> encryptionProvider, Provider<MessageDao> messageDaoProvider,
      Provider<KpiTracker> kpiTrackerProvider,
      Provider<BluetoothAdapter> bluetoothAdapterProvider) {
    this.bleScannerProvider = bleScannerProvider;
    this.gattClientProvider = gattClientProvider;
    this.encryptionProvider = encryptionProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.kpiTrackerProvider = kpiTrackerProvider;
    this.bluetoothAdapterProvider = bluetoothAdapterProvider;
  }

  @Override
  public OfflineMessagingRepository get() {
    return newInstance(bleScannerProvider.get(), gattClientProvider.get(), encryptionProvider.get(), messageDaoProvider.get(), kpiTrackerProvider.get(), bluetoothAdapterProvider.get());
  }

  public static OfflineMessagingRepository_Factory create(Provider<BleScanner> bleScannerProvider,
      Provider<BleGattCentralClient> gattClientProvider,
      Provider<MessageEncryption> encryptionProvider, Provider<MessageDao> messageDaoProvider,
      Provider<KpiTracker> kpiTrackerProvider,
      Provider<BluetoothAdapter> bluetoothAdapterProvider) {
    return new OfflineMessagingRepository_Factory(bleScannerProvider, gattClientProvider, encryptionProvider, messageDaoProvider, kpiTrackerProvider, bluetoothAdapterProvider);
  }

  public static OfflineMessagingRepository newInstance(BleScanner bleScanner,
      BleGattCentralClient gattClient, MessageEncryption encryption, MessageDao messageDao,
      KpiTracker kpiTracker, BluetoothAdapter bluetoothAdapter) {
    return new OfflineMessagingRepository(bleScanner, gattClient, encryption, messageDao, kpiTracker, bluetoothAdapter);
  }
}
