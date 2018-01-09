package com.cpjd.roblu.ui.tba;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cpjd.models.Event;
import com.cpjd.roblu.R;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.ui.teams.customsort.SelectListener;

import java.util.ArrayList;

/**
 * @since 3.5.9
 */
class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    private final Context context;
    private ArrayList<Event> events;

    private final SelectListener listener;

    private final RUI rui;

    EventAdapter(Context context, SelectListener listener) {
        this.context = context;
        this.listener = listener;

        events = new ArrayList<>();

        rui = new Loader(context).loadSettings().getRui();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.teams_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(v);
            }
        });
        return holder;
    }

    public void setEvents(ArrayList<Event> events) {
        if(events == null) return;
        this.events.addAll(events);
    }

    public void removeAll() {
        this.events.clear();
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindEvent(events.get(position));
    }

    public Event getEvent(int position) {
        return events.get(position);
    }

    @Override
    public int getItemCount() {
        if (events == null) events = new ArrayList<>();
        return events.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;
        public final TextView subtitle;
        public final TextView number;

        ViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
            subtitle = (TextView) view.findViewById(R.id.subtitle);
            number = (TextView) view.findViewById(R.id.number);
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
