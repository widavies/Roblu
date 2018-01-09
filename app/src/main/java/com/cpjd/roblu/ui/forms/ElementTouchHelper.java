package com.cpjd.roblu.ui.forms;

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
import com.cpjd.roblu.forms.elements.ESTextfield;
import com.cpjd.roblu.models.RUI;

public class ElementTouchHelper extends ItemTouchHelper.SimpleCallback {
    private final ElementsAdapter mElementsAdapter;

    // Helpers
    private final Drawable xMark, editMark;
    private final int xMarkMargin;
    private final boolean sorting;

    public ElementTouchHelper(ElementsAdapter elementsAdapter, boolean sorting) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.mElementsAdapter = elementsAdapter;
        this.sorting = sorting;

        RUI rui = new Loader(elementsAdapter.getContext()).loadSettings().getRui();

        xMark = ContextCompat.getDrawable(elementsAdapter.getContext(), R.drawable.clear);
        xMark.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_ATOP);
        xMarkMargin = 100;
        editMark = ContextCompat.getDrawable(elementsAdapter.getContext(), R.drawable.edit);
        editMark.setColorFilter(rui.getButtons(), PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        mElementsAdapter.swap(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if(sorting) return makeMovementFlags(0, 0);

        return super.getMovementFlags(recyclerView, viewHolder);
    }
    @Override
    public boolean isLongPressDragEnabled() {
        return !sorting;
    }

    @Override
    public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
        if(sorting) return;

        if(direction == ItemTouchHelper.RIGHT) {
            mElementsAdapter.postEdit(mElementsAdapter.getElement(viewHolder.getAdapterPosition()));
            return;
        }

        if (mElementsAdapter.isEditing() && mElementsAdapter.getElement(viewHolder.getAdapterPosition()).getID() <= mElementsAdapter.getInitID()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mElementsAdapter.getContext());

            builder.setTitle("Warning");
            builder.setMessage("Deleting this element will remove it and all its associated data from ALL team profiles.");

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
                    mElementsAdapter.reAdd(mElementsAdapter.getElement(viewHolder.getAdapterPosition()));
                }
            });
            AlertDialog dialog = builder.create();
            if(dialog.getWindow() != null) dialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
            dialog.show();
        } else {
            mElementsAdapter.remove(viewHolder.getAdapterPosition());
        }
    }

    @Override
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (mElementsAdapter.getElement(viewHolder.getAdapterPosition()) instanceof ESTextfield)
            return 0;
        return super.getSwipeDirs(recyclerView, viewHolder);
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


