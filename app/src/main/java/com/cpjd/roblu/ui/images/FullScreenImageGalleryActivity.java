package com.cpjd.roblu.ui.images;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;

import java.util.Locale;

import lombok.Setter;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Copyright 2015 Etienne Lawlor
 * Licensed under the Apache 2.0 License
 *
 * This activity will a full screen image.
 *
 * Note: Images should be sent to the IMAGES array in ImageGalleryActivity, not passed into this class. Intents have a payload maximum
 * that most images won't fit in, so use statics instead.
 *
 * Modifications were made to the original file to make this library compatible with Roblu.
 *
 */
public class FullScreenImageGalleryActivity extends AppCompatActivity implements FullScreenImageGalleryAdapter.FullScreenImageLoader {

    private ViewPager viewPager;

    private int position;
    @Setter
    private static FullScreenImageGalleryAdapter.FullScreenImageLoader fullScreenImageLoader;

    private boolean editable;

    private final ViewPager.OnPageChangeListener viewPagerOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            if (viewPager != null) {
                viewPager.setCurrentItem(position);

                setActionBarTitle(position);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image_gallery);

        viewPager = findViewById(R.id.vp);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        position = getIntent().getIntExtra("position", 0);
        editable = getIntent().getBooleanExtra("editable", true);

        setUpViewPager();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
    }

    /**
     * Receives the picture that was edited by the user
     * @param requestCode the request code of the child activity
     * @param resultCode the result code of the child activity
     * @param data the picture that was taken
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Constants.IMAGE_EDITED) {
            setResult(Constants.IMAGE_EDITED);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        /*
         * User wants to delete an image
         */
        else if(item.getItemId() == R.id.delete_image) {
            Intent result = new Intent();
            result.putExtra("position", viewPager.getCurrentItem());
            setResult(Constants.IMAGE_DELETED, result);
            finish();
            return true;
        }
        /*
         * User wants to do some drawing!
         */
        else if(item.getItemId() == com.cpjd.roblu.R.id.edit && editable) {
            Intent intent = new Intent(this, com.cpjd.roblu.ui.images.Drawing.class);
            intent.putExtra("position", position);
            startActivityForResult(intent, Constants.GENERAL);
            return true;
        }
        /*
         * User wants to save the image to the local gallery
         */
        else if(item.getItemId() == com.cpjd.roblu.R.id.save_to_device) {
            if(EasyPermissions.hasPermissions(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                byte[] path = ImageGalleryActivity.IMAGES.get(viewPager.getCurrentItem());
                Bitmap bitmap = BitmapFactory.decodeByteArray(path, 0, path.length);
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, String.valueOf(System.currentTimeMillis()), "");
                Utils.showSnackbar(findViewById(R.id.full_screen_activity), getApplicationContext(), "Saved image to device", false, new IO(getApplicationContext()).loadSettings().getRui().getPrimaryColor());
            } else {
                Utils.showSnackbar(findViewById(R.id.full_screen_activity), getApplicationContext(), "Storage permission is disabled. Please enable it.", true, 0);
            }
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_full_screen_actionbar, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }


    @Override
    public void loadFullScreenImage(ImageView iv, byte[] image, int width, LinearLayout bglinearLayout) {
        fullScreenImageLoader.loadFullScreenImage(iv, image, width, bglinearLayout);
    }

    private void setUpViewPager() {
        FullScreenImageGalleryAdapter fullScreenImageGalleryAdapter = new FullScreenImageGalleryAdapter(ImageGalleryActivity.IMAGES);
        fullScreenImageGalleryAdapter.setFullScreenImageLoader(this);
        viewPager.setAdapter(fullScreenImageGalleryAdapter);
        viewPager.addOnPageChangeListener(viewPagerOnPageChangeListener);
        viewPager.setCurrentItem(position);

        setActionBarTitle(position);
    }

    private void setActionBarTitle(int position) {
        if (viewPager != null && ImageGalleryActivity.IMAGES.size() > 1) {
            int totalPages = viewPager.getAdapter().getCount();

            ActionBar actionBar = getSupportActionBar();
            if(actionBar != null){
                actionBar.setTitle(String.format(Locale.ENGLISH,"%d/%d", (position + 1), totalPages));
            }
        }
    }

    private void removeListeners() {
        viewPager.removeOnPageChangeListener(viewPagerOnPageChangeListener);
    }

}
