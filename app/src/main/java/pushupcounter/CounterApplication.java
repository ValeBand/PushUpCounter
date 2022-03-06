package pushupcounter;

import android.app.Activity;
import android.app.Application;
import dagger.android.DispatchingAndroidInjector;
import javax.inject.Inject;
import pushupcounter.domain.CounterApplicationComponent;
import pushupcounter.domain.CounterModule;
import pushupcounter.domain.DaggerCounterApplicationComponent;

public class CounterApplication extends Application {
  public static String PACKAGE_NAME;

  @Inject DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;

  private static CounterApplicationComponent component;

  @Override
  public void onCreate() {
    super.onCreate();

    component =
        DaggerCounterApplicationComponent.builder().counterModule(new CounterModule(this)).build();

    PACKAGE_NAME = getApplicationContext().getPackageName();
  }

  public static CounterApplicationComponent getComponent() {
    return component;
  }
}
