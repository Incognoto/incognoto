/*
* Copyright (C) 2018 Incognoto
* License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
*/
package com.notes.sum.sec;

import android.app.ActionBar;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.notes.sum.ActivityMain;
import com.notes.sum.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.DOWNLOAD_SERVICE;

/**
 * File management for encrypted notes
 */
public class NoteManager {
    // Feeds notes to the list view
    private static ArrayAdapter<Note> adapter;
    private static ArrayAdapter<Note> filteredAdapter;
    private static Context context;
    private static LinearLayout tagLayout;
    private static ListView listView;

    // Status is non-null if decryption is successful.
    // Despite this being a public variable, it does not grant access to the data
    // since the passphrase is still required for decryption.
    public static String status;
    private static String password;

    // Separates note objects in document form
    private static final String delimiter = "\n---\n";

    // Initializes list view's adapter, loads decrypted notes if the given password is valid
    public NoteManager(final ListView listView, final Context context, final String password, final LinearLayout tagLayout) {
        this.context = context;

        // If the master password was not changed from the original generated one then use it
        this.password = password;
        if (usingDefaultPassword(context)) {
            this.password = getDefaultPassword(context);
            this.status = "Active";
        }

        this.adapter = new ArrayAdapter<Note>(context, R.layout.note_item);
        this.filteredAdapter = new ArrayAdapter<Note>(context, R.layout.note_item);
        this.tagLayout = tagLayout;
        this.listView = listView;
        this.listView.setAdapter(adapter);
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (filteredAdapter.getCount() > 0) {
                    // Filter being used, show relevent items
                    ActivityMain.noteContentPreview(false, context, filteredAdapter.getItem(position));
                } else {
                    ActivityMain.noteContentPreview(false, context, adapter.getItem(position));
                }
            }
        });

        List<Note> notes = NoteManager.decrypt();
        if (notes != null) {
            this.status = "Active";
            adapter.addAll(notes);
            showTagUI();
        }
    }

    // Given a file stream return the file contents
    private static String getFileContent(final FileInputStream fileInputStream) {
        BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
        String line;
        String allContent = "";
        try {
            while ((line = br.readLine()) != null) {
                allContent += line;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return allContent;
    }

    // Copy the encrypted notes file to the "Downloads" folder
    public static void backup() {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getPath(), "notes.txt");
            FileOutputStream output = new FileOutputStream(file);
            output.write((getFileContent(context.openFileInput("notes")) + "\n").getBytes());
            output.close();
            Toast.makeText(context, "Saved encrypted notes to 'Downloads' folder.",
                    Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Copy a text file (encrypted or plaintext) from an external location into this app
    public static void restore() {
        // First delete all notes
        ActivityMain.showDeleteConfirmation(
                true, context, "Delete All Notes?",
                "This action cannot be undone.", null);

        // TODO: Show prompt for import of data set. After selecting the file then prompt for the passphrase
    }

    // On first startup the user is given a randomly generated master password.
    // This is to enable users to use the app without having to set or remember a master password.
    // It's recommended that the generated master password is changed using the
    // "Change Master Password" option in the menu.
    public static String newDefaultKey(Context context) {
        try {
            String keyPhrase = AesCbcWithIntegrity.generateKey().getIntegrityKey().toString();
            FileOutputStream cipherStream = context.openFileOutput("default", Context.MODE_PRIVATE);
            cipherStream.write(keyPhrase.getBytes());
            cipherStream.close();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    // Detects if the user has set a new master password.
    public static boolean usingDefaultPassword(final Context context) {
        if (getDefaultPassword(context) != null)
            return true;
        return false;
    }

    private static String getDefaultPassword(final Context context) {
        try {
            return getFileContent(context.openFileInput("default"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Starts an asynchronous call to save all notes in an encrypted state
    private static void saveChanges(final ArrayAdapter<Note> adapter) {
        // This may be resource intensive, depending on the number of notes and the content,
        // so start this process on a new thread.
        // Start by copying the adapter to a new memory space so it can be asynchronous
        final ArrayAdapter<Note> tempAdapter = new ArrayAdapter<Note>(context, R.layout.note_item);
        int adapterSize = adapter.getCount();
        for(int i = 0; i < adapterSize; i++) {
            tempAdapter.add(adapter.getItem(i));
        }
        Thread encryptThread = new Thread() {
            @Override
            public void run() {
                String allData = "";
                for(int i = 0; i < tempAdapter.getCount(); i++) {
                    allData += tempAdapter.getItem(i) + "\n---\n";
                }
                NoteManager.encrypt(allData);
            }
        };
        encryptThread.start();
        showTagUI();
    }

    // Reads all tags from all notes and creates buttons at the top of the main page.
    // Tapping on a tag button shows all notes with that same tag.
    private static void showTagUI() {
        tagLayout.removeAllViews(); // Always start with a fresh view. This operation is low overhead.

        // Go through all items in the list view
        for (int i = 0; i < adapter.getCount(); i++) {
            final Note note = adapter.getItem(i);

            // For each tag in a note add a button to the main activity's tag section.
            // TODO: avoid duplicates. Currently if you have two tags with "#something" then it will create two buttons for "#something" which is redundant
            for (final String tag : note.getTags()) {
                ActivityMain.tagLayout.addView(createTagButton(tag));
            }
        }
    }

    // Create a button that's loaded into the tagLayout which allows users to filter
    // notes by their associated tags.
    private static Button createTagButton(final String tag) {
        Button tagButton = new Button(context);
        tagButton.setText(tag);
        tagButton.setLayoutParams(new LinearLayout.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
        tagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Search through all notes
                filteredAdapter = new ArrayAdapter<Note>(context, R.layout.note_item);
                for (int i = 0; i < adapter.getCount(); i++) {

                    // Search through all the tags in each note for a match
                    Note targetNote = adapter.getItem(i);
                    for (String targetTag : targetNote.getTags()) {
                        if (tag.equals(targetTag))
                            filteredAdapter.add(targetNote);
                    }
                }
                listView.setAdapter(filteredAdapter);
                tagLayout.removeAllViews();

                // Since notes with a specific tag are being shown, add a new button to
                // the tags layout that will allow users to clear the tag filter.
                // This restores the state of the main activity's list view back to normal.
                Button clearFilter = new Button(context);
                clearFilter.setText("Clear Filter For " + tag);
                clearFilter.setLayoutParams(new LinearLayout.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
                clearFilter.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Restore the non-filtered adapter, rebuild the tags layout
                        filteredAdapter.clear();
                        listView.setAdapter(adapter);
                        showTagUI();
                    }
                });
                tagLayout.addView(clearFilter, 0);
            }
        });
        return tagButton;
    }

    // Remove a note and reload the list view to reflect the changes
    public static void removeNote(Note note) {
        adapter.remove(note);
        saveChanges(adapter);
    }

    // Delete all notes and tags
    // Called by the "Delete All" menu option
    public static void clearAll() {
        adapter.clear();
        saveChanges(adapter);
    }

    // Add a note and reload the list view to reflect the changes
    public static void addNote(Note note) {
        adapter.insert(note, 0); // Always add to top (index 0)
        saveChanges(adapter);
    }


    /**
     * Reading and Writing encrypted Note objects
     */

    public static void encrypt(String message) {
        try {
            // Convert the plain text to a cipher string
            AesCbcWithIntegrity.SecretKeys key = AesCbcWithIntegrity.generateKeyFromPassword(password, getSalt());
            AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = AesCbcWithIntegrity.encrypt(message, key);
            String ciphertextString = cipherTextIvMac.toString();

            // Write cipher string to the encrypted file in private mode
            FileOutputStream cipherStream = context.openFileOutput("notes", Context.MODE_PRIVATE);
            cipherStream.write(ciphertextString.getBytes());
            cipherStream.close();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] getSalt() throws GeneralSecurityException {
        FileInputStream fileInputStream = null;
        try {
            // If the file doesn't exist then generate a salt
            File file = context.getFileStreamPath("salt");
            if (file == null || !file.exists()) {
                FileOutputStream cipherStream = context.openFileOutput("salt", Context.MODE_PRIVATE);
                cipherStream.write(AesCbcWithIntegrity.generateSalt());
                cipherStream.close();
            }

            fileInputStream = context.openFileInput("salt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
            String line;
            String allContent = "";
            while ((line = br.readLine()) != null) {
                allContent += line;
            }
            return allContent.getBytes();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return AesCbcWithIntegrity.generateSalt();
    }

    // Given the old password and the new password, check for validity
    // and re-encrypt all notes with the new password
    public static String setNewPassword(String oldPassword, final String newPassword) {
        if (oldPassword == null || oldPassword.equals(newPassword)) {
            // using default password
            password = newPassword;
            saveChanges(adapter);
            return "Success";
        } else {
            return null;
        }
    }

    // Decrypt the private internal notes, given a valid password in the NoteManager constructor.
    // If the key phrase does not decrypt the content or there's any error then return a null list of Notes.
    public static List<Note> decrypt() {
        try {
            // Read the cipher text
            FileInputStream fileInputStream = context.openFileInput("notes");
            BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
            String line;
            String allContent = "";
            while ((line = br.readLine()) != null) {
                allContent += line;
            }

            // Decrypt the cipher text and add plain text note content to the main activity
            AesCbcWithIntegrity.SecretKeys key = null;
            try {
                key = AesCbcWithIntegrity.generateKeyFromPassword(password, getSalt());
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                return null;
            }
            AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac =
                    new AesCbcWithIntegrity.CipherTextIvMac(allContent);
            try {
                allContent = AesCbcWithIntegrity.decryptString(cipherTextIvMac, key);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                return null;
            }

            List<Note> notes = new ArrayList<Note>();
            while (allContent.contains(delimiter)) {
                // Slice up each note by its delimiter
                int index = allContent.indexOf(delimiter); // Starting point of delimiter
                String content = allContent.substring(0, index);
                allContent = allContent.substring(index + delimiter.length());

                Note target = new Note(content);
                notes.add(target);
            }
            return notes;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
