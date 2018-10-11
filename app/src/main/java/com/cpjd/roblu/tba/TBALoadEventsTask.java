package com.cpjd.roblu.tba;

import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.cpjd.main.TBA;
import com.cpjd.models.standard.Event;
import com.cpjd.models.standard.Match;
import com.cpjd.models.standard.Team;
import com.cpjd.roblu.ui.tba.TBAEventAdapter;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
     * The year (e.g. "field2018") to get events from
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

    // A cloned version of the events array that events can be removed from with no big issue
    private ArrayList<Event> cloned;

    public interface LoadTBAEventsListener {
        /**
         * Called when a general error happens while attempting to pull events
         */
        void errorOccurred(String errMsg);

        /**
         * Called when a specific event is downloaded, with sub-teams, sub-matches, and scores, etc.
         * @param event the downloaded event
         */
        void eventDownloaded(Event event, Team[] teams, Match[] matches);
        /**
         * Called when this thread downloads a list of events, this interface method will tell TBAEventSelector to hold onto these
         * events for us until later
         * @param events array of events that should be saved to minimize events downloading
         */
        void eventListDownloaded(ArrayList<Event> events);
    }

    public TBALoadEventsTask(ProgressBar progressBar, RecyclerView rv, TBAEventAdapter eventAdapter, LoadTBAEventsListener listener, int teamNumber, int year, boolean onlyShowMyEvents) {
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

        // Set auth token
        TBA.setAuthToken(Constants.PUBLIC_TBA_READ_KEY);

        /*
         * This disables any sub-data returned with the events, we only want the event data!
         * Downloading sub-teams for each event would take a while.
         */
        //Settings.disableAll();
        /*
         * Now let's use the AMAZING TBA-API features to download the events, you
         * should check out the TBA-API developer, he's great.
         */
        if(events == null || events.size() == 0) {
            try {
                Event[] events;
                if(onlyShowMyEvents) events = new TBA().getEvents(teamNumber, year);
                else events = new TBA().getEvents(year);
                /*
                 * Clean up the downloaded data a bit
                 */
                for(Event e : events) {
                    while(e.getName().startsWith(" ")) e.setName(e.getName().substring(1));
                }
                this.events = new ArrayList<>();
                Collections.addAll(this.events, events);
                listener.eventListDownloaded(this.events);
            } catch(Exception e) {
                Log.d("RBS", "Error: "+e.getMessage());
                if(onlyShowMyEvents && teamNumber == 0) listener.errorOccurred("Error occurred downloading events list. Is your team number properly defined in Roblu settings?");
                else listener.errorOccurred("An error occurred while accessing TheBlueAlliance.com");
            }
        }

        cloned = new ArrayList<>(this.events);

        /*
         * Now, perform searching if necessary
         */
        if(!query.equals("")) {
            // Remove teams with no relevance
            for(int i = 0; i < cloned.size(); i++) {
                if(calculateRelevance(cloned.get(i)) == -1) {
                    cloned.remove(i);
                    i--;
                }
            }

            Collections.sort(cloned, new Comparator<Event>() {
                @Override
                public int compare(Event o1, Event o2) {
                    return Integer.compare(calculateRelevance(o1), calculateRelevance(o2));
                }
            });
            Collections.reverse(cloned);
        }
        /*
         * Sort by date!
         */
        else {
            Collections.sort(cloned);
        }

        return null;
    }

    private int calculateRelevance(Event e) {
        int relevance = -1;
        
        if(e.getName().toLowerCase().equals(query)) relevance += 5;
        if(e.getName().toLowerCase().contains(query)) relevance += 2;
        if(Utils.contains(e.getName().toLowerCase(), query)) relevance += 4;
        if(e.getStartDate().toLowerCase().contains(query)) relevance += 2;
        if(e.getStartDate().toLowerCase().equals(query)) relevance += 5;
        if(Utils.contains(e.getName().toLowerCase(), query)) relevance += 4;
        if(e.getLocationName() != null) {
            if(e.getLocationName().toLowerCase().equals(query)) relevance += 5;
            if(e.getLocationName().toLowerCase().contains(query)) relevance += 2;
            if(Utils.contains(e.getLocationName().toLowerCase(), query)) relevance += 4;
        }

        return relevance;
    }
    
    @Override
    protected void onPostExecute(Void params) {
        tbaEventAdapterWeakReference.get().setEvents(cloned);
        progressBarWeakReference.get().setVisibility(View.INVISIBLE);
        recyclerViewWeakReference.get().setVisibility(View.VISIBLE);
    }
}
