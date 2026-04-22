package com.messaging.service.online.api;

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
public final class NetworkClient_Factory implements Factory<NetworkClient> {
  private final Provider<KpiTracker> kpiTrackerProvider;

  private final Provider<TokenProvider> tokenProvider;

  public NetworkClient_Factory(Provider<KpiTracker> kpiTrackerProvider,
      Provider<TokenProvider> tokenProvider) {
    this.kpiTrackerProvider = kpiTrackerProvider;
    this.tokenProvider = tokenProvider;
  }

  @Override
  public NetworkClient get() {
    return newInstance(kpiTrackerProvider.get(), tokenProvider.get());
  }

  public static NetworkClient_Factory create(Provider<KpiTracker> kpiTrackerProvider,
      Provider<TokenProvider> tokenProvider) {
    return new NetworkClient_Factory(kpiTrackerProvider, tokenProvider);
  }

  public static NetworkClient newInstance(KpiTracker kpiTracker, TokenProvider tokenProvider) {
    return new NetworkClient(kpiTracker, tokenProvider);
  }
}
