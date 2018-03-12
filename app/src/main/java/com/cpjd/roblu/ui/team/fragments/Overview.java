package com.cpjd.roblu.ui.team.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cpjd.models.Team;
import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RBoolean;
import com.cpjd.roblu.models.metrics.RCheckbox;
import com.cpjd.roblu.models.metrics.RChooser;
import com.cpjd.roblu.models.metrics.RDivider;
import com.cpjd.roblu.models.metrics.RFieldData;
import com.cpjd.roblu.models.metrics.RFieldDiagram;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RTextfield;
import com.cpjd.roblu.ui.forms.RMetricToUI;
import com.cpjd.roblu.ui.team.TBATeamInfoTask;
import com.cpjd.roblu.ui.team.TeamViewer;
import com.cpjd.roblu.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * The overview tab will display some generic information about the team,
 * such as TBA info, size, meta information, etc.
 *
 * Keep in mind, TBA information is downloaded by TeamViewer.
 *
 * Parameters:
 * -"teamID" - the ID of the team
 * -"eventID" - the ID of the event
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class Overview extends Fragment implements TBATeamInfoTask.TBAInfoListener {

    private LinearLayoutCompat layout;
    private RMetricToUI rMetricToUI;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.overview_tab, container, false);

        Bundle bundle = this.getArguments();
        layout = view.findViewById(R.id.overview_layout);
        REvent event = (REvent) getArguments().getSerializable("event");
        RTeam team = new IO(getActivity()).loadTeam(event.getID(), TeamViewer.team.getID());
        rMetricToUI = new RMetricToUI(getActivity(), new IO(getActivity()).loadSettings().getRui(), true);

        /*
         * Do statistics generation!
         */
        // This is not really included in statistics generation, it's more of a side project so only 1 loop needs to be used
        ArrayList<RGallery> galleries = new ArrayList<>();

        for(int i = 0; i < team.getTabs().get(1).getMetrics().size(); i++) { // predictions form will be the same for every tab
            /*
             * This will store the values of whatever metric is being analyzed.
             * -For RCounter, RSlider, and RStopwatch, String key will be the match name, and the Object will be a Double value
             * -For RChooser and RCheckbox, String key will be the item name, Object will be the double percentage occurrence
             * -For RBoolean, String key 1 is YES, key 2 is NO, Object value is the percentage occurrence
             *
             * All other metrics are not valid for analyzing with this method
             */
            LinkedHashMap<String, Object> values = new LinkedHashMap<>();

            if(team.getTabs().get(1).getMetrics().get(i) instanceof RDivider) {
                layout.addView(new RMetricToUI(getActivity(), new IO(view.getContext()).loadSettings().getRui(), false).getDivider((RDivider)team.getTabs().get(1).getMetrics().get(i)));
                continue;
            }

            // Process all the values
            for(int j = 1; j < team.getTabs().size(); j++) {
                RMetric metric = team.getTabs().get(j).getMetrics().get(i);

                if(metric instanceof RGallery) {
                    galleries.add((RGallery)metric);
                    continue;
                }

                if(!metric.isModified() || metric instanceof RTextfield) continue;

                if(metric instanceof RBoolean) {
                    // Process the new value
                    String key = "Yes";
                    if(!((RBoolean) metric).isValue()) key = "No";
                    // Check for old value
                    if(values.get(key) != null) values.put(key, ((Double)values.get(key)) + 1.0);
                    else values.put(key, 1.0);
                }
                else if(metric instanceof RChooser) {
                    // Process the new value
                    String key = metric.toString();
                    // Check for old value
                    if(values.get(key) != null) values.put(key, ((Double)values.get(key)) + 1);
                    else values.put(key, 1.0);
                }
                else if(metric instanceof RCheckbox) {
                    for(Object o : ((RCheckbox) metric).getValues().keySet()) {
                        if(!((RCheckbox) metric).getValues().get(o.toString())) continue;
                        // Process the new value
                        String key = o.toString();
                        // Check for old value
                        if(values.get(key) != null) values.put(key, ((Double)values.get(key)) + 1);
                        else values.put(key, 1.0);
                    }
                } else {
                    values.put(team.getTabs().get(j).getTitle(), metric.toString());
                }
            }

            // Require at least two values for line charts
            if((team.getTabs().get(1).getMetrics().get(i) instanceof RBoolean && values.size() < 1) || values.size() <= 1) continue;

            // Return for incompatible metrics
            if(team.getTabs().get(1).getMetrics().get(i) instanceof RDivider || team.getTabs().get(1).getMetrics().get(i) instanceof RGallery
                    || team.getTabs().get(1).getMetrics().get(i) instanceof RTextfield || team.getTabs().get(1).getMetrics().get(i) instanceof RFieldDiagram || team.getTabs().get(1).getMetrics().get(i) instanceof RFieldData) continue;

            /*
             * If the metric was boolean, chooser, or checkbox, all the values need to be
             * converted to percentages
             */
            if(team.getTabs().get(1).getMetrics().get(i) instanceof RBoolean
                    || team.getTabs().get(1).getMetrics().get(i) instanceof RCheckbox
                    || team.getTabs().get(1).getMetrics().get(i) instanceof RChooser) {
                for(Object o : values.keySet()) {
                    values.put(o.toString(), ((Double)values.get(o.toString())) / (double)numModified(team.getTabs(), team.getTabs().get(1).getMetrics().get(i).getID()) * 100.0);
                }

                layout.addView(rMetricToUI.generatePieChart(team.getTabs().get(1).getMetrics().get(i).getTitle(), values));
            } else layout.addView(rMetricToUI.generateLineChart(team.getTabs().get(1).getMetrics().get(i).getTitle(), values));
        }

        /*
         * Attempt to download TBA info for this team
         */

        if(!team.hasTBAInfo()) {
            if(event.getKey() != null && event.getKey().length() >= 4) new TBATeamInfoTask(view.getContext(), team.getNumber(), event.getKey().substring(0, 4),  this);
        } else {
            // TBA info card
            layout.addView(rMetricToUI.getInfoField("TBA.com information", TeamViewer.team.getTbaInfo(), TeamViewer.team.getWebsite(), TeamViewer.team.getNumber()), 0);
            if(TeamViewer.team.getImage() != null) {
                // Image view
                Bitmap bitmap = BitmapFactory.decodeByteArray(TeamViewer.team.getImage(), 0, TeamViewer.team.getImage().length);
                layout.addView(rMetricToUI.getImageView("Robot", bitmap));
            }
        }

        /*
         * Find the image with the most entropy, and add
         * it as the "featured" image
         */
        galleryLoop: for(int j = galleries.size() - 1; j >= 0; j--) {
            if(galleries.get(j).getImages() != null && galleries.get(j).getImages().size() > 0) {
                for(int i = galleries.get(j).getImages().size() - 1; i >= 0; i--) {
                    try {
                        layout.addView(rMetricToUI.getImageView(
                                "Featured image",  BitmapFactory.decodeByteArray(galleries.get(j).getImages().get(i),
                                        0,  galleries.get(j).getImages().get(i).length)));
                        break galleryLoop;
                    } catch(Exception e) {
                        Log.d("RBS", "Failed to load featured image: "+e.getMessage());
                    }
                }
            }
        }

        /*
         * Add UI cards to the layout
         */
        // "Other" card
        layout.addView(rMetricToUI.getInfoField("Other", "Last edited: "+ Utils.convertTime(team.getLastEdit())+"\nSize on disk: "+
                new IO(view.getContext()).getTeamSize(bundle.getInt("eventID"), team.getID())+" KB", "", 0));
        return view;
    }

    /**
     * This method is called when TBA information for the team is successfully downloaded
     * @param tbaTeam the TBA team model that contains downloaded information
     */
    @Override
    public void teamRetrieved(Team tbaTeam) {
        Log.d("RBS", "Downloaded TBA information for "+ tbaTeam.nickname);
        String tbaInfo = "Locality: " + tbaTeam.locality +
                "\nRegion: " + tbaTeam.region +
                "\nCountry name: " + tbaTeam.country_name +
                "\nLocation: " + tbaTeam.location +
                "\nRookie year: " + tbaTeam.rookie_year +
                "\nMotto: " + tbaTeam.motto;
        TeamViewer.team.setTbaInfo(tbaInfo);
        TeamViewer.team.setWebsite(tbaTeam.website);

        try {
            new IO(getView().getContext()).saveTeam(getArguments().getInt("eventID"), TeamViewer.team);

            // TBA info card
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    layout.addView(rMetricToUI.getInfoField("TBA.com information", TeamViewer.team.getTbaInfo(), TeamViewer.team.getWebsite(), TeamViewer.team.getNumber()), 0);
                }
            });
        } catch(Exception e) {
            Log.d("RBS", "Failed to download TBA information.");
        }
    }

    @Override
    public void imageRetrieved(byte[] image) {
        if(image != null) {
            TeamViewer.team.setImage(image);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(TeamViewer.team.getImage(), 0, TeamViewer.team.getImage().length);
                    layout.addView(rMetricToUI.getImageView("Robot",bitmap), 1);
                }
            });
        }
    }

    private int numModified(ArrayList<RTab> tabs, int ID) {
        int num = 0;
        for(int i = 2; i < tabs.size(); i++) for(RMetric metric : tabs.get(i).getMetrics()) if(metric.getID() == ID && metric.isModified()) num++;
        return num;
    }

}
