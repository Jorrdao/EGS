package com.messaging.service.offline.ble;

import android.bluetooth.BluetoothAdapter;
import com.messaging.service.kpi.KpiTracker;
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
public final class BleScanner_Factory implements Factory<BleScanner> {
  private final Provider<BluetoothAdapter> bluetoothAdapterProvider;

  private final Provider<KpiTracker> kpiTrackerProvider;

  public BleScanner_Factory(Provider<BluetoothAdapter> bluetoothAdapterProvider,
      Provider<KpiTracker> kpiTrackerProvider) {
    this.bluetoothAdapterProvider = bluetoothAdapterProvider;
    this.kpiTrackerProvider = kpiTrackerProvider;
  }

  @Override
  public BleScanner get() {
    return newInstance(bluetoothAdapterProvider.get(), kpiTrackerProvider.get());
  }

  public static BleScanner_Factory create(Provider<BluetoothAdapter> bluetoothAdapterProvider,
      Provider<KpiTracker> kpiTrackerProvider) {
    return new BleScanner_Factory(bluetoothAdapterProvider, kpiTrackerProvider);
  }

  public static BleScanner newInstance(BluetoothAdapter bluetoothAdapter, KpiTracker kpiTracker) {
    return new BleScanner(bluetoothAdapter, kpiTracker);
  }
}
