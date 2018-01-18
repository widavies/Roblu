package com.cpjd.roblu.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;

/**
 * Lots of choices to get the UI set to the proper colors
 */
public class UIHandler {

    private RUI rui;
    private final AppCompatActivity activity;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private Menu menu;

    private boolean start;

    public UIHandler(AppCompatActivity activity, Toolbar toolbar, FloatingActionButton fab, boolean start) {
        this.activity = activity;
        this.toolbar = toolbar;
        this.fab = fab;
        this.start = start;
    }

    public UIHandler(AppCompatActivity activity, Toolbar toolbar) {
        this.activity = activity;
        this.toolbar = toolbar;
    }

    public UIHandler(AppCompatActivity activity, Toolbar toolbar, FloatingActionButton fab) {
        this.activity = activity;
        this.toolbar = toolbar;
        this.fab = fab;
    }

    public UIHandler(AppCompatActivity activity, Menu menu) {
        this.activity = activity;
        this.menu = menu;
    }

    public void update() {
        rui = new IO(activity).loadSettings().getRui();

        if(rui == null) rui = new RUI();

        // set toolbar colors
        if(toolbar != null) {
            toolbar.setBackgroundColor(rui.getPrimaryColor());
            toolbar.setTitleTextColor(rui.getText());
            toolbar.setSubtitleTextColor(rui.getText());

                if(!start) {
                    try {
                        Drawable drawable = ContextCompat.getDrawable(activity, android.support.design.R.drawable.abc_ic_ab_back_material);
                        drawable.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_ATOP);
                        if(activity.getSupportActionBar() != null) activity.getSupportActionBar().setHomeAsUpIndicator(drawable);
                    } catch(Exception e) {
                        System.out.println("System does not contain back button");
                    }
                }
        }

        // set status bar colors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(RUI.darker(rui.getPrimaryColor(), 0.85f));
            if(start && rui.getPrimaryColor() == -12627531) window.setStatusBarColor(Color.TRANSPARENT);
        }

        // activity background
        View view = activity.getWindow().getDecorView();
        view.setBackgroundColor(rui.getBackground());

        if(fab != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(rui.getPrimaryColor()));
            Drawable d = fab.getDrawable();
            d.mutate();
            d.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);
            fab.setImageDrawable(d);
        }
    }

    public void updateMenu() {
        rui = new IO(activity).loadSettings().getRui();

        // Update icon colors
        for(int i = 0; i < menu.size(); i++) {
            Drawable d = menu.getItem(i).getIcon();
            d.mutate();
            d.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_IN);
        }
    }

}
