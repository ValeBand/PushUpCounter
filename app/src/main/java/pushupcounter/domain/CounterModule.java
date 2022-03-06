package pushupcounter.domain;

import androidx.annotation.NonNull;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import pushupcounter.CounterApplication;
import pushupcounter.R;
import pushupcounter.infrastructure.BroadcastHelper;
import pushupcounter.repository.CounterStorage;
import pushupcounter.repository.SharedPrefsCounterStorage;

@Module
public class CounterModule {

  private final CounterApplication app;

  public CounterModule(CounterApplication app) {
    this.app = app;
  }

  @Provides
  @Singleton
  CounterApplication provideApp() {
    return app;
  }

  @Provides
  @Singleton
  CounterStorage<IntegerCounter> provideCounterStorage(final @NonNull CounterApplication app) {
    return new SharedPrefsCounterStorage(
        app,
        new BroadcastHelper(app),
        (String) app.getResources().getText(R.string.default_counter_name));
  }
}
