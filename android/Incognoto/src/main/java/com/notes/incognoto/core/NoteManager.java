/*
* Copyright (C) 2018 Incognoto
* License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
*/
package com.notes.incognoto.core;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Environment;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.notes.incognoto.ActivityMain;
import com.notes.incognoto.Dialogs;
import com.notes.incognoto.R;
import com.notes.incognoto.sec.AesCbcWithIntegrity;
import com.notes.incognoto.sec.PasswordGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

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

    // Only called once the app is installed: generate a default password and add welcome notes
    private void handleFirstStartup() {
        NoteManager.newDefaultKey(context);
        this.password = getDefaultPassword(context);

        // Insert all and don't save changes until all have been added to avoid having too many
        // background threats, which `addNote` would spin up.
        adapter.insert(new Note(context.getResources().getString(R.string.welcome_note_3)), 0);
        adapter.insert(new Note(context.getResources().getString(R.string.welcome_note_2)), 0);
        adapter.insert(new Note(context.getResources().getString(R.string.welcome_note_1)), 0);
        saveChanges(adapter);
    }

    // Attempt to decrypt the note content using a given password.
    // If the password does not work then return null, otherwise non-null.
    public String unlock(String password) {

        // Unlock without password prompt if this is the first time running the app
        SharedPreferences sharedPrefs = context.getSharedPreferences("temp", MODE_PRIVATE);
        if (sharedPrefs.getBoolean("firstRun", true)) {
            // Run this only once when a new users installs the app
            sharedPrefs.edit().putBoolean("firstRun", false).commit();
            this.status = "Active";
            handleFirstStartup();
            showTagUI();
            return "";
        }

        // If the master password was not changed from the original generated one then use it
        if (usingDefaultPassword(context)) {
            this.password = getDefaultPassword(context);
            this.status = "Active";
        } else {
            this.password = password;
        }

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
    public static void backup() {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getPath(), fileName);
            FileOutputStream output = new FileOutputStream(file);
            output.write((getFileContent(context.openFileInput("notes")) + "\n").getBytes());
            output.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Copy a text file (encrypted or plaintext) from an external location into this app
    public void restore() {
        // First delete all notes
        Dialogs.showDeleteConfirmation(
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
            PasswordGenerator passwordGenerator = new PasswordGenerator.PasswordGeneratorBuilder()
                    .useDigits(true)
                    .useLower(true)
                    .useUpper(true)
                    .build();
            String keyPhrase = passwordGenerator.generate();
            FileOutputStream cipherStream = context.openFileOutput("default", MODE_PRIVATE);
            cipherStream.write(keyPhrase.getBytes());
            cipherStream.close();
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

    public static String getDefaultPassword(final Context context) {
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
        ActivityMain.tagLayout.removeAllViews(); // Always start with a fresh view. This operation is low overhead.

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

    // Clear the filter adapter to show no results in the search
    // If swapAdapter is true then make the listview show the full list of notes (default)
    public static void clearSearch(boolean swapAdapters) {
        filteredAdapter.clear();
        if (swapAdapters)
            ActivityMain.listView.setAdapter(adapter);
    }

    // Given a user defined query return all Note objects that contain terms related to the query
    public static void search(String query) {
        ArrayAdapter<Note> tempAdapter = adapter;
        NoteSearch searchManager = new NoteSearch(adapter, query);
        filteredAdapter = searchManager.getFoundNoteList();
        List<Note> found = new ArrayList<>();

//        for (int i = 0; i < adapter.getCount(); i++) {
//            Note note = adapter.getItem(i);
//            String content = note.getNoteContent();
//
//            // TODO: This currently looks for exact string matches. It should split the query into words and search for each one
//            if (content.contains(query)) {
//                filteredAdapter.add(note);
//            }
//        }
        // TODO: The found notes should be ordered by relevance. If a note's content matches the search string perfectly then use found.add(0, note) to add it to the top (index zero)
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

    @TargetApi(Build.VERSION_CODES.M)
    public static void enrollFingerprint() {
        // Enroll flow for binding a password to a fingerprint
        try {
            // Register the custom password with keystore so the fingerprint is tied to the key phrase
            AesCbcWithIntegrity.SecretKeys key = AesCbcWithIntegrity.generateKeyFromPassword(password, getSalt());
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.setEntry("incognoto", new KeyStore.SecretKeyEntry(key.getIntegrityKey()),
                    new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC).build());
            Log.e("INCOGNOTO", "enrolled print");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public static void fingerprintAuth() {
        // Fingerprint API only available on from Android 6.0 (M)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // ContextCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT);
            // TODO: check if we have permission, prompt user if we don't
            FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
            if (!fingerprintManager.isHardwareDetected()) {
                // Device doesn't support fingerprint authentication
                Log.e("INCOGNOTO", "fingerprint hardware not detected");
                return;
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                // User hasn't enrolled any fingerprints to authenticate with
                // They must go into the Android settings and register the fingerprint scanner
                Log.e("INCOGNOTO", "fingerprint has not enrolled");
            } else {
                // Everything is ready for fingerprint authentication
                try {
                    Log.e("INCOGNOTO", "listening for fingerprint");
                    // Retrieve the fingerprint that was bound to the password in the key store

                    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
//                    char[] password = "some password".toCharArray();
                    ks.load(null, password.toCharArray());
                    // Store away the keystore.
                    FileOutputStream fos = new FileOutputStream("incognoto");
                    ks.store(fos, password.toCharArray());
                    fos.close();


//                    KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                    SecretKey keyStoreKey = (SecretKey) ks.getKey("incognoto", password.toCharArray());
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, keyStoreKey);
                    FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);
                    FingerprintManager.AuthenticationCallback authenticationCallback = new FingerprintManager.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            Log.e("INCOGNOTO", "fingerprint auth error");
                            Toast.makeText(context, "auth error", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            Log.e("INCOGNOTO", "fingerprint success");
                            Toast.makeText(context, "auth success", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            Log.e("INCOGNOTO", "fingerprint failed");
                            Toast.makeText(context, "auth failed", Toast.LENGTH_SHORT).show();
                        }
                    };
                    // starts listening for fingerprints with the initialised crypto object
                    fingerprintManager.authenticate(cryptoObject, new CancellationSignal(), 0,
                            authenticationCallback,null);
//                    FingerprintManager.AuthenticationResult result = new FingerprintManager.AuthenticationResult()

                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
