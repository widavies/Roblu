package com.cpjd.roblu.ui.teams;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.cpjd.roblu.R;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;

class TeamTouchHelper extends ItemTouchHelper.SimpleCallback {
    private final TeamsAdapter mElementsAdapter;

    // Helpers
    private final Drawable xMark;
    private final int xMarkMargin;

    TeamTouchHelper(TeamsAdapter elementsAdapter) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.mElementsAdapter = elementsAdapter;

        xMark = ContextCompat.getDrawable(elementsAdapter.getContext(), R.drawable.clear);
        try {
            RUI rui = new Loader(elementsAdapter.getContext()).loadSettings().getRui();
            xMark.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_ATOP);
        } catch(Exception e) {}
        xMarkMargin = 100;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
        if (direction == ItemTouchHelper.LEFT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mElementsAdapter.getContext());

            final RTeam team = mElementsAdapter.getTeam(viewHolder.getAdapterPosition());

            builder.setTitle("Are you sure?");
            builder.setMessage("Are you sure you want to delete team " + team.getName() + "?");

            builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mElementsAdapter.remove(viewHolder.getAdapterPosition());
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mElementsAdapter.reAdd(team);
                }
            });
            AlertDialog dialog = builder.create();
            if(dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
            dialog.setCancelable(false);
            dialog.show();
        }
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int swipeFlags = ItemTouchHelper.LEFT;
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
        if(dX < 0) xMark.draw(c);

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
