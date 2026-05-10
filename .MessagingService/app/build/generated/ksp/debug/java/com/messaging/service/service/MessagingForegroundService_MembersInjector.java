package com.messaging.service.service;

import android.bluetooth.BluetoothManager;
import com.messaging.service.kpi.KpiTracker;
import com.messaging.service.offline.ble.BleAdvertiser;
import com.messaging.service.offline.ble.BleGattServer;
import com.messaging.service.offline.ble.BleScanner;
import com.messaging.service.offline.repository.OfflineMessagingRepository;
import com.messaging.service.online.api.EmbeddedHttpServer;
import com.messaging.service.online.repository.OnlineMessagingRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MessagingForegroundService_MembersInjector implements MembersInjector<MessagingForegroundService> {
  private final Provider<OnlineMessagingRepository> onlineRepoProvider;

  private final Provider<OfflineMessagingRepository> offlineRepoProvider;

  private final Provider<BleScanner> bleScannerProvider;

  private final Provider<BleAdvertiser> bleAdvertiserProvider;

  private final Provider<BleGattServer> gattServerProvider;

  private final Provider<KpiTracker> kpiTrackerProvider;

  private final Provider<BluetoothManager> bluetoothManagerProvider;

  private final Provider<EmbeddedHttpServer> httpServerProvider;

  public MessagingForegroundService_MembersInjector(
      Provider<OnlineMessagingRepository> onlineRepoProvider,
      Provider<OfflineMessagingRepository> offlineRepoProvider,
      Provider<BleScanner> bleScannerProvider, Provider<BleAdvertiser> bleAdvertiserProvider,
      Provider<BleGattServer> gattServerProvider, Provider<KpiTracker> kpiTrackerProvider,
      Provider<BluetoothManager> bluetoothManagerProvider,
      Provider<EmbeddedHttpServer> httpServerProvider) {
    this.onlineRepoProvider = onlineRepoProvider;
    this.offlineRepoProvider = offlineRepoProvider;
    this.bleScannerProvider = bleScannerProvider;
    this.bleAdvertiserProvider = bleAdvertiserProvider;
    this.gattServerProvider = gattServerProvider;
    this.kpiTrackerProvider = kpiTrackerProvider;
    this.bluetoothManagerProvider = bluetoothManagerProvider;
    this.httpServerProvider = httpServerProvider;
  }

  public static MembersInjector<MessagingForegroundService> create(
      Provider<OnlineMessagingRepository> onlineRepoProvider,
      Provider<OfflineMessagingRepository> offlineRepoProvider,
      Provider<BleScanner> bleScannerProvider, Provider<BleAdvertiser> bleAdvertiserProvider,
      Provider<BleGattServer> gattServerProvider, Provider<KpiTracker> kpiTrackerProvider,
      Provider<BluetoothManager> bluetoothManagerProvider,
      Provider<EmbeddedHttpServer> httpServerProvider) {
    return new MessagingForegroundService_MembersInjector(onlineRepoProvider, offlineRepoProvider, bleScannerProvider, bleAdvertiserProvider, gattServerProvider, kpiTrackerProvider, bluetoothManagerProvider, httpServerProvider);
  }

  @Override
  public void injectMembers(MessagingForegroundService instance) {
    injectOnlineRepo(instance, onlineRepoProvider.get());
    injectOfflineRepo(instance, offlineRepoProvider.get());
    injectBleScanner(instance, bleScannerProvider.get());
    injectBleAdvertiser(instance, bleAdvertiserProvider.get());
    injectGattServer(instance, gattServerProvider.get());
    injectKpiTracker(instance, kpiTrackerProvider.get());
    injectBluetoothManager(instance, bluetoothManagerProvider.get());
    injectHttpServer(instance, httpServerProvider.get());
  }

  @InjectedFieldSignature("com.messaging.service.service.MessagingForegroundService.onlineRepo")
  public static void injectOnlineRepo(MessagingForegroundService instance,
      OnlineMessagingRepository onlineRepo) {
    instance.onlineRepo = onlineRepo;
  }

  @InjectedFieldSignature("com.messaging.service.service.MessagingForegroundService.offlineRepo")
  public static void injectOfflineRepo(MessagingForegroundService instance,
      OfflineMessagingRepository offlineRepo) {
    instance.offlineRepo = offlineRepo;
  }

  @InjectedFieldSignature("com.messaging.service.service.MessagingForegroundService.bleScanner")
  public static void injectBleScanner(MessagingForegroundService instance, BleScanner bleScanner) {
    instance.bleScanner = bleScanner;
  }

  @InjectedFieldSignature("com.messaging.service.service.MessagingForegroundService.bleAdvertiser")
  public static void injectBleAdvertiser(MessagingForegroundService instance,
      BleAdvertiser bleAdvertiser) {
    instance.bleAdvertiser = bleAdvertiser;
  }

  @InjectedFieldSignature("com.messaging.service.service.MessagingForegroundService.gattServer")
  public static void injectGattServer(MessagingForegroundService instance,
      BleGattServer gattServer) {
    instance.gattServer = gattServer;
  }

  @InjectedFieldSignature("com.messaging.service.service.MessagingForegroundService.kpiTracker")
  public static void injectKpiTracker(MessagingForegroundService instance, KpiTracker kpiTracker) {
    instance.kpiTracker = kpiTracker;
  }

  @InjectedFieldSignature("com.messaging.service.service.MessagingForegroundService.bluetoothManager")
  public static void injectBluetoothManager(MessagingForegroundService instance,
      BluetoothManager bluetoothManager) {
    instance.bluetoothManager = bluetoothManager;
  }

  @InjectedFieldSignature("com.messaging.service.service.MessagingForegroundService.httpServer")
  public static void injectHttpServer(MessagingForegroundService instance,
      EmbeddedHttpServer httpServer) {
    instance.httpServer = httpServer;
  }
}
