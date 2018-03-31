/*
* Copyright (C) 2018 Incognoto
* License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
 */
package com.notes.incognoto;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * App starts from here using the ActivityMain.xml layout file.
 */
public class ActivityMain extends Activity {

    public static LinearLayout tagLayout;
    public static ListView listView;
    public static Context context;

    // Used to import/export and unlock contents
    public static NoteManager noteManager;
    public static Intent intent;

    // Used to accept decryption input
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Secure the view: disable screenshots and block other apps from acquiring screen content
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE); // TODO: uncomment on release
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Additional screen security options in versions later than JellyBean
            // Hide notes in the "recent" app preview list
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        // Load the view and show a password prompt to decrypt the content.
        setContentView(R.layout.activity_main);
        tagLayout = (LinearLayout) findViewById(R.id.tags);
        listView = (ListView) findViewById(R.id.listview);
        context = ActivityMain.this;
        intent = getIntent(); // Any pending intents are handled after decryption
        noteManager = new NoteManager(context);

        // Start accepting a hardware based authentication method
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Attempt to decrypt the note file. This also handles the startup using a default password.
        if (noteManager.unlock("test") == null) {
            // Default password is not being used, show password prompt
            Dialogs.displayPasswordDialog(noteManager, context, "Notes Are Locked");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Accept NFC input as password
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Disable NFC input as password
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // When the app is open and in the foreground then accept NFC input as the decryption key
        final Pattern OTP_PATTERN = Pattern.compile("^https://my\\.yubico\\.com/neo/([a-zA-Z0-9!]+)$");
        Matcher matcher = OTP_PATTERN.matcher(intent.getDataString());
        if (matcher.matches()) {
            // Found yubikey NEO pattern
            handleHardwareKey(matcher.group(1));
        } else {
            // Parse the key from the from the hardware device parcelable if it's not in the NEO format
            Parcelable[] raw = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            byte[] bytes = ((NdefMessage) raw[0]).toByteArray();
            bytes = Arrays.copyOfRange(bytes, 23, bytes.length);
            handleHardwareKey(new NFCPayload().fromScanCodes(bytes));
        }
    }

    /*
    * Called when an NFC device is used when the app is in the foreground.
    */
    public void handleHardwareKey(final String data) {
        if (noteManager.status == null) {
            // The user has set an NFC password already, accept the input and attempt to decrypt
            if (Dialogs.passwordDialog != null)
                Dialogs.passwordDialog.dismiss();
            noteManager.unlock(data);
        } else {
            // The user has not set a password, ask to use the NFC tag as the password
            Dialogs.setNewHardwareKey(noteManager, context, data);
        }
    }

    // If you highlight text from another app you can select "share" then select this app.
    // Accepts string input from elsewhere, if you do it manually.
    public static void handleIntents() {
        if (intent.getType() != null) {
            if (intent.getType().toString().equals("application/octet-stream")) {
                // Accept any encrypted notes file
                // TODO: prompt for storage permission if not already given
                Uri uri = (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
                String path = uri.getLastPathSegment().replace("raw:", "");
                try {
                    String content = NoteManager.getFileContent(new FileInputStream(new File(path)));
                    Log.e("NOTES", content);
                    // TODO: If the file is encrypted then prompt for a decryption key
                    // TODO: Delete current contents and encrypt the imported file
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Press back to close the app quickly and remove it from the recent tasks list
        finishAndRemoveTask();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                noteContentPreview(true, context, null);
                break;
            case R.id.backup:
                NoteManager.backup();
                if (NoteManager.usingDefaultPassword(context))
                    Dialogs.showExportDialog(context);
                break;
            case R.id.importDatabase:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                Toast.makeText(context, "Pick an encrypted notes file to import",
                        Toast.LENGTH_SHORT).show();
                startActivityForResult(intent, 10);
                // `onActivityResult` is automatically called after an import file has been selected
                break;
            case R.id.security:
                Dialogs.showNewMasterPasswordDialog(context);
                break;
            case R.id.fingerprint:
                NoteManager.enrollFingerprint();
                break;
        }
        return true;
    }

    // Called after `restore` when a file has been selected to be imported
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            // Activity has been started with a file to be imported
            Uri uri = (Uri) data.getExtras().get(Intent.EXTRA_STREAM);
            String path = uri.getLastPathSegment().replace("raw:", "");
            String content = null;
            try {
                content = NoteManager.getFileContent(new FileInputStream(new File(path)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Log.e("NOTES", content);
            // TODO: If the file is encrypted then prompt for a decryption key
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
