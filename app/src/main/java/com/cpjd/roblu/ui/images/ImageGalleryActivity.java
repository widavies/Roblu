package com.cpjd.roblu.ui.images;

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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.ui.team.TeamViewer;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import lombok.Setter;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Copyright 2015 Etienne Lawlor
 * Licensed under the Apache 2.0 License
 *
 * This activity will display a list of images.
 *
 * Accepted parameters:
 * -"eventID" - the ID of the event the gallery is contained in
 * -"galleryID" - the ID of the RGallery model
 * -"title" - what the title should be of this activity
 * -"editable" - true if the data should be editable (leave blank for a default of true)
 *
 *
 * Note: Images should be sent to the IMAGES array, not passed into this class. Intents have a payload maximum
 * that most images won't fit in, so use statics instead.
 *
 * Modifications were made to the original file to make this library compatible with Roblu.
 *
 */
public class ImageGalleryActivity extends AppCompatActivity implements ImageGalleryAdapter.OnImageClickListener, ImageGalleryAdapter.ImageThumbnailLoader, View.OnClickListener {

    private int eventID;
    private int rTabIndex;
    private int galleryID;

    /**
     * This is the image array that all the loaded images will be loaded from
     */
    public static ArrayList<byte[]> IMAGES;
    /**
     * Stores a temporary picture file before it's converted into a byte[] and serialized
     */
    private File tempPictureFile;

    private RecyclerView recyclerView;
    private RelativeLayout layout;
    private boolean editable;

    /*
     * Image loaders
     */
    @Setter
    private static ImageGalleryAdapter.ImageThumbnailLoader imageThumbnailLoader;
    private ImageGalleryAdapter imageGalleryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_gallery);

        /*
         * Bind views
         */
        layout = findViewById(R.id.activity_image_gallery);
        recyclerView = findViewById(R.id.rv);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);

        // Receive data
        eventID = getIntent().getIntExtra("eventID", 0);
        galleryID = getIntent().getIntExtra("galleryID", 0);
        rTabIndex = getIntent().getIntExtra("rTabIndex", 0);

        editable = getIntent().getBooleanExtra("editable", true);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getIntent().getStringExtra("title"));
        }

        setUpRecyclerView();
    }

    /**
     * Refreshes the recycler view UI
     */
    private void setUpRecyclerView() {
        /*
         * Setup the recycler adapter
         */
        int numOfColumns;
        if (Utils.isInLandscapeMode(this)) {
            numOfColumns = 4;
        } else {
            numOfColumns = 3;
        }

        recyclerView.setLayoutManager(new GridLayoutManager(ImageGalleryActivity.this, numOfColumns));
        imageGalleryAdapter = new ImageGalleryAdapter(getApplicationContext(), IMAGES);
        imageGalleryAdapter.setOnImageClickListener(this);
        imageGalleryAdapter.setImageThumbnailLoader(this);

        recyclerView.setAdapter(imageGalleryAdapter);
    }

    /**
     * The user selected an image, send it to the full screen image gallery activity
     * @param position the position of the image
     */
    @Override
    public void onImageClick(int position) {
        Intent intent = new Intent(ImageGalleryActivity.this, FullScreenImageGalleryActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        bundle.putBoolean("editable", editable);
        intent.putExtras(bundle);
        startActivityForResult(intent, Constants.GENERAL);
    }

    /**
     * The user clicked the plus button and wants to add a new image
     * @param v the floating action button that was clicked
     */
    @Override
    public void onClick(View v) {
        if(!editable) return;

        if(EasyPermissions.hasPermissions(this, android.Manifest.permission.CAMERA)) {
            tempPictureFile = new IO(getApplicationContext()).getTempPictureFile();

            Uri fileUri = FileProvider.getUriForFile(getApplicationContext(), "com.cpjd.roblu", tempPictureFile);

            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

            startActivityForResult(camera, Constants.GENERAL);
        } else {
            Utils.showSnackbar(layout, getApplicationContext(), "Camera permission is disabled. Please enable it.", true, 0);
        }
    }

    /**
     * Receives the picture that was taken by the user
     * @param requestCode the request code of the child activity
     * @param resultCode the result code of the child activity
     * @param data the picture that was taken
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Constants.GENERAL && resultCode == FragmentActivity.RESULT_OK) {
            // fetch file from storage
            Bitmap bitmap = BitmapFactory.decodeFile(tempPictureFile.getPath());
            // fix rotation
            try {
                ExifInterface ei = new ExifInterface(tempPictureFile.getPath());
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
            } catch(IOException e) {
                Log.d("RBS", "Failed to remove EXIF rotation data from the picture.");
            }

            /*
             * Convert the image into a byte[] and save it to the gallery
             */

            // Convert the bitmap to a byte array
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, stream);
            byte[] array = stream.toByteArray();

            Log.d("RBS", "Saving image to rTabIndex "+rTabIndex+" with gallery ID: "+galleryID);

            // save the ID to the gallery
            for(int i = 0; i < TeamViewer.team.getTabs().get(rTabIndex).getMetrics().size(); i++) {
                if(TeamViewer.team.getTabs().get(rTabIndex).getMetrics().get(i).getID() == galleryID) {
                    ((RGallery)TeamViewer.team.getTabs().get(rTabIndex).getMetrics().get(i)).addImage(array);
                    break;
                }
            }
            TeamViewer.team.setLastEdit(System.currentTimeMillis());

            new IO(getApplicationContext()).saveTeam(eventID, TeamViewer.team);
            imageGalleryAdapter.notifyDataSetChanged();
        }
        /*
         * User edited an image
         */
        else if(resultCode == Constants.IMAGE_EDITED) {
            TeamViewer.team.setLastEdit(System.currentTimeMillis());

            new IO(getApplicationContext()).saveTeam(eventID, TeamViewer.team);
            imageGalleryAdapter.notifyDataSetChanged();
        }
        /*
         * User deleted an image
         */
        else if(resultCode == Constants.IMAGE_DELETED) {
            IMAGES.remove(data.getIntExtra("position", 0));
            imageGalleryAdapter.notifyDataSetChanged();

            // the gallery object reference actually gets removed automatically because IMAGES contains a reference to it

            TeamViewer.team.setLastEdit(System.currentTimeMillis());

            new IO(getApplicationContext()).saveTeam(eventID, TeamViewer.team);

        }
    }

    /**
     * Rotates an image
     * @param source the image to rotate
     * @param angle the angle to rotate the image by
     * @return the rotated image
     */
    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public void loadImageThumbnail(ImageView iv, byte[] imageUrl, int dimension) {
        imageThumbnailLoader.loadImageThumbnail(iv, imageUrl, dimension);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
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


}
