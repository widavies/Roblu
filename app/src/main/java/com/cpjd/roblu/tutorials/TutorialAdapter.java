package com.cpjd.roblu.tutorials;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.teams.customsort.SelectListener;

import java.util.LinkedList;

class TutorialAdapter extends RecyclerView.Adapter<TutorialAdapter.ViewHolder> {
    private final Context mContext;
    private LinkedList<RTutorial> tutorials;

    private final SelectListener listener;

    private final RUI rui;

    TutorialAdapter(Context context, SelectListener listener, LinkedList<RTutorial> tutorials) {
        this.mContext = context;
        this.listener = listener;
        this.tutorials = tutorials;

        rui = new Loader(context).loadSettings().getRui();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_movie, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(v);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindMovie(tutorials.get(position));
    }

    @Override
    public int getItemCount() {
        if (tutorials == null) tutorials = new LinkedList<>();
        return tutorials.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;
        public final TextView subtitle;

        ViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
            subtitle = (TextView) view.findViewById(R.id.subtitle);
        }

        void bindMovie(RTutorial tutorial) {
            this.title.setText(tutorial.getTitle());
            this.subtitle.setText(tutorial.getSubtitle());
            this.title.setTextColor(rui.getText());
            this.subtitle.setTextColor(rui.getText());

            this.title.setBackgroundColor(rui.getCardColor());
            this.subtitle.setBackgroundColor(rui.getCardColor());
        }
    }
}
