package com.cpjd.roblu.ui.forms;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.models.metrics.RMetric;

import java.util.ArrayList;
import java.util.Collections;

import lombok.Getter;

/**
 * FormRecyclerAdapter is the back-end to the metric recycler view in FormViewer
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class FormRecyclerAdapter extends RecyclerView.Adapter<FormRecyclerAdapter.ViewHolder>  {
    /**
     * Context reference
     */
    @Getter
    private Context context;
    /**
     * The metrics array this is being managed
     */
    @Getter
    private ArrayList<RMetric> metrics;
    /**
     * Stores the user's color preferences
     */
    private final RUI rui;
    /**
     * This ID allows us to keep track of which metrics were NEWLY added, so we can
     * confirmed which delete dialog to show (deleting an old metric is dangerous (could delete scouting data)
     * so we want to notify the user a bit more)
     */
    @Getter
    private int initID;

    public interface MetricSelectedListener {
        void metricSelected(View v);
        void metricEditRequested(RMetric metric);
    }

    /**
     * Will be notified whenever a metric is tapped from the metrics array
     */
    @Getter
    private MetricSelectedListener listener;

    public FormRecyclerAdapter(Context context, MetricSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.metrics = new ArrayList<>();

        rui = new IO(context).loadSettings().getRui();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_item_movie, parent, false);
        CardView c = (CardView)v;
        c.setRadius(rui.getTeamsRadius());
        v.setBackgroundColor(rui.getCardColor());
        final ViewHolder holder = new ViewHolder(v);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.metricSelected(v);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindMetric(metrics.get(position));
    }

    /**
     * Sets the metrics to the UI, also calculates the initID
     * @param metrics the metrics to set to the adapter
     */
    public void setMetrics(ArrayList<RMetric> metrics) {
        if(metrics == null) return;

        initID = 0;
        this.metrics = metrics;
        for(RMetric metric : this.metrics) {
            if(metric.getID() > initID) initID = metric.getID();
        }
        notifyDataSetChanged();
    }

    /**
     * Should be used to add new metrics to the array
     * @param metric the metric to add to the array
     */
    public void addMetric(RMetric metric) {
        metric.setID(getNewMetricID());
        metrics.add(metric);
        notifyDataSetChanged();
    }

    /**
     * Returns the new ID of the metric for this array, since every metric must have a unique ID
     * @return the new ID for a metric
     */
    private int getNewMetricID() {
        int topID = 0;
        for(RMetric metric : metrics) {
            int newID = metric.getID();
            if(newID > topID) topID = newID;
        }
        return topID + 1;
    }

    /**
     * a reAdd() method is required because some asynchronous actions are bound
     * to the UI swiping off form cards. For example, if the user swipes off a form card,
     * they will be prompted if they'd really like to delete it, if not, it must be re-added to the array
     * @param metric the metric to re-add to the array
     */
    void reAdd(RMetric metric) {
        for(int i = 0; i < metrics.size(); i++) {
            if(metrics.get(i).getID() == metric.getID()) {
                metrics.set(i, metric);
                break;
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return metrics.size();
    }

    /**
     * This method is called whenever the user wants to move the position of a metric
     * @param firstPosition the starting position
     * @param secondPosition the ending position
     */
    void swap(int firstPosition, int secondPosition){
        Collections.swap(metrics, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);
    }

    /**
     * Defines how Roblu should bind an RMetric model to the UI
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;
        public final TextView subtitle;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
        }

        void bindMetric(RMetric metric) {
            this.title.setText(metric.getTitle());
            this.subtitle.setText(metric.getFormDescriptor());

            this.title.setTextColor(rui.getText());
            this.subtitle.setTextColor(rui.getText());

            this.title.setBackgroundColor(rui.getCardColor());
            this.subtitle.setBackgroundColor(rui.getCardColor());
        }
    }
}
