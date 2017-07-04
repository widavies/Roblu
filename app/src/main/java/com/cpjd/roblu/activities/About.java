package com.cpjd.roblu.activities;

import android.app.Activity;
import android.os.Bundle;

import com.cpjd.roblu.R;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

/**
 * Displays information about the libraries that Roblu uses.
 * Gotta give credit where credit is due!
 */
public class About extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        new LibsBuilder().withFields(R.string.class.getFields()).withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR).withAboutDescription("Copyright 2017. A scouting app for robotics competitions focused on customization, simplicity, and functionality. Roblu is an open source" +
                " project designed to streamline your scouting exerpience. Thank you to Andy Pethan and Isaac Faulkner for all the help. App written by Will Davies.")
                .withActivityTitle("About Roblu").withLicenseShown(true).
                start(this);
        finish();
	}
}
