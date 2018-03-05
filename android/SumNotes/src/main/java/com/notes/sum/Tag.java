/*
* Copyright (C) 2018 Server Under the Mountain (SUM)
* License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
 */
package com.notes.sum;

import android.app.ActionBar;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tag extends ActivityMain {

    // Add tag buttons into a horizontal scrolling view below the search bar when
    // the search bar or dropdown is in focus
    // Add each string tag as a button in the horizontal scrolling view under search bar
    public static void loadTags(Context context, List<String> tags) {
        for(String tag : tags) {
            tagsLayout.addView(createTagButton(context, tag));
        }
    }

    // Run on start: loops through all notes to look for hashtags then adds each hashtag
    //               as a button under the search bar.
    // Starts in a new background thread since the time varies and does not require user interaction
    public static void compileTags(final Context context) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                // Loop through the rows in the database and find each instance of a tag
                Pattern tagPattern = Pattern.compile("#(\\w+)");
                ArrayList<String> tags = new ArrayList<String>();
                for(int i = 0; i < adapter.getCount(); i++) {
                    String content = adapter.getItem(i).getNoteContent();
                    Matcher matcher = tagPattern.matcher(content);
                    while (matcher.find()) {
                        tags.add(matcher.group(0));
                    }
                }
                loadTags(context, tags);
            }
        };
        t.run();
    }

    // Given a string return a button with the text inside it
    private static Button createTagButton(Context context, String tagText) {
        Button tagButton = new Button(context);
        tagButton.setText(tagText);
        tagButton.setLayoutParams(new LinearLayout.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
        tagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: show all notes with this tag
            }
        });
        return tagButton;
    }
}
