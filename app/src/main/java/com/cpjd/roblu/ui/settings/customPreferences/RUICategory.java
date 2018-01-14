package com.cpjd.roblu.ui.settings.customPreferences;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.cpjd.roblu.io.IO;

/**
 * Custom preference category that syncs colors with RUI
 *
 * @version 2
 * @since 3.6.9
 * @author Will Davies
 */
@SuppressWarnings("unused")
public class RUICategory extends PreferenceCategory {
    public RUICategory(Context context) {
        super(context);
    }

    public RUICategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RUICategory(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView titleView = view.findViewById(android.R.id.title);
        titleView.setTextColor(new IO(getContext()).loadSettings().getRui().getAccent());
    }
}
