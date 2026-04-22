package com.messaging.service.online.api;

import com.messaging.service.db.MessageDao;
import com.messaging.service.kpi.KpiTracker;
import com.messaging.service.online.repository.OnlineMessagingRepository;
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
public final class EmbeddedHttpServer_Factory implements Factory<EmbeddedHttpServer> {
  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<OnlineMessagingRepository> onlineRepoProvider;

  private final Provider<KpiTracker> kpiTrackerProvider;

  public EmbeddedHttpServer_Factory(Provider<MessageDao> messageDaoProvider,
      Provider<OnlineMessagingRepository> onlineRepoProvider,
      Provider<KpiTracker> kpiTrackerProvider) {
    this.messageDaoProvider = messageDaoProvider;
    this.onlineRepoProvider = onlineRepoProvider;
    this.kpiTrackerProvider = kpiTrackerProvider;
  }

  @Override
  public EmbeddedHttpServer get() {
    return newInstance(messageDaoProvider.get(), onlineRepoProvider.get(), kpiTrackerProvider.get());
  }

  public static EmbeddedHttpServer_Factory create(Provider<MessageDao> messageDaoProvider,
      Provider<OnlineMessagingRepository> onlineRepoProvider,
      Provider<KpiTracker> kpiTrackerProvider) {
    return new EmbeddedHttpServer_Factory(messageDaoProvider, onlineRepoProvider, kpiTrackerProvider);
  }

  public static EmbeddedHttpServer newInstance(MessageDao messageDao,
      OnlineMessagingRepository onlineRepo, KpiTracker kpiTracker) {
    return new EmbeddedHttpServer(messageDao, onlineRepo, kpiTracker);
  }
}
