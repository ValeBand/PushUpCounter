package pushupcounter.domain;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import javax.inject.Singleton;
import pushupcounter.CounterApplication;
import pushupcounter.repository.CounterStorage;

@Singleton
@Component(modules = { AndroidInjectionModule.class, CounterModule.class})
public interface CounterApplicationComponent extends AndroidInjector<CounterApplication> {

  CounterStorage<IntegerCounter> localStorage();
}
