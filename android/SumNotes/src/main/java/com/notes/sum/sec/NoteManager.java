package com.notes.sum.sec;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.notes.sum.ActivityMain;
import com.notes.sum.R;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * File management for encrypted notes
 */
public class NoteManager {
    // Feeds notes to the list view
    protected static ArrayAdapter<Note> adapter;
    private static Context context;
    private static String password;

    // Separates note objects in document form
    private static final String delimiter = "\n---\n";

    // Initializes list view's adapter, loads decrypted notes if the given password is valid
    public NoteManager(final ListView listView, final Context context, final String password, final LinearLayout tagLayout) {
        this.context = context;
        this.password = password;
        this.adapter = new ArrayAdapter<Note>(context, R.layout.note_item);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ActivityMain.noteContentPreview(false, context, adapter.getItem(position));
            }
        });

        // TODO: tell user there are no notes or failure to decrypt
        List<Note> notes = NoteManager.decrypt();
        if (notes != null) {
            adapter.addAll(notes);
        }
        new TagManager(context, tagLayout);
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
        Thread thread = new Thread() {
            @Override
            public void run() {
                String allData = "";
                for(int i = 0; i < tempAdapter.getCount(); i++) {
                    allData += tempAdapter.getItem(i) + "\n---\n";
                }
                NoteManager.encrypt(allData);
            }
        };
        thread.start();
    }

    // Remove a note and reload the list view to reflect the changes
    public static void removeNote(Note note) {
        adapter.remove(note);
        saveChanges(adapter);
        TagManager.removeTags(note.getTags());
    }

    // Delete all notes and tags
    // Called by the "Delete All" menu option
    public static void clearAll() {
        adapter.clear();
        TagManager.clearTags();
        saveChanges(adapter);
    }

    // Add a note and reload the list view to reflect the changes
    public static void addNote(Note note) {
        adapter.insert(new Note(note.getNoteContent()), 0); // Always add to top (index 0)
        saveChanges(adapter);
        TagManager.addTags(TagManager.findTags(note));
    }

    /**
     * Reading and Writing encrypted Note objects
     */
    // TODO: generate salt once and store in sandboxed app storage
    private static byte[] salt = new byte[]{'s', 'a', 'l', 't'};

    public static void encrypt(String message) {
        try {
            // Encrypt plaintext
            //String salt = saltString(generateSalt());
            AesCbcWithIntegrity.SecretKeys key = AesCbcWithIntegrity.generateKeyFromPassword(password, salt);
            AesCbcWithIntegrity.CipherTextIvMac cipherTextIvMac = AesCbcWithIntegrity.encrypt(message, key);
            String ciphertextString = cipherTextIvMac.toString();
            Log.e("NOTES", ciphertextString);

            // Write cipher to local file
            FileOutputStream outputStream = context.openFileOutput("notes", Context.MODE_PRIVATE);
            outputStream.write(ciphertextString.getBytes());
            outputStream.close();
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

    public static List<Note> decrypt() {
        try {
            // Read the file contents and decrypt it all
            FileInputStream fileInputStream = context.openFileInput("notes");
            BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
            String line;
            String allContent = "";
            while ((line = br.readLine()) != null) {
                allContent += line;
            }
            //String salt = saltString(generateSalt()); // TODO: access saved salt
            AesCbcWithIntegrity.SecretKeys key = AesCbcWithIntegrity.generateKeyFromPassword(password, salt);
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
        // TODO: handle all errors
        return null;
    }
}
