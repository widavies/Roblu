package com.cpjd.roblu.cloud.ui.fragments;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.cloud.ui.CheckoutListener;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.utils.Text;

import java.util.ArrayList;

import lombok.Getter;


/**
 * Can be used one of two ways:
 *
 * 1) Display merge conflicts, merge conflicts have the option to be resolved or discarded.
 * 2) Display inbox, just shows a history of received checkouts, but nothing is editable.
 *    Historical view just shows who completed the checkout, if it was override, time completed, and time merged.
 *
 * CheckoutAdapter will listen to Roblu's background service to see if there are any new checkouts waiting for us
 *
 * @since 3.5.9
 * @author Will Davies
 */
public class CheckoutAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    @Getter
    private final Context context;
    private CheckoutListener listener;
    private RUI rui;
    private long eventID;

    @Getter
    private ArrayList<RCheckout> checkouts;
    public static final int CONFLICTS = 1;
    public static final int INBOX = 2;
    public static final int MYMATCHES = 3;
    private int mode;

    public CheckoutAdapter(Context context, long eventID, int mode){
        this.context = context;
        this.mode = mode;
        this.eventID = eventID;
        this.rui = new Loader(getContext()).loadSettings().getRui();
    }

    public CheckoutAdapter(Context context, long eventID, int mode, CheckoutListener listener){
        this.context = context;
        this.mode = mode;
        this.eventID = eventID;
        this.listener = listener;
        this.rui = new Loader(getContext()).loadSettings().getRui();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.teams_item, parent, false);
        view.setBackgroundColor(rui.getCardColor());
        final MyViewHolder holder = new MyViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null) listener.checkoutClicked(v);
            }
        });
        return holder;
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MyViewHolder myHolder = (MyViewHolder)holder;
        myHolder.bindMovie(checkouts.get(position));
    }

    public void setCheckouts(ArrayList<RCheckout> checkouts) {
        if(this.checkouts == null) this.checkouts = new ArrayList<>();
        if(checkouts == null || checkouts.size() == 0) return;
        this.checkouts.addAll(checkouts);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if(checkouts == null) checkouts = new ArrayList<>();
        return checkouts.size();
    }

    public void remove(final int position) {
        RCheckout assignment = checkouts.get(position);
        notifyItemRemoved(position);
    }
    public Context getContext() {
        return context;
    }

    private class MyViewHolder extends RecyclerView.ViewHolder{
        public final TextView title;
        public final TextView number;
        public final TextView subtitle;

        private MyViewHolder(View view){
            super(view);

            title = (TextView) view.findViewById(R.id.title);
            subtitle = (TextView) view.findViewById(R.id.subtitle);
            number = (TextView) view.findViewById(R.id.number);
        }

        private void bindMovie(RCheckout checkout) {
            title.setTextColor(rui.getText());
            number.setTextColor(rui.getText());
            subtitle.setTextColor(rui.getText());

            if(mode == MYMATCHES) {
                title.setText(checkout.getTeam().getTabs().get(0).getTitle());
                number.setText("");
                String alliance = "blue alliance";
                if(checkout.getTeam().getTabs().get(0).isRedAlliance()) alliance = "red alliance";
                subtitle.setText("Match scheduled for "+Text.convertTime(checkout.getTeam().getTabs().get(0).getTime())+"\nYou are on the "+alliance+"\nTeammates: "+Text.concantenteTeams(checkout.getTeam().getTabs().get(0).getTeammates())
                        +"\nOpponents: "+Text.concantenteTeams(checkout.getTeam().getTabs().get(0).getOpponents()));
                return;
            }

            if(mode == CONFLICTS) {
                RTeam team = new Loader(getContext()).loadTeam(eventID, checkout.getTeam().getID());
                String subtitleText = "";
                subtitleText += "Completed by "+checkout.getCompletedBy()+"\nMerge conflict:"+checkout.getConflictType()+"\nCheckout last edit: "+ Text.convertTime(checkout.getCompletedTime());
                if(checkout.getConflictType().equals("Local copy already edited")) subtitleText += "\nLocal last edit: "+Text.convertTime(team.getLastEdit());
                subtitleText += "\nTap to view";
                subtitle.setText(subtitleText);

            } else {
                subtitle.setText("Completed by "+checkout.getCompletedBy()+"\nCheckout completed on "+Text.convertTime(checkout.getCompletedTime())+"\nMerged on "+Text.convertTime(checkout.getMergedTime()));
            }
            title.setText(checkout.getTeam().getName());
            number.setText("#"+checkout.getTeam().getNumber());

        }
    }

}

