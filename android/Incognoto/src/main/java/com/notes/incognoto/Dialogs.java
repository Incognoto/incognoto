/*
 * Copyright (C) 2018 Incognoto
 * License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
 */
package com.notes.incognoto;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.notes.incognoto.core.Note;
import com.notes.incognoto.core.NoteManager;

/**
 * Dialog boxes are a core component since there is only one main screen to display content.
 * All dialog boxes are contained here and called by `ActivityMain`.
 */
public class Dialogs {

    public static Dialog passwordDialog;

    // Displayed after the notes file has been written to external storage
    public static void showExportDialog(final Context context) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle("Export Successful");
        dialogBuilder.setCancelable(false);

        // If a default password is being used then display the generated password.
        String message = "\"notes.encrypted\" has been saved to your selected folder. You will need your password to import it elsewhere.";
        dialogBuilder.setMessage(message);

        dialogBuilder.setNegativeButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = dialogBuilder.create();
        alert.show();
    }


    // Prompt user for password input and attempt to decrypt content
    public static void displayPasswordDialog(final NoteManager noteManager, final Context context, final String title) {
        passwordDialog = new Dialog(context);
        passwordDialog.setCancelable(false);
        passwordDialog.setContentView(R.layout.dialog_input);
        passwordDialog.setCanceledOnTouchOutside(false);
        passwordDialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.DKGRAY));
        passwordDialog.setTitle(title);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(passwordDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.FILL_PARENT;
        lp.height = WindowManager.LayoutParams.FILL_PARENT;

        final EditText input = (EditText) passwordDialog.findViewById(R.id.input);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                passwordDialog.dismiss();
                if (noteManager.unlock(input.getText().toString()) == null)
                    displayPasswordDialog(noteManager, context, "Incorrect Password");
                return false;
            }
        });

        // Show the keyboard when the dialog displays
        passwordDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager)
                        context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        passwordDialog.show();
    }

    // In the note preview dialog confirm the deletion action
    // If "all" is true then clear all notes
    public static void showDeleteConfirmation(final boolean all, final Context context,
                                              final String title, final String message,
                                              final Note note) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        builder1.setTitle(title);
        builder1.setMessage(message);
        builder1.setCancelable(true);
        builder1.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (all)
                    NoteManager.clearAll();
                else
                    NoteManager.removeNote(note);
            }
        });
        builder1.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder1.create();
        alert.show();
    }

    public static void showSetFirstPassword(final Context context) {
        final Dialog inputDialog = new Dialog(context);
        inputDialog.setContentView(R.layout.dialog_password_change);
        inputDialog.setCanceledOnTouchOutside(false);
        inputDialog.setCancelable(false);
        inputDialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.DKGRAY));
        inputDialog.setTitle("Welcome to Incognoto");
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(inputDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        inputDialog.findViewById(R.id.message).setVisibility(View.VISIBLE);
        final EditText newPassInput = (EditText) inputDialog.findViewById(R.id.newPass);
        newPassInput.setHint("Password");
        final EditText confirmNewPassInput = (EditText) inputDialog.findViewById(R.id.confirmNewPass);
        confirmNewPassInput.setHint("Repeat Password");
        inputDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager)
                        context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(newPassInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        inputDialog.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newPassInput.getText().toString().length() > 0 && confirmNewPassInput.getText().toString().length() > 0) {
                    if (newPassInput.getText().toString().equals(confirmNewPassInput.getText().toString())) {
                        NoteManager.setNewPassword(newPassInput.getText().toString(), true);

                        // Never show "set first password" again
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("firstStartup", false);
                        editor.commit();
                        inputDialog.dismiss();
                    } else {
                        Toast.makeText(context, "Password Mismatch", Toast.LENGTH_LONG).show();
                        newPassInput.setText("");
                        confirmNewPassInput.setText("");
                    }
                }
            }
        });

        inputDialog.show();
    }

    // Set a new password and re-encrypt the contents with the new password
    public static void showNewMasterPasswordDialog(final Context context) {
        final Dialog inputDialog = new Dialog(context);
        inputDialog.setContentView(R.layout.dialog_password_change);
        inputDialog.setCanceledOnTouchOutside(false);
        inputDialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.DKGRAY));
        inputDialog.setTitle("Change Password");
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(inputDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        final EditText newPassInput = (EditText) inputDialog.findViewById(R.id.newPass);
        final EditText confirmNewPassInput = (EditText) inputDialog.findViewById(R.id.confirmNewPass);
        inputDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager)
                        context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(newPassInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        inputDialog.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newPassInput.getText().toString().length() > 0 && confirmNewPassInput.getText().toString().length() > 0) {
                    if (newPassInput.getText().toString().equals(confirmNewPassInput.getText().toString())) {
                        NoteManager.setNewPassword(newPassInput.getText().toString(), false);
                        Toast.makeText(context, "Success", Toast.LENGTH_LONG).show();
                        inputDialog.dismiss();
                    } else {
                        Toast.makeText(context, "Password Mismatch", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        inputDialog.show();
    }

    // An NFC device was detected, tell the user that it can be used as a password
    public static void setNewHardwareKey(final Context context, final String data) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        builder1.setTitle("Hardware Key");
        builder1.setMessage("Do you want to use this as your new password?");
        builder1.setCancelable(true);
        builder1.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                NoteManager.setNewPassword(data, false);
                context.deleteFile("default"); // Delete the default password file
                Toast notify = Toast.makeText(
                        context, "Success. Restart the app to test your authentication.", Toast.LENGTH_LONG);
                notify.setGravity(Gravity.CENTER, 0, 0);
                notify.show();
            }
        });
        builder1.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder1.create();
        alert.show();
    }
}
