package com.cpjd.roblu.ui.settings.customPreferences;

import android.content.Context;
import android.content.res.ColorStateList;
import android.preference.CheckBoxPreference;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

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

        RUI rui = new IO(getContext()).loadSettings().getRui();

        TextView titleView = view.findViewById(android.R.id.title);
        titleView.setTextColor(rui.getText());

        TextView subtitle = view.findViewById(android.R.id.summary);
        subtitle.setTextColor(rui.darker(rui.getText(), 0.60f));

        try {
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
            CompoundButtonCompat.setButtonTintList(checkbox, colorStateList);
        } catch(Exception e) {}
    }


}
