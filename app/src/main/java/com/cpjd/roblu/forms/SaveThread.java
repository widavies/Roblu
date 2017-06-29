package com.cpjd.roblu.forms;

import android.content.Context;

import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RTeam;

// Saves any changes made to team stuff
public class SaveThread implements Runnable {

    private final Thread thread;
    private final RTeam team;
    private final Context context;
    private final long eventID;

    public SaveThread(Context context, long eventID, RTeam team) {
        this.team = team;
        this.context = context;
        this.eventID = eventID;

        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        new Loader(context).saveTeam(team, eventID);
        try {
            thread.join();
        } catch(Exception e) {
            System.out.println("Failed to stop thread");
        }
    }

}
