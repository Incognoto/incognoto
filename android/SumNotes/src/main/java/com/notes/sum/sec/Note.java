/*
* Copyright (C) 2018 Server Under the Mountain (SUM)
* License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
 */
package com.notes.sum.sec;

import java.util.ArrayList;

/**
 * Each Note is a row in the database, it's the core object that's used within SUM Note.
 */
public class Note {
    private String content;
    private ArrayList<String> tags;

    // Replaces the entire arraylist of tags
    public void setTags(ArrayList<String> overrideTags) { this.tags = overrideTags; }

    // Add one tag to the array list
    public void addTag(String tag) { this.tags.add(tag); }

    // Remove one tag from the array list
    public void removeTag(String tag) { this.tags.remove(tag); }

    // Get the array list of tags
    public ArrayList<String> getTags() { return this.tags; }

    // Minimal requirements of a note. Notes do not have to have tags.
    public Note(String content) {
        this.content = content;
        this.tags = new ArrayList<String>();
    }

    // All notes have content. This includes the "#tags" that were input.
    public String getNoteContent() { return content; }

    @Override
    public String toString() {
        return getNoteContent();
    }

}
