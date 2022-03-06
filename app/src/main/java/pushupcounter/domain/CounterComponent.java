package pushupcounter.domain;

import dagger.Component;
import javax.inject.Singleton;
import pushupcounter.repository.CounterStorage;

@Singleton
@Component(modules = CounterModule.class)
public interface CounterComponent {

  CounterStorage<IntegerCounter> localStorage();
}
