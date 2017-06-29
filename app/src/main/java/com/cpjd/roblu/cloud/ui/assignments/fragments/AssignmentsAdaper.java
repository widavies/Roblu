package com.cpjd.roblu.cloud.ui.assignments.fragments;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RUI;

import java.util.LinkedList;

public class AssignmentsAdaper extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context mContext;
    private LinkedList<RCheckout> assignments;
    private RUI rui;
    private boolean inbox;

    public AssignmentsAdaper(Context context, boolean inbox){
        this.mContext = context;
        this.rui = new Loader(getContext()).loadSettings().getRui();
        this.inbox = inbox;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.teams_item, parent, false);
        view.setBackgroundColor(rui.getCardColor());
        final MyViewHolder holder = new MyViewHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        return holder;
    }

    public RCheckout getAssignment(int position) {
        return assignments.get(position);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MyViewHolder myHolder = (MyViewHolder)holder;
        myHolder.bindMovie(assignments.get(position));
    }

    public void setElements(LinkedList<RCheckout> assignments) {
        if(this.assignments == null) this.assignments = new LinkedList<>();
        if(assignments == null || assignments.size() == 0) return;
        this.assignments.addAll(assignments);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if(assignments == null) assignments = new LinkedList<>();
        return assignments.size();
    }

    public void remove(final int position) {
        RCheckout assignment = assignments.get(position);
        notifyItemRemoved(position);
    }
    public Context getContext() {
        return mContext;
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

        private void bindMovie(RCheckout checkout){


        }
    }

}

