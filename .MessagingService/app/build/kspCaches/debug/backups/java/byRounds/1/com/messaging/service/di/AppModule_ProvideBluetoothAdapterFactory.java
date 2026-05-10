package com.messaging.service.di;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideBluetoothAdapterFactory implements Factory<BluetoothAdapter> {
  private final Provider<BluetoothManager> bluetoothManagerProvider;

  public AppModule_ProvideBluetoothAdapterFactory(
      Provider<BluetoothManager> bluetoothManagerProvider) {
    this.bluetoothManagerProvider = bluetoothManagerProvider;
  }

  @Override
  public BluetoothAdapter get() {
    return provideBluetoothAdapter(bluetoothManagerProvider.get());
  }

  public static AppModule_ProvideBluetoothAdapterFactory create(
      Provider<BluetoothManager> bluetoothManagerProvider) {
    return new AppModule_ProvideBluetoothAdapterFactory(bluetoothManagerProvider);
  }

  public static BluetoothAdapter provideBluetoothAdapter(BluetoothManager bluetoothManager) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideBluetoothAdapter(bluetoothManager));
  }
}
