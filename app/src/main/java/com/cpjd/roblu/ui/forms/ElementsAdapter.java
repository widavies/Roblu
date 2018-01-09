package com.cpjd.roblu.ui.forms;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cpjd.roblu.R;
import com.cpjd.roblu.forms.elements.Element;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.ui.teams.customsort.SelectListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class ElementsAdapter extends RecyclerView.Adapter<ElementsAdapter.ViewHolder> {
    private final Context mContext;
    private LinkedList<Element> elements;
    private final boolean editing;
    private int initID;
    private final EditListener listener;

    private final boolean sorting;
    private SelectListener selectListener;
    private final RUI rui;


    ElementsAdapter(EditListener listener, Context context, boolean editing){
        this.mContext = context;
        this.editing = editing;
        this.listener = listener;
        sorting = false;

        rui = new Loader(context).loadSettings().getRui();
    }

    // for sorting
    public ElementsAdapter(Context context, SelectListener selectListener){
        this.mContext = context;
        this.editing = false;
        this.listener = null;
        this.sorting = true;
        this.selectListener = selectListener;

        rui = new Loader(context).loadSettings().getRui();
    }

    public void pushElements(ArrayList<Element> elements) {
        if(elements == null) this.elements = new LinkedList<>();
        else this.elements = new LinkedList<>(elements);

        initID = 0;
        if(elements == null) {
            this.elements = new LinkedList<>();
            return;
        }
        for(int i = 0; i < elements.size(); i++) if(elements.get(i).getID() > initID) initID = elements.get(i).getID();
    }

    boolean isEditing() {
        return editing;
    }

    int getInitID() {
        return initID;
    }

    Element getElement(int position) {
        return elements.get(position);
    }

    void postEdit(Element e) {
        listener.postEdit(e);
    }

    void removeAll() {
        if(elements == null) return;

        this.elements.clear();
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.list_item_movie, parent, false);
        CardView c = (CardView)v;
        c.setRadius(rui.getTeamsRadius());
        v.setBackgroundColor(rui.getCardColor());
        final ViewHolder holder = new ViewHolder(v);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(sorting) selectListener.onItemClick(v);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindMovie(elements.get(position));
    }

    int getNewID() {
        int ID = -1;
        for(int i = 0; i < elements.size(); i++) if(elements.get(i).getID() > ID) ID = elements.get(i).getID();
        return ID + 1;
    }

    public void add(Element element, int ID) {
        element.setID(ID);
        elements.add(element);
        notifyDataSetChanged();
    }

    void reAdd(Element e) {
        for(int i = 0; i < elements.size(); i++) {
            if(elements.get(i).getID() == e.getID()) {
                elements.remove(i);
                elements.add(i, e);
                break;
            }
        }
        notifyDataSetChanged();
    }

    public ArrayList<Element> getElements() {
        if(elements == null) return new ArrayList<>();
        return new ArrayList<>(elements);
    }

    @Override
    public int getItemCount() {
        return elements.size();
    }

    public void remove(final int position) {
        elements.remove(position);
        notifyItemRemoved(position);
    }

    public Context getContext() {
        return mContext;
    }


    void swap(int firstPosition, int secondPosition){
        Collections.swap(elements, firstPosition, secondPosition);
        notifyItemMoved(firstPosition, secondPosition);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;
        public final TextView subtitle;

        ViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
            subtitle = (TextView) view.findViewById(R.id.subtitle);
        }

        void bindMovie(Element element) {
            this.title.setText(element.getTitle());
            this.subtitle.setText(element.getSubtitle());

            this.title.setTextColor(rui.getText());
            this.subtitle.setTextColor(rui.getText());

            this.title.setBackgroundColor(rui.getCardColor());
            this.subtitle.setBackgroundColor(rui.getCardColor());
        }
    }
}
