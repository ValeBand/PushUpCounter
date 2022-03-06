package pushupcounter;

import androidx.annotation.NonNull;

public enum SharedPrefKeys {
  ACTIVE_COUNTER("activeKey"),
  KEEP_SCREEN_ON("keepScreenOn"),
  LABEL_CONTROL_ON("labelControlOn"),
  HARDWARE_BTN_CONTROL_ON("hardControlOn"),
  VIBRATION_ON("vibrationOn"),
  WIFI_RELAY_ON("wifiRelay"),
  SOUNDS_ON("soundsOn"),
  THEME("theme"),
  AUTO_STOPWATCH_ON("autoStopwatch"),
  INVERSE_COUNTER_ON("inverseCounter"),
  PROXIMITY_SENSOR_ON("proximitySensor"),
  TEXT_TO_SPEECH_ON("toSpeechOn"),
  SPEECH_ERROR_ON("SpeechErrorOn");

  @NonNull private final String name;

  SharedPrefKeys(@NonNull final String name) {
    this.name = name;
  }

  @NonNull
  public String getName() {
    return name;
  }
}
