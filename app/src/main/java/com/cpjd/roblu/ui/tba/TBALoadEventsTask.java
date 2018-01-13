package com.cpjd.roblu.ui.tba;

/**
 * Created by Will Davies on 1/13/2018.
 */

import android.os.AsyncTask;
import android.os.StrictMode;

import com.cpjd.main.Settings;
import com.cpjd.main.TBA;
import com.cpjd.models.Event;
import com.cpjd.roblu.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import lombok.Data;

/**
 * Fetch events will download the requested list of events from TBA.com,
 * these events will be set to TBAEventSelector.events.
 */
@Data
public class TBALoadEventsTask extends AsyncTask<Void, Void, Void> {
    /**
     * Contains a list of downloaded TBA events, this array stored downloaded content so that it can be searched without
     * re-downloading it
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
     * If events should be re-downloaded
     */
    private boolean reload;
    /**
     * A search query
     */
    private String query;

    public interface LoadTBAEventsListener {
        /**
         * Called when the event list is successfully downloaded or searched/updated,
         * this Event array should be sent directly to the TBAEventAdapter
         * @see TBAEventAdapter
         * @param events the array to set to the UI, managed by TBAEventAdapter
         */
        void eventListUpdated(ArrayList<Event> events);
        /**
         * Called when a general error happens while attempting to pull events
         */
        void errorOccurred(String errMsg);

        /**
         * Called when a specific event is downloaded, with sub-teams, sub-matches, and scores, etc.
         * @param event the downloaded event
         */
        void eventDownloaded(Event event);
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
        if(reload) {
            try {
                Event[] events;
                if(onlyShowMyEvents) events = new TBA().getEvents(year, false);
                else events = new TBA().getTeamEvents(teamNumber, year, false);
                /*
                 * Clean up the downloaded data a bit
                 */
                for(Event e : events) {
                    while(e.name.startsWith(" ")) e.name = e.name.substring(1);
                    e.start_date = Integer.parseInt(e.start_date.split("-")[1])+"/"+Integer.parseInt(e.start_date.split("-")[2])+"/"+e.start_date.split("-")[0];
                }
                this.events.clear();
                this.events.addAll(Arrays.asList(events));
                listener.eventListUpdated(this.events);
            } catch(Exception e) {
                listener.errorOccurred("An error occurred while accessing TheBlueAlliance.com");
            }
        }

        /*
         * Now, perform searching if necessary
         */
        if(query != null && !query.equals("")) {
            ArrayList<Event> searched = new ArrayList<>();
            for(Event e : events) {
                e.relevance = 0;
                if(e.name.toLowerCase().equals(query)) e.relevance += 500;
                if(e.name.toLowerCase().contains(query)) e.relevance += 200;
                if(Utils.contains(e.name.toLowerCase(), query)) e.relevance += 400;
                if(e.start_date.toLowerCase().contains(query)) e.relevance += 200;
                if(e.start_date.toLowerCase().equals(query)) e.relevance += 500;
                if(Utils.contains(e.start_date.toLowerCase(), query)) e.relevance += 400;
                if(e.location.toLowerCase().equals(query)) e.relevance += 500;
                if(e.location.toLowerCase().contains(query)) e.relevance += 200;
                if(Utils.contains(e.location.toLowerCase(), query)) e.relevance += 400;
                if(e.relevance != 0) searched.add(e);
            }
            Collections.sort(searched);
            Collections.reverse(searched);
            listener.eventListUpdated(searched);
        }
        return null;
    }
}
