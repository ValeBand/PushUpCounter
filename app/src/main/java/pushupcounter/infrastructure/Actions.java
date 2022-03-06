package pushupcounter.infrastructure;

import androidx.annotation.NonNull;
import pushupcounter.CounterApplication;

public enum Actions {
  COUNTER_SET_CHANGE,
  SELECT_COUNTER;

  @NonNull
  public String getActionName() {
    return String.format("%s.%s", CounterApplication.PACKAGE_NAME, this.toString());
  }
}
