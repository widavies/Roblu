package com.cpjd.roblu.sync.qr;

import android.graphics.PointF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.notifications.Notify;
import com.cpjd.roblu.sync.SyncHelper;
import com.cpjd.roblu.utils.CheckoutEncoder;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

/**
 * Scans QR codes from Roblu scouter and imports checkout data into the app.
 *
 * @since 4.3.0
 * @version 1
 * @author Will Davies
 */
public class QrReader extends AppCompatActivity implements QRCodeReaderView.OnQRCodeReadListener {

    private QRCodeReaderView qrCodeReaderView;

    private REvent event;

    private static boolean syncedAlready;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_reader);

        event = (REvent) getIntent().getSerializableExtra("event");

        qrCodeReaderView = findViewById(R.id.qrdecoderview);
        qrCodeReaderView.setOnQRCodeReadListener(this);

        // Use this function to enable/disable decoding
        qrCodeReaderView.setQRDecodingEnabled(true);

        // Use this function to change the autofocus interval (default is 5 secs)
        qrCodeReaderView.setAutofocusInterval(2000L);

        // Use this function to enable/disable Torch
        qrCodeReaderView.setTorchEnabled(true);

        // Use this function to set front camera preview
        qrCodeReaderView.setFrontCamera();

        // Use this function to set back camera preview
        qrCodeReaderView.setBackCamera();

        syncedAlready = false;
    }

    // Called when a QR is decoded
    // "text" : the text encoded in QR
    // "points" : points where QR control points are placed in View
    @Override
    public void onQRCodeRead(final String text, PointF[] points) {
        if(syncedAlready) return;

        if(!syncedAlready) {
            syncedAlready = true;
        }

        Log.d("RBS", "QR Read: "+text);
            /*
             * Decompress and import the checkout
             */
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    /*
                     * Import the checkout!
                     */
                    RCheckout checkout = new CheckoutEncoder().decodeCheckout(text);

                    new SyncHelper(getApplicationContext(), event, SyncHelper.MODES.QR).mergeCheckout(checkout);

                    new IO(getApplicationContext()).savePendingObject(checkout);

                    Notify.notifyMerged(getApplicationContext(), event.getID(), checkout);

                    finish(); // close QR scanner

                } catch(Exception e) {
                    Log.d("RBS", "Failed to import checkout from QR code: "+e.getMessage());
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        qrCodeReaderView.startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        qrCodeReaderView.stopCamera();
    }

}
