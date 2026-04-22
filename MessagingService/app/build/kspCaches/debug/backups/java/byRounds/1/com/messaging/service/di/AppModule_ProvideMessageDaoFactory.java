package com.messaging.service.di;

import com.messaging.service.db.MessageDao;
import com.messaging.service.db.MessageDatabase;
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
public final class AppModule_ProvideMessageDaoFactory implements Factory<MessageDao> {
  private final Provider<MessageDatabase> dbProvider;

  public AppModule_ProvideMessageDaoFactory(Provider<MessageDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public MessageDao get() {
    return provideMessageDao(dbProvider.get());
  }

  public static AppModule_ProvideMessageDaoFactory create(Provider<MessageDatabase> dbProvider) {
    return new AppModule_ProvideMessageDaoFactory(dbProvider);
  }

  public static MessageDao provideMessageDao(MessageDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMessageDao(db));
  }
}
