package org.lexicon.process.stemmer;

import java.util.ArrayList;

public class Derivation {

    public String root;
    public ArrayList<Affix> affixes;

    public Derivation(String root, ArrayList<Affix> affixes) {
        this.root = root;
        this.affixes = affixes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Affix affix : affixes) {
            sb.append(affix.form);
            sb.append(" + ");
        }
        sb.append(root);

        return sb.toString();
    }
}
