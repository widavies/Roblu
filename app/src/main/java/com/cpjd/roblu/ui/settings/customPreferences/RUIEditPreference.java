package com.cpjd.roblu.ui.settings.customPreferences;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;
/**
 * Custom edit preference hat syncs colors with RUI
 *
 * @version 2
 * @since 3.6.9
 * @author Will Davies
 */
@SuppressWarnings("unused")
public class RUIEditPreference extends EditTextPreference {
    public RUIEditPreference(Context context) {
        super(context);
    }

    public RUIEditPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RUIEditPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        RUI rui = new IO(getContext()).loadSettings().getRui();

        TextView titleView = view.findViewById(android.R.id.title);
        titleView.setTextColor(rui.getText());

        TextView subtitle = view.findViewById(android.R.id.summary);
        subtitle.setTextColor(rui.darker(rui.getText(), 0.60f));
    }

}
