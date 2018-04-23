package org.lexicon.process.stemmer;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;

public class Affix {

    @Expose
    public String form;

    @Expose
    public String label;

    @Expose
    public String rootType;

    @Expose
    public ArrayList<AffixPattern> patterns;

    public List<String> getRootCandidates(String word) {
        List<String> rootCandidates = new ArrayList<>();
        for (AffixPattern pattern : patterns) {
            if (AffixPattern.patternApplies(pattern, word)) {
                String stripped = AffixPattern.stripPattern(pattern, word);
                if (stripped != null) {
                    rootCandidates.add(stripped);
                }
            }
        }
        return rootCandidates;
    }
}
