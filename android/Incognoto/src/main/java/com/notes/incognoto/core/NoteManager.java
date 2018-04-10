/*
* Copyright (C) 2018 Incognoto
* License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
*/
package com.notes.incognoto.core;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;

import com.notes.incognoto.ActivityMain;
import com.notes.incognoto.Dialogs;
import com.notes.incognoto.R;
import com.notes.incognoto.sec.AesCbcWithIntegrity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * File management for encrypted notes
 */
public class NoteManager {
    // Feeds notes to the list view
    private static ArrayAdapter<Note> adapter;
    private static ArrayAdapter<Note> filteredAdapter;
    private static Context context;

    // Status is non-null if decryption is successful.
    // Despite this being a public variable, it does not grant access to the data
    // since the passphrase is still required for decryption.
    public static String status;
    private static String password;

    public static String fileName = "notes.encrypted";
    private static final String delimiter = "\n---\n"; // Separates note objects in document form

    // Initializes list view's adapter, loads decrypted notes if the given password is valid
    public NoteManager(final Context context) {
        this.context = context;
        this.adapter = new ArrayAdapter<Note>(context, R.layout.note_item);
        this.filteredAdapter = new ArrayAdapter<Note>(context, R.layout.note_item);

        ActivityMain.listView.setAdapter(adapter);
        ActivityMain.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (filteredAdapter.getCount() > 0) {
                    // Filter being used, show relevant items
                    ActivityMain.noteContentPreview(false, context, filteredAdapter.getItem(position));
                } else {
                    ActivityMain.noteContentPreview(false, context, adapter.getItem(position));
                }
            }
        });
    }

    // Attempt to decrypt the note content using a given password.
    // If the password does not work then return null, otherwise non-null.
    public String unlock(String passwordAttempt) {
        password = passwordAttempt;

        // If the decrypt function does not return a valid list of notes then the password did not work
        List<Note> notes = decrypt();
        if (notes == null) {
            this.status = null;
            return null;
        } else {
            this.status = "Active";
            adapter.addAll(notes);
            showTagUI();
            ActivityMain.handleIntents();
        }
        return "";
    }

    // Given a file stream return the file contents
    public static String getFileContent(final FileInputStream fileInputStream) {
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
        try {
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allContent;
    }

    // Copy the encrypted notes file to the "Downloads" folder
    public static void backup(String path) {
        // TODO: ask for storage permission if not given
        try {
            File file = new File(path, fileName);
            FileOutputStream output = new FileOutputStream(file);
            output.write((getFileContent(context.openFileInput("notes")) + "\n").getBytes());
            output.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Starts an asynchronous call to save all notes in an encrypted state
    private static void saveChanges(final ArrayAdapter<Note> adapter, boolean newThread) {
        status = "Active";
        if (newThread) {
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
                    for (int i = 0; i < tempAdapter.getCount(); i++)
                        allData += tempAdapter.getItem(i) + "\n---\n";
                    NoteManager.encrypt(allData);
                }
            };
            encryptThread.start();
        } else {
            String allData = "";
            for (int i = 0; i < adapter.getCount(); i++)
                allData += adapter.getItem(i) + "\n---\n";
            NoteManager.encrypt(allData);
        }
        showTagUI();
    }

    // Reads all tags from all notes and creates buttons at the top of the main page.
    // Tapping on a tag button shows all notes with that same tag.
    private static void showTagUI() {
        ActivityMain.tagLayout.removeAllViews(); // Always start with a fresh view. This operation is low overhead.

        // Go through each note in the list and each tag for each note to find all non-duplicate tags
        List<String> allTags = new ArrayList<>();
        for (int i = 0; i < adapter.getCount(); i++) {
            List<String> localTags = adapter.getItem(i).getTags();
            for (int j = 0; j < localTags.size(); j++) {

                // If the tag has already been added then ignore it
                String targetTag = localTags.get(j);
                if (!tagExists(allTags, targetTag))
                   allTags.add(targetTag);
            }
        }

        // Add new list of all non-duplicate tags to the UI
        for (String tag : allTags) {
            ActivityMain.tagLayout.addView(createTagButton(tag));
        }
    }

    // Used to check if a tag button for a specific tag is already being shown in the tags layout
    private static boolean tagExists(List<String> allTags, String tag) {
        for (int i = 0; i < allTags.size(); i++) {
            if (allTags.get(i).equals(tag))
                return true;
        }
        return false;
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
                ActivityMain.listView.setAdapter(filteredAdapter);
                ActivityMain.tagLayout.removeAllViews();

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
                        ActivityMain.listView.setAdapter(adapter);
                        showTagUI();
                    }
                });
                ActivityMain.tagLayout.addView(clearFilter, 0);
            }
        });
        return tagButton;
    }

    // Remove a note and reload the list view to reflect the changes
    public static void removeNote(Note note) {
        adapter.remove(note);
        saveChanges(adapter, true);
    }

    // Delete all notes and tags
    // Called by the "Delete All" menu option
    public static void clearAll() {
        adapter.clear();
        saveChanges(adapter, true);
    }

    // Add a note and reload the list view to reflect the changes
    public static void addNote(Note note) {
        adapter.insert(note, 0); // Always add to top (index 0)
        saveChanges(adapter, true);
    }

    // Clear the filter adapter to show no results in the search
    // If swapAdapter is true then make the listview show the full list of notes (default)
    public static void clearSearch(boolean swapAdapters) {
        filteredAdapter.clear();
        if (swapAdapters)
            ActivityMain.listView.setAdapter(adapter);
    }

    // Given a user defined query return all Note objects that contain terms related to the query
    public static void search(String query) {

            ArrayList<Note> noteArg = new ArrayList();
            for(int i = 0;i<adapter.getCount();i++){
                noteArg.add(adapter.getItem(i));
            }
            NoteSearch searchObj = new NoteSearch(noteArg,query);
            ArrayList<Note> foundNotes = searchObj.getFoundNoteList();

            for(int j = 0;j < foundNotes.size();j++){
                filteredAdapter.add(foundNotes.get(j));
            }

            ActivityMain.listView.setAdapter(filteredAdapter);

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
            FileOutputStream cipherStream = context.openFileOutput("notes", MODE_PRIVATE);
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

    // Override the existing internal notes file with an inbound cipher string
    public static void importFile(String cipherText) {
        try {
            FileOutputStream cipherStream = context.openFileOutput("notes", MODE_PRIVATE);
            cipherStream.write(cipherText.getBytes());
            cipherStream.close();
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
                FileOutputStream cipherStream = context.openFileOutput("salt", MODE_PRIVATE);
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

    // Re-encrypt all notes with the new password
    public static void setNewPassword(final String newPassword, final boolean showWelcomeNotes) {
        password = newPassword;

        if (showWelcomeNotes) {
            // Add the default notes to the encrypted contents so serve as examples
            adapter.add(new Note(context.getResources().getString(R.string.welcome_note_1)));
            adapter.add(new Note(context.getResources().getString(R.string.welcome_note_2)));
            adapter.add(new Note(context.getResources().getString(R.string.welcome_note_3)));
        }
        saveChanges(adapter, false);
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
            try {
                AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac =
                        new AesCbcWithIntegrity.CipherTextIvMac(allContent);
                allContent = AesCbcWithIntegrity.decryptString(cipherTextIvMac, key);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                return null;
            } catch (IllegalArgumentException e) {
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
