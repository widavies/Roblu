package com.cpjd.roblu.ui.teams;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.utils.Utils;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

/**
 * TeamsRecyclerAdapter is the backend to the RecyclerView in TeamsView
 * It manages the teams array and helps bind them to their UI cards.
 *
 * ***IMPORTANT***
 * This class manages the TeamsView.teams array!!
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class TeamsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /**
     * The array of teams this adapter should manage
     */
    @Getter
    @Setter
    private ArrayList<RTeam> teams;

    /**
     * A context reference required for binding UI elements
     */
    @Getter
    private final Context context;

    interface TeamSelectedListener {
        void teamSelected(View v);
        /**
         * -TeamsView should use IO to delete this team from the disk
         * -TeamsView should re-execute the LoadTeamsTask with reload flag=true when it receives
         * this method call
         */
        void teamDeleted(RTeam team);
    }

    @Getter
    private TeamSelectedListener listener;

    /**
     * Stores a reference to the RUI object, so that the adapter can tell the team cards
     * to match the user's ui preferences
     */
    @Getter
    private RUI rui;

    TeamsRecyclerAdapter(Context context, TeamSelectedListener listener){
        this.context = context;
        this.listener = listener;

        teams = new ArrayList<>();

        try {
            rui = new IO(context).loadSettings().getRui();
        } catch(Exception e) {
            rui = null;
        }
    }

    /**
     * a reAdd() method is required because some asynchronous actions are bound
     * to the UI swiping off teams cards. For example, if the user swipes off a teams card,
     * they will be prompted if they'd really like to delete it, if not, it must be re-added to the array
     * @param team the team to re-add to the array
     */
    void reAdd(RTeam team) {
        for(int i = 0; i < teams.size(); i++) {
            if(teams.get(i).getID() == team.getID()) {
                teams.set(i, team);
                break;
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Binds a team to a UI card and listeners for a user tapping it
     * @param parent the parent view group
     * @param viewType the view type
     * @return a ViewHolder representing an RTeam model
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.teams_item, parent, false);
        if(rui != null) view.setBackgroundColor(rui.getCardColor());
        final MyViewHolder holder = new MyViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null) listener.teamSelected(v);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MyViewHolder myHolder = (MyViewHolder)holder;
        myHolder.bindTeam(teams.get(position));
    }

    @Override
    public int getItemCount() {
        if(teams == null) return 0;
        return teams.size();
    }

    /**
     * Should be called INSTEAD of TeamsView.teams.remove
     * @param position the position of the item to be removed
     */
    public void remove(int position) {
        teams.remove(position);
        notifyItemRemoved(position);
    }

    /**
     * Represents an RTeam in UI
     */
    private class MyViewHolder extends RecyclerView.ViewHolder{
        /**
         * The UI holder for the team's name
         */
        public final TextView name;
        /**
         * The UI holder for the team's number
         */
        public final TextView number;
        /**
         * The UI holder for extra team information
         */
        public final TextView subtitle;

        private MyViewHolder(View view){
            super(view);

            name = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
            number = view.findViewById(R.id.number);
        }

        private void bindTeam(RTeam team){
            this.name.setText(team.getName());
            this.name.setTextColor(rui.getText());
            this.name.setMaxWidth((int)(Utils.getWidth()* 0.85));

            this.number.setText("#"+team.getNumber());
            this.number.setTextColor(rui.getText());

            String subtitle = "In "+team.getNumMatches()+" matches\nLast edited: "+ Utils.convertTime(team.getLastEdit())+team.getFilterTag();
            this.subtitle.setText(subtitle);
            this.subtitle.setTextColor(rui.getText());
        }
    }

}

