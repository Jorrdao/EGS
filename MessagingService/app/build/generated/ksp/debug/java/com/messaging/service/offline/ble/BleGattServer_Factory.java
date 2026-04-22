package com.messaging.service.offline.ble;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import com.messaging.service.kpi.KpiTracker;
import com.messaging.service.offline.repository.OfflineMessagingRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class BleGattServer_Factory implements Factory<BleGattServer> {
  private final Provider<Context> contextProvider;

  private final Provider<BluetoothManager> bluetoothManagerProvider;

  private final Provider<OfflineMessagingRepository> offlineRepoProvider;

  private final Provider<KpiTracker> kpiTrackerProvider;

  public BleGattServer_Factory(Provider<Context> contextProvider,
      Provider<BluetoothManager> bluetoothManagerProvider,
      Provider<OfflineMessagingRepository> offlineRepoProvider,
      Provider<KpiTracker> kpiTrackerProvider) {
    this.contextProvider = contextProvider;
    this.bluetoothManagerProvider = bluetoothManagerProvider;
    this.offlineRepoProvider = offlineRepoProvider;
    this.kpiTrackerProvider = kpiTrackerProvider;
  }

  @Override
  public BleGattServer get() {
    return newInstance(contextProvider.get(), bluetoothManagerProvider.get(), offlineRepoProvider.get(), kpiTrackerProvider.get());
  }

  public static BleGattServer_Factory create(Provider<Context> contextProvider,
      Provider<BluetoothManager> bluetoothManagerProvider,
      Provider<OfflineMessagingRepository> offlineRepoProvider,
      Provider<KpiTracker> kpiTrackerProvider) {
    return new BleGattServer_Factory(contextProvider, bluetoothManagerProvider, offlineRepoProvider, kpiTrackerProvider);
  }

  public static BleGattServer newInstance(Context context, BluetoothManager bluetoothManager,
      OfflineMessagingRepository offlineRepo, KpiTracker kpiTracker) {
    return new BleGattServer(context, bluetoothManager, offlineRepo, kpiTracker);
  }
}
