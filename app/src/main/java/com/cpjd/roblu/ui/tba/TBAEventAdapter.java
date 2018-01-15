package com.cpjd.roblu.ui.tba;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cpjd.models.Event;
import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

/**
 * TBAEventAdapter is a simple adapter (a sort of backend) to the TBA event list.
 *
 * Note: The events array here is what is ACTUALLY displaying on the UI. TBAEventSelector obtains a static
 * list (static in the sense that it doesn't change) so that it can be searched without being reloaded (items can
 * be removed and replaced without reloading)
 *
 * @version 2
 * @since 3.5.9
 * @author Will Davies
 */
class TBAEventAdapter extends RecyclerView.Adapter<TBAEventAdapter.ViewHolder> {

    /**
     * A context reference
     */
    private final Context context;

    /**
     * The array that this adapter is managing, in this case, Event is a model defined by the TBA-API
     */
    @Setter
    @Getter
    private ArrayList<Event> events;

    interface TBAEventSelectListener {
        void tbaEventSelected(View v);
    }

    /**
     * This listener will be notified when the user taps on an Event
     */
    private TBAEventSelectListener listener;

    /**
     * Stores a UI reference to the user's color preferences
     */
    private final RUI rui;

    /**
     * Creates an event adapter
     * @param context context reference for loading UI preferences
     * @param listener a TBAEventSelectListener that will be notified when an event is tapped
     */
    TBAEventAdapter(Context context, TBAEventSelectListener listener) {
        this.context = context;
        this.listener = listener;

        events = new ArrayList<>();
        rui = new IO(context).loadSettings().getRui();
    }

    /**
     * Sets the events to the adapter
     * @param events events to pass control of to this adapter
     * @param hideZeroRelevanceEvents if events with 0 relevance should be visible
     */
    public void setEvents(ArrayList<Event> events, boolean hideZeroRelevanceEvents) {
        if(hideZeroRelevanceEvents) {
            this.events = new ArrayList<>(events); // clones the array
            for(int i = 0; i < this.events.size(); i++) {
                if(this.events.get(i).relevance == -1) {
                    this.events.remove(i);
                    i--;
                }
            }
        } else this.events = events;
        notifyDataSetChanged();
    }

    /**
     * Creates a view holder that will hold the UI of an TBA Event model
     * @param parent the parent view group
     * @param viewType the view type
     * @return a created ViewHolder
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.teams_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.tbaEventSelected(v);
            }
        });
        return holder;
    }

    /**
     * Binds TBA Event model data into the UI
     * @param holder the holder to configure UI to
     * @param position the position of the view holder
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindEvent(events.get(position));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Specifies how data will be mapped to the UI
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;
        public final TextView subtitle;
        public final TextView number;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
            number = view.findViewById(R.id.number);
        }

        void bindEvent(Event e) {
            if(e == null) return;
            this.title.setText(e.name);
            this.subtitle.setText(e.location+"\n"+e.start_date);
            this.number.setText("");
            if(rui != null) {
                this.title.setTextColor(rui.getText());
                this.subtitle.setTextColor(rui.getText());
                this.title.setBackgroundColor(rui.getCardColor());
                this.subtitle.setBackgroundColor(rui.getCardColor());
                this.number.setBackgroundColor(rui.getCardColor());
                this.number.setTextColor(rui.getText());
            }
        }
    }
}
