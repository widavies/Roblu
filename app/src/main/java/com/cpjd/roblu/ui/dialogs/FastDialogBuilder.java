package com.cpjd.roblu.ui.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;

import com.cpjd.roblu.io.IO;
import com.cpjd.roblu.models.RUI;

/**
 * FastDialogBuilder is a extremely simple Yes/No/Neutral Dialog.
 * It works as a normal builder does. For the buttons, if the buttonText string is
 * "", then the button won't be added to the dialog at all.
 *
 * The dialog will ONLY close when one of the three buttons is pressed or the user exits
 * the app.
 *
 * @version 1
 * @since 4.0.0
 * @author Will Davies
 */
public class FastDialogBuilder {
    /**
     * The title of the dialog
     */
    private String title = "";
    /**
     * The message of the dialog
     */
    private String message = "";
    /**
     * The text to set to the positive button, "" won't add the positive button
     */
    private String positiveButtonText = "";
    /**
     * The text to set to the neutral button, "" won't add the neutral button
     */
    private String neutralButtonText = "";
    /**
     * The text to set to the negative button, "" won't add the negative button
     */
    private String negativeButtonText = "";

    public interface FastDialogListener {
        void accepted();
        void denied();
        void neutral();
    }

    /**
     * The listener that will received all dialog events
     */
    private FastDialogListener listener;

    public FastDialogBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public FastDialogBuilder setMessage(String message) {
        this.message = message;
        return this;
    }
    public FastDialogBuilder setPositiveButtonText(String text) {
        this.positiveButtonText = text;
        return this;
    }

    public FastDialogBuilder setNegativeButtonText(String text) {
        this.positiveButtonText = text;
        return this;
    }

    public FastDialogBuilder setNeutralButtonText(String text) {
        this.neutralButtonText = text;
        return this;
    }

    public FastDialogBuilder setFastDialogListener(FastDialogListener listener) {
        this.listener = listener;
        return this;
    }

    public void build(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);

        if(!positiveButtonText.equals("")) {
            builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(listener != null) listener.accepted();
                    dialog.dismiss();
                }
            });
        }

        if(!neutralButtonText.equals("")) {
            builder.setPositiveButton(neutralButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(listener != null) listener.neutral();
                    dialog.dismiss();
                }
            });
        }

        if(!negativeButtonText.equals("")) {
            builder.setPositiveButton(negativeButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(listener != null) listener.denied();
                    dialog.dismiss();
                }
            });
        }

        RUI rui = new IO(context).loadSettings().getRui();

        AlertDialog dialog = builder.create();
        if(dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = rui.getAnimation();
            if(dialog.getButton(Dialog.BUTTON_NEGATIVE) != null) dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(rui.getAccent());
            if(dialog.getButton(Dialog.BUTTON_POSITIVE) != null) dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(rui.getAccent());
            if(dialog.getButton(Dialog.BUTTON_NEUTRAL) != null) dialog.getButton(Dialog.BUTTON_NEUTRAL).setTextColor(rui.getAccent());

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(rui.getBackground()));
        }
        dialog.setCancelable(false);
        dialog.show();
    }


}
