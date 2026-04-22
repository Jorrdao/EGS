package com.messaging.service.kpi;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class KpiTracker_Factory implements Factory<KpiTracker> {
  @Override
  public KpiTracker get() {
    return newInstance();
  }

  public static KpiTracker_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static KpiTracker newInstance() {
    return new KpiTracker();
  }

  private static final class InstanceHolder {
    private static final KpiTracker_Factory INSTANCE = new KpiTracker_Factory();
  }
}
