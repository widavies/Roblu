package com.cpjd.roblu.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.preference.CheckBoxPreference;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;
import android.view.View;

import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.utils.Text;

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
        if(Text.getAPI() >= 21) checkbox.setSupportButtonTintList(colorStateList);

    }


}
