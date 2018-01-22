package com.cpjd.roblu.ui.team;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.cpjd.main.TBA;
import com.cpjd.models.Media;
import com.cpjd.models.Team;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;

/**
 * Downloads team information from TheBlueAlliance.com
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class TBATeamInfoTask implements Runnable {

    private final int teamNumber;
    private final String year;
    private final Thread thread;
    private Context context;

    public interface TBAInfoListener {
        void teamRetrieved(Team team);
        void imageRetrieved(byte[] image);
    }

    private TBAInfoListener listener;

    public TBATeamInfoTask(Context context, int teamNumber, String year, TBAInfoListener listener) {
        this.teamNumber = teamNumber;
        this.listener = listener;
        this.year = year;
        this.context = context;

        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        listener.teamRetrieved(new TBA().getTeam(teamNumber));

        try {
            Media[] medias = new TBA().getMedia(teamNumber, Integer.parseInt(year));
            for(Media media : medias) {
                String url = (String)media.details.get("thumbnail_url");
                if(url == null || url.equals("")) continue;
                Log.d("RBS", "Attempting to download image at URL: "+url);
                Bitmap b = Picasso.with(context).load(url).get();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                b.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                listener.imageRetrieved(byteArray);
                break;
            }
        } catch(Exception e) {
            Log.d("RBS", "Failed to download team picture: "+e.getMessage());
        }

        try {
            thread.join();
        } catch(Exception e) {
            Log.d("RBS", "Failed to stop TBATeamInfoTask thread.");
        }
    }

}

