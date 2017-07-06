package com.cpjd.roblu.ui;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RUI;
import com.jrummyapps.android.colorpicker.ColorPickerView;
import com.jrummyapps.android.colorpicker.ColorPreference;

public class UICustomizer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uicustomizer);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("UI Customizer");
        getSupportActionBar().setSubtitle("Note: App must be relaunched for some changes to take effect");

        getFragmentManager().beginTransaction().replace(R.id.blankFragment, new SettingsFragment()).commit();

        new UIHandler(this, toolbar).update();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @SuppressWarnings("WeakerAccess")
    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener, ColorPickerView.OnColorChangedListener {

        private RSettings settings;
        private RUI rui;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);


            if (!(getActivity() instanceof UICustomizer)) {
                getActivity().finish();
                return;
            }

            settings = new Loader(getActivity()).loadSettings();
            rui = settings.getRui();

            addPreferencesFromResource(R.xml.ui_preferences);

            ColorPreference preference = (ColorPreference) findPreference("primary");
            preference.setOnPreferenceChangeListener(this);

            findPreference("primary").setOnPreferenceChangeListener(this);
            findPreference("accent").setOnPreferenceChangeListener(this);
            findPreference("card").setOnPreferenceChangeListener(this);
            findPreference("background").setOnPreferenceChangeListener(this);
            findPreference("buttons").setOnPreferenceChangeListener(this);
            findPreference("text").setOnPreferenceChangeListener(this);
            findPreference("animation").setOnPreferenceClickListener(this);
            findPreference("presets").setOnPreferenceClickListener(this);
            MyEditPreference forms = (MyEditPreference) findPreference("forms_radius");
            forms.setDefaultValue(rui.getFormRadius());
            forms.setOnPreferenceChangeListener(this);
            MyEditPreference teams = (MyEditPreference) findPreference("teams_radius");
            teams.setDefaultValue(rui.getTeamsRadius());
            teams.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if(preference.getKey().equals("animation")) {
                addAnimation(rui.getDialogDirection());
                return true;
            }
            else if(preference.getKey().equals("presets")) {
                addPreset(rui.getPreset());
                return true;
            }
            return false;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            if(preference.getKey().equals("primary")) {
                Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
                toolbar.setBackgroundColor((int)o);
                rui.setPrimaryColor((int)o);
            }
            else if(preference.getKey().equals("accent")) rui.setAccent((int)o);
            else if(preference.getKey().equals("card")) rui.setCardColor((int)o);
            else if(preference.getKey().equals("background")) {
                rui.setBackground((int)o);
            }
            else if(preference.getKey().equals("buttons")) rui.setButtons((int)o);
            else if(preference.getKey().equals("text")) rui.setText((int)o);
            else if(preference.getKey().equals("forms_radius")) {
                int num;
                try {
                    num = Integer.parseInt(o.toString());
                } catch(Exception e) {
                    num = 150;
                }
                if(num > 150) num = 150;

                rui.setFormRadius(num);
            }
            else if(preference.getKey().equals("teams_radius")) {
                int num;
                try {
                    num = Integer.parseInt(o.toString());
                } catch(Exception e) {
                    num = 150;
                }
                if(num > 150) num = 150;

                rui.setTeamsRadius(num);
            }

            settings.setRui(rui);
            rui.setPreset(3);
            new Loader(getActivity()).saveSettings(settings);
            Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
            new UIHandler((AppCompatActivity) getActivity(), toolbar).update();
            return true;
        }

        @Override
        public void onColorChanged(int newColor) {

        }

        private void addPreset(int previous) {
            final CharSequence[] items = { " Default ", " Light ", " Custom "};

            final Dialog d = new Dialog(getActivity());
            d.setContentView(R.layout.dialog_sort);
            TextView view = (TextView) d.findViewById(R.id.sort_title);
            view.setText(R.string.ui_preset);
            view.setTextColor(rui.getText());
            RadioGroup group  = (RadioGroup) d.findViewById(R.id.filter_group);
            for(int i = 0; i < group.getChildCount(); i++) {
                AppCompatRadioButton rb = (AppCompatRadioButton) group.getChildAt(i);
                if(i == previous) rb.setChecked(true);
                    rb.setTextColor(rui.getText());
                ColorStateList colorStateList = new ColorStateList (
                        new int[][]{
                                new int[]{-android.R.attr.state_checked},
                                new int[]{android.R.attr.state_checked}
                        },
                        new int[] { rui.getText(),rui.getAccent(), }
                );
                rb.setSupportButtonTintList(colorStateList);
                rb.setText(items[i]);
            }

            group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                    int pos = 0;
                    if(checkedId == R.id.numerical) pos = 0;
                    else if(checkedId == R.id.alphabetical) pos = 1;
                    else if(checkedId == R.id.last_edited) pos = 2;
                    else if(checkedId == R.id.custom) pos = 3;
                    rui.setPreset(pos);
                    settings.setRui(rui);
                    new Loader(getActivity()).saveSettings(settings);
                    Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
                    new UIHandler((AppCompatActivity)getActivity(), toolbar).update();
                    d.dismiss();
                }
            });

            if(d.getWindow() != null) {
                d.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
                d.getWindow().getAttributes().windowAnimations = rui.getAnimation();
            }
            d.show();
        }

        private void addAnimation(int previous) {
            final CharSequence[] items = { " Slide up ",  " Slide left ", " Fade "};

            final Dialog d = new Dialog(getActivity());
            d.setContentView(R.layout.dialog_animation);
            TextView view = (TextView) d.findViewById(R.id.sort_title);
            view.setTextColor(rui.getText());
            RadioGroup group  = (RadioGroup) d.findViewById(R.id.filter_group);
            for(int i = 0; i < group.getChildCount(); i++) {
                AppCompatRadioButton rb = (AppCompatRadioButton) group.getChildAt(i);
                if(i == previous) rb.setChecked(true);
                rb.setTextColor(rui.getText());
                ColorStateList colorStateList = new ColorStateList (
                        new int[][]{
                                new int[]{-android.R.attr.state_checked},
                                new int[]{android.R.attr.state_checked}
                        },
                        new int[] { rui.getText(),rui.getAccent(), }
                );
                rb.setSupportButtonTintList(colorStateList);
                rb.setText(items[i]);
            }

            group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                    int pos = 0;
                    if(checkedId == R.id.numerical) pos = 0;
                    else if(checkedId == R.id.alphabetical) pos = 1;
                    else if(checkedId == R.id.last_edited) pos = 2;
                    else if(checkedId == R.id.custom) pos = 3;
                    rui.setDialogDirection(pos);
                    settings.setRui(rui);
                    new Loader(getActivity()).saveSettings(settings);
                    d.dismiss();
                }
            });

            if(d.getWindow() != null) {
                d.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
                d.getWindow().getAttributes().windowAnimations = rui.getAnimation();
            }
            d.show();

        }
    }
}
