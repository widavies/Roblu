package com.cpjd.roblu.teams.statistics;

import com.cpjd.main.TBA;

public class DATThread implements Runnable {

    private final StatsListener listener;
    private final int teamNumber;
    private final Thread thread;

    public DATThread(int teamNumber, StatsListener listener) {
        this.teamNumber = teamNumber;
        this.listener = listener;

        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        listener.retrievedDatabase(new TBA().getTeam(teamNumber));

        try {
            thread.join();
        } catch(Exception e) {
            System.out.println("Failed to stop database thread");
        }
    }

}
