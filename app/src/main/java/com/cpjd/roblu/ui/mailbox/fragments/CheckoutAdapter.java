package com.cpjd.roblu.ui.mailbox.fragments;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.ui.mailbox.CheckoutListener;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.utils.Utils;

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
    private CheckoutListener listener; // called when a checkout is tapped
    private final RUI rui;

    // the list of all the checkouts to display
    private ArrayList<RCheckout> checkouts;

    private final int mode; // CheckoutAdapter is shared by the following three views: (we need to know which one for several purposes)
    public static final int CONFLICTS = 1;
    public static final int INBOX = 2;
    public static final int MYMATCHES = 3;

    public CheckoutAdapter(Context context, int mode, CheckoutListener listener){
        this.context = context;
        this.mode = mode;
        this.listener = listener;
        this.rui = new Loader(getContext()).loadSettings().getRui();
    }

    @Override // Called for each CardView, displays checkout information on it
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

    // gets the checkout at the specified position
    public RCheckout getCheckout(int position) {
        if(checkouts == null || checkouts.size() == 0) return null;
        return checkouts.get(position);
    }

    @Override // binds a checkouts information to it's CardView, this will change depending on @var mode
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MyViewHolder myHolder = (MyViewHolder)holder;
        myHolder.bindMovie(checkouts.get(position));
    }

    // sets the checkouts in this adapter
    public void setCheckouts(ArrayList<RCheckout> checkouts) {
        if(this.checkouts == null) this.checkouts = new ArrayList<>();
        if(checkouts == null || checkouts.size() == 0) return;
        this.checkouts.addAll(checkouts);
        notifyDataSetChanged();
    }

    @Override // gets the amount of checkouts in this adapter
    public int getItemCount() {
        if(checkouts == null) checkouts = new ArrayList<>();
        return checkouts.size();
    }

    // removes all the checkouts in this adapter
    public void removeAll() {
       if(checkouts != null) checkouts.clear();
    }

    // removes a checkout at the specified position
    public void remove(final int position) {
        checkouts.remove(position);
        notifyItemRemoved(position);
    }

    // Specifies what and where information from a checkout should be binded to on a CardView
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
            if(checkout == null) return;

            title.setTextColor(rui.getText());
            number.setTextColor(rui.getText());
            subtitle.setTextColor(rui.getText());
            this.title.setMaxWidth((int)(Utils.getWidth()* 0.85));
            if(mode == MYMATCHES) {
                title.setText(checkout.getTeam().getTabs().get(0).getTitle());
                number.setText("");
                String alliance = "blue alliance";
                if(checkout.getTeam().getTabs().get(0).isRedAlliance()) alliance = "red alliance";
                subtitle.setText("Match scheduled for "+ Utils.convertTime(checkout.getTeam().getTabs().get(0).getTime())+"\nYou are on the "+alliance+"\nTeammates: "+ Utils.concantenteTeams(checkout.getTeam().getTabs().get(0).getTeammates())
                        +"\nOpponents: "+ Utils.concantenteTeams(checkout.getTeam().getTabs().get(0).getOpponents()));
                return;
            }
            if(mode == INBOX) {
                if(checkout.getConflictType() == null || checkout.getConflictType().equals("")) subtitle.setText(checkout.getTeam().getTabs().get(0).getTitle()+"\nCompleted by "+checkout.getStatus().replace("Completed by", "")+"\nAutomatically merged on "+ Utils.convertTime(checkout.getMergedTime()));
                else if(checkout.getConflictType().equals("local-edit")) subtitle.setText("Locally edited on "+ Utils.convertTime(checkout.getTeam().getLastEdit())+" and uploaded to server.");
                else {
                    if(checkout.getConflictType().startsWith("not-found")) subtitle.setText(checkout.getTeam().getTabs().get(0).getTitle()+"\nCompleted by "+checkout.getStatus().replace("Completed by", "")+"\nConflict (not found) resolved and merged on "+ Utils.convertTime(checkout.getMergedTime()));
                    else subtitle.setText(checkout.getTeam().getTabs().get(0).getTitle()+"\nCompleted by "+checkout.getStatus().replace("Completed by", "")+"\nConflict (already edited) resolved and merged on "+ Utils.convertTime(checkout.getMergedTime()));
                }
            }
            if(mode == CONFLICTS) {
                if(checkout.getConflictType().startsWith("not-found")) subtitle.setText(checkout.getTeam().getTabs().get(0).getTitle()+"\nDoesn't exist in local repository");
                else subtitle.setText(checkout.getTeam().getTabs().get(0).getTitle()+"\nLocal copy is already edited");
            }
            title.setText(checkout.getTeam().getName());
            String numberText = "#"+checkout.getTeam().getNumber();
            number.setText(numberText);
        }
    }

}

