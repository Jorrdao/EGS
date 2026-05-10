package com.messaging.service.di;

import android.content.Context;
import com.messaging.service.db.MessageDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideMessageDatabaseFactory implements Factory<MessageDatabase> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideMessageDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public MessageDatabase get() {
    return provideMessageDatabase(contextProvider.get());
  }

  public static AppModule_ProvideMessageDatabaseFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideMessageDatabaseFactory(contextProvider);
  }

  public static MessageDatabase provideMessageDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMessageDatabase(context));
  }
}
