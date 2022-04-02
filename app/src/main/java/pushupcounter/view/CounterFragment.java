package pushupcounter.view;

import static android.content.Context.SENSOR_SERVICE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import pushupcounter.CounterApplication;
import pushupcounter.R;
import pushupcounter.SharedPrefKeys;
import pushupcounter.activities.MainActivity;
import pushupcounter.domain.IntegerCounter;
import pushupcounter.repository.CounterStorage;
import pushupcounter.repository.exceptions.MissingCounterException;
import pushupcounter.view.dialogs.DeleteDialog;
import pushupcounter.view.dialogs.EditDialog;

public class CounterFragment extends Fragment implements SensorEventListener {

  private static final String TAG = CounterFragment.class.getSimpleName();
  public static final String COUNTER_NAME_ATTRIBUTE = "COUNTER_NAME";
  public static final int DEFAULT_VALUE = 0;
  public static final long DEFAULT_TIME = 0L;
  private static final long DEFAULT_VIBRATION_DURATION = 30; // Milliseconds
  private static final String STATE_SELECTED_COUNTER_NAME = "SELECTED_COUNTER_NAME";
  private static final String RELAY_URL = "http://192.168.19.91/relay_switch";

  /** Name is used as a key to look up and modify counter value in storage. */
  private String name;

  private SharedPreferences sharedPrefs;
  private Vibrator vibrator;
  private SensorManager sensorManager;
  private Sensor sensor;
  private TextView counterLabel;
  private Button incrementButton;
  private Button decrementButton;
  // Флаги нужны, чтобы реализовать логику срабатываний при однократных и длительных нажатий физических клавиш
  private Boolean onKeyPressFlag = false;
  private Boolean onKeyPressFlag2 = false;
  private Boolean isStopwatchStarted = false;
  private Boolean isProximitySensorTriggered = false;
  private long startTime = 0L;
  private long startSensorProximityTime = 0L;

  private Button startButton;
  private Button stopButton;
  private TextView chronometer;
  private Handler customHandler = new Handler();

  private IntegerCounter counter;

  private MediaPlayer incrementSoundPlayer;
  private MediaPlayer decrementSoundPlayer;

  private TextToSpeech textToSpeech;
  private boolean textToSpeechReady;

  private void restoreSavedState(@Nullable final Bundle savedInstanceState) {
    if (savedInstanceState == null) return;

    this.name = savedInstanceState.getString(STATE_SELECTED_COUNTER_NAME);
  }

  @Override
  public void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    restoreSavedState(savedInstanceState);

    vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());

    initCounter();

    textToSpeech = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        Log.e("TTS", "TextToSpeech.OnInitListener.onInit...");
        setTextToSpeechLanguage();
      }
    });

    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(
          @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.counter, container, false);

    incrementButton = view.findViewById(R.id.incrementButton);
    incrementButton.setOnClickListener(v -> increment.run());

    decrementButton = view.findViewById(R.id.decrementButton);
    decrementButton.setOnClickListener(v -> decrement());

    counterLabel = view.findViewById(R.id.counterLabel);
    counterLabel.setOnClickListener(
            v -> {
              if (sharedPrefs.getBoolean(SharedPrefKeys.LABEL_CONTROL_ON.getName(), true)) {
                increment.run();
              }
            });

    chronometer = view.findViewById(R.id.chronometer); // Нахожу секундомер на View и устанавливаю стандартное значение
    chronometer.setText(counter.getTimerValue());

    startButton = view.findViewById(R.id.startButton);
    startButton.setOnClickListener(v -> startChronometer());

    stopButton = view.findViewById(R.id.stopButton);
    stopButton.setOnClickListener(v -> stopChronometer());

    invalidateUI();
    return view;
  }

  private void setTextToSpeechLanguage() {
    Locale language = new Locale(Locale.getDefault().getLanguage());
    if (language == null) {
      textToSpeechReady = false;
      return;
    }
    int result = textToSpeech.setLanguage(language);
    if (result == TextToSpeech.LANG_MISSING_DATA) {
      Toast.makeText(getActivity(), getResources().getText(R.string.toast_unable_to_speech_on), Toast.LENGTH_SHORT).show(); // если устройство не поддерживает озвучку, вывожу сообщение
      textToSpeechReady = false;
      return;
    } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
      Toast.makeText(getActivity(), getResources().getText(R.string.toast_unable_to_speech_on), Toast.LENGTH_SHORT).show(); // если устройство не поддерживает озвучку, вывожу сообщение
      textToSpeechReady = false;
      return;
    } else {
      textToSpeechReady = true;
      Locale currentLanguage = textToSpeech.getVoice().getLocale();
    }
  }

  private void initCounter() {
    final CounterStorage<IntegerCounter> storage = CounterApplication.getComponent().localStorage();
    try {
      final String requestedCounter = requireArguments().getString(COUNTER_NAME_ATTRIBUTE);
      if (requestedCounter == null) {
        this.counter = storage.readAll(true).get(0);
        return;
      }
      this.counter = storage.read(requestedCounter);
    } catch (MissingCounterException e) {
      Log.w(TAG, "Unable to find provided counter. Retrieving a different one.", e);
      this.counter = storage.readAll(true).get(0);
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
    savedInstanceState.putString(STATE_SELECTED_COUNTER_NAME, this.name);
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();

    registerProximitySensor();
    /* Setting up sounds */
    incrementSoundPlayer = MediaPlayer.create(getContext(), R.raw.increment_sound);
    decrementSoundPlayer = MediaPlayer.create(getContext(), R.raw.decrement_sound);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (isStopwatchStarted)
      stopChronometer();
    if (textToSpeech != null) {
      textToSpeech.stop();
      textToSpeech.shutdown();
    }
    unregisterProximitySensor();
    incrementSoundPlayer.reset();
    incrementSoundPlayer.release();
    decrementSoundPlayer.reset();
    decrementSoundPlayer.release();
  }

  private void startChronometer() {
    if (!isStopwatchStarted) {
      counter.startChronometer();
      customHandler.post(updateTimerThread);
      vibrate(DEFAULT_VIBRATION_DURATION + 20);
      isStopwatchStarted = true;

      saveValue.run();
    }
  }
  private void stopChronometer() {
    counter.stopChronometer();
    customHandler.removeCallbacks(updateTimerThread);
    vibrate(DEFAULT_VIBRATION_DURATION + 20);
    isStopwatchStarted = false;

    saveValue.run();
  }
  private Runnable updateTimerThread = new Runnable() {
    public void run() {
      counter.setTimeInMilliseconds(SystemClock.uptimeMillis() - counter.getStartTime());
      long updatedTime = counter.getTimeSwapBuff() + counter.getTimeInMilliseconds();

      if (updatedTime >= 3599999) {
        stopChronometer();
        return;
      }
      int secs = (int) (updatedTime / 1000);
      int mins = secs / 60;
      secs = secs % 60;
      int milliseconds = (int) (updatedTime % 1000) / 10;
      counter.setTimerValue(new StringBuilder().append("").append(String.format("%02d", mins)).append(":").append(String.format("%02d", secs)).append(".").append(String.format("%02d", milliseconds)).toString());
      chronometer.setText(counter.getTimerValue());
      customHandler.postDelayed(this, 0);
    }
  };

  @Override
  public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
    inflater.inflate(R.menu.counter_menu, menu);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull final Menu menu) {
    boolean isDrawerOpen = ((MainActivity) requireActivity()).isNavigationOpen();

    MenuItem editItem = menu.findItem(R.id.menu_edit);
    editItem.setVisible(!isDrawerOpen);

    MenuItem deleteItem = menu.findItem(R.id.menu_delete);
    deleteItem.setVisible(!isDrawerOpen);

    MenuItem resetItem = menu.findItem(R.id.menu_reset);
    resetItem.setVisible(!isDrawerOpen);
  }

  public boolean onKeyLongPress(int keyCode, KeyEvent event) {
    Log.d(TAG, "onKeyUp: ");
    switch (keyCode) {
      case KeyEvent.KEYCODE_VOLUME_UP:
        if (sharedPrefs.getBoolean(SharedPrefKeys.HARDWARE_BTN_CONTROL_ON.getName(), true)) {
          onKeyPressFlag2 = true;
          return true;
        }
        return false;

      case KeyEvent.KEYCODE_VOLUME_DOWN:
        if (sharedPrefs.getBoolean(SharedPrefKeys.HARDWARE_BTN_CONTROL_ON.getName(), true)) {
          onKeyPressFlag2 = true;
          return true;
        }
        return false;

      default:
        return false;
    }
  }
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_VOLUME_UP:
        if (sharedPrefs.getBoolean(SharedPrefKeys.HARDWARE_BTN_CONTROL_ON.getName(), true)) {
          event.startTracking();
          if (!onKeyPressFlag)
            startTime = SystemClock.uptimeMillis();
          onKeyPressFlag = true;
          return true;
        }
        return false;

      case KeyEvent.KEYCODE_VOLUME_DOWN:
        if (sharedPrefs.getBoolean(SharedPrefKeys.HARDWARE_BTN_CONTROL_ON.getName(), true)) {
          event.startTracking();
          if (!onKeyPressFlag)
            startTime = SystemClock.uptimeMillis();
          onKeyPressFlag = true;
          return true;
        }
        return false;

      default:
        return false;
    }
  }
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_VOLUME_UP:
        if (sharedPrefs.getBoolean(SharedPrefKeys.HARDWARE_BTN_CONTROL_ON.getName(), true)) {
          event.startTracking();
          if (onKeyPressFlag && !onKeyPressFlag2)
            increment.run();
          else {
            long different = SystemClock.uptimeMillis();
            different -= startTime;
            if (different < 3000)
              increment.run();
          }
          onKeyPressFlag = false;
          onKeyPressFlag2 = false;
          return true;
        }
        return false;

      case KeyEvent.KEYCODE_VOLUME_DOWN:
        if (sharedPrefs.getBoolean(SharedPrefKeys.HARDWARE_BTN_CONTROL_ON.getName(), true)) {
          event.startTracking();
          if (onKeyPressFlag && !onKeyPressFlag2)
            decrement();
          else {
            long different = SystemClock.uptimeMillis();
            different -= startTime;
            if (different < 3000)
              decrement();
          }
          onKeyPressFlag = false;
          onKeyPressFlag2 = false;
          return true;
        }
        return false;

      default:
        return false;
    }
  }
  @SuppressLint("NonConstantResourceId")
  @Override
  public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_reset:
        showResetConfirmationDialog();
        return true;
      case R.id.menu_edit:
        showEditDialog();
        return true;
      case R.id.menu_delete:
        showDeleteDialog();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void showResetConfirmationDialog() {
    final Dialog dialog =
        new AlertDialog.Builder(getActivity())
            .setMessage(getResources().getText(R.string.dialog_reset_title))
            .setCancelable(false)
            .setPositiveButton(
                getResources().getText(R.string.dialog_button_reset), (d, id) -> reset())
            .setNegativeButton(getResources().getText(R.string.dialog_button_cancel), null)
            .create();
    Objects.requireNonNull(dialog.getWindow())
        .setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    dialog.show();
  }

  private void showEditDialog() {
    final EditDialog dialog = EditDialog.newInstance(counter.getName(), counter.getValue(), counter.getTimerValue());
    dialog.show(getParentFragmentManager(), EditDialog.TAG);
  }

  private void showDeleteDialog() {
    final DeleteDialog dialog = DeleteDialog.newInstance(counter.getName());
    dialog.show(getParentFragmentManager(), DeleteDialog.TAG);
  }
  // Озвучивание текста
  private void speakOut(String toSpeak) {
    if (sharedPrefs.getBoolean(SharedPrefKeys.TEXT_TO_SPEECH_ON.getName(), true)) {
      if (!textToSpeechReady) {
        return;
      }
      // A random String (Unique ID).
      String utteranceId = UUID.randomUUID().toString();
      textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }
  }
  private Runnable increment = new Runnable() {
    public void run() {
      // Если включена инверсия, значение уменьшается, а не повышается
      if (sharedPrefs.getBoolean(SharedPrefKeys.INVERSE_COUNTER_ON.getName(), false)) {
        counter.decrement();
        // Подача сигнала на wifi реле esp 01
        if (sharedPrefs.getBoolean(SharedPrefKeys.WIFI_RELAY_ON.getName(), false))
          switchWifiRelay();
      }
      else {
        counter.increment();
      }
      vibrate(DEFAULT_VIBRATION_DURATION);

      //Если включена инверсия, включаем озвучку уменьшения
      if(sharedPrefs.getBoolean(SharedPrefKeys.INVERSE_COUNTER_ON.getName(), false)) {
        // цокания
        playSound(decrementSoundPlayer);
        // Если озвучивание слова "ошибка" включено, то озвучивать значение счётчика не нужно
        if (sharedPrefs.getBoolean(SharedPrefKeys.SPEECH_ERROR_ON.getName(), true))
          speakOut(getResources().getString(R.string.error));
        else if (sharedPrefs.getBoolean(SharedPrefKeys.TEXT_TO_SPEECH_ON.getName(), true))
          speakOut(String.valueOf(counter.getValue()));
      } else {
        // Если инверсии нет
        playSound(incrementSoundPlayer);
        speakOut(String.valueOf(counter.getValue()));
      }

      // Запуск секундомера вместе со счётчиком, если не включена инверсия
      if (counter.isStop() && sharedPrefs.getBoolean(SharedPrefKeys.AUTO_STOPWATCH_ON.getName(), true) && !sharedPrefs.getBoolean(SharedPrefKeys.INVERSE_COUNTER_ON.getName(), false))
        startChronometer();

      // Обновить интерфейс и сохранить значение в памяти
      invalidateUI();
      saveValue.run();
    }
  };

  private void decrement() {
    // Если включена инверсия, значение увеличивается, а не уменьшается
    if (sharedPrefs.getBoolean(SharedPrefKeys.INVERSE_COUNTER_ON.getName(), false)) {
      counter.increment();
    }
    else {
      // Подача сигнала на wifi реле esp 01
      if (sharedPrefs.getBoolean(SharedPrefKeys.WIFI_RELAY_ON.getName(), false))
        switchWifiRelay();
      counter.decrement();
    }
    vibrate(DEFAULT_VIBRATION_DURATION + 20);

    //Если включена инверсия, включаем озвучку увеличения
    if(sharedPrefs.getBoolean(SharedPrefKeys.INVERSE_COUNTER_ON.getName(), false)) {
      // цокания
      playSound(incrementSoundPlayer);
      speakOut(String.valueOf(counter.getValue()));
    } else {
      // цокания
      playSound(decrementSoundPlayer);
      // Если озвучивание слова "ошибка" включено, то озвучивать значение счётчика не нужно
      if (sharedPrefs.getBoolean(SharedPrefKeys.SPEECH_ERROR_ON.getName(), true))
        speakOut(getResources().getString(R.string.error));
      else if (sharedPrefs.getBoolean(SharedPrefKeys.TEXT_TO_SPEECH_ON.getName(), true))
        speakOut(String.valueOf(counter.getValue()));
    }
    // Запуск секундомера вместе со счётчиком, если включена инверсия
    if (counter.isStop() && sharedPrefs.getBoolean(SharedPrefKeys.AUTO_STOPWATCH_ON.getName(), true) && sharedPrefs.getBoolean(SharedPrefKeys.INVERSE_COUNTER_ON.getName(), false))
      startChronometer();
    // Обновить интерфейс и сохранить значение в памяти
    invalidateUI();
    saveValue.run();
  }

  private void reset() {
    // Останавливаем всё и обнуляем значения
    try {
      stopChronometer();
      counter.reset();
    } catch (Exception e) {
      Log.getStackTraceString(e);
      throw new RuntimeException(e);
    }

    // Обновить интерфейс и сохранить значение в памяти
    invalidateUI();
    saveValue.run();
  }

  /** Updates UI elements of the fragment based on current value of the counter. */
  @SuppressLint("SetTextI18n")
  private void invalidateUI() {
    counterLabel.setText(Integer.toString(counter.getValue()));

    incrementButton.setEnabled(counter.getValue() < IntegerCounter.MAX_VALUE);
    decrementButton.setEnabled(counter.getValue() > IntegerCounter.MIN_VALUE);
    chronometer.setText(counter.getTimerValue());
  }

  private Runnable saveValue = new Runnable() {
    public void run() {
      final CounterStorage<IntegerCounter> storage = CounterApplication.getComponent().localStorage();
      storage.write(counter);
    }
  };

  /** Triggers vibration for a specified duration, if vibration is turned on. */
  private void vibrate(long duration) {
    if (sharedPrefs.getBoolean(SharedPrefKeys.VIBRATION_ON.getName(), true)) {
      try {
        vibrator.vibrate(duration);
      } catch (Exception e) {
        Log.e(TAG, "Unable to vibrate", e);
      }
    }
  }

  /** Plays sound if sounds are turned on. */
  private void playSound(@NonNull final MediaPlayer soundPlayer) {
    if (sharedPrefs.getBoolean(SharedPrefKeys.SOUNDS_ON.getName(), true)) {
      try {
        if (soundPlayer.isPlaying()) {
          soundPlayer.seekTo(0);
        }
        soundPlayer.start();
      } catch (Exception e) {
        Log.e(TAG, "Unable to play sound", e);
      }
    }
  }
  private void registerProximitySensor(){
    if (sharedPrefs.getBoolean(SharedPrefKeys.PROXIMITY_SENSOR_ON.getName(), false)) {
      sensorManager = (SensorManager) requireActivity().getSystemService(SENSOR_SERVICE);
      sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
      if (sensor != null)
        sensorManager.registerListener(this, sensor, 4*100*1000);
      else
        Log.i(TAG, "onCreate: Датчик приближения недоступен");
    }
  }
  private void unregisterProximitySensor(){
    if (sensorManager != null)
      sensorManager.unregisterListener(this, sensor);
  }
  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    // При срабатывании датчика приближения
    if(sensorEvent.values[0] < sensor.getMaximumRange()) {
      isProximitySensorTriggered = true;
    } else if (sensorEvent.values[0] >= sensor.getMaximumRange() && isProximitySensorTriggered) {
      increment.run();
      isProximitySensorTriggered = false;
    }
  }

  public void switchWifiRelay() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        URL url;
        HttpURLConnection connection = null;

        try {
          url = new URL(RELAY_URL);
          connection = (HttpURLConnection) url.openConnection();
          InputStream in = new BufferedInputStream(connection.getInputStream());
          Log.d(TAG, "run: Сработало реле");
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          if (connection != null) {
            connection.disconnect();
          }
        }
      }
    }).start();
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {

  }

  @Override
  public void onStop() {
    super.onStop();
  }
}
