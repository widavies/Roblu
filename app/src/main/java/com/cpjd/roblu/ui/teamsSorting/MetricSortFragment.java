package com.cpjd.roblu.ui.teamsSorting;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.ui.forms.FormRecyclerTouchHelper;
import com.cpjd.roblu.ui.forms.FormRecyclerAdapter;
import com.cpjd.roblu.utils.Constants;
import com.cpjd.roblu.utils.Utils;

import java.util.ArrayList;

/**
 * MetricSortFragment is the front-end to a metrics array of different sort methods.
 *
 * It expects the following parameters from the Intent:
 * -"processMethod" - an integer defined by TeamMetricProcessor.PROCESS_METHOD, (-1 for PROCESS_METHOD.IN_MATCH, PROCESS_METHOD.MATCH_WINS, PROCESS_METHOD.RESET, or any new ones)
 * -"metrics" - the array of metrics is should display
 * -"eventID" - only required for processMethod == -1, used for showing the matches in event dialog
 *
 * It will return in an Intent to the calling activity:
 * -"sortToken" - string in format [processMethod:metricID:matchTitle] example: 2:5:Quals 1 (note: the last item WILL ONLY be returned if
 * processMethod==PROCESS_METHOD.IN_MATCH (and metricID should be ignored).
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class MetricSortFragment extends Fragment implements FormRecyclerAdapter.MetricSelectedListener {

    /**
     * The UI object that manages UI loading of the metrics array
     */
    private RecyclerView rv;
    /**
     * Metrics representing all the possible sort methods
     */
    private ArrayList<RMetric> metrics;
    /**
     * The process method, as defined by TeamMetricProcessor, if process method is
     * not PROCESS_METHOD.PIT, PROCESS_METHOD.PREDICTIONS, PROCESS_METHOD.MATCHES, use -1
     */
    private int processMethod;
    /**
     * This is stored because this fragment needs to be able to display a list of all matches
     * within a specific event
     */
    private int eventID;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.metric_tab, container, false);

        /*
         * Receive parameters
         */
        Bundle bundle = this.getArguments();

        metrics = (ArrayList<RMetric>) bundle.getSerializable("metrics");
        processMethod = bundle.getInt("processMethod");
        eventID = bundle.getInt("eventID");

        /*
         * Attach metrics to the RecyclerView
         */
        rv = view.findViewById(R.id.metric_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(view.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(linearLayoutManager);
        ((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);
        FormRecyclerAdapter adapter = new FormRecyclerAdapter(view.getContext(), this);
        rv.setAdapter(adapter);
        // setup gesture listener
        ItemTouchHelper.Callback callback = new FormRecyclerTouchHelper(adapter, true);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rv);
        adapter.setMetrics(metrics);

        return view;
    }

    /**
     * This method is called when the user taps on a metric
     * @param v the View that the user tapped on (used for inferring the RMetric object)
     */
    @Override
    public void metricSelected(View v) {
        int position = rv.getChildLayoutPosition(v);
        // User selected the "In Match" option, now we have to display a list of all the matches within the event
        if(metrics.get(position).getID() == TeamMetricProcessor.PROCESS_METHOD.IN_MATCH) {
            final Dialog d = new Dialog(getActivity());
            d.setTitle("Select match");
            d.setContentView(R.layout.event_import_dialog);
            final Spinner spinner = d.findViewById(R.id.type);
            TextView t = d.findViewById(R.id.spinner_tip);
            t.setText(R.string.match);
            final String[] values = Utils.getMatchTitlesWithinEvent(getContext(), eventID);
            if(values == null) {
                Toast.makeText(getActivity(), "Error occurred while loading matches. Do any matches exist?", Toast.LENGTH_LONG).show();
                return;
            }
            ArrayAdapter<String> adp = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, values);
            adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adp);
            Button button = d.findViewById(R.id.button7);
            button.setText(R.string.select);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent result = new Intent();
                    result.putExtra("sortToken", TeamMetricProcessor.PROCESS_METHOD.IN_MATCH+":0:"+values[spinner.getSelectedItemPosition()]);
                    getActivity().setResult(Constants.CUSTOM_SORT_CONFIRMED, result);
                    getActivity().finish();
                    d.dismiss();
                }
            });
            if(d.getWindow() != null) d.getWindow().getAttributes().windowAnimations = new IO(getActivity()).loadSettings().getRui().getAnimation();
            d.show();
            return;
        }

        String sortToken = processMethod+":"+metrics.get(position).getID();
        Intent result = new Intent();
        result.putExtra("sortToken", sortToken);
        if(getActivity() != null) {
            getActivity().setResult(Constants.CUSTOM_SORT_CONFIRMED, result);
            getActivity().finish();
        }
    }

}
