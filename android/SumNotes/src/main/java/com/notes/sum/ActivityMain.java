/*
* Copyright (C) 2018 Server Under the Mountain (SUM)
* License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
 */
package com.notes.sum;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.notes.sum.sec.Note;
import com.notes.sum.sec.NoteManager;

/**
 * App starts from here using the ActivityMain.xml layout file.
 */
public class ActivityMain extends Activity {

    public static LinearLayout tagLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Secure the view: disable screenshots and block other apps from acquiring screen content
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Additional screen security options in versions later than JellyBean
            // Hide notes in the "recent" app preview list
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        // Load the view and show a password prompt to decrypt the content.
        setContentView(R.layout.activity_main);
        tagLayout = (LinearLayout) findViewById(R.id.tags);

        SharedPreferences sharedPrefs = getSharedPreferences("temp", MODE_PRIVATE);
        if (sharedPrefs.getBoolean("default", true)) {
            // This is the first startup and user does not have a generated master password
            NoteManager.newDefaultKey(ActivityMain.this);
            sharedPrefs.edit().putBoolean("default", false).commit(); // Never generate pass again
        }
        if (NoteManager.usingDefaultPassword(ActivityMain.this)) {
            loadNotes(null);
        } else {
            displayPasswordDialog("Notes Are Locked");
        }
    }

    // Prompt user for password input and attempt to decrypt content
    public void displayPasswordDialog(String title) {
        final Dialog inputDialog = new Dialog(ActivityMain.this);
        inputDialog.setCancelable(false);
        inputDialog.setContentView(R.layout.dialog_input);
        inputDialog.setCanceledOnTouchOutside(false);
        inputDialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        inputDialog.setTitle(title);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(inputDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;

        final EditText input = (EditText) inputDialog.findViewById(R.id.input);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                loadNotes(input.getText().toString());
                inputDialog.dismiss();
                return false;
            }
        });
        inputDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager)
                        ActivityMain.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        inputDialog.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadNotes(input.getText().toString());
                inputDialog.dismiss();
            }
        });

        inputDialog.show();
    }

    public void loadNotes(String password) {
        NoteManager nm = new NoteManager(
                (ListView) findViewById(R.id.listview), ActivityMain.this, password,
                (LinearLayout) findViewById(R.id.tags));
        if (nm.status == null || nm == null) {
            displayPasswordDialog("Invalid Password");
            return;
        }

        // If you highlight text from another app you can select "share" then select this app.
        // Accepts string input from elsewhere, if you do it manually.
        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null) {
                NoteManager.addNote(new Note(sharedText));
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Press back to close the app quickly and remove it from the recent tasks list
        finishAndRemoveTask();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                noteContentPreview(true, ActivityMain.this, null);
                break;
            case R.id.search:
                // TODO: show search on action bar
                break;
            case R.id.deleteAll:
                ActivityMain.showDeleteConfirmation(
                        true, ActivityMain.this, "Delete All Notes?",
                        "This action cannot be undone.", null);
                break;
            case R.id.backup:
                NoteManager.backup();
                break;
            case R.id.restore:
                NoteManager.restore();
                break;
            case R.id.newPassword:
                showNewMasterPasswordDialog();
                break;
            default:
                break;
        }
        return true;
    }

    // Used to change note content or make a new note.
    // If `newNote` is true then this will insert a new note into the database,
    // else it will change the given note's content.
    public static void noteContentPreview(final boolean newNote,
                                          final Context context, final Note note) {
        final Dialog notePreviewDialog = new Dialog(context);
        notePreviewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        notePreviewDialog.setContentView(R.layout.dialog_note_view);
        notePreviewDialog.setCanceledOnTouchOutside(false);
        notePreviewDialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(notePreviewDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        lp.y -= 216;
        lp.dimAmount=0.8f;
        notePreviewDialog.getWindow().setAttributes(lp);

        final EditText noteNameField = (EditText) notePreviewDialog.findViewById(R.id.textInput);
        if (note != null) {
            // This is not a new note, the user is making changes.
            noteNameField.setText(note.getNoteContent());
            notePreviewDialog.findViewById(R.id.mainLayout).requestFocus();
        } else {
            // This is a new note, optimize fast input
            // Pop up the keyboard when the dialog shows. Used for quick user input.
            notePreviewDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    InputMethodManager imm = (InputMethodManager)
                            context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(noteNameField, InputMethodManager.SHOW_IMPLICIT);
                }
            });
            noteNameField.requestFocus();
        }
        noteNameField.setMaxHeight(Resources.getSystem().getDisplayMetrics().heightPixels - 450);

        // Safety: if the user accidentally hits the back button. Prevents changes from being lost.
        notePreviewDialog.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (note != null && note.getNoteContent().trim().equals(
                            noteNameField.getText().toString().trim())) {
                        notePreviewDialog.dismiss();
                    } else if (note == null && noteNameField.getText().
                            toString().trim().length() == 0) {
                        // This is a new note but no content was entered.
                        notePreviewDialog.dismiss();
                    }
                }
                return true;
            }
        });

        // Update button changes the text value of the note
        notePreviewDialog.findViewById(R.id.update).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String content = noteNameField.getText().toString();
                if (!newNote)
                    NoteManager.removeNote(note); // Remove then re-addNote
                NoteManager.addNote(new Note(content));
                notePreviewDialog.dismiss();
            }
        });

        // Remove the note from the database
        notePreviewDialog.findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (note != null)
                    showDeleteConfirmation(false, context,
                            "Delete This Note?", "This action cannot be undone.", note);
                notePreviewDialog.dismiss();
            }
        });

        // Export the note's string content
        notePreviewDialog.findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, noteNameField.getText().toString());
                context.startActivity(Intent.createChooser(sharingIntent, "Send a copy to:"));
            }
        });

        notePreviewDialog.show();
    }

    public void showNewMasterPasswordDialog() {
        final Dialog inputDialog = new Dialog(ActivityMain.this);
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
        final boolean usingDefaultPassword = NoteManager.usingDefaultPassword(ActivityMain.this);
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
                        ActivityMain.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(oldPassInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        inputDialog.findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usingDefaultPassword) {
                    if (newPassInput.getText().toString().equals(confirmNewPassInput.getText().toString()))  {
                        ActivityMain.this.deleteFile("default"); // Delete the default password file
                        NoteManager.setNewPassword(null, newPassInput.getText().toString());
                        inputDialog.dismiss();
                        Toast.makeText(ActivityMain.this, "Password Changed", Toast.LENGTH_LONG).show();
                    } else {
                        // Check that the new password strings do not match
                        oldPassInput.setText("");
                        newPassInput.setText("");
                        Toast.makeText(ActivityMain.this, "Password Do Not Match", Toast.LENGTH_LONG).show();
                    }
                } else if (newPassInput.getText().toString().equals(confirmNewPassInput.getText().toString())) {
                    // Changing master password without a default
                    inputDialog.dismiss();
                    if (NoteManager.setNewPassword(oldPassInput.getText().toString(), newPassInput.getText().toString()) == null)
                        Toast.makeText(ActivityMain.this, "Password Changed", Toast.LENGTH_LONG).show();
                } else {
                    oldPassInput.setText("");
                    Toast.makeText(ActivityMain.this, "Old Password Incorrect", Toast.LENGTH_LONG).show();
                }
            }
        });

        inputDialog.show();
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
}
