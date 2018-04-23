package org.lexicon.process.stemmer;

import java.util.ArrayList;

import com.google.gson.annotations.Expose;

public class AffixGroup {

    @Expose
    public String name;

    @Expose
    public ArrayList<Affix> affixes;

    public AffixGroup(String name) {
        this(name, new ArrayList<>());
    }

    public AffixGroup(String name, ArrayList<Affix> affixes) {
        this.name = name;
        this.affixes = affixes;
    }
}
