package com.notes.sum.sec;

import android.app.ActionBar;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.notes.sum.ActivityMain;
import com.notes.sum.R;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File management for encrypted notes
 */
public class NoteManager {
    // Feeds notes to the list view
    private static ArrayAdapter<Note> adapter;
    private static ArrayAdapter<Note> filteredAdapter;
    private static Context context;
    private static String password;
    private static LinearLayout tagLayout;
    private static ListView listView;

    // Separates note objects in document form
    private static final String delimiter = "\n---\n";

    // Initializes list view's adapter, loads decrypted notes if the given password is valid
    public NoteManager(final ListView listView, final Context context, final String password, final LinearLayout tagLayout) {
        this.context = context;
        this.password = password;
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

        // TODO: tell user there are no notes or failure to decrypt
        List<Note> notes = NoteManager.decrypt();
        if (notes != null) {
            adapter.addAll(notes);
            showTagUI();
        }
    }

    public static void backup() {
        // TODO: export a copy of the encrypted notes to another application (share intent with file)
    }

    public static void restore() {
        // TODO: import an encrypted backup from another application (via share intent with file)
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

    private static void showTagUI() {
        tagLayout.removeAllViews(); // Start with a fresh view. This operation is low overhead.

        // Go through all items in the list view
        for (int i = 0; i < adapter.getCount(); i++) {
            final Note note = adapter.getItem(i);

            // For each tag in a note add a button to the main activity's tag section.
            // TODO: avoid duplicates
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
            AesCbcWithIntegrity.SecretKeys key = AesCbcWithIntegrity.generateKeyFromPassword(password, getSalt());
            AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac =
                    new AesCbcWithIntegrity.CipherTextIvMac(allContent);
            allContent = AesCbcWithIntegrity.decryptString(cipherTextIvMac, key);

            List<Note> notes = new ArrayList<Note>();
            while(allContent.contains(delimiter)) {
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return new ArrayList<Note>();
    }
}
