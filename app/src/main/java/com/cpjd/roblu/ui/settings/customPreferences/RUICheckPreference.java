package com.cpjd.roblu.ui.settings.customPreferences;

import android.content.Context;
import android.content.res.ColorStateList;
import android.preference.CheckBoxPreference;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;
import android.view.View;

import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;
/**
 * Custom check preference that syncs colors with RUI
 *
 * @version 2
 * @since 3.6.9
 * @author Will Davies
 */
@SuppressWarnings("unused")
public class RUICheckPreference extends CheckBoxPreference {

    public RUICheckPreference(Context context) {
        super(context);
    }

    public RUICheckPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RUICheckPreference(Context context, AttributeSet attrs,
                              int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        try {
            RUI rui = new IO(getContext()).loadSettings().getRui();
            AppCompatCheckBox checkbox = view.findViewById(android.R.id.checkbox);
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
            //if(android.os.Build.VERSION.SDK_INT >= 21) checkbox.setSupportButtonTintList(colorStateList);
        } catch(Exception e) {}
    }


}
