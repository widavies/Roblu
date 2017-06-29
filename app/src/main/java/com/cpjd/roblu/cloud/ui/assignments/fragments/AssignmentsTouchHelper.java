package com.cpjd.roblu.cloud.ui.assignments.fragments;

import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.Loader;
import com.cpjd.roblu.models.RUI;

class AssignmentsTouchHelper extends ItemTouchHelper.SimpleCallback {
    private final AssignmentsAdaper mElementsAdapter;

    // Helpers
    private final Drawable xMark, editMark;
    private final int xMarkMargin;
    private RUI rui;
    private boolean inbox;

    AssignmentsTouchHelper(AssignmentsAdaper elementsAdapter, boolean inbox) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.mElementsAdapter = elementsAdapter;
        this.inbox = inbox;

        rui = new Loader(elementsAdapter.getContext()).loadSettings().getRui();

        xMark = ContextCompat.getDrawable(elementsAdapter.getContext(), R.drawable.confirm);
        xMark.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_ATOP);
        xMarkMargin = 100;
        editMark = ContextCompat.getDrawable(elementsAdapter.getContext(), R.drawable.clear);
        editMark.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
        if(inbox) {
        } else {

        }
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int swipeFlags = ItemTouchHelper.LEFT;
        if(inbox) swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;

        return makeMovementFlags(0, swipeFlags);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        if (viewHolder.getAdapterPosition() == -1) {
            return;
        }

        int itemHeight = itemView.getBottom() - itemView.getTop();
        int intrinsicWidth = xMark.getIntrinsicWidth();
        int intrinsicHeight = xMark.getIntrinsicWidth();

        int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
        int xMarkRight = itemView.getRight() - xMarkMargin;
        int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight)/2;
        int xMarkBottom = xMarkTop + intrinsicHeight;
        xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
        xMarkLeft = itemView.getLeft() + xMarkMargin ;
        xMarkRight = itemView.getLeft() + intrinsicWidth + xMarkMargin;
        editMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
        if(dX > 0) editMark.draw(c);
        else if(dX < 0) xMark.draw(c);

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
