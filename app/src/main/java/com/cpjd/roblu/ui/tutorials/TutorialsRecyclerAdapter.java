package com.cpjd.roblu.ui.tutorials;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;

import java.util.ArrayList;

/**
 * TutorialsRecyclerAdapter is the backend to the tutorials recycler view in
 * the Tutorial activity.
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 *
 */
class TutorialsRecyclerAdapter extends RecyclerView.Adapter<TutorialsRecyclerAdapter.ViewHolder> {
    private final Context mContext;
    private ArrayList<RTutorial> tutorials;

    interface TutorialListener {
        void tutorialSelected(View v);
    }

    /**
     * This listener will be notified when the user taps on a tutorial
     */
    private final TutorialListener listener;
    /**
     * User color preferences
     */
    private final RUI rui;

    TutorialsRecyclerAdapter(Context context, TutorialListener listener, ArrayList<RTutorial> tutorials) {
        this.mContext = context;
        this.listener = listener;
        this.tutorials = tutorials;

        rui = new IO(context).loadSettings().getRui();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_movie, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.tutorialSelected(v);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindTutorial(tutorials.get(position));
    }

    @Override
    public int getItemCount() {
        return tutorials.size();
    }

    /**
     * Specifies how a RTutorial object should be binded to a UI element
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;
        public final TextView subtitle;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
        }

        void bindTutorial(RTutorial tutorial) {
            this.title.setText(tutorial.getTitle());
            this.subtitle.setText(tutorial.getSubtitle());
            if(rui != null) {
                this.title.setTextColor(rui.getText());
                this.subtitle.setTextColor(rui.getText());
                this.title.setBackgroundColor(rui.getCardColor());
                this.subtitle.setBackgroundColor(rui.getCardColor());
            }

        }
    }
}
