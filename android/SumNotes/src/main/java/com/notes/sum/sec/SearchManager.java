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
    private ArrayList<String> searchWords = new ArrayList();
    private ArrayList<Integer> searchOperands = new ArrayList();
    //USE FIRST DUMMY VALUE OF -1, HAVE 0 REP AND, 1 REP OR, 2 REP NOT

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

    public void initializeDumboVals(){
        for(Integer i = 0; i <= 10; i++){
            noteList.add(i.toString());
        }
    }

    public void search(ArrayList<String> noteList, String searchString){
        setNoteList(noteList);
        setSearchString(searchString);
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
        for(int i = 0; i < noteList.size(); i++){
            if(noteList.get(i).equals(search)){
                addToFoundNotes(noteList.get(i));
            }
        }
    }
    private void andOperation(String search){
        for(int i = 0; i < foundNoteList.size(); i++){
            if(!foundNoteList.get(i).equals(search)){
                foundNoteList.remove(i);
            }
        }
    }
    private void orOperation(String search){
        for(int i = 0; i < noteList.size(); i++){
            if(noteList.get(i).equals(search)){
                addToFoundNotes(noteList.get(i));
            }
        }
    }
    private void notOperation(String search){
        for(int i = 0; i < foundNoteList.size(); i++){
            if(foundNoteList.get(i).equals(search)){
                foundNoteList.remove(i);
            }
        }
    }

    private boolean contains(String note, String search){
        //regular expression
        return false;
    }

}