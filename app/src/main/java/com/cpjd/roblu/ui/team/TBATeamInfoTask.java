package com.cpjd.roblu.ui.team;

import android.util.Log;

import com.cpjd.main.TBA;
import com.cpjd.models.Team;

/**
 * Downloads team information from TheBlueAlliance.com
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class TBATeamInfoTask implements Runnable {

    private final int teamNumber;
    private final Thread thread;

    public interface TBAInfoListener {
        void teamRetrieved(Team team);
    }

    private TBAInfoListener listener;

    public TBATeamInfoTask(int teamNumber, TBAInfoListener listener) {
        this.teamNumber = teamNumber;
        this.listener = listener;

        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        listener.teamRetrieved(new TBA().getTeam(teamNumber));

        try {
            thread.join();
        } catch(Exception e) {
            Log.d("RBS", "Failed to stop TBATeamInfoTask thread.");
        }
    }

}

