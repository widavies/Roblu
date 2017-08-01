package com.cpjd.roblu.forms.images;

import android.Manifest;
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
import android.widget.Toast;

import com.cpjd.roblu.forms.elements.EGallery;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Text;
import com.etiennelawlor.imagegallery.library.R;

import java.util.ArrayList;
import java.util.Locale;

import pub.devrel.easypermissions.EasyPermissions;

import static android.R.attr.path;

/**
 * Copyright 2015 Etienne Lawlor

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 Modifications were made to this file.

 -Modificiations include the ability to save the current image to device and delete the image. Various comments and methods were rearranged.

 @since 3.5.0

 */
public class FullScreenImageGalleryActivity extends AppCompatActivity implements FullScreenImageGalleryAdapter.FullScreenImageLoader {

    private long eventID;
    private RTeam team;
    private int tab;
    private int galleryID;

    private static final String KEY_POSITION = "KEY_POSITION";

    private Toolbar toolbar;
    private ViewPager viewPager;

    private ArrayList<byte[]> images;
    private int position;
    private static FullScreenImageGalleryAdapter.FullScreenImageLoader fullScreenImageLoader;

    private LinearLayout layout;
    private RUI rui;
    private boolean readOnly;

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

        layout = (LinearLayout) findViewById(com.cpjd.roblu.R.id.full_screen_activity);

        bindViews();

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);


        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                position = extras.getInt(KEY_POSITION);
                eventID = extras.getLong("eventID");
                team = new Loader(getApplicationContext()).loadTeam(eventID, extras.getLong("team"));
                galleryID = extras.getInt("galleryID");
                tab = extras.getInt("tab");
                readOnly = extras.getBoolean("readOnly");
            }
        }

        // Load images
        for(int i = 0; i < team.getTabs().get(tab).getElements().size(); i++) {
            if(team.getTabs().get(tab).getElements().get(i).getID() == galleryID) {
                EGallery gallery = (EGallery) team.getTabs().get(tab).getElements().get(i);
                images = gallery.getImages();
                break;
            }
        }

        rui = new Loader(getApplicationContext()).loadSettings().getRui();

        setUpViewPager();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
    }
    // endregion

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.cpjd.roblu.R.menu.image_full_screen_actionbar, menu);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Constants.IMAGE_EDITED) {
           Bundle b = data.getExtras();
            Intent result = new Intent();
            result.putExtras(b);
            setResult(Constants.IMAGE_EDITED, result);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else if(item.getItemId() == com.cpjd.roblu.R.id.edit && !readOnly) {
            Intent intent = new Intent(this, Drawing.class);
            intent.putExtra("eventID", eventID);
            intent.putExtra("tabID", tab);
            intent.putExtra("ID", galleryID);
            intent.putExtra("team", team.getID());
            intent.putExtra("position", position);
            startActivityForResult(intent, Constants.GENERAL);
            return true;
        }
        else if(item.getItemId() == com.cpjd.roblu.R.id.save_to_device) {
            if(EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                byte[] path = images.get(viewPager.getCurrentItem());
                Bitmap bitmap = BitmapFactory.decodeByteArray(path, 0, path.length);
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, String.valueOf(System.currentTimeMillis()), "");
                Text.showSnackbar(layout, getApplicationContext(), "Saved image to device", false, rui.getPrimaryColor());
            } else {
                Text.showSnackbar(layout, getApplicationContext(), "Storage permission is disabled. Please enable it.", true, 0);
            }


            return true;
        }
        else if(item.getItemId() == com.cpjd.roblu.R.id.delete_image && !readOnly) {
            for(int i = 0; i < team.getTabs().get(tab).getElements().size(); i++) {
                if(team.getTabs().get(tab).getElements().get(i).getID() == galleryID) {
                    EGallery gal = (EGallery) team.getTabs().get(tab).getElements().get(i);
                    gal.removeImage(viewPager.getCurrentItem());
                    team.getTabs().get(tab).getElements().set(i, gal);
                    new Loader(getApplicationContext()).saveTeam(team, eventID);
                    break;
                }
            }
            Intent result = new Intent();
            result.putExtra("team", team.getID());
            result.putExtra("file", path);
            setResult(Constants.IMAGE_DELETED, result);
            finish();
            Toast.makeText(getApplicationContext(), "Image deleted", Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // region FullScreenImageGalleryAdapter.FullScreenImageLoader Methods
    @Override
    public void loadFullScreenImage(ImageView iv, byte[] image, int width, LinearLayout bglinearLayout) {
        fullScreenImageLoader.loadFullScreenImage(iv, image, width, bglinearLayout);
    }
    // endregion

    // region Helper Methods
    private void bindViews() {
        viewPager = (ViewPager) findViewById(R.id.vp);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
    }

    private void setUpViewPager() {
        ArrayList<byte[]> imageList = new ArrayList<>();
        imageList.addAll(images);

        FullScreenImageGalleryAdapter fullScreenImageGalleryAdapter = new FullScreenImageGalleryAdapter(imageList);
        fullScreenImageGalleryAdapter.setFullScreenImageLoader(this);
        viewPager.setAdapter(fullScreenImageGalleryAdapter);
        viewPager.addOnPageChangeListener(viewPagerOnPageChangeListener);
        viewPager.setCurrentItem(position);

        setActionBarTitle(position);
    }

    private void setActionBarTitle(int position) {
        if (viewPager != null && images.size() > 1) {
            int totalPages = viewPager.getAdapter().getCount();

            ActionBar actionBar = getSupportActionBar();
            if(actionBar != null){
                actionBar.setTitle(String.format(Locale.getDefault(), "%d/%d", (position + 1), totalPages));
            }
        }
    }

    private void removeListeners() {
        viewPager.removeOnPageChangeListener(viewPagerOnPageChangeListener);
    }

    public static void setFullScreenImageLoader(FullScreenImageGalleryAdapter.FullScreenImageLoader loader) {
        fullScreenImageLoader = loader;
    }
    // endregion
}