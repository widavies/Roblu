package com.cpjd.roblu.ui.settings.customPreferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;
import com.jrummyapps.android.colorpicker.ColorPreference;
/**
 * Custom color picker preference that syncs colors with RUI (used for UICustomizer)
 *
 * @version 2
 * @since 3.6.9
 * @author Will Davies
 */
@SuppressWarnings("unused")
public class RUIColorPreference extends ColorPreference {
    public RUIColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RUIColorPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        RUI rui = new IO(getContext()).loadSettings().getRui();

        TextView titleView = view.findViewById(android.R.id.title);
        titleView.setTextColor(rui.getText());

        TextView subtitle = view.findViewById(android.R.id.summary);
        subtitle.setTextColor(rui.darker(rui.getText(), 0.6f));
    }
}
