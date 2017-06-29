package com.cpjd.roblu.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatEditText;
import android.view.Display;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.forms.elements.EBoolean;
import com.cpjd.roblu.forms.elements.ECheckbox;
import com.cpjd.roblu.forms.elements.EChooser;
import com.cpjd.roblu.forms.elements.ECounter;
import com.cpjd.roblu.forms.elements.EGallery;
import com.cpjd.roblu.forms.elements.ESTextfield;
import com.cpjd.roblu.forms.elements.ESlider;
import com.cpjd.roblu.forms.elements.EStopwatch;
import com.cpjd.roblu.forms.elements.ETextfield;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/*******************************************************
 * Copyright (C) 2016 Will Davies wdavies973@gmail.com
 *
 * This file is part of Roblu
 *
 * Roblu cannot be distributed for a price or to people outside of your local robotics team.
 *******************************************************/

// util library idk why it's called text but it's easy to type
public class Text {

    private static int width;

    public static void initWidth(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
    }

    public static void setInputTextLayoutColor(final int accent, final int text, TextInputLayout textInputLayout, final AppCompatEditText edit) {
        edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                AppCompatEditText edit2 = (AppCompatEditText)v;
                if(hasFocus) edit2.setSupportBackgroundTintList(ColorStateList.valueOf(accent));
                else edit2.setSupportBackgroundTintList(ColorStateList.valueOf(text));
            }
        });
        setCursorColor(edit, accent);

        try {
            if(textInputLayout == null) return;

            Field field = textInputLayout.getClass().getDeclaredField("mFocusedTextColor");
            field.setAccessible(true);
            int[][] states = new int[][]{
                    new int[]{}
            };
            int[] colors = new int[]{
                    accent
            };
            ColorStateList myList = new ColorStateList(states, colors);
            field.set(textInputLayout, myList);

            Field fDefaultTextColor = TextInputLayout.class.getDeclaredField("mDefaultTextColor");
            fDefaultTextColor.setAccessible(true);
            fDefaultTextColor.set(textInputLayout, myList);

            Method method = textInputLayout.getClass().getDeclaredMethod("updateLabelState", boolean.class);
            method.setAccessible(true);
            method.invoke(textInputLayout, true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setCursorColor(AppCompatEditText view, @ColorInt int color) {
        try {
            // Get the cursor resource id
            Field field = TextView.class.getDeclaredField("mCursorDrawableRes");
            field.setAccessible(true);
            int drawableResId = field.getInt(view);

            // Get the editor
            field = TextView.class.getDeclaredField("mEditor");
            field.setAccessible(true);
            Object editor = field.get(view);

            // Get the drawable and set a color filter
            Drawable drawable = ContextCompat.getDrawable(view.getContext(), drawableResId);
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            Drawable[] drawables = {drawable, drawable};

            // Set the drawables
            field = editor.getClass().getDeclaredField("mCursorDrawable");
            field.setAccessible(true);
            field.set(editor, drawables);
        } catch (Exception ignored) {
        }
    }

    public static int getWidth() {
        return width;
    }

    public static double round (double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }


    public static void showSnackbar(View layout, Context context, String text, boolean error, int primary) {
        Snackbar s = Snackbar.make(layout, text, Snackbar.LENGTH_LONG);
        if(error) s.getView().setBackgroundColor(ContextCompat.getColor(context, R.color.red));
        else s.getView().setBackgroundColor(primary);
        s.show();
    }

    public static RForm createEmpty() {
        ArrayList<Element> pit = new ArrayList<>();
        ArrayList<Element> matches = new ArrayList<>();
        ESTextfield name = new ESTextfield("Team name", false);
        ESTextfield number = new ESTextfield("Team number", true);
        name.setID(0); number.setID(1);
        pit.add(name);
        pit.add(number);
        return new RForm(pit, matches);
    }

    public static String guessMatchKey(String matchName) {
        matchName = matchName.toLowerCase();
        if(matchName.startsWith("quals")) return "qm"+matchName.split("\\s+")[1];
        else if(matchName.startsWith("quarters")) return "qf"+matchName.split("\\s+")[1]+"m"+matchName.split("\\s+")[3];
        else if(matchName.startsWith("semis")) return "sf"+matchName.split("\\s+")[1] +"m"+matchName.split("\\s+")[3];
        else if(matchName.startsWith("finals")) return "f1"+matchName.split("\\s+")[1];
        return "";
    }

    /*
     * Used for sorting matches
     */
    public static long getMatchScore(String name) {
        long score = 0;
        String matchName = name.toLowerCase();
        String[] tokens = matchName.split("\\s+");

        // let's give the local match a score
        if(matchName.startsWith("pit")) score -= 100000;
        else if(matchName.startsWith("predictions")) score -= 1000;
        else if(matchName.startsWith("quals")) score = Integer.parseInt(matchName.split("\\s+")[1]);
        else if(matchName.startsWith("quarters")) {
            if(Integer.parseInt(tokens[1]) == 1) score += 1000;
            else if(Integer.parseInt(tokens[1]) == 2) score += 10000;
            else if(Integer.parseInt(tokens[1]) == 3) score += 100000;
            else if(Integer.parseInt(tokens[1]) == 4) score += 1000000;

            score += Integer.parseInt(tokens[3]);
        }
        else if(matchName.startsWith("semis")) {
            if(Integer.parseInt(tokens[1]) == 1) score += 10000000;
            else if(Integer.parseInt(tokens[1]) == 2) score += 100000000;

            score += Integer.parseInt(tokens[3]);
        }
        else if(matchName.startsWith("finals")) {
            score += 1000000000; // d a b, perfect coding right here
            score += Integer.parseInt(tokens[1]);
        }
        return score;
    }

    public static ArrayList<Element> createNew(ArrayList<Element> elements) {
        ArrayList<Element> newElements = new ArrayList<>();
        for(Element e : elements) {
            newElements.add(createNew(e));
        }
        return newElements;
    }

    public static Element createNew(Element e) {
        Element t = null;
        if(e instanceof EBoolean) {
            t = new EBoolean(e.getTitle(), ((EBoolean) e).getValue());
        }
        else if(e instanceof ECheckbox) {
            t = new ECheckbox(e.getTitle(), ((ECheckbox) e).getValues(), ((ECheckbox) e).getChecked());
        }
        else if(e instanceof ETextfield) {
            t = new ETextfield(e.getTitle(), ((ETextfield) e).getText());
        }
        else if(e instanceof EStopwatch) {
            t = new EStopwatch(e.getTitle(), ((EStopwatch) e).getTime());
        }
        else if(e instanceof ECounter) {
            t = new ECounter(e.getTitle(), ((ECounter) e).getMin(), ((ECounter) e).getMax(), ((ECounter) e).getIncrement(), ((ECounter) e).getCurrent());
        }
        else if(e instanceof EChooser) {
            t = new EChooser(e.getTitle(), ((EChooser) e).getValues(), ((EChooser) e).getSelected());
        }
        else if(e instanceof ESlider) {
            t = new ESlider(e.getTitle(), ((ESlider) e).getMax(), ((ESlider) e).getCurrent());
        } else if(e instanceof ESTextfield) {
            t = new ESTextfield(e.getTitle(), ((ESTextfield) e).isNumberOnly());
        }
        else if(e instanceof EGallery) {
            t = new EGallery(e.getTitle());
        }
        if(t != null) t.setID(e.getID());
        return t;
    }

	public static boolean launchEventPicker(Context context,final EventSelectListener listener) {
        final Dialog d = new Dialog(context);
        d.setTitle("Pick event:");
        d.setContentView(R.layout.event_import_dialog);
        final Spinner spinner = (Spinner) d.findViewById(R.id.type);
        String[] values;
        final REvent[] events = new Loader(context).getEvents();
        if(events == null || events.length == 0) return false;
        values = new String[events.length];
        for(int i = 0; i < values.length; i++) {
            values[i] = events[i].getName();
        }
        ArrayAdapter<String> adp = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, values);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adp);

        Button button = (Button) d.findViewById(R.id.button7);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.eventSelected(events[spinner.getSelectedItemPosition()].getID());
                d.dismiss();
            }
        });
        if(d.getWindow() != null) d.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
        d.show();
        return true;
    }

	// Converts unix millisecond time into a human readable time
	public static String convertTime(long timeMillis) {
        if(timeMillis == 0) return "Never";
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());    
		Date resultdate = new Date(timeMillis);
		return sdf.format(resultdate);

	}

	public static String convertTimeOnly() {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        Date result = new Date(System.currentTimeMillis());
        return sdf.format(result);
    }

	public static void loadAd(AdView adView) {
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

	public static int getAPI() {
		return android.os.Build.VERSION.SDK_INT;
	}

	public static String getDay(int dayOfWeek) {
		return Constants.daysOfWeek[dayOfWeek];
	}
	
	public static String getMonth(int month) {
		return Constants.monthsOfYear[month];
	}

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    public static int generateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public static int DPToPX(Context context, int dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }

    // with whitespace
    public static boolean contains(String string, String query) {
        if(string.equals(query)) return false;
        if(!string.contains(query)) return false;
        else if(string.indexOf(query) == 0 && string.length() > query.length()) return string.charAt(query.length()) == ' ';
        else if(string.indexOf(query) == string.length() - query.length() && string.length() > query.length() ) return string.charAt(string.length() - 1 - query.length()) == ' ';
        query = " " + query + " ";
        return string.contains(query);
    }



}
