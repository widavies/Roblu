package com.cpjd.roblu.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.ui.team.TeamViewer;
import com.cpjd.roblu.utils.Utils;

/**
 * Allows notifications to be sent to the user for various
 * events.
 *
 * @version 2
 * @since 3.6.3
 * @author Will Davies
 */
public class Notify {

    /**
     * Notifies the user that some scouting data has been received.
     * If the user taps on the notification, TeamViewer will open
     *
     * @param context context reference
     * @param eventID the event ID of the active event
     * @param checkout the checkout that was merged
     */
    public static void notifyMerged(Context context, int eventID, RCheckout checkout) {
        Intent intent = new Intent(context, TeamViewer.class);
        intent.putExtra("eventID", eventID);
        intent.putExtra("teamID", checkout.getTeam().getID());
        notify(context, "Scouting data received from "+checkout.getNameTag(),
                checkout.getTeam().getName()+" #"+checkout.getTeam().getNumber()+"\n"+checkout.getTeam().getTabs().get(0).getTitle()+" was merged at "+ Utils.convertTime(checkout.getTime())+"\nTap to view scouting data", intent);
    }

    private static void notify(Context activity, String title, String content, Intent action) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, "roblu-master").setSmallIcon(R.drawable.launcher)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentTitle(title);

        PendingIntent pending = PendingIntent.getActivity(activity, 0, action, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pending);

        NotificationManager notifyMgr = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        if(notifyMgr != null) notifyMgr.notify((int) ((System.currentTimeMillis() / 1000L) % Integer.MAX_VALUE), builder.build());
    }

    public static void notifyNoAction(Context activity, String title, String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, "roblu-master").setSmallIcon(R.drawable.launcher)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentTitle(title);

        NotificationManager notifyMgr = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        if(notifyMgr != null) notifyMgr.notify((int) ((System.currentTimeMillis() / 1000L) % Integer.MAX_VALUE), builder.build());
    }
}
