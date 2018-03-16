package com.notes.sum.sec;

import java.util.ArrayList;

/**
 * Created by Luke on 3/14/2018.
 */

public class SearchManager {
    // Takes array of notes and parses for string
    // String is the search string to be parsed

    private ArrayList<String> noteList = new ArrayList();
    private ArrayList<String> foundNoteList = new ArrayList();
    private String searchString;

    public void setNoteList(ArrayList<String> nl){
        this.noteList = nl;
    }
    public ArrayList<String> getNoteList(){
        return this.noteList;
    }

    public void setFoundNoteList(ArrayList<String> fnl){
        this.foundNoteList = fnl;
    }
    public ArrayList<String> getFoundNoteList(){
        return this.foundNoteList;
    }

    public void setSearchString(String ss){
        this.searchString = ss;
    }
    public String getSearchString(){
        return this.searchString;
    }

    public ArrayList<String> search(ArrayList<String> noteList, String searchString){
        //yadayadayada logic for search goes here
        //Set the found note list before final
        //Remember that this method will have to be recursive for and/or/not
        return getFoundNoteList();
    }

    private ArrayList<String> removeNotNotes(ArrayList<String> tempNoteList, String notKeyword){
        //Parses tempNoteList to remove any notes that contain notKeyword
        return getFoundNoteList();
    }

}
