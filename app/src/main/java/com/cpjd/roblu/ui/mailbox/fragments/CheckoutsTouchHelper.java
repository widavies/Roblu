package com.cpjd.roblu.ui.mailbox.fragments;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.cpjd.roblu.R;
import com.cpjd.roblu.ui.mailbox.Mailbox;
import com.cpjd.roblu.models.RCheckout;
import com.cpjd.roblu.models.RTeam;
import com.cpjd.roblu.models.RUI;

import java.util.ArrayList;

/**
 * Manages interface of checkout cards. This class can be used in one of two ways:
 *
 * 1) Conflicts mode, swipe left to confirm, right to discard
 * 2) Inbox mode, everything disabled
 *
 * @since 3.5.9
 * @author Will Davies
 */
public class CheckoutsTouchHelper extends ItemTouchHelper.SimpleCallback {

    // Helpers
    private final Drawable xMark, editMark;
    private final int xMarkMargin;
    private final int mode;
    private long eventID;

    private CheckoutAdapter elementsAdapter;

    public CheckoutsTouchHelper(CheckoutAdapter elementsAdapter, int mode, long eventID) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.mode = mode;
        this.elementsAdapter = elementsAdapter;

        RUI rui = new Loader(elementsAdapter.getContext()).loadSettings().getRui();

        xMark = ContextCompat.getDrawable(elementsAdapter.getContext(), R.drawable.confirm);
        xMark.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_ATOP);
        xMarkMargin = 100;
        editMark = ContextCompat.getDrawable(elementsAdapter.getContext(), R.drawable.clear);
        editMark.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_ATOP);

        this.eventID = eventID;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
        if(mode == CheckoutAdapter.CONFLICTS) {
            Loader l = new Loader(elementsAdapter.getContext());
            RCheckout checkout = l.loadCheckoutConflict(elementsAdapter.getCheckout(viewHolder.getAdapterPosition()).getID());
            if(direction == ItemTouchHelper.LEFT) {
                // Merge the checkout
                checkout.setMergedTime(System.currentTimeMillis());
                checkout.setSyncRequired(true);
                l.deleteCheckoutConflict(checkout.getID());
                checkout.setHistoryID(l.getNewCheckoutID());
                l.saveCheckout(checkout);
                elementsAdapter.remove(viewHolder.getAdapterPosition());
                // Updates the main list
                Intent broadcast2 = new Intent();
                broadcast2.setAction("com.cpjd.roblu.broadcast.main");
                elementsAdapter.getContext().sendBroadcast(broadcast2);

                if(checkout.getConflictType().equals("edited")) {
                    // Merge into master repo
                    RTeam team = l.loadTeam(eventID, checkout.getTeam().getID());
                    for(int j = 0; j < team.getTabs().size(); j++) {
                        if(team.getTabs().get(j).getTitle().equals(checkout.getTeam().getTabs().get(0).getTitle())) {
                            for(int k = 0; k < checkout.getTeam().getTabs().size(); k++) {
                                team.getTabs().set(j + k, checkout.getTeam().getTabs().get(k));
                                if(team.getTabs().get(j + k).getEditors() == null) {
                                    team.getTabs().get(j + k).setEditors(new ArrayList<String>());
                                    team.getTabs().get(j + k).setEditTimes(new ArrayList<Long>());
                                }
                                team.getTabs().get(j + k).getEditors().add(checkout.getStatus().replace("Completed by", ""));
                                team.getTabs().get(j + k).getEditTimes().add(checkout.getCompletedTime());
                            }
                            team.updateEdit();

                            l.saveTeam(team, eventID);
                            break;
                        }
                    }
                } else {
                    RTeam team = checkout.getTeam().duplicate();
                    team.updateEdit();
                    team.setID(l.getNewTeamID(eventID));
                    l.saveTeam(team, eventID);
                    Mailbox.addedConflict = true;
                }

            }  else {
                // Discard the checkout
                l.deleteCheckoutConflict(checkout.getID());
                elementsAdapter.remove(viewHolder.getAdapterPosition());
                Intent broadcast3 = new Intent();
                broadcast3.setAction("com.cpjd.roblu.broadcast.main");
                elementsAdapter.getContext().sendBroadcast(broadcast3);
            }
        }
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int swipeFlags = 0;
        if(mode == CheckoutAdapter.CONFLICTS) swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
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
