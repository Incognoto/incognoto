/*
* Copyright (C) 2018 Incognoto
* License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
*/
package com.notes.incognoto.core;

import android.widget.ArrayAdapter;

import java.util.ArrayList;

/**
 * Created by Luke on 3/14/2018.
 */

public class NoteSearch {
    // Takes array of notes and parses for string
    // String is the search string to be parsed

    private ArrayAdapter<Note> noteList;
    private ArrayAdapter<Note> foundNoteList;
    private String searchString;
    private ArrayList<String> searchWords = new ArrayList();
    private ArrayList<Integer> searchOperands = new ArrayList();
    //USE FIRST DUMMY VALUE OF -1, HAVE 0 REP AND, 1 REP OR, 2 REP NOT

    public void setNoteList(ArrayAdapter<Note> nl){
        this.noteList = nl;
    }
    public ArrayAdapter<Note> getNoteList(){
        return this.noteList;
    }

    public void setFoundNoteList(ArrayAdapter<Note> fnl){
        this.foundNoteList = fnl;
    }
    public ArrayAdapter<Note> getFoundNoteList(){return this.foundNoteList;}
    private void addToFoundNotes(Note match){
        this.foundNoteList.add(match);
    }

    public void setSearchString(String ss){
        this.searchString = ss;
    }
    public String getSearchString(){
        return this.searchString;
    }

    public NoteSearch(ArrayAdapter<Note> masterList, String query){
        setNoteList(masterList);
        setSearchString(query);
        search();
    }

    public void search(){
        parseforops(searchString);

        for(int i = 0; i < searchOperands.size(); i++){
            int j  = searchOperands.get(i);
            switch(j){
                case -1:
                    initSearch(searchWords.get(i));
                case 0:
                    andOperation(searchWords.get(i));
                case 1:
                    orOperation(searchWords.get(i));
                case 2:
                    notOperation(searchWords.get(i));
            }
        }

    }

    //Logic for "or" operation in place
    private void parseforops(String search){
        StringBuilder sb = new StringBuilder();
        searchOperands.add(-1);
        for(int i = 0; i < search.length(); i++){
            if(search.charAt(i) == '+'){
                searchWords.add(sb.toString());
                searchOperands.add(0);
                sb = new StringBuilder();
            }
            else if(search.charAt(i) == '|' ){
                searchWords.add(sb.toString());
                searchOperands.add(1);
                sb = new StringBuilder();
            }
            else if(search.charAt(i) == '~'){
                searchWords.add(sb.toString());
                searchOperands.add(2);
                sb = new StringBuilder();
            }
            else{
                sb.append(search.charAt(i));
            }
        }
        searchWords.add(sb.toString());
    }

    private void initSearch(String search){
        for(int i = 0; i < noteList.getCount(); i++){
            if(noteList.getItem(i).getNoteContent().contains(search)){
                addToFoundNotes(noteList.getItem(i));
            }
        }
    }
    private void andOperation(String search){
        for(int i = 0; i < foundNoteList.getCount(); i++){
            if(!foundNoteList.getItem(i).getNoteContent().contains(search)){
                foundNoteList.remove(foundNoteList.getItem(i));
                i--;
            }
        }
    }
    private void orOperation(String search){
        for(int i = 0; i < noteList.getCount(); i++){
            if(noteList.getItem(i).getNoteContent().contains(search)){
                addToFoundNotes(noteList.getItem(i));
            }
        }
    }
    private void notOperation(String search){
        for(int i = 0; i < foundNoteList.getCount(); i++){
            if(foundNoteList.getItem(i).getNoteContent().contains(search)){
                foundNoteList.remove(foundNoteList.getItem(i));
                i--;
            }
        }
    }

//    private boolean contains(Note note, String search){
//        String noteString = note.getNoteContent();
//        int i = 0;
//        int j = search.length() - 1;
//
//        while(j < noteString.length()){
//            if(noteString.substring(i,j).equals(search)){
//                return true;
//            }
//            i++;
//            j++;
//        }
//        return false;
//    }

}