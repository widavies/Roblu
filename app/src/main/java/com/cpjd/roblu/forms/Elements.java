package com.cpjd.roblu.forms;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.forms.images.FullScreenImageGalleryActivity;
import com.cpjd.roblu.forms.images.FullScreenImageGalleryAdapter;
import com.cpjd.roblu.forms.images.ImageGalleryActivity;
import com.cpjd.roblu.forms.images.ImageGalleryAdapter;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Text;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This is the most janky class in the entire project, fear not (it works).
 *
 * Elements produces, programmatically, form cards that can be edited and interacted with. They match the color scheme defined by RUI too!
 *
 * @since 3.2.0
 * @author Will Davies
 */
public class Elements implements ImageGalleryAdapter.ImageThumbnailLoader, FullScreenImageGalleryAdapter.FullScreenImageLoader {

    private final Activity activity;
    private final ElementsListener listener;
    private final boolean modifyMode;
    private final int width;

    // Values that might need to be modified
    public static int min, max, increment;

    private final RUI rui;

    public Elements(Activity activity, RUI rui, ElementsListener listener, boolean modifyMode) {
        this.listener = listener;
        this.activity = activity;
        this.modifyMode = modifyMode;
        this.rui = rui;

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x / 2;
    }

    public CardView getBoolean(final int ID, final String name, final int value, final boolean usingNA) {
        listener.nameInited(name);

        RadioGroup group = new RadioGroup(activity);
        AppCompatRadioButton yes = new AppCompatRadioButton(activity);
        AppCompatRadioButton no = new AppCompatRadioButton(activity);
        AppCompatRadioButton na = new AppCompatRadioButton(activity);

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
        yes.setSupportButtonTintList(colorStateList);
        no.setSupportButtonTintList(colorStateList);
        na.setSupportButtonTintList(colorStateList);
        group.setId(Text.generateViewId());
        yes.setId(Text.generateViewId());
        no.setId(Text.generateViewId());
        na.setId(Text.generateViewId());
        yes.setText(R.string.yes);
        no.setText(R.string.no);
        na.setText("N.O.");

        if(value == 1) yes.setChecked(true);
        else if(value == 0) no.setChecked(true);
        else na.setChecked(true);

        group.addView(yes);
        group.addView(no);
        if(usingNA) group.addView(na);

        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int value = -1;
                if(((RadioButton)radioGroup.getChildAt(0)).isChecked()) value = 1;
                else if(((RadioButton)radioGroup.getChildAt(1)).isChecked()) value = 0;
                listener.booleanUpdated(ID, value);
            }
        });

        TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setText(name);
        title.setMaxWidth(width);
        title.setTextSize(20);
        title.setId(Text.generateViewId());

        RelativeLayout layout = new RelativeLayout(activity);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        group.setLayoutParams(params);
        group.setPadding(group.getPaddingLeft(), group.getPaddingTop(), 50, group.getPaddingBottom());
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        title.setPadding(18, title.getPaddingTop(), title.getPaddingRight(), title.getPaddingBottom());
        title.setLayoutParams(params);

        layout.addView(title);
        layout.addView(group);
        return getCard(layout);
    }

    public CardView getCounter(final int ID, final String name, final int initMin, final int initMax, final int initIncrement, final int value, final boolean notObserved) {
        listener.nameInited(name);

        min = initMin;
        max = initMax;
        increment = initIncrement;

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setTextSize(20);
        title.setId(Text.generateViewId());
        title.setText(name);
        title.setMaxWidth(width);
        title.setPadding(Text.DPToPX(activity, 8), title.getPaddingTop(), title.getPaddingRight(), title.getPaddingBottom());
        title.setLayoutParams(params);

        Drawable add = ContextCompat.getDrawable(activity, R.drawable.add_small);
        add.mutate();
        add.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);
        Drawable minus = ContextCompat.getDrawable(activity,R.drawable.minus_small);
        minus.mutate();
        minus.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        ImageView addButton = new ImageView(activity);
        addButton.setId(Text.generateViewId());
        addButton.setBackground(add);
        addButton.setPadding(Text.DPToPX(activity, 8), Text.DPToPX(activity, 6), Text.DPToPX(activity, 8), Text.DPToPX(activity, 6));
        addButton.setLayoutParams(params);

        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.LEFT_OF, addButton.getId());
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        final TextView number = new TextView(activity);
        number.setTextSize(25);
        number.setTextColor(rui.getText());
        number.setId(Text.generateViewId());
        number.setText(String.valueOf(value));
        if(notObserved) number.setText("N.O.");
        number.setLayoutParams(params);
        number.setPadding(Text.DPToPX(activity, 20), number.getPaddingTop(), Text.DPToPX(activity, 20), number.getPaddingBottom());

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(number.getText().toString().equals("N.O.")) {
                    number.setText(String.valueOf(value));
                    listener.counterUpdated(ID, value);
                    return;
                }

                int value = Integer.parseInt(number.getText().toString());
                if(modifyMode) {
                    value += increment;
                    if(value > max) value = max;
                } else {
                    value += initIncrement;
                    if(value > initMax) value = initMax;
                }
                number.setText(String.valueOf(value));
                listener.counterUpdated(ID, value);
            }
        });

        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.LEFT_OF, number.getId());
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        ImageView minusButton = new ImageView(activity);
        minusButton.setBackground(minus);
        minusButton.setId(Text.generateViewId());
        minusButton.setLayoutParams(params);
        minusButton.setPadding(Text.DPToPX(activity, 8), Text.DPToPX(activity, 6), Text.DPToPX(activity, 8), Text.DPToPX(activity, 6));
        minusButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                int value = Integer.parseInt(number.getText().toString());
                if(modifyMode) {
                    value -= increment;
                    if(value < min) value = min;
                } else {
                    value -= initIncrement;
                    if(value < initMin) value = initMin;
                }
                number.setText(String.valueOf(value));
                listener.counterUpdated(ID, value);
            }
        });

        RelativeLayout layout = new RelativeLayout(activity);
        layout.addView(title);
        layout.addView(minusButton);
        layout.addView(number);
        layout.addView(addButton);
        return getCard(layout);
    }

    public CardView getSlider(final int ID, final String name, final int initMax, final int value, final boolean notObserved) {
        listener.nameInited(name);
        max = initMax;

        TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setText(name);
        title.setTextSize(15);
        title.setMaxWidth(width);
        title.setId(Text.generateViewId());

        SeekBar sb = new SeekBar(activity);
        sb.getThumb().setColorFilter(rui.getAccent(), PorterDuff.Mode.SRC_IN);
        sb.getProgressDrawable().setColorFilter(rui.getAccent(), PorterDuff.Mode.SRC_IN);
        sb.setMax(max);
        sb.setProgress(value);
        sb.setId(Text.generateViewId());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.addRule(RelativeLayout.BELOW, title.getId());
        sb.setLayoutParams(params);

        TextView minv = new TextView(activity);
        minv.setTextColor(rui.getText());
        minv.setId(Text.generateViewId());
        TextView max = new TextView(activity);
        max.setTextColor(rui.getText());
        max.setId(Text.generateViewId());
        final TextView current = new TextView(activity);
        current.setTextColor(rui.getText());
        current.setId(Text.generateViewId());
        current.setText(String.valueOf(value));
        if(notObserved) current.setText("N.O.");
        current.setTextColor(Color.WHITE);
        minv.setText(String.valueOf(0));
        max.setText(String.valueOf(initMax));

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                current.setText(String.valueOf(progress));
                seekBar.setProgress(progress);
                listener.sliderUpdated(ID, progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        RelativeLayout layout = new RelativeLayout(activity);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.BELOW, sb.getId());
        minv.setLayoutParams(params);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.addRule(RelativeLayout.BELOW, sb.getId());
        current.setLayoutParams(params);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.BELOW, sb.getId());
        max.setLayoutParams(params);

        layout.addView(title);
        layout.addView(sb);
        layout.addView(minv);
        layout.addView(current);
        layout.addView(max);

        return getCard(layout);
    }

    public CardView getChooser(final int ID, final String name, final ArrayList<String> values, final int selected) {
        listener.nameInited(name);
        Spinner spinner = new Spinner(activity);
        spinner.setId(Text.generateViewId());
        spinner.setPadding(400, spinner.getPaddingTop(), spinner.getPaddingRight(), spinner.getPaddingBottom());
        if(values != null) {
            ArrayAdapter<String> adapter =
                    new ArrayAdapter<String>(activity, R.layout.spinner_item, values)
                    {
                        @NonNull
                        public View getView(@NonNull int position, @NonNull View convertView, @NonNull ViewGroup parent) {
                            View v = super.getView(position, convertView, parent);

                            ((TextView) v).setTextSize(16);
                            ((TextView) v).setTextColor(rui.getText());

                            return v;
                        }

                        @Override
                        public View getDropDownView(int position, @NonNull View convertView,@NonNull ViewGroup parent) {
                            View v = super.getDropDownView(position, convertView, parent);
                            v.setBackgroundColor(rui.getBackground());

                            ((TextView) v).setTextColor(rui.getText());
                            ((TextView) v).setGravity(Gravity.CENTER);
                            return v;
                        }
                    };
            spinner.setAdapter(adapter);
            spinner.setSelection(selected);
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                listener.chooserUpdated(ID, i);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setText(name);
        title.setTextSize(20);
        title.setMaxWidth(width);
        title.setId(Text.generateViewId());
        title.setPadding(18, title.getPaddingTop(), title.getPaddingRight(), title.getPaddingBottom());
        RelativeLayout layout = new RelativeLayout(activity);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        spinner.setLayoutParams(params);

        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        title.setLayoutParams(params);
        layout.addView(title);
        layout.addView(spinner);

        return getCard(layout);
    }

    public CardView getCheckbox(final int ID, final String name, final ArrayList<String> values, final ArrayList<Boolean> checked) {
        listener.nameInited(name);
        TextView title = new TextView(activity);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

        title.setId(Text.generateViewId());
        title.setMaxLines(15);
        title.setMaxWidth(width);
        title.setPadding(18, title.getPaddingTop(), 100, title.getPaddingBottom());
        title.setTextSize(20);
        title.setTextColor(rui.getText());
        title.setText(name);
        title.setLayoutParams(params);

        RelativeLayout layout = new RelativeLayout(activity);
        layout.addView(title);

        if(values != null && checked != null) {
            final AppCompatCheckBox[] boxes = new AppCompatCheckBox[values.size()];
            for (int i = 0; i < boxes.length; i++) {
                params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.RIGHT_OF, title.getId());
                if (i > 0) params.addRule(RelativeLayout.BELOW, boxes[i - 1].getId());
                AppCompatCheckBox box = new AppCompatCheckBox(activity);
                box.setText(values.get(i));
                box.setId(Text.generateViewId());
                box.setTextColor(rui.getText());
                box.setChecked(checked.get(i));
                box.setLayoutParams(params);
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
                box.setSupportButtonTintList(colorStateList);
                box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        ArrayList<Boolean> newValues = new ArrayList<>();
                        for(AppCompatCheckBox box : boxes) newValues.add(box.isChecked());
                        listener.checkboxUpdated(ID, newValues);
                    }
                });
                boxes[i] = box;
                layout.addView(boxes[i]);
            }
        }

        return getCard(layout);
    }

    public CardView getStopwatch(final int ID, final String name, final double time, final boolean notObserved) {
        listener.nameInited(name);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setText(name);
        title.setTextSize(20);
        title.setMaxWidth((int)(width * 0.8));
        title.setId(Text.generateViewId());
        title.setPadding(Text.DPToPX(activity, 8), title.getPaddingTop(), title.getPaddingRight(), title.getPaddingBottom());
        title.setLayoutParams(params);

        final Drawable play = ContextCompat.getDrawable(activity, R.drawable.play);
        final Drawable pause = ContextCompat.getDrawable(activity,R.drawable.pause);
        final Drawable reset = ContextCompat.getDrawable(activity,R.drawable.replay);

        play.mutate();
        play.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);

        pause.mutate();
        pause.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);

        reset.mutate();
        reset.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);

        final ImageView playButton = new ImageView(activity);
        playButton.setBackground(play);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        playButton.setId(Text.generateViewId());
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        playButton.setLayoutParams(params);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.LEFT_OF, playButton.getId());
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        final ImageView button = new ImageView(activity);
        button.setBackground(reset);
        button.setId(Text.generateViewId());
        button.setLayoutParams(params);
        final TextView timer = new TextView(activity);
        timer.setTextSize(25);
        timer.setPadding(timer.getPaddingLeft(), timer.getPaddingTop(), Text.DPToPX(activity, 15), timer.getPaddingBottom());
        timer.setText(time+"s");
        if(notObserved) timer.setText("N.O.");
        timer.setTextColor(rui.getText());
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.LEFT_OF, button.getId());
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        timer.setLayoutParams(params);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        timer.setText(R.string.no_time);
                        listener.stopwatchUpdated(ID, 0.0);
                    }
                });
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            Timer time;
            TimerTask task;
            int mode = 0;
            double t;
            @Override
            public void onClick(View view) {
                if(mode == 0) {
                    time = new Timer();
                    task = new TimerTask() {
                        public void run() {

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(timer.getText().equals("N.O.")) t = 0;
                                    else t = Double.parseDouble(timer.getText().toString().replace("s", ""));
                                    t+=0.1;
                                    timer.setText(String.valueOf(Text.round(t, 1))+"s");

                                }
                            });
                        }
                    };
                    time.schedule(task, 0, 100);
                    mode = 1;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            playButton.setBackground(pause);
                        }
                    });

                } else {
                    task.cancel();
                    task = null;

                    playButton.setBackground(play);
                    mode = 0;
                    listener.stopwatchUpdated(ID, t);
                }
            }
        });

        RelativeLayout layout = new RelativeLayout(activity);
        layout.addView(title);
        layout.addView(timer);
        layout.addView(button);
        layout.addView(playButton);
        return getCard(layout);
    }

    public CardView getTextfield(final int ID, final String name, final String value) {
        listener.nameInited(name);
        RelativeLayout layout = new RelativeLayout(activity);
        TextView textView = new TextView(activity);
        textView.setTextColor(rui.getText());
        textView.setText(name);
        textView.setId(Text.generateViewId());

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, textView.getId());
        AppCompatEditText et = new AppCompatEditText(activity);
        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                AppCompatEditText et2 = (AppCompatEditText)v;
                if(hasFocus) et2.setSupportBackgroundTintList(ColorStateList.valueOf(rui.getAccent()));
                else et2.setSupportBackgroundTintList(ColorStateList.valueOf(rui.getText()));
            }
        });
        Text.setCursorColor(et, rui.getAccent());
        et.setText(value);
        et.setTextColor(rui.getText());
        et.setHighlightColor(rui.getAccent());
        Drawable d = et.getBackground();
        d.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_ATOP);
        et.setBackground(d);
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                listener.textfieldUpdated(ID, charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        et.setSingleLine(false);
        et.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        et.setFocusableInTouchMode(true);
        et.setLayoutParams(params);

        layout.addView(textView); layout.addView(et);
        return getCard(layout);
    }

    public CardView getGallery(final int ID, final String name, final ArrayList<File> files , final boolean demo, final REvent event, final RTeam team, final int tabID) {
        listener.nameInited(name);
        RelativeLayout layout = new RelativeLayout(activity);
        TextView textView = new TextView(activity);
        textView.setTextColor(rui.getText());
        textView.setText(name);
        textView.setId(Text.generateViewId());
        textView.setMaxWidth(width);
        textView.setWidth(width);
        textView.setMaxLines(1);

        Button open = new Button(activity);
        open.setText(R.string.open);
        open.setTextColor(rui.getText());
        open.setId(Text.generateViewId());
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(demo) return;
                ImageGalleryActivity.setImageThumbnailLoader(Elements.this);
                FullScreenImageGalleryActivity.setFullScreenImageLoader(Elements.this);
                Intent intent = new Intent(activity, ImageGalleryActivity.class);
                Bundle bundle = new Bundle();
                if(files != null) bundle.putSerializable(ImageGalleryActivity.KEY_IMAGES, files);
                bundle.putString(ImageGalleryActivity.KEY_TITLE, name);
                bundle.putInt("ID", ID);
                bundle.putSerializable("event", event);
                bundle.putSerializable("team", team);
                bundle.putSerializable("tabID", tabID);
                intent.putExtras(bundle);
                activity.startActivityForResult(intent, Constants.GENERAL);
            }
        });

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        open.setLayoutParams(params);
        open.setPadding(open.getPaddingLeft(), open.getPaddingTop(), Text.DPToPX(activity, 6), open.getPaddingBottom());
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        textView.setPadding(Text.DPToPX(activity, 8), textView.getPaddingTop(),textView.getPaddingRight(), textView.getPaddingBottom());
        textView.setLayoutParams(params);

        TextView tip = new TextView(activity);
        tip.setTag("tip");
        if(files != null){
            if(files.size() == 1) tip.setText(R.string.contains_one_image);
            else tip.setText("Contains "+files.size()+" images");
        }
        else tip.setText(R.string.contains_no_images);
        tip.setId(Text.generateViewId());
        tip.setTextColor(rui.getText());
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        textView.setLayoutParams(params);
        params.addRule(RelativeLayout.BELOW, textView.getId());
        tip.setLayoutParams(params);
        tip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tip.setPadding(Text.DPToPX(activity, 8), Text.DPToPX(activity, 30), tip.getPaddingRight(), tip.getPaddingBottom());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        tip.setMaxLines(1);

        layout.setTag(ID);

        layout.addView(textView);
        layout.addView(tip);
        layout.addView(open);
        return getCard(layout);
    }

    public CardView getEditHistory(ArrayList<String> edits) {
        RelativeLayout layout = new RelativeLayout(activity);
        TextView textView = new TextView(activity);
        textView.setText("Edit history");
        textView.setTextColor(rui.getText());
        textView.setId(Text.generateViewId());

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, textView.getId());
        TextView et = new TextView(activity);
        et.setId(Text.generateViewId());
        et.setTextColor(rui.getText());
        et.setText(Text.concatenateArraylist(edits));
        et.setSingleLine(false);
        et.setEnabled(false);
        et.setFocusableInTouchMode(false);
        et.setLayoutParams(params);

        layout.addView(textView); layout.addView(et);
        return getCard(layout);
    }

    public CardView getInfoField(final String name, String data, final String website, final int number) {
        RelativeLayout layout = new RelativeLayout(activity);
        TextView textView = new TextView(activity);
        textView.setText(name);
        textView.setTextColor(rui.getText());
        textView.setId(Text.generateViewId());

        if(number != -1) {
            final Drawable reset = ContextCompat.getDrawable(activity, R.drawable.export);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            final ImageView page = new ImageView(activity);
            page.setBackground(reset);
            page.setLayoutParams(params);
            page.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("https://www.thebluealliance.com/team/" + number));
                    activity.startActivity(i);
                }
            });
            layout.addView(page);
        }
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, textView.getId());
        TextView et = new TextView(activity);
        et.setId(Text.generateViewId());
        et.setTextColor(rui.getText());
        et.setText(data);
        et.setSingleLine(false);
        et.setEnabled(false);
        et.setFocusableInTouchMode(false);
        et.setLayoutParams(params);

        if(website != null && !website.equals("")) {
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.BELOW, et.getId());
            Button b = new Button(activity);
            b.setTextColor(rui.getText());
            b.setText(website);
            b.setLayoutParams(params);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(website));
                    activity.startActivity(i);
                }
            });
            layout.addView(b);
        }

        layout.addView(textView); layout.addView(et);
        return getCard(layout);
    }

    public CardView getSTextfield(final int ID, final String name, final String value, final boolean numberOnly) {
        listener.nameInited(name);
        RelativeLayout layout = new RelativeLayout(activity);
        TextView textView = new TextView(activity);
        textView.setText(name);
        textView.setTextColor(rui.getText());
        textView.setId(Text.generateViewId());

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, textView.getId());
        AppCompatEditText et = new AppCompatEditText(activity);
        Text.setCursorColor(et, rui.getAccent());
        if(numberOnly) et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setMaxLines(1);
        et.setTextColor(rui.getText());
        et.setHighlightColor(rui.getAccent());
        Drawable d = et.getBackground();
        d.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_ATOP);
        et.setBackground(d);

        et.setText(value);
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                listener.textfieldUpdated(ID, charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        et.setFocusableInTouchMode(true);
        et.setLayoutParams(params);

        layout.addView(textView); layout.addView(et);
        return getCard(layout);
    }

    private CardView getCard(View layout) {
        CardView card = new CardView(activity);
        if(modifyMode) {
            Toolbar.LayoutParams params = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.MATCH_PARENT, Gravity.CENTER);
            params.rightMargin = 65;
            card.setLayoutParams(params);
            card.setMaxCardElevation(0);
        } else {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            card.setLayoutParams(params);
            card.setCardElevation(5);
        }
        card.setUseCompatPadding(true);
        card.setRadius(rui.getFormRadius());
        card.setContentPadding(Text.DPToPX(activity, 8), Text.DPToPX(activity, 8), Text.DPToPX(activity, 8), Text.DPToPX(activity, 8));
        card.setCardBackgroundColor(rui.getCardColor());
        card.addView(layout);
        return card;
    }

    @Override
    public void loadImageThumbnail(ImageView iv, File imageUrl, int dimension) {
        if(imageUrl != null) {
            Picasso.with(iv.getContext()).load(imageUrl).resize(dimension, dimension).centerCrop().into(iv);
        } else {
            iv.setImageDrawable(null);
        }
    }

    @Override
    public void loadFullScreenImage(ImageView iv, File imageUrl, int width, LinearLayout bglinearLayout) {
        if(imageUrl != null) {
            Picasso.with(iv.getContext()).load(imageUrl).resize(width, 0).into(iv);
        } else {
            iv.setImageDrawable(null);
        }
    }
}
