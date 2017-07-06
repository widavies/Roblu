package com.cpjd.roblu.ui;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RUI;

public class MyPreference extends Preference {
    public MyPreference(Context context) {
        super(context);
    }

    public MyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyPreference(Context context, AttributeSet attrs,
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
        subtitle.setTextColor(rui.darker(rui.getText(), 0.60f));
    }
}
