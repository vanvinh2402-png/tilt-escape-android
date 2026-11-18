package com.vanvinh2402.tiltescape.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.vanvinh2402.tiltescape.R;

import java.util.Objects;

/** Keeps the Game Over new-record label tied to the currently finished run. */
public final class NewRecordUiController {
    private final TextView newRecordText;
    private int generation;

    public NewRecordUiController(Activity activity) {
        Objects.requireNonNull(activity, "activity");
        newRecordText = activity.findViewById(R.id.new_record_text);
    }

    public int beginPendingResult() {
        int currentGeneration = ++generation;
        newRecordText.setVisibility(View.GONE);
        return currentGeneration;
    }

    public void onRecordSaved(int resultGeneration, boolean isNewRecord) {
        if (resultGeneration != generation) return;
        newRecordText.setVisibility(isNewRecord ? View.VISIBLE : View.GONE);
    }

    public void reset() {
        generation++;
        newRecordText.setVisibility(View.GONE);
    }
}
