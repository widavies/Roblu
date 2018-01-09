package com.cpjd.roblu.ui.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.preference.CheckBoxPreference;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;
import android.view.View;

import com.cpjd.roblu.models.RUI;

public class MyCheckPreference extends CheckBoxPreference {

    public MyCheckPreference(Context context) {
        super(context);
    }

    public MyCheckPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyCheckPreference(Context context, AttributeSet attrs,
                          int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        try {
            RUI rui = new Loader(getContext()).loadSettings().getRui();
            AppCompatCheckBox checkbox = (AppCompatCheckBox) view.findViewById(android.R.id.checkbox);
            checkbox.animate();
            ColorStateList colorStateList = new ColorStateList(
                    new int[][] {
                            new int[] { -android.R.attr.state_checked }, // unchecked
                            new int[] {  android.R.attr.state_checked }  // checked
                    },
                    new int[] {
                            rui.getText(),
                            rui.getAccent()
                    }
            );
            if(android.os.Build.VERSION.SDK_INT >= 21) checkbox.setSupportButtonTintList(colorStateList);
        } catch(Exception e) {}


    }


}
