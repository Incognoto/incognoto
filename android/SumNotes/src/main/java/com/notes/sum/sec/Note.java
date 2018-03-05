/*
* Copyright (C) 2018 Server Under the Mountain (SUM)
* License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
 */
package com.notes.sum.sec;

import android.content.Context;
import android.util.Log;

import com.notes.sum.ActivityMain;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Each Note is a row in the database, it's the core object that's used within SUM Note.
 */
public class Note {
    private int id;
    private String content;

    // Separates note objects in document form
    private static final String delimiter = "\n---\n";

    public Note(int id, String content) {
        this.id = id;
        this.content = content;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNoteContent() { return content; }
    public void setNoteContent(String content) { this.content = content; }

    @Override
    public String toString() {
        return getNoteContent();
    }


    /**
     * File management for encrypted notes
     */
    public static void backup() {
        // TODO: export a copy of the encrypted notes to another application (share intent with file)
    }

    public static void restore() {
        // TODO: import an encrypted backup from another application (via share intent with file)
    }

    // Called every time a note is added, edited, or deleted.
    // Saves the list view's items to the encrypted database.
    public static void saveChanges(final Context context, final String password) {
        // This may be resource intensive, depending on the number of notes and the content,
        // so start this process on a new thread.
        Thread thread = new Thread() {
            @Override
            public void run() {
                String allData = "";
                for(int i = 0; i < ActivityMain.adapter.getCount(); i++) {
                    allData += ActivityMain.adapter.getItem(i) + "\n---\n";
                }
                Note.encrypt(context, allData, password);
                // TODO: re-run tag parsing
            }
        };
        thread.start();
    }

    /**
     * Reading and Writing encrypted Note objects
     */
    // TODO: generate salt once and store in sandboxed app storage
    private static byte[] salt = new byte[]{'s', 'a', 'l', 't'};

    public static void encrypt(final Context context, final String message, final String password) {
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

    public static List<Note> decrypt(final Context context, final String password) {
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
            int i = 0;
            while(allContent.contains(delimiter)) {
                // Slice up each note by its delimiter
                int index = allContent.indexOf(delimiter); // Starting point of delimiter
                String content = allContent.substring(0, index);
                allContent = allContent.substring(index + delimiter.length());

                Note target = new Note(i, content);
                notes.add(target);
                i++;
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
