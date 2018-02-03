package com.cpjd.roblu.notifications;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.cpjd.roblu.R;


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
    public static void notify(Context activity, String title, String content) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, "roblu-master").setSmallIcon(R.drawable.launcher)
                .setContentTitle(title).setContentText(content);

        //Intent result = new Intent(activity, Mailbox.class);
        //PendingIntent pending = PendingIntent.getActivity(activity, 0, result, PendingIntent.FLAG_UPDATE_CURRENT);
        //builder.setContentIntent(pending);

        NotificationManager notifyMgr = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        if(notifyMgr != null) notifyMgr.notify((int) ((System.currentTimeMillis() / 1000L) % Integer.MAX_VALUE), builder.build());
    }
}
