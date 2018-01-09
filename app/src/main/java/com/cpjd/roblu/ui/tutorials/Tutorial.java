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
import com.cpjd.roblu.ui.teams.customsort.SelectListener;
import com.cpjd.roblu.ui.UIHandler;

import java.util.LinkedList;

public class Tutorial extends AppCompatActivity implements SelectListener{

    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("Tutorials");
        if(getSupportActionBar() != null) getSupportActionBar().setSubtitle("Tutorials for Roblu Master");

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // List of tutorials
        LinkedList<RTutorial> tuts = new LinkedList<>();
        tuts.add(new RTutorial("The basics", "Roblu's mission, description of platforms, terms, etc."));
        tuts.add(new RTutorial("Events", "Learn how to create, manage, backup, organize, and export events"));
        tuts.add(new RTutorial("Forms", "Learn how to create, manage, edit, organize, master form"));
        tuts.add(new RTutorial("Scouting", "Learn how to scout with the RTeam model, sort, organize, search"));
        tuts.add(new RTutorial("Roblu Cloud Setup", "Learn how to register an account and setup Roblu Cloud for your team"));
        tuts.add(new RTutorial("Roblu Cloud", "Learn how to use Roblu cloud to sync events between teammates, admin controls"));
        tuts.add(new RTutorial("Bluetooth [Coming soon]", "Learn how to use setup Roblu's Bluetooth server for syncing events"));
        tuts.add(new RTutorial("Analytics [Coming soon]", "Edit, organize, view, machine learning"));
        tuts.add(new RTutorial("MEGA-TUTORIAL [Coming soon]", "Feeling ambitious? Learn about everything in one video."));
        tuts.add(new RTutorial("Roblu Devlogs", "For those interested in watching the development process"));

        rv = (RecyclerView) findViewById(R.id.teams_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        TutorialAdapter adapter = new TutorialAdapter(this, this, tuts);
        rv.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new TutorialTouchHelper();
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);

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

    @Override
    public void onItemClick(View v) {
        int position = rv.getChildLayoutPosition(v);
        if(position == 0) watchYoutubeVideo("9j6ysvJJyQg");
        if(position == 1) watchYoutubeVideo("KoylfzTBvKM");
        if(position == 2) watchYoutubeVideo("LpWvnavebNw");
        if(position == 3) watchYoutubeVideo("5ktHjyQq4XY");
        if(position == 4) watchYoutubeVideo("zEFSt9HhxOw");
        if(position == 5) watchYoutubeVideo("2QcXZoyctyw");
        if(position == 9) {
            String url = "https://www.youtube.com/playlist?list=PLjv2hkWcHVGZAlplguiS4rR_45-KLS28a";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }

    }

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
}
