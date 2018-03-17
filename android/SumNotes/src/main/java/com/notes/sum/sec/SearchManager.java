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
    private void addToFoundNotes(String match){
        this.foundNoteList.add(match);
    }

    public void setSearchString(String ss){
        this.searchString = ss;
    }
    public String getSearchString(){
        return this.searchString;
    }

    public void search(ArrayList<String> noteList, String searchString){
        if(!parseforops(searchString)) {
            for (int i = 0; i < noteList.size(); i++) {
                if (contains(noteList.get(i), searchString)) {
                    addToFoundNotes(noteList.get(i));
                }
            }
            //yadayadayada logic for search goes here
            //Set the found note list before final
            //Remember that this method will have to be recursive for and/or/not
        }
    }

    //Logic for "and" operation in place
    private boolean parseforops(String search){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < search.length(); i++){
            if(search.charAt(i) == '+' || search.charAt(i) == '|' || search.charAt(i) == '~'){
                search(noteList,sb.toString());
                sb = new StringBuilder();
            }
            else{
                sb.append(search.charAt(i));
            }
        }
        return false;
    }
    private boolean contains(String note, String search){
        //regular expression
        return false;
    }
    private ArrayList<String> removeNotNotes(ArrayList<String> tempNoteList, String notKeyword){
        //Parses tempNoteList to remove any notes that contain notKeyword
        return getFoundNoteList();
    }

}
