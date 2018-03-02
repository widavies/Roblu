package com.cpjd.roblu.sync.qr;

import android.graphics.PointF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RForm;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.notifications.Notify;
import com.cpjd.roblu.utils.CheckoutEncoder;
import com.cpjd.roblu.utils.HandoffStatus;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import java.util.Collections;

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

                    RForm form = new IO(getApplicationContext()).loadForm(event.getID());

                    checkout.getTeam().verify(form);

                    IO io = new IO(getApplicationContext());

                    /*
                     * BEGIN MERGING
                     * -Let's check for possible conflicts
                     */
                    RTeam team = io.loadTeam(event.getID(), checkout.getTeam().getID());

                    Log.d("RBS", "Checkout ID: "+checkout.getTeam().getID());

                    // The team doesn't exist locally, so create it anew
                    if(team == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "QR merge did not find a local team to merge with. QR import ONLY supports merging, not team creation.", Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                    // Data already exists, so do a 'smart' merge
                    if(team != null) {
                        team.verify(form);

                        for(RTab downloadedTab : checkout.getTeam().getTabs()) {
                            boolean matchLocated = false;
                            for(RTab localTab : team.getTabs()) {
                                localTab.setWon(downloadedTab.isWon());

                                // Found the match, start merging
                                if(localTab.getTitle().equalsIgnoreCase(downloadedTab.getTitle())) {
                                    /*
                                     * Copy over the edit tabs
                                     */
                                    if(downloadedTab.getEdits() != null) localTab.setEdits(downloadedTab.getEdits());

                                    for(RMetric downloadedMetric : downloadedTab.getMetrics()) {
                                        for(RMetric localMetric : localTab.getMetrics()) {
                                            // Found the metric, determine if a merge needs to occur
                                            if(downloadedMetric.getID() == localMetric.getID()) {
                                                // If the local metric is already edited, keep whichever data is newest
                                                if(localMetric.isModified()) {
                                                    if(checkout.getTeam().getLastEdit() >= team.getLastEdit()) {
                                                        int replaceIndex = localTab.getMetrics().indexOf(localMetric);
                                                        localTab.getMetrics().set(replaceIndex, downloadedMetric);
                                                    }
                                                }
                                                // Otherwise, just do a straight override
                                                else {
                                                    int replaceIndex = localTab.getMetrics().indexOf(localMetric);
                                                    localTab.getMetrics().set(replaceIndex, downloadedMetric);
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    matchLocated = true;
                                    break;
                                }
                            }
                            if(!matchLocated) {
                                // Add as a new match if a merge wasn't performed
                                team.addTab(checkout.getTeam().getTabs().get(0));
                                Collections.sort(team.getTabs());
                            }
                        }
                        if(checkout.getTeam().getLastEdit() > team.getLastEdit()) team.setLastEdit(checkout.getTeam().getLastEdit());
                        io.saveTeam(event.getID(), team);

                        checkout.setTeam(team);
                        checkout.setTime(System.currentTimeMillis());
                        checkout.setStatus(HandoffStatus.COMPLETED);

                        // Prevent spamming the user with notifications
                        Notify.notifyMerged(getApplicationContext(), event.getID(), checkout);
                    }

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
