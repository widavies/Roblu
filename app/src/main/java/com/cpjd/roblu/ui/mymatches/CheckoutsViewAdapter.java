package com.cpjd.roblu.ui.mymatches;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RSettings;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.utils.Utils;

import java.util.ArrayList;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Think of this as the backend of the RecyclerView. It's what's actually handling RCheckout loading and insertion, removal, etc.
 *
 * @since 1.0.1
 * @version 3
 * @author Will Davies
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuppressWarnings("unused")
public class CheckoutsViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    /**
     * Reference to the context
     */
    private final Context context;
    /**
     * This is the array of checkouts that this recycler view will manage.
     */
    private ArrayList<RCheckout> checkouts;
    /**
     * UI configuration
     */
    private RUI rui;
    /**
     * All checkout clicks will be reported to this listener
     */
    private CheckoutClickListener listener;

    interface CheckoutClickListener {
        void checkoutClicked(View v);
    }

    public CheckoutsViewAdapter(Context context, RSettings settings){
        this.context = context;
        this.rui = settings.getRui();
    }

    /**
     * Loads the checkouts into the array to be managed
     * @param checkouts the checkouts to pass control off to the array
     * @param hideZeroRelevanceItems if checkouts with 0 relevance should be visible
     */
    public void setCheckouts(ArrayList<RCheckout> checkouts, boolean hideZeroRelevanceItems) {
        if(hideZeroRelevanceItems) {
            this.checkouts = new ArrayList<>(checkouts); // clones the array
            for(int i = 0; i < this.checkouts.size(); i++) {
                if(this.checkouts.get(i).getCustomRelevance() == 0) {
                    this.checkouts.remove(i);
                    i--;
                }
            }
        } else this.checkouts = checkouts;

        notifyDataSetChanged();
    }

    /**
     * Okay, so essentially certain UI actions will remove the card from the recycler view.
     * Sometimes a dialog prompt will allow the user to reverse their decision, so, we have a method
     * that allows us to reinsert a card that's just been removed
     * @param handoff the handoff to reinsert, position will be restored automatically
     */
    public void reAdd(RCheckout handoff) {
        for(int i = 0; i < checkouts.size(); i++) {
            if(checkouts.get(i) == null) continue;

            if(checkouts.get(i).getID() == handoff.getID()) {
                checkouts.remove(i);
                checkouts.add(i, handoff);
                break;
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Removes all items from the recycler view
     */
    public void removeAll() {
        if(checkouts == null) return;
        this.checkouts.clear();
        notifyDataSetChanged();
    }

    /**
     * Removes a handoff from the specified position
     * @param position the position of the handoff to be removed
     */
    void remove(int position) {
        checkouts.remove(position);
        notifyItemRemoved(position);
    }

    /**
     * Gets the number of items being managed by this array
     * @return int array size
     */
    @Override
    public int getItemCount() {
        if(checkouts == null) checkouts = new ArrayList<>();
        return checkouts.size();
    }

    /**
     * Binds UI data from the backend handoff model to the UI
     * @param holder the recycler view to bind
     * @param position the position of the handoff to load UI to
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MyViewHolder myHolder = (MyViewHolder)holder;
        myHolder.bindCheckoutToView(checkouts.get(position));
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.assignments_item, parent, false);
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

    /**
     * Specifies how UI elements will be mapped from a RCheckout model to UI
     */
    private class MyViewHolder extends RecyclerView.ViewHolder{
        public final TextView title;
        public final TextView number;
        public final TextView subtitle;

        private MyViewHolder(View view) {
            super(view);

            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
            number = view.findViewById(R.id.number);

            title.setMaxWidth((int)(Utils.WIDTH* 0.85));
            title.setTextColor(rui.getText());
            subtitle.setTextColor(rui.getText());
            number.setTextColor(rui.getText());
        }

        private void bindCheckoutToView(RCheckout handoff){
            if(handoff == null || handoff.getTeam() == null) return;
            title.setText(handoff.getTeam().getTabs().get(0).getTitle());
            number.setText("#"+handoff.getTeam().getNumber());
            String alliance = "blue alliance";
            if(handoff.getTeam().getTabs().get(0).isRedAlliance()) alliance = "red alliance";
            subtitle.setText("Match scheduled for "+ Utils.convertTime(handoff.getTeam().getTabs().get(0).getTime())+"\nYou are on the "+alliance+"\nTeammates: "+ Utils.concatenateTeams(handoff.getTeam().getTabs().get(0).getTeammates())
                    +"\nOpponents: "+ Utils.concatenateTeams(handoff.getTeam().getTabs().get(0).getOpponents()));
        }
    }
}


