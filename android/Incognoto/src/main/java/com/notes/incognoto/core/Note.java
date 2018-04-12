/*
* Copyright (C) 2018 Incognoto
* License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
*/
package com.notes.incognoto.core;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Each Note is a row in the database, it's the core object stored.
 */
public class Note {
    private String content;

    // Dynamically assigned as the note's content changes. Not stored on the back end.
    private ArrayList<String> tags;

    // Get the array list of tags
    public ArrayList<String> getTags() { return this.tags; }

    // Minimal requirements of a note. Notes do not have to have tags.
    public Note(String content) {
        setNoteContent(content);
    }

    // Set the raw string content of a single note.
    // Must be called in order to re-parse the content for tags.
    // Do not reference tags from the object directly.
    public void setNoteContent(String content) {
        this.content = content;
        parseTags();
    }

    // All notes have content. This includes the "#tags" that were input.
    public String getNoteContent() { return content; }

    // Search through the note content for items with "#tags"
    private void parseTags() {
        Pattern tagPattern = Pattern.compile("#(\\S+)");
        ArrayList<String> parsedTags = new ArrayList<String>();
        Matcher matcher = tagPattern.matcher(content);
        while (matcher.find())
            parsedTags.add(matcher.group(0).toLowerCase());
        this.tags = parsedTags;
    }

    @Override
    public String toString() {
        return getNoteContent();
    }
}
