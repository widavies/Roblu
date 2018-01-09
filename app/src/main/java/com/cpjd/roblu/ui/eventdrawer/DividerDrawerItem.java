package com.cpjd.roblu.ui.eventdrawer;

import android.support.annotation.LayoutRes;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.cpjd.roblu.R;
import com.mikepenz.materialdrawer.model.AbstractDrawerItem;

import java.util.List;

/**
 * Created by mikepenz on 03.02.15.
 *
 * Modified by Will Davies to support color change progamtically
 */
public class DividerDrawerItem extends AbstractDrawerItem<DividerDrawerItem, DividerDrawerItem.ViewHolder> {

    private final int color;

    public DividerDrawerItem(int color) {
        this.color = color;
    }

    @Override
    public int getType() {
        return R.id.material_drawer_item_divider;
    }

    @Override
    @LayoutRes
    public int getLayoutRes() {
        return R.layout.material_drawer_item_divider;
    }

    @Override
    public void bindView(ViewHolder viewHolder, List payloads) {
        super.bindView(viewHolder, payloads);

        //set the identifier from the drawerItem here. It can be used to run tests
        viewHolder.itemView.setId(hashCode());

        //define how the divider should look like
        viewHolder.view.setClickable(false);
        viewHolder.view.setEnabled(false);
        viewHolder.view.setMinimumHeight(1);
        ViewCompat.setImportantForAccessibility(viewHolder.view,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);

        //set the color for the divider
        viewHolder.divider.setBackgroundColor(color);

        //call the onPostBindView method to trigger post bind view actions (like the listener to modify the item if required)
        onPostBindView(this, viewHolder.itemView);
    }

    @Override
    public ViewHolder getViewHolder(View v) {
        return new ViewHolder(v);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View view;
        private final View divider;

        private ViewHolder(View view) {
            super(view);
            this.view = view;
            this.divider = view.findViewById(R.id.material_drawer_divider);
        }
    }
}
