/*
 * Copyright (C) 2018 Incognoto
 * License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
 */
package com.notes.incognoto;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
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

    public static void showExportDialog(final Context context) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle("Export Successful");
        dialogBuilder.setCancelable(false);

        // If a default password is being used then display the generated password.
        String message = "\"notes.encrypted\" has been saved to your \"downloads\" folder. You will need your password to import it elsewhere.";
        if (NoteManager.usingDefaultPassword(context)) {
            final String password = NoteManager.getDefaultPassword(context);
            if (password != null) {
                message = "To import these notes you will need to enter \"" + password + "\". ";
                message += "DO NOT lose this password! Your data cannot be recovered if the password is forgotten.";

                dialogBuilder.setPositiveButton("Copy To Clipboard", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("", password);
                        clipboard.setPrimaryClip(clip);
                        dialog.cancel();
                    }
                });
            }
        }
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
        NoteManager.fingerprintAuth();

        passwordDialog = new Dialog(context);
        passwordDialog.setCancelable(false);
        passwordDialog.setContentView(R.layout.dialog_input);
        passwordDialog.setCanceledOnTouchOutside(false);
        passwordDialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT));
        passwordDialog.setTitle(title);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(passwordDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        final EditText input = (EditText) passwordDialog.findViewById(R.id.input);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                passwordDialog.dismiss();
                noteManager.unlock(input.getText().toString());
                return false;
            }
        });
        passwordDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager)
                        context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        passwordDialog.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passwordDialog.dismiss();
                noteManager.unlock(input.getText().toString());
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

    public static void showNewMasterPasswordDialog(final Context context) {
        final Dialog inputDialog = new Dialog(context);
        //inputDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        inputDialog.setContentView(R.layout.dialog_password_change);
        inputDialog.setCanceledOnTouchOutside(false);
        inputDialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.DKGRAY));
        inputDialog.setTitle("Change Master Password");
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(inputDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        final EditText oldPassInput = (EditText) inputDialog.findViewById(R.id.oldPass);
        final boolean usingDefaultPassword = NoteManager.usingDefaultPassword(context);
        if (usingDefaultPassword) {
            // Hide the 'old password' input since there is no old password set by the user
            oldPassInput.setVisibility(View.GONE);
        }

        final EditText newPassInput = (EditText) inputDialog.findViewById(R.id.newPass);
        final EditText confirmNewPassInput = (EditText) inputDialog.findViewById(R.id.confirmNewPass);
        inputDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager)
                        context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(oldPassInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        inputDialog.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newPassInput.getText().toString().length() > 3 && confirmNewPassInput.getText().toString().length() > 3) {
                    if (usingDefaultPassword) {
                        // Using a generated password, don't require old password
                        if (newPassInput.getText().toString().equals(confirmNewPassInput.getText().toString())) {
                            context.deleteFile("default"); // Delete the default password file
                            NoteManager.setNewPassword(null, newPassInput.getText().toString());
                            inputDialog.dismiss();
                            Toast.makeText(context, "Password Changed", Toast.LENGTH_LONG).show();
                        } else {
                            // Check that the new password strings do not match
                            oldPassInput.setText("");
                            newPassInput.setText("");
                            Toast.makeText(context, "Password Do Not Match", Toast.LENGTH_LONG).show();
                        }
                    } else if (newPassInput.getText().toString().equals(confirmNewPassInput.getText().toString())) {
                        // Changing master password without a default password, required to put in old password
                        inputDialog.dismiss();
                        if (NoteManager.setNewPassword(oldPassInput.getText().toString(), newPassInput.getText().toString()) == null)
                            Toast.makeText(context, "Password Changed", Toast.LENGTH_LONG).show();
                    } else {
                        oldPassInput.setText("");
                        Toast.makeText(context, "Old Password Incorrect", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        inputDialog.show();
    }

    // An NFC device was detected, tell the user that it can be used as a password
    public static void setNewHardwareKey(final NoteManager noteManager, final Context context, final String data) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        builder1.setTitle("Hardware Key");
        builder1.setMessage("Do you want to use this as your new password?");
        builder1.setCancelable(true);
        builder1.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                NoteManager.setNewPassword(null, data);
                context.deleteFile("default"); // Delete the default password file
                Toast notify = Toast.makeText(
                        context, "Success. Test your authentication.", Toast.LENGTH_LONG);
                notify.setGravity(Gravity.CENTER, 0, 0);
                notify.show();
                Dialogs.displayPasswordDialog(noteManager, context, "Notes Are Locked");
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
