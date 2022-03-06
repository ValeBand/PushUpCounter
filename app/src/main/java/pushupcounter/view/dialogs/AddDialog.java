package pushupcounter.view.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import java.util.Objects;
import pushupcounter.CounterApplication;
import pushupcounter.R;
import pushupcounter.activities.MainActivity;
import pushupcounter.domain.IntegerCounter;
import pushupcounter.domain.exception.CounterException;
import pushupcounter.infrastructure.BroadcastHelper;
import pushupcounter.view.CounterFragment;

public class AddDialog extends DialogFragment {

  public static final String TAG = AddDialog.class.getSimpleName();

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    final MainActivity activity = (MainActivity) getActivity();

    final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit, null);

    final EditText nameInput = dialogView.findViewById(R.id.edit_name);
    final EditText valueInput = dialogView.findViewById(R.id.edit_value);
    final EditText timeInput = dialogView.findViewById(R.id.edit_stopwatch);

    final InputFilter[] valueFilter = new InputFilter[1];
    valueFilter[0] = new InputFilter.LengthFilter(IntegerCounter.getValueCharLimit());
    valueInput.setFilters(valueFilter);

    // Создаю диалоговое окно
    final Dialog dialog =
        new AlertDialog.Builder(getActivity())
            .setView(dialogView)
            .setTitle(getString(R.string.dialog_add_title))
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(getResources().getText(R.string.dialog_button_cancel), null)
            .create();

    // По умолчанию после нажатий на любую кнопку диалоговое окно закрывается. Чтобы этого не происходило, если
    // есть ошибки при заполнении, я переопределяю слушатель кнопок. Код взят со стаковерфлоу.
    dialog.setOnShowListener(new DialogInterface.OnShowListener() {

      @Override
      public void onShow(DialogInterface dialogInterface) {

          Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
          button.setOnClickListener(new View.OnClickListener() {

              @Override
              public void onClick(View view) {
                  final String name = nameInput.getText().toString().trim();
                  if (name.isEmpty()) {
                      Toast.makeText(
                              getActivity(),
                              getResources().getText(R.string.toast_no_name_message),
                              Toast.LENGTH_SHORT)
                              .show();
                      return;
                  }

                  int value;
                  final String valueInputContents = valueInput.getText().toString().trim();
                  if (!valueInputContents.isEmpty()) {
                      try {
                          value = Integer.parseInt(valueInputContents);
                      } catch (NumberFormatException e) {
                          Log.w(TAG, "Unable to parse new value", e);
                          Toast.makeText(
                                  activity,
                                  getResources().getText(R.string.toast_unable_to_modify),
                                  Toast.LENGTH_SHORT)
                                  .show();
                          return;
                      }
                  } else {
                      value = CounterFragment.DEFAULT_VALUE;
                  }

                  long time = 0L;
                  final String timeInputContents = timeInput.getText().toString().trim();
                  if (!timeInputContents.isEmpty()) {
                      try {
                          time = IntegerCounter.isStopwatchValueCorrect(timeInputContents); // Преобразую строку в миллисекунды
                      } catch (NumberFormatException e) {
                          Log.w(TAG, "Unable to parse time", e);
                          Toast.makeText(
                                  activity,
                                  getResources().getText(R.string.toast_unable_to_parse_time),
                                  Toast.LENGTH_SHORT)
                                  .show();
                          return;
                      }
                  } else {
                      time = CounterFragment.DEFAULT_TIME;
                  }
                  if (time < 0) {
                      Log.w(TAG, "Unable to parse time");
                      Toast.makeText(
                              activity,
                              getResources().getText(R.string.toast_unable_to_parse_time),
                              Toast.LENGTH_SHORT)
                              .show();
                      return;
                  }


                  try {
                      addCounter(new IntegerCounter(name, value, time));
                  } catch (CounterException e) {
                      Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                  }

                  //Dismiss once everything is OK.
                  dialog.dismiss();
              }
          });
      }
    });

    dialog.setCanceledOnTouchOutside(true);
    dialog.show();
    Objects.requireNonNull(dialog.getWindow())
        .setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

    return dialog;
  }

  private void addCounter(@NonNull final IntegerCounter counter) {
    CounterApplication.getComponent().localStorage().write(counter);
    new BroadcastHelper(requireContext()).sendSelectCounterBroadcast(counter.getName());
  }
}
