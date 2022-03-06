package pushupcounter.domain;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import pushupcounter.domain.exception.CounterException;
import pushupcounter.domain.exception.InvalidNameException;
import pushupcounter.domain.exception.InvalidValueException;

/**
 * Variation of the {@link Counter} that uses {@link Integer} type as a value. Names (identifiers)
 * of counters are always {@link String}.
 */
public class IntegerCounter implements Counter<Integer> {

  private static final String TAG = IntegerCounter.class.getSimpleName();

  public static final int MAX_VALUE = 1000000000 - 1;
  public static final int MIN_VALUE = -100000000 + 1;
  private static final int DEFAULT_VALUE = 0;

  private String name;
  private Integer value = DEFAULT_VALUE;

  private long timeInMilliseconds = 0L;
  private long startTime = 0L;
  private long timeSwapBuff = 0L;
  private String timerValue = "00:00.00";
  private Boolean isStopFlag = true;

  public IntegerCounter(@NonNull final String name) throws CounterException {
    if (!isValidName(name)) {
      throw new InvalidNameException("Provided name is invalid");
    }
    this.name = name;
  }

  public IntegerCounter(@NonNull final String name, @NonNull final Integer value)
          throws CounterException {
    if (!isValidName(name)) {
      throw new InvalidNameException("Provided name is invalid");
    }
    this.name = name;

    if (!isValidValue(value)) {
      throw new InvalidValueException(
              String.format(
                      "Desired value (%s) is outside of allowed range: %s to %s",
                      value, MIN_VALUE, MAX_VALUE));
    }
    this.value = value;
  }

  public IntegerCounter(@NonNull final String name, @NonNull final Integer value, @NonNull final long time)
          throws CounterException {
    if (!isValidName(name)) {
      throw new InvalidNameException("Provided name is invalid");
    }
    this.name = name;

    if (!isValidValue(value)) {
      throw new InvalidValueException(
              String.format(
                      "Desired value (%s) is outside of allowed range: %s to %s",
                      value, MIN_VALUE, MAX_VALUE));
    }
    this.value = value;

    if (time > 0) {
      timeSwapBuff = time;
      int secs = (int) (time / 1000);
      int mins = secs / 60;
      secs = secs % 60;
      int milliseconds = (int) (time % 1000) / 10;
      setTimerValue(new StringBuilder().append("").append(String.format("%02d", mins)).append(":").append(String.format("%02d", secs)).append(".").append(String.format("%02d", milliseconds)).toString());
    }
  }

  public IntegerCounter(@NonNull final String name, @NonNull final String data)
          throws CounterException {
    if (!isValidName(name)) {
      throw new InvalidNameException("Provided name is invalid");
    }
    this.name = name;

    String[] dataArr = data.split(",");
    value = Integer.parseInt(dataArr[0]);
    timeSwapBuff = Long.parseLong(dataArr[1]);
    timerValue = dataArr[2];
  }

  public void startChronometer() {
    startTime = SystemClock.uptimeMillis();
    isStopFlag = false;
  }
  public long getStartTime() {
    return startTime;
  }
  public long getTimeInMilliseconds() {
    return timeInMilliseconds;
  }
  public void setTimeInMilliseconds(long timeInMilliseconds) {
    this.timeInMilliseconds = timeInMilliseconds;
  }
  public long getTimeSwapBuff() {
    return timeSwapBuff;
  }
  public void stopChronometer() {
    timeSwapBuff += timeInMilliseconds;
    isStopFlag = true;
  }
  public String getTimerValue() {
    return timerValue;
  }
  public void setTimerValue(String timerValue) {
    this.timerValue = timerValue;
  }
  public Boolean isStop() {
    if (isStopFlag)
      return true;
    else
      return false;
  }

  @NonNull
  public String getName() {
    return this.name;
  }

  public void setName(@NonNull String newName) throws InvalidNameException {
    if (!isValidName(newName)) {
      throw new InvalidNameException("Provided name is invalid");
    }
    this.name = newName;
  }

  @NonNull
  public Integer getValue() {
    return this.value;
  }

  public void setValue(@NonNull final Integer newValue) throws InvalidValueException {
    if (!isValidValue(newValue)) {
      throw new InvalidValueException(
          String.format(
              "Desired value (%s) is outside of allowed range: %s to %s",
              newValue, MIN_VALUE, MAX_VALUE));
    }
    this.value = newValue;
  }

  public void increment() {
    try {
      setValue(this.value + 1);
    } catch (InvalidValueException e) {
      Log.e(TAG, "Unable to increment the counter", e);
    }
  }

  public void decrement() {
    try {
      setValue(this.value - 1);
    } catch (InvalidValueException e) {
      Log.e(TAG, "Unable to decrement the counter", e);
    }
  }

  public void reset() {
    try {
      setValue(DEFAULT_VALUE);
      timeInMilliseconds = DEFAULT_VALUE;
      startTime = DEFAULT_VALUE;
      timeSwapBuff = DEFAULT_VALUE;
      timerValue = "00:00.00";
    } catch (Exception e) {
      // This should never happen, but we should still handle the exception...
      throw new RuntimeException(e);
    }
  }

  /**
   * Provides max number of characters that would fit within the {@link IntegerCounter} value
   * limits.
   */
  public static int getValueCharLimit() {
    return String.valueOf(IntegerCounter.MAX_VALUE).length() - 1;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private static boolean isValidName(@NonNull final String name) {
    return name.length() > 0;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private static boolean isValidValue(@NonNull final Integer value) {
    return MIN_VALUE <= value && value <= MAX_VALUE;
  }
  // Функция получает значение секундомера в виде строки, введённой вручную пользователем, проверяет на ошибки и возвращает количество миллисекунд
  public static long isStopwatchValueCorrect(String time) {
    long milliseconds = 0L;
      try {
        String[] s = time.split(","); // делю строку формата "00.00.00" на подстроки
        int tmp = Integer.parseInt(s[0]); // перевожу минуты в миллисекунды
        if (tmp >= 0 && tmp < 60) {
          milliseconds += tmp * 60 * 1000;
        } else {
          milliseconds = -1;
          return milliseconds;
        }
        tmp = Integer.parseInt(s[1]); // перевожу секунды в миллисекунды
        if (tmp >= 0 && tmp < 60) {
          milliseconds += tmp * 1000;
        } else {
          milliseconds = -1;
          return milliseconds;
        }
        tmp = Integer.parseInt(s[2]); // проверяю миллисекунды
        if (tmp >= 0 && tmp < 100) {
          milliseconds += tmp * 10;
        } else {
          milliseconds = -1;
          return milliseconds;
        }
      } catch (Exception e) {
        milliseconds = -1;
        return milliseconds;
      }
    return milliseconds;
  }
}
