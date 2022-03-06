package pushupcounter.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pushupcounter.domain.Counter;
import pushupcounter.domain.IntegerCounter;
import pushupcounter.domain.exception.CounterException;
import pushupcounter.infrastructure.Actions;
import pushupcounter.infrastructure.BroadcastHelper;
import pushupcounter.repository.exceptions.MissingCounterException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * Counter storage that uses {@link SharedPreferences} as a medium.
 *
 * <p>This implementation us based on the {@link IntegerCounter} variation of the {@link Counter}.
 */
public class SharedPrefsCounterStorage implements CounterStorage<IntegerCounter> {

  private static final String TAG = SharedPrefsCounterStorage.class.getSimpleName();
  private static final String DATA_FILE_NAME = "counters";
  private static final String COUNTER_AND_STOPWATCH_TIMES = "times";

  private final SharedPreferences sharedPreferences;
  private final BroadcastHelper broadcastHelper;
  private final String defaultCounterName;

  /** @param defaultCounterName Name that will be assigned to a default counter. */
  public SharedPrefsCounterStorage(
      @NonNull final Context context,
      @NonNull final BroadcastHelper broadcastHelper,
      @NonNull final String defaultCounterName) {
    this.sharedPreferences = context.getSharedPreferences(DATA_FILE_NAME, Context.MODE_PRIVATE);
    this.broadcastHelper = broadcastHelper;
    this.defaultCounterName = defaultCounterName;
  }

  @Override
  @NonNull
  public List<IntegerCounter> readAll(boolean addDefault) {
    final List<IntegerCounter> counters = new LinkedList<>();

    final Map<String, ?> dataMap = sharedPreferences.getAll();
    try {
      if (dataMap.isEmpty() && addDefault) {
        final IntegerCounter defaultCounter = new IntegerCounter(this.defaultCounterName);
        counters.add(defaultCounter);
        write(defaultCounter);
      } else {

        for (Map.Entry<String, ?> entry : dataMap.entrySet()) {
          counters.add(new IntegerCounter(entry.getKey(), (String) entry.getValue()));
        }
      }
    } catch (CounterException e) {
      throw new RuntimeException(e);
    }

    return counters;
  }

  @NonNull
  @Override
  public IntegerCounter read(@NonNull final Object identifierObj) throws MissingCounterException {
    final String name = identifierObj.toString();
    final List<IntegerCounter> counters = readAll(false);

    for (IntegerCounter c : counters) {
      if (c.getName().equals(name)) return c;
    }

    throw new MissingCounterException(String.format("Unable find counter: %s", name));
  }

  @NonNull
  @Override
  public IntegerCounter getFirst() {
    return readAll(true).get(0);
  }

  /**
   * Saves provided counter in storage. If it's identifier is already defined, existing counter will
   * be overwritten.
   */
  @SuppressLint("ApplySharedPref")
  @Override
  public void write(@NonNull final IntegerCounter counter) {
    final SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
    StringBuilder builder = new StringBuilder();
    builder.append(counter.getValue()).append(",").append(counter.getTimeSwapBuff()).append(",").append(counter.getTimerValue());
    prefsEditor.putString(counter.getName(), String.valueOf(builder));
    prefsEditor.apply();

    broadcastHelper.sendBroadcast(Actions.COUNTER_SET_CHANGE);
  }

  @SuppressLint("ApplySharedPref")
  @Override
  public void overwriteAll(@NonNull List<IntegerCounter> counters) {
    Log.i(TAG, String.format("Writing %s counters to storage", counters.size()));

    final SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
    prefsEditor.clear();
    for (IntegerCounter c : counters) {
      StringBuilder builder = new StringBuilder();
      builder.append(c.getValue()).append(",").append(c.getTimeSwapBuff()).append(",").append(c.getTimerValue());
      prefsEditor.putString(c.getName(), String.valueOf(builder));
    }
    boolean success = prefsEditor.commit();

    if (success) {
      Log.i(TAG, "Writing has been completed");
    } else {
      Log.e(TAG, "Failed to overwrite counters to storage");
    }

    broadcastHelper.sendBroadcast(Actions.COUNTER_SET_CHANGE);
  }

  @SuppressLint("ApplySharedPref")
  @Override
  public void delete(@NonNull Object counterName) {
    final SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
    prefsEditor.remove(counterName.toString());
    prefsEditor.commit();

    broadcastHelper.sendBroadcast(Actions.COUNTER_SET_CHANGE);
  }

  @SuppressLint("ApplySharedPref")
  @Override
  public void wipe() {
    SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
    prefsEditor.clear();
    prefsEditor.commit();

    broadcastHelper.sendBroadcast(Actions.COUNTER_SET_CHANGE);
  }

  @NonNull
  @Override
  public String toCSV() throws IOException {

    final StringBuilder output = new StringBuilder();
    final CSVPrinter csvPrinter = new CSVPrinter(output, CSVFormat.DEFAULT);

    for (final IntegerCounter c : readAll(false)) {
      csvPrinter.printRecord(c.getName(), c.getValue(), c.getTimerValue());
    }

    return output.toString();
  }
}
