package com.cpjd.roblu.ui.images;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.graphics.CanvasView;
import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.ui.UIHandler;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;
import com.jrummyapps.android.colorpicker.ColorPickerDialog;
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener;

import java.io.ByteArrayOutputStream;

/**
 * A utility for drawing on Bitmaps!
 * We use a wonderful utility to help us do this.
 *
 * @since 3.5.9
 * @author Will Davies
 */
public class Drawing extends AppCompatActivity implements ColorPickerDialogListener {

    private CanvasView canvas = null;
    private RUI rui;
    private int color;
    private int position;

    public static byte[] DRAWINGS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);

        rui = new IO(getApplicationContext()).loadSettings().getRui();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        position = getIntent().getIntExtra("position", 0);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        View content = getWindow().findViewById(Window.ID_ANDROID_CONTENT);
        int temp = 0;
        if(content != null) temp = content.getHeight();

        if(getIntent().getBooleanExtra("fieldDiagram", false)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            this.canvas = this.findViewById(R.id.canvas);

            // Load image
            if(getIntent().getByteArrayExtra("fieldDrawings") != null) {
                Bitmap b = BitmapFactory.decodeByteArray(getIntent().getByteArrayExtra("fieldDrawings"), 0, getIntent().getByteArrayExtra("fieldDrawings").length);
                Bitmap bitmap = Bitmap.createScaledBitmap(b, displayMetrics.widthPixels, displayMetrics.heightPixels - toolbar.getHeight() - temp, false);
                if(bitmap != null) this.canvas.drawBitmap(bitmap);
            }

            LinearLayout layout = findViewById(R.id.drawing_layout);
            layout.setBackgroundResource(getIntent().getIntExtra("fieldDiagramID", R.drawable.field2018));

            this.canvas.setBaseColor(rui.getBackground());
            this.canvas.setPaintStrokeWidth(7F);
            canvas.setPaintStrokeColor(Color.YELLOW);
        } else {
            // Load image
            Bitmap b = BitmapFactory.decodeByteArray(ImageGalleryActivity.IMAGES.get(position), 0, ImageGalleryActivity.IMAGES.get(position).length);
            Bitmap bitmap = Bitmap.createScaledBitmap(b, displayMetrics.widthPixels, displayMetrics.heightPixels - toolbar.getHeight() - temp, false);
            this.canvas = this.findViewById(R.id.canvas);
            this.canvas.setBaseColor(rui.getBackground());
            if(bitmap != null) this.canvas.drawBitmap(bitmap);
            this.canvas.setPaintStrokeWidth(7F);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.drawing, menu);
        new UIHandler(this, menu).updateMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else if(item.getItemId() == R.id.undo) {
            canvas.undo();
        } else if(item.getItemId() == R.id.mode) {
            showPopup();
            return true;
        }
        else if(item.getItemId() == R.id.confirm) {
            // save bitmap back to file system
            Bitmap bitmap = canvas.getBitmap();

            if(getIntent().getBooleanExtra("fieldDiagram", false)) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                DRAWINGS = stream.toByteArray();
                Intent result = new Intent();
                result.putExtras(getIntent().getExtras());
                setResult(Constants.FIELD_DIAGRAM_EDITED, result);
                finish();
            } else {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] array = stream.toByteArray();

                // set the image back to the image gallery
                ImageGalleryActivity.IMAGES.set(position, array);
                setResult(Constants.IMAGE_EDITED);
                finish();
            }

            return true;
        }

        return false;
    }

    private void showPopup(){
        View menuItemView = findViewById(R.id.mode);
        final PopupMenu popup = new PopupMenu(Drawing.this, menuItemView);
        MenuInflater inflate = popup.getMenuInflater();
        inflate.inflate(R.menu.drawing_modes, popup.getMenu());

        final PopupMenu.OnMenuItemClickListener popupListener = new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                canvas.setMode(CanvasView.Mode.DRAW);

                if(item.getItemId() == R.id.pen) {
                    canvas.setDrawer(CanvasView.Drawer.PEN);
                    return true;
                } else if(item.getItemId() == R.id.eraser) {
                    canvas.setBaseColor(Color.TRANSPARENT);
                    //canvas.setMode(CanvasView.Mode.ERASER);
                    return true;
                }
                else if(item.getItemId() == R.id.line) {
                    canvas.setDrawer(CanvasView.Drawer.LINE);
                    return true;
                } else if(item.getItemId() == R.id.rectangle) {
                    canvas.setDrawer(CanvasView.Drawer.RECTANGLE);
                    return true;
                } else if(item.getItemId() == R.id.circle) {
                    canvas.setDrawer(CanvasView.Drawer.CIRCLE);
                    return true;
                } else if(item.getItemId() == R.id.ellipse) {
                    canvas.setDrawer(CanvasView.Drawer.ELLIPSE);
                    return true;
                } else if(item.getItemId() == R.id.quad) {
                    canvas.setDrawer(CanvasView.Drawer.QUADRATIC_BEZIER);
                    return true;
                } else if(item.getItemId() == R.id.stroke) {
                    changeWidth();
                    return true;
                } else if(item.getItemId() == R.id.color) {
                    ColorPickerDialog.newBuilder().setAllowPresets(true).setPresets(new int[] {
                            Color.RED, Color.BLUE, Color.CYAN, Color.YELLOW, Color.MAGENTA, Color.BLACK, Color.WHITE, Color.GRAY, Color.GREEN, Color.DKGRAY, Color.LTGRAY }
                    ).setColor(color).setShowColorShades(true).show(Drawing.this);
                    return true;
                }
                return false;
            }
        };
        popup.setOnMenuItemClickListener(popupListener);
        popup.show();

    }

    private void changeWidth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final AppCompatEditText input = new AppCompatEditText(this);
        Utils.setInputTextLayoutColor(rui.getAccent(), null, input);
        input.setHighlightColor(rui.getAccent());
        input.setHintTextColor(rui.getText());
        input.setTextColor(rui.getText());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(30);
        input.setFilters(FilterArray);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                canvas.setPaintStrokeWidth(Float.parseFloat(input.getText().toString()));
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { dialog.cancel(); }
        });
        TextView view = new TextView(this);
        view.setTextSize(Utils.DPToPX(getApplicationContext(), 5));
        view.setPadding(Utils.DPToPX(this, 18), Utils.DPToPX(this, 18), Utils.DPToPX(this, 18), Utils.DPToPX(this, 18));
        view.setText(R.string.set_line_width);
        view.setTextColor(rui.getText());
        AlertDialog dialog = builder.create();
        dialog.setCustomTitle(view);
        if(dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = rui.getDialogDirection();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
        }
        dialog.show();
        dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
        dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());
    }

    @Override
    public void onColorSelected(int dialogId, @ColorInt int color) {
        this.color = color;
        canvas.setPaintStrokeColor(color);
    }

    @Override
    public void onDialogDismissed(int dialogId) {}
}