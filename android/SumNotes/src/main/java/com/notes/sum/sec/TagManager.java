/*
* Copyright (C) 2018 Server Under the Mountain (SUM)
* License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
 */
package com.notes.sum.sec;

import android.app.ActionBar;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TagManager {

    private static LinearLayout tagLayout;
    private static ArrayList<String> tags;
    private static Context context;

    public TagManager(Context context, LinearLayout tagLayout) {
        this.tagLayout = tagLayout;
        this.context = context;
        this.tags = new ArrayList<>();
        loadTagsFromDatabase();
    }

    // Add tag buttons into a horizontal scrolling view in the main view
    public static void addTags(ArrayList<String> tags) {
        for(String tag : tags) {
            TagManager.tags.add(tag);

            Button tagButton = new Button(TagManager.context);
            tagButton.setText(tag);
            tagButton.setLayoutParams(new LinearLayout.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
            tagButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: show all notes with this tag
                }
            });
            TagManager.tagLayout.addView(tagButton);
        }
    }

    // Given a string return the list of #tags in it
    public static ArrayList<String> findTags(final Note note) {
        Pattern tagPattern = Pattern.compile("#(\\w+)");
        ArrayList<String> parsedTags = new ArrayList<String>();
        Matcher matcher = tagPattern.matcher(note.getNoteContent());
        while (matcher.find())
            parsedTags.add(matcher.group(0));
        note.setTags(parsedTags);
        return parsedTags;
    }

    // Run on start: loops through all notes to look for hashtags then adds each hashtag
    //               as a button under the search bar.
    // Starts in a new background thread since the time varies and does not require user interaction
    public static void loadTagsFromDatabase() {
        final ArrayAdapter<Note> tempAdapter = NoteManager.adapter;
        final Thread t = new Thread() {
            @Override
            public void run() {
                // Loop through the rows in the database and find each instance of a tag
                // Copy an instance of the adapter since this is async
                for (int i = 0; i < tempAdapter.getCount(); i++) {
                    addTags(findTags(tempAdapter.getItem(i)));
                }
            }
        };
        t.run();
    }

    // Remove all tag buttons from the main screen
    public static void clearTags() {
        TagManager.tagLayout.removeAllViews();
    }

    // Remove one or more tag buttons from the main screen
    public static void removeTags(ArrayList<String> removals) {
        TagManager.clearTags();
        TagManager.tags.removeAll(removals);
        TagManager.addTags(TagManager.tags);
    }
}
