package com.cpjd.roblu.ui.events;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.LayoutRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.sync.cloud.ui.Mailbox;
import com.cpjd.roblu.ui.forms.EditForm;
import com.cpjd.roblu.ui.settings.AdvSettings;
import com.cpjd.roblu.ui.tutorials.Tutorial;
import com.cpjd.roblu.utils.Constants;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.AbstractDrawerItem;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Helps reduce the amount of code in TeamsView by managing the event drawer and its contents
 */
public class EventDrawerManager implements Drawer.OnDrawerItemClickListener {

    /**
     * A reference to the TeamsView activity
     */
    private Activity activity;

    /**
     * The event drawer that should be managed
     */
    private Drawer eventDrawer;

    /**
     * A list of ALL teams that Roblu Master can find, this array is normally sorted by ID, so no changes need
     * to be made to it
     */
    private ArrayList<REvent> events;

    /**
     * Stores a reference to the currently active event
     */
    @Getter
    @Setter
    private REvent event;

    /**
     * Defines the color and UI preferences
     */
    private RUI rui;

    public interface EventSelectListener {
        void eventSelected();
    }

    /**
     * This listener will report when an event has been selected
     */
    private EventSelectListener listener;

    /**
     * Instantiates an EventDrawerManager that will handle loading and receiving actions from the REvent ui drawer
     * @param activity the TeamsView activity reference
     * @param toolbar the TeamsView toolbar (the UI drawer will insert a hamburger button)
     * @param listener an EventSelectListener that will be called when the TeamsView activity should reload the teams list
     */
    public EventDrawerManager(Activity activity, Toolbar toolbar, EventSelectListener listener) {
        this.activity = activity;
        this.listener = listener;

        // Load dependencies objects
        rui = new IO(activity).loadSettings().getRui();

        /*
         * Load the various drawable icons for the drawers
         */
        Drawable create, masterForm, tutorials, settings, cloud;
        create = ContextCompat.getDrawable(activity, R.drawable.create);
        masterForm = ContextCompat.getDrawable(activity, R.drawable.master);
        tutorials = ContextCompat.getDrawable(activity, R.drawable.school);
        settings = ContextCompat.getDrawable(activity, R.drawable.settings_circle);
        cloud = ContextCompat.getDrawable(activity, R.drawable.cloud);
        // Configure them to match the UI preferences
        create.mutate(); create.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        masterForm.mutate(); masterForm.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        tutorials.mutate(); tutorials.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        settings.mutate(); settings.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        cloud.mutate(); cloud.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);

        /*
         * Specify all the different items that will be contained in our drawer
         */
        ArrayList<IDrawerItem> items = new ArrayList<>();
        items.add(new PrimaryDrawerItem().withIdentifier(Constants.CREATE_EVENT).withName("Create event").withIcon(create).withTextColor(rui.getText()));
        items.add(new DividerDrawerItem(rui.getText()));
        items.add(new SectionDrawerItem().withName("Events").withTextColor(rui.getText()).withDivider(false));
        items.add(new DividerDrawerItem(rui.getText()));
        items.add(new SecondaryDrawerItem().withTextColor(rui.getText()).withIdentifier(Constants.EDIT_MASTER_FORM).withName("Edit master form").withIcon(masterForm));
        items.add(new SecondaryDrawerItem().withTextColor(rui.getText()).withIdentifier(Constants.TUTORIALS).withName("Tutorials").withIcon(tutorials));
        items.add(new SecondaryDrawerItem().withTextColor(rui.getText()).withIdentifier(Constants.SETTINGS).withName("Settings").withIcon(settings));

        /*
         * Create the drawer
         */
        eventDrawer = new DrawerBuilder()
                .withActivity(activity)
                .withToolbar(toolbar)
                .withDrawerItems(items)
                .withSelectedItem(-1)
                .withActionBarDrawerToggleAnimated(true)
                .withTranslucentStatusBar(false)
                .withSliderBackgroundColor(rui.getBackground())
                .withOnDrawerItemClickListener(this).build();

        // Set the hamburger button to the UI preferences
        if(eventDrawer.getActionBarDrawerToggle() != null) {
            eventDrawer.getActionBarDrawerToggle().getDrawerArrowDrawable().setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_ATOP);
            eventDrawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
        }

        loadEventsToDrawer();
    }

    /**
     * This method is called when one of the items in the drawer is clicked
     * @param view the view that's been clicked
     * @param position its vertical position index
     * @param drawerItem the drawerItem object reference
     * @return true if the event is consumed
     */
    @Override
    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
        long identifier = drawerItem.getIdentifier();

        if(identifier == Constants.CREATE_EVENT) {
            activity.startActivityForResult(new Intent(activity, CreateEventPicker.class), Constants.CREATE_EVENT_PICKER);
            eventDrawer.setSelectionAtPosition(-1);
        }
        else if(identifier == Constants.SETTINGS) {
            activity.startActivityForResult(new Intent(activity, AdvSettings.class), Constants.GENERAL);
            eventDrawer.setSelectionAtPosition(-1);
        }
        else if(identifier == Constants.SCOUT) {
            selectEvent((int)drawerItem.getTag());
        }
        else if(identifier == Constants.TUTORIALS) {
            activity.startActivity(new Intent(activity, Tutorial.class));
            eventDrawer.setSelectionAtPosition(-1);
        }
        else if(identifier == Constants.EVENT_SETTINGS) {
            for(int i = 0; i < events.size(); i++) {
                if(events.get(i).getID() == (Long)drawerItem.getTag()) {
                    Intent intent = new Intent(activity, EventSettings.class);
                    intent.putExtra("event", events.get(i));
                    activity.startActivityForResult(intent, Constants.GENERAL);
                    eventDrawer.setSelectionAtPosition(-1);
                    break;
                }
            }
        }
        else if(identifier == Constants.EDIT_MASTER_FORM) {
            Intent start = new Intent(activity, EditForm.class);
            start.putExtra("master", true);
            activity.startActivityForResult(start, Constants.MASTER_FORM);
            eventDrawer.setSelectionAtPosition(-1);
        }
        else if(identifier == Constants.MAILBOX) {
            for(int i = 0; i < events.size(); i++) {
                if(events.get(i).getID() == (Long)drawerItem.getTag()) {
                    Intent intent = new Intent(activity, Mailbox.class);
                    intent.putExtra("eventID", events.get(i).getID());
                    activity.startActivityForResult(intent, Constants.GENERAL);
                    eventDrawer.setSelectionAtPosition(-1);
                    break;
                }
            }
        }
        eventDrawer.closeDrawer();
        return true;
    }

    /**
     * Selects an event and flags the main activity to start loading
     * teams and updating the UI appropriately
     * @param ID the ID of the event to set as active
     */
    public void selectEvent(int ID) {
        if(events == null || events.size() == 0) return;

        for(REvent e : events) {
            if(e.getID() == ID) {
                event = e;
                // Update the action bar title, it will get updated again when the teams are loaded by the LoadTeamsTask AsyncTask
                if(((AppCompatActivity)activity).getSupportActionBar() != null) {
                    ((AppCompatActivity)activity).getSupportActionBar().setTitle(event.getName());
                    ((AppCompatActivity)activity).getSupportActionBar().setSubtitle("Teams");
                }
                // Save the event the user just selected to settings so Roblu can remember where they left off when the app is relaunched
                RSettings settings = new IO(activity).loadSettings();
                settings.setLastEventID(ID);
                new IO(activity).saveSettings(settings);

                // Tell the main activity to start loading the teams
                listener.eventSelected(); // we'll provide a REvent reference for convenience, but TeamsView still has access to it with a getter
                return;
            }
        }
    }

    /**
     * Loads events from the file system into the event drawer.
     * Note: loadEvents() must be called after the drawer UI is setup, it will insert
     * REvents into the pre-created UI drawer
     */
    public void loadEventsToDrawer() {
        /*
         * Load events
         */
        REvent[] loaded = new IO(activity).loadEvents();
        if(loaded == null) {
            if(((AppCompatActivity)activity).getSupportActionBar() != null) ((AppCompatActivity)activity).getSupportActionBar().setSubtitle("No events");
            return;
        }
        // Set loaded events to the managed array-list
        events = new ArrayList<>(Arrays.asList(loaded));

        // Sort descending by event ID, most recently created event will appear first
        Collections.sort(events);
        Collections.reverse(events);

        // Load icons
        Drawable folder, scout, options, mail;
        folder = ContextCompat.getDrawable(activity, R.drawable.event);
        scout = ContextCompat.getDrawable(activity, R.drawable.match);
        options = ContextCompat.getDrawable(activity, R.drawable.settings_circle);
        mail = ContextCompat.getDrawable(activity, R.drawable.mail);
        // Set UI preferences to drawable icon
        folder.mutate(); folder.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        scout.mutate(); scout.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        options.mutate(); options.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);
        mail.mutate(); mail.setColorFilter(rui.getText(), PorterDuff.Mode.SRC_IN);

        // Specify the list of items that have to be added to the drawer
        ArrayList<IDrawerItem> items = new ArrayList<>();
        if(events != null) for(REvent e : events) {
            if(e.isCloudEnabled()) {
                items.add(new ExpandableDrawerItem().withTextColor(rui.getText()).withName(e.getName()).withTag(e.getID()).withArrowColor(rui.getText()).withIcon(folder).withIdentifier(Constants.HEADER).withSelectable(false).withSubItems(
                        new SecondaryDrawerItem().withTextColor(rui.getText()).withName("Scout").withLevel(2).withIcon(scout).withIdentifier(Constants.SCOUT).withTag(e.getID()),
                        new SecondaryDrawerItem().withTextColor(rui.getText()).withName("Mailbox").withLevel(2).withIcon(mail).withIdentifier(Constants.MAILBOX).withTag(e.getID()),
                        new SecondaryDrawerItem().withTextColor(rui.getText()).withName("Settings").withLevel(2).withIcon(options).withIdentifier(Constants.EVENT_SETTINGS).withTag(e.getID()))
                );
            } else {
                items.add(new ExpandableDrawerItem().withTextColor(rui.getText()).withName(e.getName()).withTag(e.getID()).withArrowColor(rui.getText()).withIcon(folder).withIdentifier(Constants.HEADER).withSelectable(false).withSubItems(
                        new SecondaryDrawerItem().withTextColor(rui.getText()).withName("Scout").withLevel(2).withIcon(scout).withIdentifier(Constants.SCOUT).withTag(e.getID()),
                        new SecondaryDrawerItem().withTextColor(rui.getText()).withName("Settings").withLevel(2).withIcon(options).withIdentifier(Constants.EVENT_SETTINGS).withTag(e.getID()))
                );
            }
        }

        // Clear old events from the drawer
        for(int i = 0; i < eventDrawer.getDrawerItems().size(); i++) {
            long identifier = eventDrawer.getDrawerItems().get(i).getIdentifier();
            if(identifier == Constants.HEADER || identifier == Constants.SCOUT || identifier == Constants.EVENT_SETTINGS || identifier == Constants.MAILBOX) {
                eventDrawer.removeItemByPosition(i);
                i = 0;
            }
        }

        // Set defined list of items to drawer UI
        for(int i = 0; i < items.size(); i++) eventDrawer.addItemAtPosition(items.get(i), i + 3);
    }

    /**
     * This is only down here so that it isn't wasting space some where else
     */
    public class DividerDrawerItem extends AbstractDrawerItem<EventDrawerManager.DividerDrawerItem, EventDrawerManager.DividerDrawerItem.ViewHolder> {

        private final int color;

        DividerDrawerItem(int color) {
            this.color = color;
        }

        @Override
        public int getType() {
            return R.id.material_drawer_item_divider;
        }

        @Override
        @LayoutRes
        public int getLayoutRes() {
            return R.layout.material_drawer_item_divider;
        }

        @Override
        public void bindView(EventDrawerManager.DividerDrawerItem.ViewHolder viewHolder, List payloads) {
            super.bindView(viewHolder, payloads);

            //set the identifier from the drawerItem here. It can be used to run tests
            viewHolder.itemView.setId(hashCode());

            //define how the divider should look like
            viewHolder.view.setClickable(false);
            viewHolder.view.setEnabled(false);
            viewHolder.view.setMinimumHeight(1);
            ViewCompat.setImportantForAccessibility(viewHolder.view,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);

            //set the color for the divider
            viewHolder.divider.setBackgroundColor(color);

            //call the onPostBindView method to trigger post bind view actions (like the listener to modify the item if required)
            onPostBindView(this, viewHolder.itemView);
        }

        @Override
        public EventDrawerManager.DividerDrawerItem.ViewHolder getViewHolder(View v) {
            return new EventDrawerManager.DividerDrawerItem.ViewHolder(v);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public final View view;
            public final View divider;

            private ViewHolder(View view) {
                super(view);
                this.view = view;
                this.divider = view.findViewById(R.id.material_drawer_divider);
            }
        }

    }

}
