package com.cpjd.robluscouter.ui.team.forms;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
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

import com.cpjd.robluscouter.R;
import com.cpjd.robluscouter.models.RCheckout;
import com.cpjd.robluscouter.models.RUI;
import com.cpjd.robluscouter.models.metrics.RBoolean;
import com.cpjd.robluscouter.models.metrics.RCheckbox;
import com.cpjd.robluscouter.models.metrics.RChooser;
import com.cpjd.robluscouter.models.metrics.RCounter;
import com.cpjd.robluscouter.models.metrics.RGallery;
import com.cpjd.robluscouter.models.metrics.RSlider;
import com.cpjd.robluscouter.models.metrics.RStopwatch;
import com.cpjd.robluscouter.models.metrics.RTextfield;
import com.cpjd.robluscouter.ui.team.forms.images.FullScreenImageGalleryActivity;
import com.cpjd.robluscouter.ui.team.forms.images.FullScreenImageGalleryAdapter;
import com.cpjd.robluscouter.ui.team.forms.images.ImageGalleryActivity;
import com.cpjd.robluscouter.ui.team.forms.images.ImageGalleryAdapter;
import com.cpjd.robluscouter.utils.Constants;
import com.cpjd.robluscouter.utils.Utils;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import lombok.Setter;

/**
 * RMetricToUI loads a RMetric instance into a UI element (a CardView).
 * Make sure to attach an ElementsListener to listener for updated UI elements
 *
 * @version 2
 * @since 3.2.0
 * @author Will Davies
 */
public class RMetricToUI implements ImageGalleryAdapter.ImageThumbnailLoader, FullScreenImageGalleryAdapter.FullScreenImageLoader {
    /**
     * Activity reference
     */
    private final Activity activity;
    /**
     * True if we're allowed to edit the forms
     */
    private final boolean editable;
    /**
     * UI config
     */
    private final RUI rui;
    private final int width;

    /**
     * Make sure to attach a listener to this!
     */
    @Setter
    private ElementsListener listener;

    public interface ElementsListener {
        /**
         * Called when a change is made to ANY element, since this class stores all the references, just save everything and you're good to go.
         */
        void changeMade();
    }

    public RMetricToUI(Activity activity, RUI rui, boolean editable) {
        this.activity = activity;
        this.editable = editable;
        this.rui = rui;

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x / 2;
    }

    /**
     * Gets the Boolean UI card from an RBoolean reference
     * @param bool RBoolean reference to be set to the UI
     * @return a UI CardView
     */
    public CardView getBoolean(final RBoolean bool) {
        RadioGroup group = new RadioGroup(activity);
        AppCompatRadioButton yes = new AppCompatRadioButton(activity);
        AppCompatRadioButton no = new AppCompatRadioButton(activity);

        yes.setEnabled(!editable);
        no.setEnabled(!editable);

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
        //yes.setSupportButtonTintList(colorStateList);
        //no.setSupportButtonTintList(colorStateList);
        group.setId(Utils.generateViewId());
        yes.setId(Utils.generateViewId());
        no.setId(Utils.generateViewId());
        yes.setText(R.string.yes);
        no.setText(R.string.no);

        // don't check either if the boolean isn't modified
        if(bool.isModified()) {
            yes.setChecked(bool.isValue());
            no.setChecked(!bool.isValue());
        }


        group.addView(yes);
        group.addView(no);

        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                bool.setValue(((RadioButton)radioGroup.getChildAt(0)).isChecked());
                listener.changeMade();
            }
        });

        TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setText(bool.getTitle());
        title.setMaxWidth(width);
        title.setTextSize(20);
        title.setId(Utils.generateViewId());

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

    /**
     * Gets the Counter UI card from an RCounter reference
     * @param counter RCounter reference to be set to the UI
     * @return a UI CardView
     */
    public CardView getCounter(final RCounter counter) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setTextSize(20);
        title.setId(Utils.generateViewId());
        title.setText(counter.getTitle());
        title.setMaxWidth(width);
        title.setPadding(Utils.DPToPX(activity, 8), title.getPaddingTop(), title.getPaddingRight(), title.getPaddingBottom());
        title.setLayoutParams(params);

        Drawable add = ContextCompat.getDrawable(activity, R.drawable.add_small);
        if(add != null) {
            add.mutate();
            add.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);
        }
        Drawable minus = ContextCompat.getDrawable(activity,R.drawable.minus_small);
        if(minus != null) {
            minus.mutate();
            minus.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);
        }
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        ImageView addButton = new ImageView(activity);
        addButton.setId(Utils.generateViewId());
        addButton.setEnabled(!editable);
        addButton.setBackground(add);
        addButton.setPadding(Utils.DPToPX(activity, 8), Utils.DPToPX(activity, 6), Utils.DPToPX(activity, 8), Utils.DPToPX(activity, 6));
        addButton.setLayoutParams(params);

        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.LEFT_OF, addButton.getId());
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        final TextView number = new TextView(activity);
        number.setTextSize(25);
        number.setTextColor(rui.getText());
        number.setId(Utils.generateViewId());
        number.setText(String.valueOf(counter.getTextValue()));
        if(!counter.isModified()) number.setText("N.O.");
        number.setLayoutParams(params);
        number.setPadding(Utils.DPToPX(activity, 20), number.getPaddingTop(), Utils.DPToPX(activity, 20), number.getPaddingBottom());

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                counter.add();
                number.setText(String.valueOf(counter.getValue()));
                listener.changeMade();
            }
        });

        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.LEFT_OF, number.getId());
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        ImageView minusButton = new ImageView(activity);
        minusButton.setBackground(minus);
        minusButton.setId(Utils.generateViewId());
        minusButton.setEnabled(!editable);
        minusButton.setLayoutParams(params);
        minusButton.setPadding(Utils.DPToPX(activity, 8), Utils.DPToPX(activity, 6), Utils.DPToPX(activity, 8), Utils.DPToPX(activity, 6));
        minusButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                counter.minus();
                number.setText(String.valueOf(counter.getValue()));
                listener.changeMade();
            }
        });

        RelativeLayout layout = new RelativeLayout(activity);
        layout.addView(title);
        layout.addView(minusButton);
        layout.addView(number);
        layout.addView(addButton);
        return getCard(layout);
    }
    /**
     * Gets the Slider UI card from an RSlider reference
     * @param slider RSlider reference to be set to the UI
     * @return a UI CardView
     */
    public CardView getSlider(final RSlider slider) {
        TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setText(slider.getTitle());
        title.setTextSize(15);
        title.setMaxWidth(width);
        title.setId(Utils.generateViewId());

        SeekBar sb = new SeekBar(activity);
        sb.getThumb().setColorFilter(rui.getAccent(), PorterDuff.Mode.SRC_IN);
        sb.getProgressDrawable().setColorFilter(rui.getAccent(), PorterDuff.Mode.SRC_IN);
        sb.setMax(slider.getMax());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sb.setMin(slider.getMin());
        }
        sb.setEnabled(!editable);
        sb.setProgress(slider.getValue());
        sb.setId(Utils.generateViewId());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.addRule(RelativeLayout.BELOW, title.getId());
        sb.setLayoutParams(params);

        TextView minv = new TextView(activity);
        minv.setTextColor(rui.getText());
        minv.setId(Utils.generateViewId());
        TextView max = new TextView(activity);
        max.setTextColor(rui.getText());
        max.setId(Utils.generateViewId());
        final TextView current = new TextView(activity);
        current.setTextColor(rui.getText());
        current.setId(Utils.generateViewId());
        current.setText(String.valueOf(slider.getValue()));
        if(!slider.isModified()) current.setText("N.O.");
        current.setTextColor(Color.WHITE);
        minv.setText(String.valueOf(slider.getMin()));
        max.setText(String.valueOf(slider.getMax()));

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                current.setText(String.valueOf(progress));
                slider.setValue(progress);
                seekBar.setProgress(progress);
                listener.changeMade();
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

    /**
     * Gets the Chooser UI card from an RChooser reference
     * @param chooser RChooser reference to be set to the UI
     * @return a UI CardView
     */
    public CardView getChooser(final RChooser chooser) {
        Spinner spinner = new Spinner(activity);
        spinner.setId(Utils.generateViewId());
        spinner.setEnabled(!editable);
        spinner.setPadding(400, spinner.getPaddingTop(), spinner.getPaddingRight(), spinner.getPaddingBottom());
        if(chooser.getValues() != null) {
            ArrayAdapter<String> adapter =
                    new ArrayAdapter<String>(activity, R.layout.spinner_item, chooser.getValues())
                    {
                        @NonNull
                        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                            View v = super.getView(position, convertView, parent);

                            ((TextView) v).setTextSize(16);
                            ((TextView) v).setTextColor(rui.getText());

                            return v;
                        }

                        @Override
                        public View getDropDownView(int position, View convertView,@NonNull ViewGroup parent) {
                            View v = super.getDropDownView(position, convertView, parent);
                            v.setBackgroundColor(rui.getBackground());

                            ((TextView) v).setTextColor(rui.getText());
                            ((TextView) v).setGravity(Gravity.CENTER);
                            return v;
                        }
                    };
            spinner.setAdapter(adapter);
            spinner.setSelection(chooser.getSelectedIndex());
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first;
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(!first) {
                    first = true;
                } else {
                    chooser.setSelectedIndex(i);
                    listener.changeMade();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setText(chooser.getTitle());
        title.setTextSize(20);
        title.setMaxWidth(width);
        title.setId(Utils.generateViewId());
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
    /**
     * Gets the Counter UI card from an RCheckbox reference
     * @param checkbox RCheckbox reference to be set to the UI
     * @return a UI CardView
     */
    public CardView getCheckbox(final RCheckbox checkbox) {
        TextView title = new TextView(activity);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

        title.setId(Utils.generateViewId());
        title.setMaxLines(15);
        title.setMaxWidth(width);
        title.setPadding(18, title.getPaddingTop(), 100, title.getPaddingBottom());
        title.setTextSize(20);
        title.setTextColor(rui.getText());
        title.setText(checkbox.getTitle());
        title.setLayoutParams(params);

        RelativeLayout layout = new RelativeLayout(activity);
        layout.addView(title);

        if(checkbox.getValues() != null) {
            final AppCompatCheckBox[] boxes = new AppCompatCheckBox[checkbox.getValues().size()];
            int i = 0;
            for(Object o : checkbox.getValues().keySet()) {
                Map.Entry pair = (Map.Entry) o;

                params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.RIGHT_OF, title.getId());
                if (i > 0) params.addRule(RelativeLayout.BELOW, boxes[i - 1].getId());
                AppCompatCheckBox box = new AppCompatCheckBox(activity);
                box.setText(pair.getKey().toString());
                box.setTag(pair.getKey());
                box.setId(Utils.generateViewId());
                box.setTextColor(rui.getText());
                box.setChecked((Boolean)pair.getValue());
                box.setEnabled(!editable);
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
                //box.setSupportButtonTintList(colorStateList);
                box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        checkbox.getValues().put(compoundButton.getTag().toString(), b);
                        listener.changeMade();
                    }
                });
                boxes[i] = box;
                layout.addView(boxes[i]);
                i++;
            }
        }

        return getCard(layout);
    }

    /**
     * Gets the Slider UI card from an RSliderreference
     * @param stopwatch RSlider reference to be set to the UI
     * @return a UI CardView
     */
    public CardView getStopwatch(final RStopwatch stopwatch) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setText(stopwatch.getTitle());
        title.setTextSize(20);
        title.setMaxWidth((int)(width * 0.8));
        title.setId(Utils.generateViewId());
        title.setPadding(Utils.DPToPX(activity, 8), title.getPaddingTop(), title.getPaddingRight(), title.getPaddingBottom());
        title.setLayoutParams(params);

        final Drawable play = ContextCompat.getDrawable(activity, R.drawable.play);
        final Drawable pause = ContextCompat.getDrawable(activity,R.drawable.pause);
        final Drawable reset = ContextCompat.getDrawable(activity,R.drawable.replay);

        if(play != null) {
            play.mutate();
            play.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);
        }

        if(pause != null) {
            pause.mutate();
            pause.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);
        }

        if(reset != null) {
            reset.mutate();
            reset.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);
        }

        final ImageView playButton = new ImageView(activity);
        playButton.setBackground(play);
        playButton.setEnabled(!editable);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        playButton.setId(Utils.generateViewId());
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        playButton.setLayoutParams(params);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.LEFT_OF, playButton.getId());
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        final ImageView button = new ImageView(activity);
        button.setBackground(reset);
        button.setId(Utils.generateViewId());
        button.setEnabled(!editable);
        button.setLayoutParams(params);
        final TextView timer = new TextView(activity);
        timer.setTextSize(25);
        timer.setPadding(timer.getPaddingLeft(), timer.getPaddingTop(), Utils.DPToPX(activity, 15), timer.getPaddingBottom());
        timer.setText(stopwatch.getTime()+"s");
        if(!stopwatch.isModified()) timer.setText("N.O.");
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
                                    timer.setText(String.valueOf(Utils.round(t, 1))+"s");

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
                    stopwatch.setTime(t);
                    listener.changeMade();
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
    /**
     * Gets the Textfield UI card from an RTextfield reference
     * @param textfield RTextfield reference to be set to the UI
     * @return a UI CardView
     */
    public CardView getTextfield(final RTextfield textfield) {
        RelativeLayout layout = new RelativeLayout(activity);
        TextView textView = new TextView(activity);
        textView.setTextColor(rui.getText());
        textView.setText(textfield.getTitle());
        textView.setId(Utils.generateViewId());

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, textView.getId());
        AppCompatEditText et = new AppCompatEditText(activity);
        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                AppCompatEditText et2 = (AppCompatEditText)v;
               // if(hasFocus) et2.setSupportBackgroundTintList(ColorStateList.valueOf(rui.getAccent()));
                //else et2.setSupportBackgroundTintList(ColorStateList.valueOf(rui.getText()));
            }
        });
        Utils.setCursorColor(et, rui.getAccent());
        et.setText(textfield.getText());
        et.setEnabled(!editable);
        et.setTextColor(rui.getText());
        if(textfield.isNumericalOnly()) et.setInputType(InputType.TYPE_CLASS_NUMBER);
        if(textfield.isOneLine()) et.setMaxLines(1);
        et.setHighlightColor(rui.getAccent());
        Drawable d = et.getBackground();
        d.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_ATOP);
        et.setBackground(d);
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                textfield.setText(charSequence.toString());
                listener.changeMade();
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

    /**
     * Gets the Gallery UI card from an RGallery reference
     * @param gallery RGallery reference to be set to the UI
     * @return a UI CardView
     */
    public CardView getGallery(final boolean demo, final RCheckout handoff, final RGallery gallery) {
        RelativeLayout layout = new RelativeLayout(activity);
        TextView textView = new TextView(activity);
        textView.setTextColor(rui.getText());
        textView.setText(gallery.getTitle());
        textView.setId(Utils.generateViewId());
        textView.setMaxWidth(width);
        textView.setWidth(width);
        textView.setMaxLines(1);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);

        Button open = new Button(activity);
        open.setText(R.string.open);
        open.setTextColor(rui.getText());
        open.setId(Utils.generateViewId());
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(demo) return;
                ImageGalleryActivity.setImageThumbnailLoader(RMetricToUI.this);
                FullScreenImageGalleryActivity.setFullScreenImageLoader(RMetricToUI.this);
                Intent intent = new Intent(activity, ImageGalleryActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(ImageGalleryActivity.KEY_TITLE, gallery.getTitle());
                bundle.putInt("ID", gallery.getID());
                bundle.putInt("checkout", handoff.getID());
                //bundle.putInt("tabID", tabID);
                bundle.putBoolean("readOnly", editable);
                intent.putExtras(bundle);
                activity.startActivityForResult(intent, Constants.GENERAL);
            }
        });

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        open.setLayoutParams(params);
        open.setPadding(open.getPaddingLeft(), open.getPaddingTop(), Utils.DPToPX(activity, 6), open.getPaddingBottom());
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        textView.setPadding(Utils.DPToPX(activity, 8), textView.getPaddingTop(),textView.getPaddingRight(), textView.getPaddingBottom());
        textView.setLayoutParams(params);
        layout.setTag(gallery.getID());

        layout.addView(textView);
        layout.addView(open);
        return getCard(layout);
    }

    private CardView getCard(View layout) {
        CardView card = new CardView(activity);
        if(editable) {
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
        card.setContentPadding(Utils.DPToPX(activity, 8), Utils.DPToPX(activity, 8), Utils.DPToPX(activity, 8), Utils.DPToPX(activity, 8));
        card.setCardBackgroundColor(rui.getCardColor());
        card.addView(layout);
        return card;
    }

    @Override
    public void loadImageThumbnail(ImageView iv, byte[] image, int dimension) {
        Bitmap bitmap = scaleCenterCrop(BitmapFactory.decodeByteArray(image, 0, image.length), dimension, dimension);

        if(bitmap != null) {
            iv.setImageBitmap(bitmap);
        } else {
            iv.setImageDrawable(null);
        }
    }

    @Override
    public void loadFullScreenImage(ImageView iv, byte[] image, int width, LinearLayout bglinearLayout) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);

        if(bitmap != null) {
            iv.setImageBitmap(bitmap);
        } else {
            iv.setImageDrawable(null);
        }
    }

    private Bitmap scaleCenterCrop(Bitmap source, int newHeight, int newWidth) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }
}
