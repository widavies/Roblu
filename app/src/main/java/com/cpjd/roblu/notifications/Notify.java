package com.cpjd.roblu.notifications;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.cpjd.roblu.R;
import com.cpjd.roblu.teams.TeamsView;


/*
 * Notifications are used in Roblu Master to notify the user that one of the following has happened:
 *
 * -A scouter uploaded a completed assignment
 * - That's it.
 *
 * We need a background service as well that does the following:
 * - Checks for completed assignments
 */

public class Notify {
    public static void notify(Activity activity, String title, String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity).setSmallIcon(R.drawable.launcher)
                .setContentTitle(title).setContentText(content);

        Intent result = new Intent(activity, TeamsView.class);
        PendingIntent pending = PendingIntent.getActivity(activity, 0, result, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pending);

        NotificationManager notifyMgr = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        notifyMgr.notify(001, builder.build());
    }
}
