/*
* Copyright (C) 2018 Incognoto
* License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
 */
package com.notes.incognoto;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
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
import android.widget.SearchView;
import android.widget.Toast;

import com.notes.incognoto.sec.NFCPayload;
import com.notes.incognoto.core.Note;
import com.notes.incognoto.core.NoteManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * App starts from here using the ActivityMain.xml layout file.
 */
public class ActivityMain extends Activity {

    // Core UI components of the primary activity
    public static LinearLayout tagLayout;
    public static ListView listView;
    public static Context context;

    // Used to import/export and unlock contents
    public static NoteManager noteManager;
    public static Intent intent;

    // Used to accept decryption input
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;

    // Codes for app intents
    private final int REQUEST_FILE_INTENT = 10;       // Result code after an import file is selected
    private final int REQUEST_DIRECTORY_INTENT = 11;  // Result code after an export location is selected
    private final int REQUEST_IMPORT_PERMISSION = 12; // Result code after storage permission is requested in the import option
    private final int REQUEST_EXPORT_PERMISSION = 13; // Result code after storage permission is requested in the backup option

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Secure the view: disable screenshots and block other apps from acquiring screen content
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        // Additional screen security options in versions later than JellyBean
        // Hide notes in the "recent" app preview
        // Removed the 'if' check because we raised the minimum SDK to 21
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // Load the view and show a password prompt to decrypt the content.
        setContentView(R.layout.activity_main);
        tagLayout = findViewById(R.id.tags);
        listView = findViewById(R.id.listview);
        context = ActivityMain.this;

        // Any pending intents are handled after decryption
        intent = getIntent();
        noteManager = new NoteManager(context);

        // Start accepting a hardware based authentication method
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // If it's the first startup let the user assign a password, otherwise prompt for decryption phrase
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean("firstStartup", true)) {
            Dialogs.showSetFirstPassword(context);
        } else {
            // Prompt for a password or a partial password if it has been enabled
            Dialogs.displayPasswordDialog(noteManager, context, "Password");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Accept NFC input as password
        if (nfcAdapter != null)
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Disable NFC input as password
        if (nfcAdapter != null)
            nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // When the app is open and in the foreground then accept NFC input as the decryption key
        NFCPayload.handleIntent(intent);
    }

    /*
    * Called when an NFC device is used when the app is in the foreground.
    */
    public static void handleHardwareKey(final String data) {
        if (noteManager.unlock(data) == null) {
            // Not currently the key. If it's unlocked then ask to use the NFC tag as the key.
            if (noteManager.status != null)
                Dialogs.setNewHardwareKey(context, data);
        }
    }

    // If you highlight text from another app you can select "share" then select this app.
    // Accepts string input from elsewhere, if you do it manually.
    public static void handleIntents() {
        if (intent.getType() != null) {
            if (intent.getType().equals("application/octet-stream")) {
                try {
                    // Accept any encrypted notes file
                    // TODO: prompt for storage permission if not already given
                    Uri uri = (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
                    String path = uri.getLastPathSegment().replace("raw:", "");
                    String content = NoteManager.getFileContent(new FileInputStream(new File(path)));
                    Log.e("NOTES", content);

                    // TODO: If the file is encrypted then prompt for a decryption key
                    // TODO: Delete current contents and encrypt the imported file
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
                // Accept plain text strings
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    NoteManager.addNote(new Note(sharedText));
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView search = (SearchView) menu.findItem(R.id.search).getActionView();
        search.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                NoteManager.clearSearch(false);
                NoteManager.search(query);
                return true;
            }
        });

        return true;
    }

    @Override
    public void onBackPressed() {
        if (tagLayout.getChildCount() == 1 && tagLayout.getChildAt(0).getTag().equals("clearFilter")) {
            // "Clear Filter" button is showing, allow back button to go back to main content
            NoteManager.clearFilter();
        } else {
            // Press back to close the app quickly and remove it from the recent tasks list
            finishAndRemoveTask();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                noteContentPreview(true, context, null);
                break;

            case R.id.backup:
                backupNotes();
                break;

            case R.id.importDatabase:
                importNotes();
                break;

            case R.id.password:
                Dialogs.showNewMasterPasswordDialog(context);
                break;

            case R.id.deleteAll:
                Dialogs.showDeleteConfirmation(
                        true, context, "Delete All Notes?",
                        "This action cannot be undone.", null);
                break;
        }
        return true;
    }

    // Opens the default file manager and lets the user pick a file to import
    private void importNotes() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Storage permission granted, start file picker
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_FILE_INTENT);
            Toast toast = Toast.makeText(context, "Pick an encrypted notes file to import",
                    Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            // `onActivityResult` is automatically called after an import file has been selected
        } else {
            requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_IMPORT_PERMISSION);
        }
    }

    // Opens the default file manager and lets the user pick a location to export the encrypted notes file
    private void backupNotes() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent backupIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.setType("*/");
            startActivityForResult(backupIntent, REQUEST_DIRECTORY_INTENT);
            Toast toast = Toast.makeText(context, "Select a location to export the encrypted notes to.",
                    Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            // `onActivityResult` is automatically called after an export location has been selected
        } else {
            requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_EXPORT_PERMISSION);
        }
    }

    // Called after `restore` when a file has been selected to be imported
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILE_INTENT && data != null) {
            // Import encrypted file
            // Returned from file picker intent after a file was selected
            Uri uri = data.getData();
            String path = Environment.getExternalStorageDirectory().getPath() + "/" + uri.getLastPathSegment();
            path = path.replace("primary:", "");
            String content = null;
            try {
                NoteManager.importFile(NoteManager.getFileContent(new FileInputStream(new File(path))));
                // Success - re-load the activity with the new notes
                Intent i = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast toast = Toast.makeText(context, "Error importing notes", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        } else if (requestCode == REQUEST_DIRECTORY_INTENT && data != null) {
            // Export / Backup
            // Returned from directory picker intent after an export directory was selected
            Uri uri = data.getData();
            try {
                String path = Environment.getExternalStorageDirectory().getPath() + "/" + uri.getLastPathSegment();
                path = path.replace("primary:", "").concat("/");
                NoteManager.backup(path);
            } catch (NullPointerException e) {
                e.printStackTrace();
                Toast toast = Toast.makeText(context, "Error selecting an export directory", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // Called after importNotes or exportNotes if a permission
        if (requestCode == REQUEST_IMPORT_PERMISSION || requestCode == REQUEST_EXPORT_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                Toast.makeText(context, "Storage permission is required to import and export files.", Toast.LENGTH_LONG).show();
            else {
                // Permission granted, call the method again
                if (requestCode == REQUEST_IMPORT_PERMISSION)
                    importNotes();
                else
                    backupNotes();
            }
        }
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
                    Dialogs.showDeleteConfirmation(false, context,
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
}
