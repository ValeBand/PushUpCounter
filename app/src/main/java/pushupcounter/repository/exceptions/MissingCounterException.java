package pushupcounter.repository.exceptions;

import pushupcounter.domain.exception.CounterException;

public class MissingCounterException extends CounterException {

  public MissingCounterException(String message) {
    super(message);
  }
}
