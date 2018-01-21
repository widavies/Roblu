package com.cpjd.roblu.ui.tba;

import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

import com.cpjd.main.Settings;
import com.cpjd.main.TBA;
import com.cpjd.models.Event;
import com.cpjd.roblu.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Fetch events will download the requested list of events from TBA.com,
 * these events will be set to TBAEventSelector.events.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TBALoadEventsTask extends AsyncTask<Void, Void, Void> {
    /**
     * If reload is false, the user can pass in a ArrayList of events that can be processed instead of downloading them again
     */
    private ArrayList<Event> events;
    /**
     * If true, LoadEventsTask will only download events that contain the team specified
     * by teamNumber below.
     */
    private boolean onlyShowMyEvents;
    /**
     * The team number to find events for if onlyShowMyEvents is true
     */
    private int teamNumber;
    /**
     * The year (e.g. "2018") to get events from
     */
    private int year;
    /**
     * A search query
     */
    private String query;
    /**
     * Stores a reference to the event adapter so new events can be added to it
     */
    private WeakReference<TBAEventAdapter> tbaEventAdapterWeakReference;
    /**
     * Stores a reference to the progress bar so it can be hidden again when this task finishes
     */
    private WeakReference<ProgressBar> progressBarWeakReference;
    /**
     * Stores a reference to the recycler view so it can be displayed again when this task finishes
     */
    private WeakReference<RecyclerView> recyclerViewWeakReference;

    public interface LoadTBAEventsListener {
        /**
         * Called when a general error happens while attempting to pull events
         */
        void errorOccurred(String errMsg);

        /**
         * Called when a specific event is downloaded, with sub-teams, sub-matches, and scores, etc.
         * @param event the downloaded event
         */
        void eventDownloaded(Event event);
        /**
         * Called when this thread downloads a list of events, this interface method will tell TBAEventSelector to hold onto these
         * events for us until later
         * @param events array of events that should be saved to minimize events downloading
         */
        void eventListDownloaded(ArrayList<Event> events);
    }

    TBALoadEventsTask(ProgressBar progressBar, RecyclerView rv, TBAEventAdapter eventAdapter, LoadTBAEventsListener listener, int teamNumber, int year, boolean onlyShowMyEvents) {
        this.tbaEventAdapterWeakReference = new WeakReference<>(eventAdapter);
        this.progressBarWeakReference = new WeakReference<>(progressBar);
        this.recyclerViewWeakReference = new WeakReference<>(rv);
        this.listener = listener;
        this.teamNumber = teamNumber;
        this.year = year;
        this.onlyShowMyEvents = onlyShowMyEvents;
        this.query = "";
    }

    /**
     * This listener will be notified when certain API events happen
     */
    private LoadTBAEventsListener listener;

    /**
     * Executes the task
     * @param params no parameters should all be set by the getters and setters for the LoadEventsTask variables
     * @return null (listener will receive the data)
     */
    @Override
    protected Void doInBackground(Void... params) {
        if(events == null) events = new ArrayList<>();

        /*
         * We need to make sure that this thread has access to the internet
         */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);
        /*
         * This disables any sub-data returned with the events, we only want the event data!
         * Downloading sub-teams for each event would take a while.
         */
        Settings.disableAll();
        /*
         * Now let's use the AMAZING TBA-API features to download the events, you
         * should check out the TBA-API developer, he's great.
         */
        if(events == null || events.size() == 0) {
            try {
                Event[] events;
                if(onlyShowMyEvents) events = new TBA().getTeamEvents(teamNumber, year, false);
                else events = new TBA().getEvents(year, false);
                /*
                 * Clean up the downloaded data a bit
                 */
                for(Event e : events) {
                    while(e.name.startsWith(" ")) e.name = e.name.substring(1);
                    e.start_date = Integer.parseInt(e.start_date.split("-")[1])+"/"+Integer.parseInt(e.start_date.split("-")[2])+"/"+e.start_date.split("-")[0];
                }
                this.events = new ArrayList<>();
                Collections.addAll(this.events, events);
                listener.eventListDownloaded(this.events);
            } catch(Exception e) {
                e.printStackTrace();
                if(onlyShowMyEvents && teamNumber == 0) listener.errorOccurred("Error occurred downloading events list. Is your team number properly defined in Roblu settings?");
                else listener.errorOccurred("An error occurred while accessing TheBlueAlliance.com");
            }
        }

        /*
         * Now, perform searching if necessary
         */
        if(!query.equals("")) {
            for(Event e : this.events) {
                /*
                 * Because of the janky API sort method, relevance can NEVER be zero (otherwise the event will get sorted by date),
                 * so make sure that zero is not possible!
                 */
                e.relevance = -1;
                /*
                 * It's tempting to use if statements here, don't! They are INDIVIDUAL search criteria scores
                 */
                if(e.name.toLowerCase().equals(query)) e.relevance += 5;
                if(e.name.toLowerCase().contains(query)) e.relevance += 2;
                if(Utils.contains(e.name.toLowerCase(), query)) e.relevance += 4;
                if(e.start_date.toLowerCase().contains(query)) e.relevance += 2;
                if(e.start_date.toLowerCase().equals(query)) e.relevance += 5;
                if(Utils.contains(e.start_date.toLowerCase(), query)) e.relevance += 4;
                if(e.location.toLowerCase().equals(query)) e.relevance += 5;
                if(e.location.toLowerCase().contains(query)) e.relevance += 2;
                if(Utils.contains(e.location.toLowerCase(), query)) e.relevance += 4;
            }

            Collections.sort(this.events);
            Collections.reverse(this.events);
        }
        /*
         * Sort by date!
         */
        else {
            // make sure to reset old relevance (from past searches)
            for(Event e : this.events) e.relevance = 0;

            Collections.sort(this.events);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void params) {
        tbaEventAdapterWeakReference.get().setEvents(this.events, !query.equals(""));
        progressBarWeakReference.get().setVisibility(View.INVISIBLE);
        recyclerViewWeakReference.get().setVisibility(View.VISIBLE);
    }
}
