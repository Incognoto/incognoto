/*
 * Copyright (C) 2018 Incognoto
 * License: GPL version 2 or higher http://www.gnu.org/licenses/gpl.html
 */
package com.notes.incognoto.core;

import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

/**
 * Created by Luke on 3/14/2018.
 */

public class NoteSearch {
    // Takes array of notes and parses for string
    // String is the search string to be parsed

    private ArrayList<Note> noteList = new ArrayList();
    private ArrayList<Note> foundNoteList = new ArrayList();
    private String searchString;
    private ArrayList<String> searchWords = new ArrayList();
    private ArrayList<Integer> searchOperands = new ArrayList();
    //USE FIRST DUMMY VALUE OF -1, HAVE 0 REP AND, 1 REP OR, 2 REP NOT

    public void setNoteList(ArrayList<Note> nl){
        this.noteList = nl;
    }
    public ArrayList<Note> getNoteList(){
        return this.noteList;
    }

    public void setFoundNoteList(ArrayList<Note> fnl){
        this.foundNoteList = fnl;
    }
    public ArrayList<Note> getFoundNoteList(){return this.foundNoteList;}
    private void addToFoundNotes(Note match){
        this.foundNoteList.add(match);
    }

    public void setSearchString(String ss){
        this.searchString = ss;
    }
    public String getSearchString(){
        return this.searchString;
    }

    public NoteSearch(ArrayList<Note> masterList, String query){
        setNoteList(masterList);
        setSearchString(query);
        search();
    }

    public void search(){
        parseforops(searchString);

        for(int i = 0; i < searchOperands.size(); i++){
            int j  = searchOperands.get(i);
            if(j==-1)
                initSearch(searchWords.get(i));
            else if(j==0)
                andOperation(searchWords.get(i));
            else if(j==1)
                orOperation(searchWords.get(i));
            else if(j==2)
                notOperation(searchWords.get(i));
        }

    }

    //Logic for "or" operation in place
    private void parseforops(String search){
        StringBuilder sb = new StringBuilder();
        searchOperands.add(-1);
        for(int i = 0; i < search.length(); i++){
            if(search.charAt(i) == '+'){
                sb = cutOffSpace(sb);
                searchWords.add(sb.toString().toLowerCase());
                searchOperands.add(0);
                if(i+1<search.length()){if(search.charAt(i+1) == ' '){i++;}}
                sb = new StringBuilder();
            }
            else if(search.charAt(i) == '|' ){
                sb = cutOffSpace(sb);
                searchWords.add(sb.toString().toLowerCase());
                searchOperands.add(1);
                if(i+1<search.length()){if(search.charAt(i+1) == ' '){i++;}}
                sb = new StringBuilder();
            }
            else if(search.charAt(i) == '~'){
                sb = cutOffSpace(sb);
                searchWords.add(sb.toString().toLowerCase());
                searchOperands.add(2);
                if(i+1<search.length()){if(search.charAt(i+1) == ' '){i++;}}
                sb = new StringBuilder();
            }
            else{
                sb.append(search.charAt(i));
            }
        }
        sb = cutOffSpace(sb);
        searchWords.add(sb.toString().toLowerCase());
    }

    private void initSearch(String search){
        for(int i = 0; i < noteList.size(); i++){
            if(noteList.get(i).getNoteContent().toLowerCase().contains(search)){
                addToFoundNotes(noteList.get(i));
            }
        }
    }
    private void andOperation(String search){
        for(int i = 0; i < foundNoteList.size(); i++){
            if(!foundNoteList.get(i).getNoteContent().toLowerCase().contains(search)){
                foundNoteList.remove(foundNoteList.get(i));
                i--;
            }
        }
    }
    private void orOperation(String search){
        for(int i = 0; i < noteList.size(); i++){
            if(noteList.get(i).getNoteContent().toLowerCase().contains(search) && notDuplicate(noteList.get(i))){
                addToFoundNotes(noteList.get(i));
            }
        }
    }
    private void notOperation(String search) {
        for (int i = 0; i < foundNoteList.size(); i++) {
            if (foundNoteList.get(i).getNoteContent().toLowerCase().contains(search)) {
                foundNoteList.remove(foundNoteList.get(i));
                i--;
            }
        }
    }

    private boolean notDuplicate(Note note){
        for(int i = 0; i < foundNoteList.size(); i++){
            if(foundNoteList.get(i) == note){
                return false;
            }
        }
        return true;
    }

    private StringBuilder cutOffSpace(StringBuilder sb){
        int length = sb.length();
        int lastIndex = length-1;
        if(length > 0){
            if(sb.charAt(lastIndex) == ' '){
                sb.deleteCharAt(lastIndex);
            }
        }
        return sb;
    }
}