package com.messaging.service.online.repository;

import com.messaging.service.db.MessageDao;
import com.messaging.service.kpi.KpiTracker;
import com.messaging.service.online.api.NetworkClient;
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
public final class OnlineMessagingRepository_Factory implements Factory<OnlineMessagingRepository> {
  private final Provider<NetworkClient> networkClientProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<KpiTracker> kpiTrackerProvider;

  public OnlineMessagingRepository_Factory(Provider<NetworkClient> networkClientProvider,
      Provider<MessageDao> messageDaoProvider, Provider<KpiTracker> kpiTrackerProvider) {
    this.networkClientProvider = networkClientProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.kpiTrackerProvider = kpiTrackerProvider;
  }

  @Override
  public OnlineMessagingRepository get() {
    return newInstance(networkClientProvider.get(), messageDaoProvider.get(), kpiTrackerProvider.get());
  }

  public static OnlineMessagingRepository_Factory create(
      Provider<NetworkClient> networkClientProvider, Provider<MessageDao> messageDaoProvider,
      Provider<KpiTracker> kpiTrackerProvider) {
    return new OnlineMessagingRepository_Factory(networkClientProvider, messageDaoProvider, kpiTrackerProvider);
  }

  public static OnlineMessagingRepository newInstance(NetworkClient networkClient,
      MessageDao messageDao, KpiTracker kpiTracker) {
    return new OnlineMessagingRepository(networkClient, messageDao, kpiTracker);
  }
}
