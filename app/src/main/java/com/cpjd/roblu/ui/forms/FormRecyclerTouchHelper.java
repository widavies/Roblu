package com.cpjd.roblu.ui.forms;

import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.cpjd.roblu.R;
import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;
import com.cpjd.roblu.models.metrics.RGallery;
import com.cpjd.roblu.models.metrics.RMetric;
import com.cpjd.roblu.models.metrics.RTextfield;
import com.cpjd.roblu.ui.dialogs.FastDialogBuilder;

/**
 * This class manages the UI gestures available in the forms view
 *
 * @version 2
 * @since 3.0.0
 * @author Will Davies
 */
public class FormRecyclerTouchHelper extends ItemTouchHelper.SimpleCallback {
    /**
     * Stores a reference to the adapter
     */
    private final FormRecyclerAdapter mMetricsAdapter;
    /**
     * If true, this flag will prevent the cards from being moved or swiped
     */
    private boolean lockMovement;

    /*
     * Helper variables
     */
    private final Drawable xMark, editMark;
    private final int xMarkMargin;

    FormRecyclerTouchHelper(FormRecyclerAdapter metricsAdapter) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.mMetricsAdapter = metricsAdapter;

        /*
         * Load dependencies
         */
        RUI rui = new IO(metricsAdapter.getContext()).loadSettings().getRui();

        /*
         * Load icons
         */
        xMark = ContextCompat.getDrawable(metricsAdapter.getContext(), R.drawable.clear);
        xMark.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_ATOP);
        xMarkMargin = 100;
        editMark = ContextCompat.getDrawable(metricsAdapter.getContext(), R.drawable.edit);
        editMark.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_ATOP);
    }

    public FormRecyclerTouchHelper(FormRecyclerAdapter mMetricsAdapter, boolean lockMovement) {
        this(mMetricsAdapter);
        this.lockMovement = true;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        mMetricsAdapter.swap(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if(lockMovement) return 0;
        return super.getMovementFlags(recyclerView, viewHolder);
    }
    @Override
    public boolean isLongPressDragEnabled() {
        return !lockMovement;
    }

    /**
     * Called when a form card is swiped in a certain direction
     * @param viewHolder the view holder containing the swiped card
     * @param direction the direction the card was swiped in
     */
    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
        final RMetric metric = mMetricsAdapter.getMetrics().get(viewHolder.getAdapterPosition());

        /*
         * User wants to delete the RMetric
         */
        if(direction == ItemTouchHelper.LEFT) {
            /*
             * Decide which delete message to display, if the user is deleting a form metric that contains scouting data somewhere else,
             * we'll want to notify them a bit more
             */
            if(metric.getID() <= mMetricsAdapter.getInitID()) {
                new FastDialogBuilder()
                        .setTitle("Warning")
                        .setMessage("Deleting this metric will remove it and all its associated scouting data from ALL team profiles.")
                        .setPositiveButtonText("Delete")
                        .setNegativeButtonText("Cancel")
                        .setFastDialogListener(new FastDialogBuilder.FastDialogListener() {
                            @Override
                            public void accepted() {
                                mMetricsAdapter.getMetrics().remove(viewHolder.getAdapterPosition());
                                mMetricsAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                            }
                            @Override
                            public void denied() {
                                mMetricsAdapter.reAdd(metric);
                                mMetricsAdapter.notifyDataSetChanged();
                            }
                            @Override
                            public void neutral() {}
                        }).build(mMetricsAdapter.getContext());

            } else {
                mMetricsAdapter.getMetrics().remove(viewHolder.getAdapterPosition());
                mMetricsAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
            }
        }
        /*
         * User wants to edit the RMetric, we can't actually handle that here, so pass it back tot he FormViewer activity
         */
        else if(direction == ItemTouchHelper.RIGHT) {
            /*
             * One quick thing - Intents have a payload maximum, galleries will trigger this easily
             * because of their size, so if the user requested a RGallery edit, remove all picture information
             * from the reference.
             */
            if(metric instanceof RGallery) ((RGallery)metric).setImages(null);

            mMetricsAdapter.getListener().metricEditRequested(metric);
        }
    }

    /**
     * Defines if the card can be swiped to a certain direction, this is only disabled for the team name and team number card.
     * @param recyclerView the recycler view containing the cards
     * @param viewHolder the view holder
     * @return int representing allowed swipe directions
     */
    @Override
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if(lockMovement) return 0;

        RMetric metric = mMetricsAdapter.getMetrics().get(viewHolder.getAdapterPosition());
        if((metric.getID() == 0 && metric instanceof RTextfield && ((RTextfield)metric).isOneLine())
                || (metric.getID() == 1 && metric instanceof RTextfield && ((RTextfield)metric).isNumericalOnly())) {
            return 0;
        }
        return super.getSwipeDirs(recyclerView, viewHolder);
    }

    /*
     * Specifies icon draw behavior
     */
    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        if(viewHolder.getAdapterPosition() == -1) {
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


