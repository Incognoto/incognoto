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
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.notes.sum.sec.Note;

import java.util.List;

/**
 * App starts from here using the ActivityMain.xml layout file.
 */
public class ActivityMain extends Activity {

    private ListView listView;
    private LinearLayout searchLayout;
    private EditText searchInput;

    public static ArrayAdapter<Note> adapter;
    public static LinearLayout tagsLayout;

    private static String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Secure the view: disable screenshots and block other apps from acquiring screen content
        // Also hide notes in the "recent" app preview list
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Additional screen security options in versions later than JellyBean
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        // TODO: prompt for a password and attempt to decrypt
        setContentView(R.layout.activity_main);
        password = "mypass"; // TODO: have user set this, store salt
        List<Note> notes = Note.decrypt(ActivityMain.this, password);
        adapter = new ArrayAdapter<Note>(this, R.layout.note_item);
        if (notes != null) {
            // TODO: tell user there are no notes or failure to decrypt
            adapter.addAll(notes);
        }

        // If you highlight text from another app you can select "share" then select this app.
        // Accepts string input from elsewhere, if you do it manually.
        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            // TODO: must enter password first!
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null) {
                Note note = new Note(adapter.getCount() + 1, sharedText);
                adapter.add(note);
                Note.saveChanges(ActivityMain.this, password);
            }
        }

        // Create an empty adapter we will use to display the loaded data.
        listView = (ListView) findViewById(R.id.listview);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Tag.noteContentPreview(false, ActivityMain.this, adapter.getItem(position));
            }
        });

        // Show clickable tag buttons for quick categorical searches
        searchLayout = (LinearLayout) findViewById(R.id.search_layout);
        searchInput = (EditText) findViewById(R.id.searchInput);
        tagsLayout = (LinearLayout) findViewById(R.id.tags);
        Tag.compileTags(this);
    }

    // Toggle the hide/show of the search bar, options, and tags layout.
    // If 'hide' is true then don't toggle, just hide the layout.
    public void toggleSearchDisplay() {
        if (searchLayout.getVisibility() == View.GONE)
            showSearchDisplay();
        else
            hideSearchDisplay();
    }

    private void showSearchDisplay() {
        searchLayout.setVisibility(View.VISIBLE);
        searchInput.requestFocus();
    }
    private void hideSearchDisplay() {
        searchInput.setText("");
        searchLayout.setVisibility(View.GONE);
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
                hideSearchDisplay();
                noteContentPreview(true, ActivityMain.this, null);
                break;
            case R.id.search:
                toggleSearchDisplay();
                break;
            case R.id.deleteAll:
                ActivityMain.showDeleteConfirmation(
                        true, ActivityMain.this, 0, "Delete All Note?",
                        "This action cannot be undone.");
                break;
            case R.id.backup:
                Note.backup();
                break;
            case R.id.restore:
                Note.restore();
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
                                          final Context context, final Note noteCard) {
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
        if (noteCard != null) {
            // This is not a new note, the user is making changes.
            noteNameField.setText(noteCard.getNoteContent());
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
                    if (noteCard != null && noteCard.getNoteContent().trim().equals(
                            noteNameField.getText().toString().trim())) {
                        notePreviewDialog.dismiss();
                    } else if (noteCard == null && noteNameField.getText().
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
                    adapter.remove(noteCard);
                adapter.add(new Note(adapter.getCount() +1, content));
                notePreviewDialog.dismiss();
                Note.saveChanges(context, password);
            }
        });

        // Remove the note from the database
        notePreviewDialog.findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (noteCard != null)
                    showDeleteConfirmation(false, context, noteCard.getId(),
                            "Delete This Note?", "This action cannot be undone.");
                notePreviewDialog.dismiss();

                Note.saveChanges(context, password);
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

    // In the note preview dialog confirm the deletion action
    // If "all" is true then clear all notes
    public static void showDeleteConfirmation(final boolean all, final Context context,
                                              final int position, final String title,
                                              final String message) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        builder1.setTitle(title);
        builder1.setMessage(message);
        builder1.setCancelable(true);
        builder1.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (all) {
                    adapter.clear();
                    Note.encrypt(context, "", password);
                    return;
                }
                adapter.remove(adapter.getItem(position));
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
