package com.cpjd.roblu.ui.mymatches;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * Defines gestures & other attributes available to the RCheckout recycler view cards
 *
 * @author Will Davies
 * @version 3
 * @since 3.6.9
 */
public class CheckoutsViewTouchHelper extends ItemTouchHelper.SimpleCallback {

    CheckoutsViewTouchHelper() {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
    }

    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {}

    /**
     * This is an important method. It locks/unlocks swipe directions which depends on what tab we are in.
     */
    @Override
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return 0;
    }

    /*
     * The rest of this class just manages drawing for the cards and some other basic settings
     */
    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return super.getMovementFlags(recyclerView, viewHolder);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }
}
