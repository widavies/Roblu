package com.cpjd.roblu.ui.forms;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CompoundButtonCompat;
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
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.models.metrics.RBoolean;
import com.cpjd.roblu.models.metrics.RCalculation;
import com.cpjd.roblu.models.metrics.RCheckbox;
import com.cpjd.roblu.models.metrics.RChooser;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RDivider;
import com.cpjd.roblu.models.metrics.RFieldData;
import com.cpjd.roblu.models.metrics.RFieldDiagram;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RSlider;
import com.cpjd.roblu.models.metrics.RStopwatch;
import com.cpjd.roblu.models.metrics.RTextfield;
import com.cpjd.roblu.ui.dialogs.FastDialogBuilder;
import com.cpjd.roblu.ui.images.Drawing;
import com.cpjd.roblu.ui.images.FullScreenImageGalleryActivity;
import com.cpjd.roblu.ui.images.FullScreenImageGalleryAdapter;
import com.cpjd.roblu.ui.images.ImageGalleryActivity;
import com.cpjd.roblu.ui.images.ImageGalleryAdapter;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private MetricListener listener;

    public interface MetricListener {
        /**
         * Called when a change is made to ANY element, since this class stores all the references, just save everything and you're good to go.
         */
        void changeMade(RMetric metric);
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

        yes.setEnabled(editable);
        no.setEnabled(editable);

        ColorStateList colorStateList = new ColorStateList(
                new int[][] {
                        new int[] { -android.R.attr.state_checked }, // unchecked
                        new int[] {  android.R.attr.state_checked }  // checked
                },
                new int[] {
                        rui.getButtons(),
                        rui.getAccent()
                }
        );
        CompoundButtonCompat.setButtonTintList(yes, colorStateList);
        CompoundButtonCompat.setButtonTintList(no, colorStateList);
        group.setId(Utils.generateViewId());
        yes.setId(Utils.generateViewId());
        no.setId(Utils.generateViewId());
        yes.setTextColor(rui.getText());
        no.setTextColor(rui.getText());
        yes.setText(R.string.yes);
        no.setText(R.string.no);

        final RelativeLayout layout = new RelativeLayout(activity);
        final TextView observed = new TextView(activity);

        // don't check either if the boolean isn't modified
        yes.setChecked(bool.isValue());
        yes.setHighlightColor(rui.getAccent());
        no.setChecked(!bool.isValue());
        no.setPadding(0, 0, 0, Utils.DPToPX(activity, 10));
        no.setHighlightColor(rui.getAccent());
        group.addView(yes);
        group.addView(no);
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout.removeView(observed);

                listener.changeMade(bool);
            }
        });
        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout.removeView(observed);

                listener.changeMade(bool);
            }
        });

        // Observed field
        observed.setTextColor(rui.getText());
        observed.setText(R.string.not_observed_yet);
        observed.setTextSize(10);
        observed.setPadding(observed.getPaddingLeft(), Utils.DPToPX(activity, 15), observed.getPaddingRight(), observed.getPaddingBottom());
        observed.setId(Utils.generateViewId());
        observed.setTag("N.O.");
        observed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!editable) return;
                layout.removeView(observed);

                listener.changeMade(bool);
            }
        });

        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                layout.removeView(observed);

                bool.setValue(((RadioButton)radioGroup.getChildAt(0)).isChecked());
                listener.changeMade(bool);
            }
        });

        TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setText(bool.getTitle());
        title.setMaxWidth(width);
        title.setTextSize(20);
        title.setId(Utils.generateViewId());

        // observed params
        RelativeLayout.LayoutParams oParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        oParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        oParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        oParams.addRule(RelativeLayout.BELOW, group.getId());
        observed.setLayoutParams(oParams);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        group.setLayoutParams(params);
        group.setPadding(group.getPaddingLeft(), group.getPaddingTop(), Utils.DPToPX(activity, 10), group.getPaddingBottom());
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        title.setPadding(Utils.DPToPX(activity, 4), title.getPaddingTop(), title.getPaddingRight(), title.getPaddingBottom());
        title.setLayoutParams(params);

        layout.addView(title);
        layout.addView(group);
        if(!bool.isModified()) layout.addView(observed);
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

        final TextView observed = new TextView(activity);
        RelativeLayout.LayoutParams oParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        final RelativeLayout layout = new RelativeLayout(activity);

        layout.addView(title);

        if(!counter.isVerboseInput()) {
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
            addButton.setEnabled(editable);
            addButton.setBackground(add);
            addButton.setPadding(Utils.DPToPX(activity, 4), Utils.DPToPX(activity, 3), Utils.DPToPX(activity, 4), Utils.DPToPX(activity, 3));
            addButton.setScaleX(1.5f);
            addButton.setScaleY(1.5f);
            addButton.setLayoutParams(params);

            final TextView number = new TextView(activity);

            ImageView minusButton = new ImageView(activity);

            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.LEFT_OF, addButton.getId());
            params.addRule(RelativeLayout.CENTER_VERTICAL);

            number.setTextSize(25);
            number.setTextColor(rui.getText());
            number.setId(Utils.generateViewId());
            number.setText(String.valueOf(counter.getTextValue()));
            number.setLayoutParams(params);
            number.setPadding(Utils.DPToPX(activity, 20), number.getPaddingTop(), Utils.DPToPX(activity, 20), number.getPaddingBottom());
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    layout.removeView(observed);
                    counter.add();
                    number.setText(counter.getTextValue());
                    listener.changeMade(counter);
                }
            });

            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.LEFT_OF, number.getId());
            params.addRule(RelativeLayout.CENTER_VERTICAL);

            minusButton.setBackground(minus);
            minusButton.setId(Utils.generateViewId());
            minusButton.setEnabled(editable);
            minusButton.setScaleY(1.5f);
            minusButton.setScaleX(1.5f);
            minusButton.setLayoutParams(params);
            minusButton.setPadding(Utils.DPToPX(activity, 4), Utils.DPToPX(activity, 3), Utils.DPToPX(activity, 4), Utils.DPToPX(activity, 3));

            minusButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    layout.removeView(observed);

                    counter.minus();
                    number.setText(String.valueOf(counter.getTextValue()));
                    listener.changeMade(counter);
                }
            });

            // observed params

            oParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            oParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            oParams.addRule(RelativeLayout.BELOW, number.getId());
            observed.setPadding(0, Utils.DPToPX(activity, 20), 0, 0);
            observed.setLayoutParams(oParams);


            layout.addView(minusButton);
            layout.addView(number);
            layout.addView(addButton);
        } else {
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            title.setTextSize(14f);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            title.setLayoutParams(params);
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.BELOW, title.getId());
            AppCompatEditText et = new AppCompatEditText(activity);
            Utils.setCursorColor(et, rui.getAccent());
            et.setText(String.valueOf(counter.getValue()));
            et.setEnabled(editable);
            et.setTextColor(rui.getText());
            et.setId(Utils.generateViewId());
            et.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
            et.setRawInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
            et.setSingleLine();
            et.setMaxLines(1);
            et.setHighlightColor(rui.getAccent());
            Drawable d = et.getBackground();
            d.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_ATOP);
            et.setBackground(d);
            et.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    layout.removeView(observed);

                    try {
                        counter.setValue(Double.parseDouble(charSequence.toString()));
                    } catch(NumberFormatException e) {
                        counter.setValue(0);
                    }
                    listener.changeMade(counter);
                }

                @Override
                public void afterTextChanged(Editable editable) {}
            });
            et.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
            et.setFocusableInTouchMode(true);
            et.setLayoutParams(params);
            layout.addView(et);

            oParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            oParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            oParams.addRule(RelativeLayout.BELOW, et.getId());
            observed.setPadding(0, Utils.DPToPX(activity, 20), 0, 0);
            observed.setLayoutParams(oParams);

        }

        // Observed field
        observed.setTextColor(rui.getText());
        observed.setText(R.string.not_observed_yet);
        observed.setTextSize(10);
        observed.setId(Utils.generateViewId());
        observed.setTag("N.O.");
        observed.setPadding(0, Utils.DPToPX(activity, 20), 0, 0);
        observed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!editable) return;
                layout.removeView(observed);
                listener.changeMade(counter);
            }
        });

        if(!counter.isModified()) layout.addView(observed);
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
        sb.setMax(slider.getMax() - slider.getMin());
        sb.setEnabled(editable);
        sb.setProgress(slider.getValue() - slider.getMin());
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
        current.setTextColor(Color.WHITE);
        minv.setText(String.valueOf(slider.getMin()));
        max.setText(String.valueOf(slider.getMax()));
        final RelativeLayout layout = new RelativeLayout(activity);

        // Observed field
        final TextView observed = new TextView(activity);
        observed.setTextColor(rui.getText());
        observed.setText(R.string.not_observed_yet);
        observed.setTextSize(10);
        observed.setId(Utils.generateViewId());
        observed.setTag("N.O.");
        observed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!editable) return;
                layout.removeView(observed);

                listener.changeMade(slider);
            }
        });

        observed.setPadding(Utils.DPToPX(activity, 8), Utils.DPToPX(activity, 4), observed.getPaddingRight(), observed.getPaddingBottom());

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                layout.removeView(observed);
                slider.setValue(progress + slider.getMin());
                current.setText(String.valueOf(slider.getValue()));
                seekBar.setProgress(progress);
                listener.changeMade(slider);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

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
        // observed params
        RelativeLayout.LayoutParams oParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        oParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        oParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        oParams.addRule(RelativeLayout.BELOW, max.getId());
        observed.setLayoutParams(oParams);

        layout.addView(title);
        layout.addView(sb);
        layout.addView(minv);
        layout.addView(current);
        layout.addView(max);
        if(!slider.isModified()) layout.addView(observed);
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
        spinner.setEnabled(editable);
        spinner.setPadding(Utils.DPToPX(activity, 140), spinner.getPaddingTop(), spinner.getPaddingRight(), spinner.getPaddingBottom());
        if(chooser.getValues() != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, chooser.getValues());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            if(chooser.getSelectedIndex() >= chooser.getValues().length) chooser.setSelectedIndex(0);
            spinner.setSelection(chooser.getSelectedIndex());
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        // Observed field
        final TextView observed = new TextView(activity);
        observed.setTextColor(rui.getText());
        observed.setText(R.string.not_observed_yet);
        observed.setTextSize(10);
        observed.setId(Utils.generateViewId());
        observed.setTag("N.O.");
        observed.setPadding(Utils.DPToPX(activity, 8), Utils.DPToPX(activity, 4), observed.getPaddingRight(), observed.getPaddingBottom());

        final RelativeLayout layout = new RelativeLayout(activity);
        observed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!editable) return;
                layout.removeView(observed);
                listener.changeMade(chooser);
            }
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first;
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(!first) {
                    first = true;
                } else {
                    layout.removeView(observed);
                    chooser.setSelectedIndex(i);
                    listener.changeMade(chooser);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setText(chooser.getTitle());
        title.setTextSize(20);
        title.setId(Utils.generateViewId());
        title.setPadding(Utils.DPToPX(activity, 10), title.getPaddingTop(), title.getPaddingRight(), title.getPaddingBottom());

        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        //params.addRule(RelativeLayout.RIGHT_OF, title.getId());
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.BELOW, title.getId());
        spinner.setLayoutParams(params);

        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        title.setLayoutParams(params);

        // observed params
        RelativeLayout.LayoutParams oParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        oParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        oParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        oParams.addRule(RelativeLayout.BELOW, spinner.getId());
        observed.setLayoutParams(oParams);

        layout.addView(title);
        layout.addView(spinner);
        if(!chooser.isModified()) layout.addView(observed);
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
        title.setPadding(Utils.DPToPX(activity, 10), title.getPaddingTop(), 100, title.getPaddingBottom());
        title.setTextSize(20);
        title.setTextColor(rui.getText());
        title.setText(checkbox.getTitle());
        title.setLayoutParams(params);

        final RelativeLayout layout = new RelativeLayout(activity);
        layout.addView(title);

        // Observed field
        final TextView observed = new TextView(activity);
        observed.setTextColor(rui.getText());
        observed.setText(R.string.not_observed_yet);
        observed.setTextSize(10);
        observed.setId(Utils.generateViewId());
        observed.setTag("N.O.");
        observed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!editable) return;
                layout.removeView(observed);
                listener.changeMade(checkbox);
            }
        });

        RelativeLayout.LayoutParams oParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        if(checkbox.getValues() != null) {
            final AppCompatCheckBox[] boxes = new AppCompatCheckBox[checkbox.getValues().size()];
            int i = 0;
            for(Object o : checkbox.getValues().keySet()) {
                params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.RIGHT_OF, title.getId());
                if (i > 0) params.addRule(RelativeLayout.BELOW, boxes[i - 1].getId());
                AppCompatCheckBox box = new AppCompatCheckBox(activity);
                box.setText(o.toString());
                box.setTag(o.toString());
                box.setId(Utils.generateViewId());
                box.setTextColor(rui.getText());
                box.setHighlightColor(rui.getAccent());
                box.setChecked(checkbox.getValues().get(o.toString()));
                box.setEnabled(editable);
                box.setLayoutParams(params);
                ColorStateList colorStateList = new ColorStateList(
                        new int[][] {
                                new int[] { -android.R.attr.state_checked }, // unchecked
                                new int[] {  android.R.attr.state_checked }  // checked
                        },
                        new int[] {
                                rui.getButtons(),
                                rui.getAccent()
                        }
                );
                CompoundButtonCompat.setButtonTintList(box, colorStateList);
                box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        layout.removeView(observed);
                        checkbox.getValues().put(compoundButton.getTag().toString(), b);
                        listener.changeMade(checkbox);
                    }
                });
                boxes[i] = box;
                layout.addView(boxes[i]);
                i++;
            }
            oParams.addRule(RelativeLayout.BELOW, boxes[boxes.length - 1].getId());
        }
        oParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        oParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        observed.setLayoutParams(oParams);
        if(!checkbox.isModified()) layout.addView(observed);

        return getCard(layout);
    }

    public CardView getFieldDiagram(final int position, final RFieldDiagram fieldDiagram) {
        RelativeLayout layout = new RelativeLayout(activity);
        TextView textView = new TextView(activity);
        textView.setText(fieldDiagram.getTitle());
        textView.setPadding(textView.getPaddingLeft(), textView.getPaddingTop(), textView.getPaddingRight(), Utils.DPToPX(activity, 10));;
        textView.setTextColor(rui.getText());
        textView.setId(Utils.generateViewId());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, textView.getId());
        layout.addView(textView);
        ImageView imageView = new ImageView(activity);
        imageView.setAdjustViewBounds(true);

        // Get field diagram
        final Bitmap field = BitmapFactory.decodeResource(activity.getResources(), R.drawable.field2018);

        // Get drawings
        if(fieldDiagram.getDrawings() != null) {
            Bitmap drawings = BitmapFactory.decodeByteArray(fieldDiagram.getDrawings(), 0, fieldDiagram.getDrawings().length);
            Bitmap mutableBitmap = field.copy(Bitmap.Config.ARGB_8888, true);
            Canvas c = new Canvas(mutableBitmap);
            Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
            c.drawBitmap(drawings, 0, 0, p);
            layout.setBackground(new BitmapDrawable(activity.getResources(), field));
            imageView.setImageBitmap(drawings);
            imageView.setAlpha(0.55f);
        } else imageView.setImageBitmap(field);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(position == -1 || !editable) return;

                Intent intent = new Intent(activity, Drawing.class);
                intent.putExtra("fieldDrawings", fieldDiagram.getDrawings());
                intent.putExtra("fieldDiagramID", fieldDiagram.getPictureID());
                intent.putExtra("fieldDiagram", true);
                intent.putExtra("position", position);
                intent.putExtra("ID", fieldDiagram.getID());
                activity.startActivityForResult(intent, Constants.GENERAL);
            }
        });
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        layout.addView(imageView);
        return getCard(layout);
    }

    /**
     * Gets the Slider UI card from an RSlider reference
     * @param stopwatch RSlider reference to be set to the UI
     * @return a UI CardView
     */
    public CardView getStopwatch(final RStopwatch stopwatch, final boolean demo) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        final TextView title = new TextView(activity);
        title.setTextColor(rui.getText());
        title.setText(stopwatch.getTitle());
        title.setTextSize(20);
        title.setId(Utils.generateViewId());
        title.setPadding(Utils.DPToPX(activity, 8), title.getPaddingTop(), title.getPaddingRight(), title.getPaddingBottom());
        title.setLayoutParams(params);

        final Drawable play = ContextCompat.getDrawable(activity, R.drawable.play);
        final Drawable pause = ContextCompat.getDrawable(activity,R.drawable.pause);
        final Drawable reset = ContextCompat.getDrawable(activity,R.drawable.replay);
        final Drawable lap = ContextCompat.getDrawable(activity, R.drawable.lap);

        if(play != null) {
            play.mutate();
            play.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);
        }
        if(lap != null) {
            lap.mutate();
            lap.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);
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
        playButton.setEnabled(editable);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.BELOW, title.getId());
        playButton.setId(Utils.generateViewId());
        playButton.setLayoutParams(params);

        final ImageView lapButton = new ImageView(activity);
        lapButton.setScaleX(0.80f);
        lapButton.setScaleY(0.80f);
        lapButton.setBackground(lap);
        lapButton.setEnabled(editable);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, title.getId());
        lapButton.setId(Utils.generateViewId());
        params.addRule(RelativeLayout.LEFT_OF, playButton.getId());
        lapButton.setLayoutParams(params);

        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, title.getId());
        params.addRule(RelativeLayout.LEFT_OF, lapButton.getId());
        final ImageView button = new ImageView(activity);
        button.setBackground(reset);
        button.setId(Utils.generateViewId());
        button.setEnabled(editable);
        button.setLayoutParams(params);

        final TextView timer = new TextView(activity);
        timer.setTextSize(25);
        timer.setPadding(timer.getPaddingLeft(), timer.getPaddingTop(), Utils.DPToPX(activity, 5), timer.getPaddingBottom());
        timer.setText(Utils.round(stopwatch.getTime(),1)+"s");
        timer.setTextColor(rui.getText());
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, title.getId());
        params.addRule(RelativeLayout.LEFT_OF, button.getId());
        timer.setLayoutParams(params);

        // Observed field
        final TextView observed = new TextView(activity);
        observed.setTextColor(rui.getText());
        observed.setText(R.string.not_observed_yet);
        observed.setTextSize(10);
        observed.setId(Utils.generateViewId());
        observed.setTag("N.O.");
        observed.setPadding(Utils.DPToPX(activity, 8), Utils.DPToPX(activity, 10), observed.getPaddingRight(), observed.getPaddingBottom());
        final RelativeLayout layout = new RelativeLayout(activity);
        observed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!editable) return;
                layout.removeView(observed);
                listener.changeMade(stopwatch);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                layout.removeView(observed);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopwatch.setTime(0.0);
                        listener.changeMade(stopwatch);
                        timer.setText(R.string.no_time);
                        if(playButton.getBackground() == pause) {
                            playButton.callOnClick();
                            timer.setText("0.0s");
                        }
                    }
                });
            }
        });


        final ArrayList<Button> laps = new ArrayList<>();

        // Load times into the thing
        if(stopwatch.getTimes() != null) {
            for(double d : stopwatch.getTimes()) addStopwatchLapButton(stopwatch, layout, playButton, laps, d);
        }

        lapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(demo || !editable) return;
                layout.removeView(observed);
                // Add the current time on the stopwatch to the view
                double t = Double.parseDouble(timer.getText().toString().replace("s", ""));
                ArrayList<Double> times = stopwatch.getTimes();
                if(times == null) times = new ArrayList<>();
                times.add(t);
                stopwatch.setTimes(times);
                addStopwatchLapButton(stopwatch, layout, playButton, laps, t);
                listener.changeMade(stopwatch);
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            Timer time;
            TimerTask task;
            int mode = 0;
            double t;
            @Override
            public void onClick(View view) {
                layout.removeView(observed);
                if(mode == 0) {
                    time = new Timer();
                    task = new TimerTask() {
                        public void run() {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    t = Double.parseDouble(timer.getText().toString().replace("s", ""));
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
                    listener.changeMade(stopwatch);
                }
            }
        });

        // observed params
        RelativeLayout.LayoutParams oParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        oParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        oParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        oParams.addRule(RelativeLayout.BELOW, button.getId());
        observed.setLayoutParams(oParams);

        layout.addView(title);
        layout.addView(timer);
        layout.addView(button);
        layout.addView(lapButton);
        layout.addView(playButton);

        if(!stopwatch.isModified()) layout.addView(observed);
        return getCard(layout);
    }

    private void addStopwatchLapButton(final RStopwatch stopwatch, final RelativeLayout layout, final ImageView playButton, final ArrayList<Button> buttons, double time) {
        final Button b = new Button(activity);
        b.setText("Lap "+(buttons.size() +1)+": "+time+" seconds");
        b.setId(Utils.generateViewId());
        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        if(buttons.size() != 0) params.addRule(RelativeLayout.BELOW, buttons.get(buttons.size() - 1).getId());
        else params.addRule(RelativeLayout.BELOW, playButton.getId());
        b.setTextColor(rui.getText());
        b.setLayoutParams(params);
        b.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(RUI.darker(rui.getCardColor(), 0.75f), PorterDuff.Mode.SRC));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new FastDialogBuilder()
                        .setTitle("Are you sure?")
                        .setMessage("Are you sure you want to delete this lap time?")
                        .setPositiveButtonText("Yes")
                        .setNegativeButtonText("No")
                        .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                            @Override
                            public void accepted() {
                                // Remove the time
                                stopwatch.getTimes().remove(buttons.indexOf(b));
                                layout.removeView(b);
                                buttons.remove(b);

                                // Redo positions
                                for(int i = 0; i < buttons.size(); i++) {
                                    final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                                    if(i == 0) params.addRule(RelativeLayout.BELOW, playButton.getId());
                                    else params.addRule(RelativeLayout.BELOW, buttons.get(i - 1).getId());
                                    buttons.get(i).setLayoutParams(params);
                                }

                                listener.changeMade(stopwatch);
                            }

                            @Override
                            public void denied() {

                            }

                            @Override
                            public void neutral() {

                            }
                        }).build(activity);
            }
        });

        buttons.add(b);
        layout.addView(b);
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
        Utils.setCursorColor(et, rui.getAccent());
        et.setText(textfield.getText());
        et.setEnabled(editable);
        et.setTextColor(rui.getText());
        if(textfield.isNumericalOnly()) et.setInputType(InputType.TYPE_CLASS_NUMBER);
        if(textfield.isOneLine()) {
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            et.setSingleLine();
            et.setMaxLines(1);
        }
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
                listener.changeMade(textfield);
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
     * Gets the Divider UI card from an RTextfield reference
     * @param divider reference to be set to the UI
     * @return a UI CardView
     */
    public CardView getDivider(final RDivider divider) {
        RelativeLayout layout = new RelativeLayout(activity);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        TextView et = new TextView(activity);
        et.setGravity(Gravity.CENTER);
        et.setText(divider.getTitle());
        et.setEnabled(false);
        et.setTextColor(rui.getText());
        et.setTextSize(28f);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setHighlightColor(rui.getAccent());
        et.setSingleLine(false);
        et.setFocusableInTouchMode(true);
        et.setLayoutParams(params);
        layout.addView(et);
        return getCard(layout);
    }

    /**
     * Gets the Gallery UI card from an RGallery reference
     * @param gallery RGallery reference to be set to the UI
     * @return a UI CardView
     */
    public CardView getGallery(final boolean demo, final int tabIndex, final int eventID, final RGallery gallery) {
        RelativeLayout layout = new RelativeLayout(activity);
        TextView textView = new TextView(activity);
        textView.setTextColor(rui.getText());
        textView.setText(gallery.getTitle());
        textView.setId(Utils.generateViewId());
        textView.setMaxWidth(width / 3);
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

                /*
                 * Load images from disk into memory
                 */
                IO io = new IO(activity);
                ImageGalleryActivity.IMAGES = new ArrayList<>();
                for(int i = 0; gallery.getPictureIDs() != null && i < gallery.getPictureIDs().size(); i++) {
                    ImageGalleryActivity.IMAGES.add(io.loadPicture(eventID, gallery.getPictureIDs().get(i)));
                }

                ImageGalleryActivity.setImageThumbnailLoader(RMetricToUI.this);
                FullScreenImageGalleryActivity.setFullScreenImageLoader(RMetricToUI.this);

                Intent intent = new Intent(activity, ImageGalleryActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("title", gallery.getTitle());
                bundle.putInt("galleryID", gallery.getID());
                bundle.putInt("eventID", eventID);
                bundle.putInt("rTabIndex", tabIndex);
                bundle.putBoolean("editable", editable);
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

    public CardView getFieldData(RFieldData fieldData) {
        RelativeLayout layout = new RelativeLayout(activity);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        TextView text = new TextView(activity);
        text.setText(fieldData.getTitle());
        text.setId(Utils.generateViewId());
        TableLayout tableLayout = new TableLayout(activity);
        tableLayout.setId(Utils.generateViewId());

        if(fieldData.getData() != null) {
            for(Object key : fieldData.getData().keySet()) {

                TableRow tableRow = new TableRow(activity);
                TextView red = new TextView(activity);
                red.setGravity(Gravity.CENTER_HORIZONTAL);
                red.setBackgroundColor(Color.RED);
                red.setText(fieldData.getData().get(key).get(0).toString());

                TextView title = new TextView(activity);
                title.setPadding(Utils.DPToPX(activity, 15), title.getPaddingTop(), Utils.DPToPX(activity, 15), title.getPaddingBottom());
                title.setGravity(Gravity.CENTER_HORIZONTAL);
                title.setText(key.toString());

                TextView blue = new TextView(activity);
                blue.setBackgroundColor(Color.BLUE);
                blue.setGravity(Gravity.CENTER_HORIZONTAL);
                blue.setText(fieldData.getData().get(key).get(1).toString());

                tableRow.setBackgroundResource(R.drawable.row_border);

                tableRow.addView(red);
                tableRow.addView(title);
                tableRow.addView(blue);
                tableRow.setGravity(Gravity.CENTER_HORIZONTAL);
                tableLayout.addView(tableRow);
            }
        }
        params.addRule(RelativeLayout.BELOW, text.getId());
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        tableLayout.setLayoutParams(params);
        tableLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(text);
        layout.addView(tableLayout);

        return getCard(layout);
    }

    public CardView getInfoField(final String name, String data, final String website, final int number) {
        RelativeLayout layout = new RelativeLayout(activity);
        TextView textView = new TextView(activity);
        textView.setText(name);
        textView.setTextColor(rui.getText());
        textView.setId(Utils.generateViewId());

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, textView.getId());
        TextView et = new TextView(activity);
        et.setId(Utils.generateViewId());
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
            b.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(RUI.darker(rui.getCardColor(), 0.75f), PorterDuff.Mode.SRC));
            b.setText(R.string.Team_website);
            b.setLayoutParams(params);
            b.setId(Utils.generateViewId());
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(website));
                    activity.startActivity(i);
                }
            });
            layout.addView(b);
            Button tbaTeamSite = new Button(activity);
            tbaTeamSite.setText(R.string.Blue_Alliance_Team_Page);
            tbaTeamSite.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(RUI.darker(rui.getCardColor(), 0.75f), PorterDuff.Mode.SRC));
            tbaTeamSite.setId(Utils.generateViewId());
            tbaTeamSite.setTextColor(rui.getText());
            tbaTeamSite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("https://www.thebluealliance.com/team/" + number));
                    activity.startActivity(i);
                }
            });
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.BELOW, b.getId());
            tbaTeamSite.setLayoutParams(params);
            layout.addView(tbaTeamSite);
        }

        layout.addView(textView); layout.addView(et);
        return getCard(layout);
    }

    public CardView getEditHistory(LinkedHashMap<String, Long> edits) {
        RelativeLayout layout = new RelativeLayout(activity);
        TextView textView = new TextView(activity);
        textView.setText(R.string.edit_history);
        textView.setTextColor(rui.getText());
        textView.setId(Utils.generateViewId());

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, textView.getId());
        TextView et = new TextView(activity);
        et.setId(Utils.generateViewId());
        et.setTextColor(rui.getText());
        /*
         * Generate string
         */
        StringBuilder editHistory = new StringBuilder();
        for(Object o : edits.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            editHistory.append(pair.getKey()).append(" on ").append(Utils.convertTime((Long)pair.getValue())).append("\n");
        }
        et.setText(editHistory.toString());
        et.setSingleLine(false);
        et.setEnabled(false);
        et.setFocusableInTouchMode(false);
        et.setLayoutParams(params);

        layout.addView(textView); layout.addView(et);
        return getCard(layout);
    }

    public CardView getImageView(String title, Bitmap bitmap) {
        RelativeLayout layout = new RelativeLayout(activity);
        TextView textView = new TextView(activity);
        textView.setText(title);
        textView.setPadding(textView.getPaddingLeft(), textView.getPaddingTop(), textView.getPaddingRight(), Utils.DPToPX(activity, 10));;
        textView.setTextColor(rui.getText());
        textView.setId(Utils.generateViewId());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, textView.getId());
        layout.addView(textView);
        ImageView imageView = new ImageView(activity);
        imageView.setAdjustViewBounds(true);
        imageView.setImageBitmap(bitmap);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        layout.addView(imageView);
        return getCard(layout);
    }

    private CardView getCard(View layout) {
        CardView card = new CardView(activity);
        if(editable) {
            Toolbar.LayoutParams params = new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            params.rightMargin = 65;
            card.setLayoutParams(params);
            card.setCardElevation(5);
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
    public void loadFullScreenImage(ImageView iv, byte[] image, int width, LinearLayout bglinearLayout) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);

        if(bitmap != null) {
            iv.setImageBitmap(bitmap);
        } else {
            iv.setImageDrawable(null);
        }
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

    /**
     * Scales a bitmap
     * @param source the source to scale
     * @param newHeight the new height
     * @param newWidth the new width
     * @return the scaled bitmap
     */
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

    /*
     * Statistics!
     */
    public CardView generateLineChart(String metricName, LinkedHashMap<String, Object> data) {
        LineChart chart = new LineChart(activity);
        chart.setNoDataTextColor(rui.getText());
        chart.getXAxis().setValueFormatter(new MyAxisValueFormatter(data));
        chart.getXAxis().setTextColor(rui.getText());
        chart.getXAxis().setGranularity(1f);
        chart.getAxis(YAxis.AxisDependency.LEFT).setTextColor(rui.getText());
        chart.getAxis(YAxis.AxisDependency.RIGHT).setTextColor(rui.getText());
        chart.getLegend().setTextColor(rui.getText());
        chart.getLegend().setTextSize(15f);
        chart.setMinimumHeight(1000);
        chart.getDescription().setEnabled(false);
        List<Entry> entries = new ArrayList<>();

        // Calculate average also
        double sum = 0;

        int index = 0;
        for(Object o : data.keySet()) {
            if(data.get(o.toString()) instanceof String) {
                entries.add(new Entry(index, Float.parseFloat((String)data.get(o.toString()))));
                sum += Float.parseFloat((String)data.get(o.toString()));
            }
            else {
                entries.add(new Entry(index, ((Double)data.get(o.toString())).floatValue()));
                sum += ((Double)data.get(o.toString())).floatValue();
            }
            index++;
        }

        // Calculate the average
        String average = "\n(Average: "+Utils.round(sum / (double)data.size(), 2)+")";

        LineDataSet set = new LineDataSet(entries, metricName+average);
        set.setValueTextSize(12f);
        set.setValueTextColor(rui.getText());
        LineData lineData = new LineData(set);
        chart.setData(lineData);
        chart.invalidate();
        return getCard(chart);
    }

    public CardView getCalculationMetric(ArrayList<RMetric> metrics, RCalculation calculation) {
        String value;
        if(metrics != null) value = calculation.getTitle()+"\nValue: "+calculation.getValue(metrics);
        else value = calculation.getTitle()+"\nValue:";

        RelativeLayout layout = new RelativeLayout(activity);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        TextView et = new TextView(activity);
        et.setGravity(Gravity.CENTER);
        et.setText(value);
        et.setEnabled(false);
        et.setTextColor(rui.getText());
        et.setTextSize(28f);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setHighlightColor(rui.getAccent());
        et.setSingleLine(false);
        et.setFocusableInTouchMode(true);
        et.setLayoutParams(params);
        layout.addView(et);

        CardView card = getCard(layout);
        card.setTag("CALC:"+calculation.getID());
        return card;
    }

    public CardView generatePieChart(String metricName, LinkedHashMap<String, Object> data) {
        PieChart chart = new PieChart(activity);
        chart.setMinimumHeight(1000);
        chart.setDrawHoleEnabled(false);
        chart.setUsePercentValues(true);
        chart.getLegend().setTextColor(rui.getText());
        chart.setCenterText(metricName);
        chart.setCenterTextColor(rui.getText());
        chart.setCenterTextSize(15f);
        chart.getDescription().setEnabled(false);
        List<PieEntry> entries = new ArrayList<>();
        for(Object o : data.keySet()) {
            entries.add(new PieEntry(((Double)data.get(o.toString())).floatValue(), o.toString()));
        }
        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextSize(12f);
        set.setValueTextColor(rui.getText());
        PieData pieData = new PieData(set);
        pieData.setValueFormatter(new PercentFormatter());
        chart.setData(pieData);
        chart.invalidate();
        return getCard(chart);
    }

    public class MyAxisValueFormatter implements IAxisValueFormatter {
        private String[] titles;

        MyAxisValueFormatter(LinkedHashMap<String, Object> values) {
            titles = new String[values.size()];
            int index = 0;
            for(Object o : values.keySet()) {
                titles[index] = o.toString();
                index++;
            }
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            if(value < 0) value = 0;
            return titles[(int)value];
        }
    }
}
