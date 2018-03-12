package com.cpjd.roblu.ui.tutorials;


import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;

import com.cpjd.roblu.R;
import com.cpjd.roblu.ui.UIHandler;

import java.util.ArrayList;

/**
 * Tutorial is a simply utility to display a list of tutorials for Roblu.
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class Tutorial extends AppCompatActivity implements TutorialsRecyclerAdapter.TutorialListener {

    private RecyclerView rv;
    private ArrayList<RTutorial> tutorials;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        /*
         * Setup UI
         */
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Tutorials");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // List of tutorials
        tutorials = new ArrayList<>();
        tutorials.add(new RTutorial("Text based", "View the Roblu text based tutorial", "https://docs.google.com/document/d/1DqpgKPdtfZDUc7Zu3MqdJHL59aB-ht8H1ZwllYlzMuc/edit?usp=sharing"));
        tutorials.add(new RTutorial("The basics", "Roblu's mission, description of platforms, terms, etc.", "9j6ysvJJyQg"));
        tutorials.add(new RTutorial("Events", "Learn how to create, manage, backup, organize, and export events", "6BLlLxltppk"));
        tutorials.add(new RTutorial("Forms", "Learn how to create, manage, edit, organize, master form", "LpWvnavebNw"));
        tutorials.add(new RTutorial("QR Codes", "Learn how to use QR code syncing", "RF4evYIlU04"));
        tutorials.add(new RTutorial("CSV Exporting", "Learn about CSV exporting and defining your own CSV export schemes", "RF4evYIlU04"));
        tutorials.add(new RTutorial("How to get Roblu Cloud for free", "Get Roblu Cloud for free", "dQw4w9WgXcQ"));
        tutorials.add(new RTutorial("Roblu Devlogs", "For those interested in watching the development process", ""));

        /*
         * Add the tutorials to the recycler view
         */
        rv = findViewById(R.id.teams_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        TutorialsRecyclerAdapter adapter = new TutorialsRecyclerAdapter(this, this, tutorials);
        rv.setAdapter(adapter);

        // Gesture listener
        ItemTouchHelper.Callback callback = new TutorialsRecyclerTouchHelper();
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

        // Sync UI with settings
        new UIHandler(this, toolbar).update();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    /**
     * Opens a YouTube video in the app, if no app is found, then the web browser
     * @param id the ID of the YouTube video
     */
    void watchYoutubeVideo(String id){
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://www.youtube.com/watch?v=" + id));
        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            startActivity(webIntent);
        }
    }

    /**
     * This method is called when the user selects a tutorial,
     * tutorial at position==tutorials.size() - 1 is the devlog selector and will
     * go to a YouTube playlist instead of a video
     * @param v the view that was selected
     */
    @Override
    public void tutorialSelected(View v) {
        int position = rv.getChildLayoutPosition(v);
        if(position == tutorials.size() - 1) {
            String url = "https://www.youtube.com/playlist?list=PLjv2hkWcHVGZAlplguiS4rR_45-KLS28a";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
        else if(position == 0) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(tutorials.get(position).getYoutubeID()));
            startActivity(i);
        }
        else {
            watchYoutubeVideo(tutorials.get(position).getYoutubeID());
        }
    }
}
