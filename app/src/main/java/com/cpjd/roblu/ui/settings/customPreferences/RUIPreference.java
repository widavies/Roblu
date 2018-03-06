package com.cpjd.roblu.ui.settings.customPreferences;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;
/**
 * Custom preference that syncs colors with RUI
 *
 * @version 2
 * @since 3.6.9
 * @author Will Davies
 */
@SuppressWarnings("unused")
public class RUIPreference extends Preference {
    public RUIPreference(Context context) {
        super(context);
    }

    public RUIPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RUIPreference(Context context, AttributeSet attrs,
                         int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        try {
            RUI rui = new IO(view.getContext()).loadSettings().getRui();

            TextView titleView = view.findViewById(android.R.id.title);
            titleView.setTextColor(rui.getText());

            TextView subtitle = view.findViewById(android.R.id.summary);
            subtitle.setTextColor(rui.darker(rui.getText(), 0.60f));
        } catch(Exception e) {
            Log.d("RBS", "Failed to bind UI to settings preference.");
        }

    }
}
