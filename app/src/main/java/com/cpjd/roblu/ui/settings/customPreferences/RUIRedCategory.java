package com.cpjd.roblu.ui.settings.customPreferences;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * Custom preference category that is always red to signify DANGER (like deleting stuff preferences)
 *
 * @version 2
 * @since 3.6.9
 * @author Will Davies
 */
@SuppressWarnings("unused")
public class RUIRedCategory extends PreferenceCategory {
    public RUIRedCategory(Context context) {
        super(context);
    }

    public RUIRedCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RUIRedCategory(Context context, AttributeSet attrs,
                          int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView titleView = view.findViewById(android.R.id.title);
        titleView.setTextColor(Color.RED);
    }
}
