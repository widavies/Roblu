package com.cpjd.roblu.forms.images;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.cpjd.roblu.forms.elements.EGallery;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Text;
import com.etiennelawlor.imagegallery.library.R;
import com.etiennelawlor.imagegallery.library.activities.FullScreenImageGalleryActivity;
import com.etiennelawlor.imagegallery.library.utilities.DisplayUtility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

import pub.devrel.easypermissions.EasyPermissions;

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

 @since 3.5.0
 */
public class ImageGalleryActivity extends AppCompatActivity implements ImageGalleryAdapter.OnImageClickListener, ImageGalleryAdapter.ImageThumbnailLoader, View.OnClickListener {

    public static final String KEY_IMAGES = "KEY_IMAGES";
    public static final String KEY_TITLE = "KEY_TITLE";

    private Toolbar toolbar;
    private RecyclerView recyclerView;

    private ArrayList<byte[]> images;
    private String title;
    private static ImageGalleryAdapter.ImageThumbnailLoader imageThumbnailLoader;

    private int ID;
    private RTeam team;
    private REvent event;

    private ImageGalleryAdapter imageGalleryAdapter;
    private File pic;

    private int tabID;

    private RelativeLayout layout;
    private boolean readOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.cpjd.roblu.R.layout.activity_image_gallery);

        layout = (RelativeLayout)findViewById(com.cpjd.roblu.R.id.activity_image_gallery);

        FloatingActionButton fab = (FloatingActionButton) findViewById(com.cpjd.roblu.R.id.fab);
        fab.setOnClickListener(this);

        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                title = extras.getString(KEY_TITLE);
                ID = extras.getInt("ID");
                event = (REvent) extras.getSerializable("event");
                team = new Loader(getApplicationContext()).loadTeam(event.getID(), extras.getLong("team"));
                tabID = extras.getInt("tabID");
                readOnly = extras.getBoolean("readOnly");
            }
        }

        // Load images
        for(int i = 0; i < team.getTabs().get(tabID).getElements().size(); i++) {
            if(team.getTabs().get(tabID).getElements().get(i).getID() == ID) {
                EGallery gallery = (EGallery) team.getTabs().get(tabID).getElements().get(i);
                images = gallery.getImages();
                break;
            }
        }

        bindViews();

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(title);
        }

        setUpRecyclerView();
    }
    private void launchParent() {
        if(images == null) images = new ArrayList<>();
        Intent intent = new Intent();
        intent.putExtra("tabID", tabID);
        intent.putExtra("team", team.getID());
        setResult(Constants.GALLERY_EXIT, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        launchParent();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            launchParent();
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setUpRecyclerView();
    }

    @Override
    public void onImageClick(int position) {
        Intent intent = new Intent(ImageGalleryActivity.this, com.cpjd.roblu.forms.images.FullScreenImageGalleryActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt(FullScreenImageGalleryActivity.KEY_POSITION, position);
        bundle.putLong("eventID", event.getID());
        bundle.putInt("tab", tabID);
        bundle.putInt("galleryID", ID);
        bundle.putLong("team", team.getID());
        bundle.putBoolean("readOnly", readOnly);
        intent.putExtras(bundle);
        startActivityForResult(intent, Constants.GENERAL);
    }

    private void bindViews() {
        recyclerView = (RecyclerView) findViewById(R.id.rv);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
    }

    private void setUpRecyclerView() {
        int numOfColumns;
        if (DisplayUtility.isInLandscapeMode(this)) {
            numOfColumns = 4;
        } else {
            numOfColumns = 3;
        }

        recyclerView.setLayoutManager(new GridLayoutManager(ImageGalleryActivity.this, numOfColumns));
        imageGalleryAdapter = new ImageGalleryAdapter(this, images);
        imageGalleryAdapter.setOnImageClickListener(this);
        imageGalleryAdapter.setImageThumbnailLoader(this);

        recyclerView.setAdapter(imageGalleryAdapter);
    }

    public static void setImageThumbnailLoader(ImageGalleryAdapter.ImageThumbnailLoader loader) {
        imageThumbnailLoader = loader;
    }

    @Override
    public void onClick(View v) {
        if(readOnly) return;
        if(EasyPermissions.hasPermissions(this, android.Manifest.permission.CAMERA)) {
            pic = new Loader(getApplicationContext()).getTempPictureFile();

            if(pic == null) {
                Text.showSnackbar(layout, getApplicationContext(), "Something went wrong with the camera :(", true, 0);
                return;
            }

            Uri fileUri = FileProvider.getUriForFile(getApplicationContext(), "com.cpjd.roblu", pic);

            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

            startActivityForResult(camera, Constants.CAMERA_REQUEST);
        } else {
            Text.showSnackbar(layout, getApplicationContext(), "Camera permission is disabled. Please enable it.", true, 0);
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Constants.IMAGE_EDITED) {
            team = new Loader(getApplicationContext()).loadTeam(event.getID(), data.getExtras().getLong("team"));
            // reload images
            for(int i = 0; i < team.getTabs().get(tabID).getElements().size(); i++) {
                if(team.getTabs().get(tabID).getElements().get(i).getID() == ID) {
                    EGallery gallery = (EGallery) team.getTabs().get(tabID).getElements().get(i);
                    images = gallery.getImages();
                    break;
                }
            }
            imageGalleryAdapter.notifyDataSetChanged();
            setUpRecyclerView();
            return;
        }
        if (requestCode == Constants.CAMERA_REQUEST && resultCode == FragmentActivity.RESULT_OK) {
            // fetch file from storage
            Bitmap bitmap = BitmapFactory.decodeFile(pic.getPath());
            // fix rotation
            try {
                ExifInterface ei = new ExifInterface(pic.getPath());
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

                switch(orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        bitmap = rotateImage(bitmap, 90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        bitmap = rotateImage(bitmap, 180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        bitmap = rotateImage(bitmap, 270);
                        break;
                    default:
                        break;
                }
            } catch(Exception e) {
                System.out.println("Failed to rotate image");
            }

            // get the actual file path and add that to the gallery!
            save(bitmap);
            imageGalleryAdapter.notifyDataSetChanged();
        }
        else if(resultCode == Constants.IMAGE_DELETED) {
            team = new Loader(getApplicationContext()).loadTeam(event.getID(), data.getLongExtra("team", 0));
            // Load images
            for(int i = 0; i < team.getTabs().get(tabID).getElements().size(); i++) {
                if(team.getTabs().get(tabID).getElements().get(i).getID() == ID) {
                    EGallery gallery = (EGallery) team.getTabs().get(tabID).getElements().get(i);
                    images = gallery.getImages();
                    break;
                }
            }
            setUpRecyclerView();
        }
    }
    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
    private void save(Bitmap bitmap) {
        // Convert the bitmap to a byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] array = stream.toByteArray();

        // Find the gallery element and add the picture to it
        for(int i = 0; i < team.getTabs().get(tabID).getElements().size(); i++) {
            if(team.getTabs().get(tabID).getElements().get(i).getID() == ID) {
                EGallery gallery = (EGallery) team.getTabs().get(tabID).getElements().get(i);
                gallery.addImage(array);
                team.getTabs().get(tabID).getElements().set(i, gallery);
                images = gallery.getImages();
                break;
            }
        }

        // Save all changes
        team.updateEdit();
        new Loader(getApplicationContext()).saveTeam(team, event.getID());
    }

    @Override
    public void loadImageThumbnail(ImageView iv, byte[] image, int dimension) {
        imageThumbnailLoader.loadImageThumbnail(iv, image, dimension);
    }
}
