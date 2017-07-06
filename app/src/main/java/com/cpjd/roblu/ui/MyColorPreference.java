package com.cpjd.roblu.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RUI;
import com.jrummyapps.android.colorpicker.ColorPreference;

public class MyColorPreference extends ColorPreference {
    public MyColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyColorPreference(Context context, AttributeSet attrs,
                             int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        RUI rui = new Loader(getContext()).loadSettings().getRui();

        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        titleView.setTextColor(rui.getText());

        TextView subtitle = (TextView) view.findViewById(android.R.id.summary);
        subtitle.setTextColor(rui.darker(rui.getText(), 0.6f));
    }
}
