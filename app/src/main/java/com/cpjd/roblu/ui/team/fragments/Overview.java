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

import com.cpjd.models.standard.Team;
import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.REvent;
import com.cpjd.roblu.models.RTab;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.metrics.RBoolean;
import com.cpjd.roblu.models.metrics.RCalculation;
import com.cpjd.roblu.models.metrics.RCheckbox;
import com.cpjd.roblu.models.metrics.RChooser;
import com.cpjd.roblu.models.metrics.RCounter;
import com.cpjd.roblu.models.metrics.RDivider;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RSlider;
import com.cpjd.roblu.models.metrics.RStopwatch;
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

        try {
        /*
         * Do statistics generation, this will generate graphs for certain metrics
         */
            // Stores pie chart values, with the sub linked hash map <item,occurrences>, this will need to be processed later into a percent
            LinkedHashMap<String, LinkedHashMap<String, Double>> pieValues = new LinkedHashMap<>();

            // Stores line chart values, with the sub linked hash map <matchName,value>
            LinkedHashMap<String, LinkedHashMap<String, Double>> lineValues = new LinkedHashMap<>();

            // This isn't directly related, more of a side project
            ArrayList<RGallery> galleries = new ArrayList<>();

            for(RTab tab : team.getTabs()) {
                // Rule out disallowed tabs
                if(tab.getTitle().equalsIgnoreCase("PIT")) continue;

                // Start processing metrics
                for(RMetric metric : tab.getMetrics()) {
                    if(metric instanceof RGallery) {
                        galleries.add((RGallery) metric);
                    }

                    if(!metric.isModified()) continue;

                    // Pie graph metrics, scan these here
                    if(metric instanceof RBoolean) {
                        LinkedHashMap<String, Double> temp = pieValues.get(metric.getTitle());
                        if(temp == null) temp = new LinkedHashMap<>();
                        String key = ((RBoolean) metric).isValue() ? "Yes" : "No";
                        if(temp.get(key) == null) temp.put(key, 1.0);
                        else temp.put(key, temp.get(key) + 1);
                        pieValues.put(metric.getTitle(), temp);
                    } else if(metric instanceof RCheckbox) {
                        if(((RCheckbox) metric).getValues() != null) {
                            for(Object key : ((RCheckbox) metric).getValues().keySet()) {
                                LinkedHashMap<String, Double> temp = pieValues.get(metric.getTitle());
                                if(temp == null) temp = new LinkedHashMap<>();
                                if(temp.get(key.toString()) == null) temp.put(key.toString(), 1.0);
                                else temp.put(key.toString(), temp.get(key.toString()) + 1);
                                pieValues.put(metric.getTitle(), temp);
                            }
                        }
                    } else if(metric instanceof RChooser) {
                        LinkedHashMap<String, Double> temp = pieValues.get(metric.getTitle());
                        if(temp == null) temp = new LinkedHashMap<>();
                        if(temp.get(metric.toString()) == null) temp.put(metric.toString(), 1.0);
                        else temp.put(metric.toString(), temp.get(metric.toString()) + 1);
                        pieValues.put(metric.getTitle(), temp);
                    }
                    // Line chart metrics
                    else if(metric instanceof RCounter || metric instanceof RSlider || metric instanceof RStopwatch || metric instanceof RCalculation) {
                        LinkedHashMap<String, Double> temp = lineValues.get(metric.getTitle());
                        if(temp == null) temp = new LinkedHashMap<>();
                        temp.put(tab.getTitle(), Double.parseDouble(metric.toString()));
                        lineValues.put(metric.getTitle(), temp);
                    }
                }
            }

            // Add the divider metrics by position, -1 if no metric after it, or at the end
            ArrayList<RDivider> addedDividers = new ArrayList<>();

        /*
         * Add the charts!
         */
            for(Object key : lineValues.keySet()) {
                if(lineValues.get(key.toString()).size() >= 2) {

                    loop:
                    for(RTab tab : team.getTabs()) {
                        for(int i = 0; i < tab.getMetrics().size(); i++) {
                            if(tab.getMetrics().get(i).getTitle().equals(key.toString())) {
                                // See if there is a RDivider hiding above this metric
                                for(int j = i; j >= 0; j--) {
                                    if(tab.getMetrics().get(j) instanceof RDivider && !addedDividers.contains(tab.getMetrics().get(j))) {
                                        layout.addView(rMetricToUI.getDivider((RDivider) tab.getMetrics().get(j)));
                                        addedDividers.add((RDivider) tab.getMetrics().get(j));
                                        break loop;
                                    }
                                }
                            }

                        }
                    }


                    layout.addView(rMetricToUI.generateLineChart(key.toString(), lineValues.get(key.toString())));
                }
            }

            // Process the pie charts
            for(Object key : pieValues.keySet()) {
                if(pieValues.get(key.toString()).size() <= 1) continue;

                int metricID = 0;

                loop:
                for(RTab tab : team.getTabs()) {
                    for(int i = 0; i < tab.getMetrics().size(); i++) {
                        if(tab.getMetrics().get(i).getTitle().equals(key.toString())) {
                            metricID = tab.getMetrics().get(i).getID();
                            // See if there is a RDivider hiding above this metric
                            for(int j = i; j >= 0; j--) {
                                if(tab.getMetrics().get(j) instanceof RDivider && !addedDividers.contains(tab.getMetrics().get(j))) {
                                    layout.addView(rMetricToUI.getDivider((RDivider) tab.getMetrics().get(j)));
                                    addedDividers.add((RDivider) tab.getMetrics().get(j));
                                    break loop;
                                }
                            }
                        }

                    }
                }

                for(Object key2 : pieValues.get(key.toString()).keySet()) {
                    if(numModified(team.getTabs(), metricID) != 0)
                        pieValues.get(key.toString()).put(key2.toString(), pieValues.get(key.toString()).get(key2.toString()) / (double) numModified(team.getTabs(), metricID));
                }

                layout.addView(rMetricToUI.generatePieChart(key.toString(), pieValues.get(key.toString())));
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

        } catch(Exception e) {
            Log.d("RBS", "Failed to generate graphs for this team profile.");
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
        Log.d("RBS", "Downloaded TBA information for "+ tbaTeam.getNickname());
        String tbaInfo =
                "Country name: " + tbaTeam.getCountry() +
                "\nLocation: " + tbaTeam.getLocationName() +
                "\nRookie year: " + tbaTeam.getRookieYear() +
                "\nMotto: " + tbaTeam.getMotto();
        TeamViewer.team.setTbaInfo(tbaInfo);
        TeamViewer.team.setWebsite(tbaTeam.getWebsite());

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
