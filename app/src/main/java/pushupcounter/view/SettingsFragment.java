package pushupcounter.view;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;
import pushupcounter.R;
import pushupcounter.SharedPrefKeys;
import pushupcounter.activities.SettingsActivity;

public final class SettingsFragment extends PreferenceFragmentCompat {

  private static final String TAG = SettingsFragment.class.getSimpleName();

  private OnPreferenceClickListener onRemoveCountersClickListener;
  private OnPreferenceClickListener onExportClickListener;
  private String appVersion;
  private String theme;

  public void setOnRemoveCountersClickListener(
      final @NonNull OnPreferenceClickListener onRemoveCountersClickListener) {
    this.onRemoveCountersClickListener = onRemoveCountersClickListener;
  }

  public void setOnExportClickListener(
      final @NonNull OnPreferenceClickListener onExportClickListener) {
    this.onExportClickListener = onExportClickListener;
  }

  public void setAppVersion(String appVersion) {
    this.appVersion = appVersion;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    try {

      // Setting summaries for necessary preferences
      findPreference(SharedPrefKeys.THEME.getName()).setSummary(theme);
      findPreference(SettingsActivity.KEY_VERSION).setSummary(appVersion);

      findPreference(SettingsActivity.KEY_REMOVE_COUNTERS)
          .setOnPreferenceClickListener(onRemoveCountersClickListener);
      findPreference(SettingsActivity.KEY_EXPORT_COUNTERS).setOnPreferenceClickListener(onExportClickListener);

    } catch (NullPointerException e) {
      Log.e(TAG, "Unable to retrieve one of the preferences", e);
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.settings, rootKey);
  }
}
