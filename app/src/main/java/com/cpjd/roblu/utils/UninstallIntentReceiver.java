package com.cpjd.roblu.utils;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * Detects if the user is trying to uninstall the ap
 *
 * @since 3.6.1
 * @author Will Davies
 */

public class UninstallIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // fetching package names from extras
        String[] packageNames = intent.getStringArrayExtra("android.intent.extra.PACKAGES");

        if(packageNames!=null){
            for(String packageName: packageNames){
                if(packageName!=null && packageName.equals("com.cpjd.roblu")){
                    // User has selected our application under the Manage Apps settings
                    // now initiating background thread to watch for activity
                    new ListenActivities(context).start();

                }
            }
        }
    }

    class ListenActivities extends Thread{
        boolean exit = false;
        ActivityManager am = null;
        Context context = null;

        public ListenActivities(Context con){
            context = con;
            am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        }

        public void run(){

            Looper.prepare();

            while(!exit){

                // get the info from the currently running task
                List< ActivityManager.RunningTaskInfo > taskInfo = am.getRunningTasks(MAX_PRIORITY);

                String activityName = taskInfo.get(0).topActivity.getClassName();


                Log.d("topActivity", "CURRENT Activity ::"
                        + activityName);

                if (activityName.equals("com.android.packageinstaller.UninstallerActivity")) {
                    // User has clicked on the Uninstall button under the Manage Apps settings

                    //do whatever pre-uninstallation task you want to perform here
                    // show dialogue or start another activity or database operations etc..etc..

                    // context.startActivity(new Intent(context, MyPreUninstallationMsgActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    exit = true;



                    Toast.makeText(context, "Done with preuninstallation tasks... Exiting Now", Toast.LENGTH_SHORT).show();
                } else if(activityName.equals("com.android.settings.ManageApplications")) {
                    // back button was pressed and the user has been taken back to Manage Applications window
                    // we should close the activity monitoring now
                    exit=true;
                }
            }
            Looper.loop();
        }
    }

}
