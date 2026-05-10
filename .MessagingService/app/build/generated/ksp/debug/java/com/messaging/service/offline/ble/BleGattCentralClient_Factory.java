package com.messaging.service.offline.ble;

import android.content.Context;
import com.messaging.service.kpi.KpiTracker;
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
public final class BleGattCentralClient_Factory implements Factory<BleGattCentralClient> {
  private final Provider<Context> contextProvider;

  private final Provider<KpiTracker> kpiTrackerProvider;

  public BleGattCentralClient_Factory(Provider<Context> contextProvider,
      Provider<KpiTracker> kpiTrackerProvider) {
    this.contextProvider = contextProvider;
    this.kpiTrackerProvider = kpiTrackerProvider;
  }

  @Override
  public BleGattCentralClient get() {
    return newInstance(contextProvider.get(), kpiTrackerProvider.get());
  }

  public static BleGattCentralClient_Factory create(Provider<Context> contextProvider,
      Provider<KpiTracker> kpiTrackerProvider) {
    return new BleGattCentralClient_Factory(contextProvider, kpiTrackerProvider);
  }

  public static BleGattCentralClient newInstance(Context context, KpiTracker kpiTracker) {
    return new BleGattCentralClient(context, kpiTracker);
  }
}
