package com.cpjd.roblu.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
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
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RBoolean;
import com.cpjd.roblu.models.metrics.RCheckbox;
import com.cpjd.roblu.models.metrics.RChooser;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RSlider;
import com.cpjd.roblu.models.metrics.RStopwatch;
import com.cpjd.roblu.models.metrics.RTextfield;
import com.cpjd.roblu.ui.events.EventDrawerManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Utility library for odds and ends functions.
 *
 * @version 2
 * @since 1.0.0
 * @author Will Davies
 */
public class Utils {

    /**
     * The width of the screen in pixels, used for sizing metrics, mainly in RMetricToUI
     */
    public static int WIDTH;

    /**
     * The width variable is used for controlling a maximum size for certain
     * UI elements. We have to set the width video at startup by reading it
     * from the display.
     * @param activity reference to the main activity
     */
    public static void initWidth(Activity activity) {
        // these lines of code get the width of the phone's screen, in pixels.
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        WIDTH = size.x;
    }

    /**
     * Checks if the device has an active WiFi or Data connection
     * @param context context reference
     * @return true if a connection is available
     */
    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Sets TextInputLayout colors to match the scheme set in RUI. (RUI is the UI model in /models that stores
     * all the UI attributes)
     * @param accent the accent color
     * @param textInputLayout the element to set colors to, layout
     * @param edit the actual edit text element
     */
    public static void setInputTextLayoutColor(final int accent, TextInputLayout textInputLayout, final AppCompatEditText edit) {
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

    /**
     * If you look in a text field, there is usually a vertical line that blinks.
     * This is the CURSOR, and we can change it's color here.
     * @param view The edit text to change the cursor color of
     * @param color the color to change it to
     */
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

    /**
     * Rounds a decimal to the specified number of digits (right of decimal point)
     * @param value the value to round
     * @param precision the amount of digits to keep
     * @return rounded double
     */
    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    /**
     * A snackbar is a neat little UI element, it's that horizontal banner that
     * can display messages at the bottom of the screen
     * @param layout the anchor layout to display the snackbar on
     * @param context context reference
     * @param text the text to display
     * @param error true if this is an error message
     * @param primary the color of the snackbar, if it's not an error message
     */
    public static void showSnackbar(View layout, Context context, String text, boolean error, int primary) {
        Snackbar s = Snackbar.make(layout, text, Snackbar.LENGTH_LONG);
        if(error) s.getView().setBackgroundColor(ContextCompat.getColor(context, R.color.red));
        else s.getView().setBackgroundColor(primary);
        s.show();
    }

    /**
     * We can almost always determine the match key of a match based off
     * it's name.
     * @param matchName the name of the match
     * @return the guessed TBA key for this match
     */
    public static String guessMatchKey(String matchName) {
        matchName = matchName.toLowerCase();
        if(matchName.startsWith("quals")) return "qm"+matchName.split("\\s+")[1];
        else if(matchName.startsWith("quarters")) return "qf"+matchName.split("\\s+")[1]+"m"+matchName.split("\\s+")[3];
        else if(matchName.startsWith("semis")) return "sf"+matchName.split("\\s+")[1] +"m"+matchName.split("\\s+")[3];
        else if(matchName.startsWith("finals")) return "f1"+matchName.split("\\s+")[1];
        return "";
    }

    /**
     * Used for displaying a list of teams in a comma seperated string
     * @param teams teams to concatenate
     * @return a string of concatenated teams string
     */
    public static String concatenateTeams(ArrayList<RTeam> teams) {
        if(teams == null || teams.size() == 0) return "";

        StringBuilder temp = new StringBuilder();
        for(int i = 0; i < teams.size(); i++) {
            if(i != teams.size() - 1) temp.append("#").append(teams.get(i).getNumber()).append(", ");
            else temp.append("#").append(teams.get(i).getNumber());
        }
        return temp.toString();
    }

    /**
     * For certain things, the user may need to select an event from a list of locally stored event,
     * this method does just that! The EventSelectListener method will trigger when an event is successfully
     * selected.
     * @param context context reference
     * @param listener listener to respond to events
     * @return true if some events exist
     */
	public static boolean launchEventPicker(Context context, final EventDrawerManager.EventSelectListener listener) {
        final Dialog d = new Dialog(context);
        d.setTitle("Pick event:");
        d.setContentView(R.layout.event_import_dialog);
        final Spinner spinner = d.findViewById(R.id.type);
        String[] values;
        final REvent[] events = new IO(context).loadEvents();
        if(events == null || events.length == 0) return false;
        values = new String[events.length];
        for(int i = 0; i < values.length; i++) {
            values[i] = events[i].getName();
        }
        ArrayAdapter<String> adp = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, values);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adp);

        Button button = d.findViewById(R.id.button7);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.eventSelected(events[spinner.getSelectedItemPosition()]);
                d.dismiss();
            }
        });
        if(d.getWindow() != null) d.getWindow().getAttributes().windowAnimations = new IO(context).loadSettings().getRui().getAnimation();
        d.show();
        return true;
    }

    /**
     * Time in Roblu is stored as milliseconds, or "Unix" time, this is the amount of time that has passed since
     * January 1, 1970. This is because computers are good at keeping track of simple numbers, but things get a bit more
     * tricky when we have to account for hours, minutes, days, timezones, day light savings, etc. Any who, this method
     * converts those milliseconds into a nice friendly string like "Aug 1, 2017 8:23 am"
     * @param timeMillis the unix time in milliseconds
     * @return the nicely formatted date string
     */
	public static String convertTime(long timeMillis) {
        if(timeMillis == 0) return "Never";
		SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());    
		Date resultDate = new Date(timeMillis);
		return sdf.format(resultDate);

	}


    /**
     * This is actually not my code as cool as an AtomicInteger sounds, essentially, we really like
     * using Android's generateViewId method, but we want to also support sdk 16, so we manually
     * implement this method for differentiating ui elements
     */
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

    /**
     * Converts DP to pixels. Look it up, its hard to explain.
     * @param context application context
     * @param dp the dp to convert
     * @return pixels (converted from dp)
     */
    public static int DPToPX(Context context, int dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }

    /**
     * This is not just any run of the mill "contains" method, it's a contains method that makes our
     * search algorithms a bit better. For example, if we were to search "rush"
     * and we had the local teams "Team RUSH" and "Team CRUSH", Team CRUSH would actually be ranked
     * higher in search results that "Team RUSH". So, this method gives items a bit more relevance
     * points if, like in 'Team RUSH", the query has whitespace before or after it.
     * @param string the string to search
     * @param query the query term
     * @return true if this applies
     */
    public static boolean contains(String string, String query) {
        string = string.toLowerCase();
        query = query.toLowerCase();
        if(string.equals(query)) return false;
        if(!string.contains(query)) return false;
        else if(string.indexOf(query) == 0 && string.length() > query.length()) return string.charAt(query.length()) == ' ';
        else if(string.indexOf(query) == string.length() - query.length() && string.length() > query.length() ) return string.charAt(string.length() - 1 - query.length()) == ' ';
        query = " " + query + " ";
        return string.contains(query);
    }

    public static ArrayList<RMetric> duplicateRMetricArray(ArrayList<RMetric> metrics) {
        if(metrics == null || metrics.size() == 0) return new ArrayList<>();
        ArrayList<RMetric> newMetrics = new ArrayList<>();
        for(RMetric m : metrics) {
            newMetrics.add(m.clone());
        }
        return newMetrics;
    }


    /**
     * Creates an empty form for an event (not technically empty).
     *
     * Makes sure that nothing is null and that we have the required
     * team name and team number fields
     * @return creates an empty RForm
     */
    public static RForm createEmpty() {
        ArrayList<RMetric> pit = new ArrayList<>();
        ArrayList<RMetric> matches = new ArrayList<>();
        RTextfield name = new RTextfield(0, "Team name", false, true, "");
        RTextfield number = new RTextfield(1, "Team number", true, true, "");
        pit.add(name);
        pit.add(number);
        return new RForm(pit, matches);
    }

    /**
     * Requests a UI refresh in teams view.
     *
     * Note: This will reload teams from the disk.
     * @param context a context reference
     */
    public static void requestUIRefresh(Context context) {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.SERVICE_ID);
        context.sendBroadcast(broadcast);
    }

    public static void requestTeamViewerRefresh(Context context, int teamID) {
        Intent broadcast = new Intent();
        broadcast.putExtra("teamViewerOnly", true);
        broadcast.putExtra("teamID", teamID);
        broadcast.setAction(Constants.SERVICE_ID);
        context.sendBroadcast(broadcast);
    }

    /**
     * Updates the server health string in the event drawer
     * @param context a context reference
     * @param status the status to update to
     */
    public static void requestServerHealthRefresh(Context context, String status) {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.SERVICE_ID);
        broadcast.putExtra("serverHealth", status);
        context.sendBroadcast(broadcast);
    }

    /**
     * Checks if the Roblu background service is running
     * @param context context references
     * @return true if the background service is running
     */
    public static boolean isMyServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            String localService = "com.cpjd.roblu.sync.cloud.Service";
            if(localService.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the match titles within an REvent
     * @return a String[] containing ALL match titles within an REvent, may be null
     */
    public static String[] getMatchTitlesWithinEvent(Context context, int eventID) {
        RTeam[] local = new IO(context).loadTeams(eventID);
        // no teams found
        if(local == null || local.length == 0) return null;

        ArrayList<RTab> tabs = new ArrayList<>();

        RForm form = new IO(context).loadForm(eventID);

        for(RTeam team : local) {
            team.verify(form);
            // check if the match already exists
            if(team.getTabs() == null || team.getTabs().size() == 0) continue;
            for(RTab tab : team.getTabs()) {
                if(tab.getTitle().equalsIgnoreCase("pit") || tab.getTitle().equalsIgnoreCase("predictions")) continue;
                boolean found = false;
                for(RTab temp : tabs) {
                    if(temp.getTitle().equalsIgnoreCase(tab.getTitle())) {
                        found = true;
                        break;
                    }
                }
                if(!found) tabs.add(tab);
            }
        }

        if(tabs.size() == 0) return null;

        Collections.sort(tabs);

        // Convert to String[]
        String[] values = new String[tabs.size()];
        for(int i = 0; i < tabs.size(); i++) {
            values[i] = tabs.get(i).getTitle();
        }

        return values;
    }

    /*
     * Array conversion methods
     */
    public static double[] objectListToDoubleArray(ArrayList<String> list) {
        double[] values = new double[list.size()];
        for(int i = 0; i < list.size(); i++) values[i] = Double.parseDouble(list.get(i));
        return values;
    }

    public static String[] stringListToArray(ArrayList<String> list) {
        String[] values = new String[list.size()];
        for(int i = 0; i < list.size(); i++) values[i] = list.get(i);
        return values;
    }

    public static boolean isInLandscapeMode(@NonNull Context context) {
        boolean isLandscape = false;
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
        }
        return isLandscape;
    }

    public static int getScreenWidth(@NonNull Context context) {
        Point size = new Point();
        ((Activity) context).getWindowManager().getDefaultDisplay().getSize(size);
        return size.x;
    }

    /**
     * This code will randomize an event with random metric data for the purposes of show casing an app
     * or testing
     */
    public static void randomizeTeamMetrics(ArrayList<RTab> tabs) {
        Random r = new Random();

        if(tabs != null) {
            for(RTab tab : tabs) {
                for(RMetric metric : tab.getMetrics()) {
                    metric.setModified(true);

                    if(metric instanceof RSlider) {
                        ((RSlider) metric).setMax(100);
                        ((RSlider) metric).setValue(r.nextInt(100));
                    }
                    else if(metric instanceof RCounter) {
                        ((RCounter) metric).setValue(r.nextDouble() * 100);
                    }
                    else if(metric instanceof RStopwatch) {
                        ((RStopwatch) metric).setTime(r.nextDouble() * 10);
                        ((RStopwatch) metric).setTimes(new ArrayList<Double>());
                        for(int i = 0; i < r.nextInt(5); i++) {
                            ((RStopwatch) metric).getTimes().add(Utils.round(r.nextDouble() * 8.2, 2));
                        }
                    }
                    else if(metric instanceof RBoolean) {
                        ((RBoolean) metric).setValue(r.nextDouble() <= 0.5);
                    }
                    else if(metric instanceof RCheckbox) {
                        for(Object o : ((RCheckbox) metric).getValues().keySet()) {
                            ((RCheckbox) metric).getValues().put(o.toString(), r.nextDouble() <= 0.50);
                        }
                    }
                    else if(metric instanceof RChooser) {
                        ((RChooser) metric).setSelectedIndex(r.nextInt(((RChooser) metric).getValues().length - 1));
                    }
                    else if(metric instanceof RTextfield) {
                        if(!((RTextfield) metric).isOneLine()) ((RTextfield) metric).setText("RTextfield has been randomized to: "+getSaltString());
                    }
                }
            }
        }
    }

    private static String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 200) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }
}

