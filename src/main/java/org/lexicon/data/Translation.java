package org.lexicon.data;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

public class Translation implements Comparable<Translation> {

    @Expose
    private String word;

    @Expose
    private String pos;

    public Translation(JsonObject obj) {
        this.word = obj.get("translation").getAsString();
        this.pos = obj.get("pos").getAsString();
    }

    public Translation(String word, String pos) {
        this.word = word;
        this.pos = pos;
    }

    public String getWord() {
        return word;
    }

    public String getPOS() {
        return pos;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public void getPOS(String pos) {
        this.pos = pos;
    }

    @Override
    public int compareTo(Translation other) {
        return this.getWord().compareTo(other.getWord());
    }
}
