package com.cpjd.roblu.ui.teams;

import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.ui.dialogs.FastDialogBuilder;

/**
 * This class manages the UI gestures available in the teams view.
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
class TeamsRecyclerTouchHelper extends ItemTouchHelper.SimpleCallback {
    /**
     * Stores a reference to the teams adapter that is managing the TeamsView.teams array
     */
    private final TeamsRecyclerAdapter teamsAdapter;

    /*
     * UI helpers
     */
    private final Drawable xMark;
    private final int xMarkMargin;

    TeamsRecyclerTouchHelper(TeamsRecyclerAdapter teamsAdapter) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.teamsAdapter = teamsAdapter;

        /*
         * Setup UI stuff
         */
        xMark = ContextCompat.getDrawable(teamsAdapter.getContext(), R.drawable.clear);
        if(xMark != null) xMark.setColorFilter(teamsAdapter.getRui().getButtons(), PorterDuff.Mode.SRC_ATOP);
        xMarkMargin = 100;
    }

    /**
     * This method is called when a card is swiped, in this case, the only swipe direction is for the team
     * delete swipe
     * @param viewHolder the viewHolder that was swiped
     * @param direction the direction the card was swiped in
     */
    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
        /*
         * User wants to delete a team, let's confirm it with them
         */
        if(direction == ItemTouchHelper.LEFT) {
            final RTeam team = TeamsView.teams.get(viewHolder.getAdapterPosition());

            new FastDialogBuilder()
                    .setTitle("Are you sure?")
                    .setMessage("Are you sure you want to delete team "+team.getName()+"?")
                    .setPositiveButtonText("Delete")
                    .setNegativeButtonText("Cancel")
                    .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                        @Override
                        public void accepted() {
                            teamsAdapter.remove(viewHolder.getAdapterPosition());
                            teamsAdapter.notifyDataSetChanged();
                            /*
                             * Also, we need to tell TeamsView that the LoadTeamsTask now contains an invalid internal teams array and must
                             * refresh it from the local disk
                             */
                            teamsAdapter.getListener().teamDeleted(team);
                        }

                        @Override
                        public void denied() {
                            teamsAdapter.reAdd(team);
                        }

                        @Override
                        public void neutral() {}
                    }).build(teamsAdapter.getContext());
        }
    }

    /*
     * Returns the acceptable swipe directions, in this case, only to the left for deleting at eam
     */
    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int swipeFlags = ItemTouchHelper.LEFT;
        return makeMovementFlags(0, swipeFlags);
    }

    /*
     * Disables re-arranged of cards
     */
    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    /*
     * Disables re-arranging of cards
     */
    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    /*
     * Makes an 'x' show up when the card is swiped a bit
     */
    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        if(viewHolder.getAdapterPosition() == -1) return;

        int itemHeight = itemView.getBottom() - itemView.getTop();
        int intrinsicWidth = xMark.getIntrinsicWidth();
        int intrinsicHeight = xMark.getIntrinsicWidth();

        int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
        int xMarkRight = itemView.getRight() - xMarkMargin;
        int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight)/2;
        int xMarkBottom = xMarkTop + intrinsicHeight;
        xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
        if(dX < 0) xMark.draw(c);

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
