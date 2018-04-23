package org.lexicon.process.stemmer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.annotations.Expose;

public class AffixPattern {

    @Expose
    public String pattern;

    @Expose
    public String root;

    @Expose
    public String compiledPattern;

    public static boolean patternApplies(AffixPattern pattern, String word) {
        String compiledPattern = pattern.compiledPattern;
        if (compiledPattern == null) {
            throw new NullPointerException("Pattern was not compiled");
        }
        else {
            // return word.matches(compiledPattern);
            Matcher matcher = Pattern.compile(compiledPattern).matcher(word);
            return matcher.matches();
        }
    }

    public static String stripPattern(AffixPattern pattern, String word) {
        String compiledPattern = pattern.compiledPattern;

        if (patternApplies(pattern, word)) {
            String replacement = pattern.root != null ? pattern.root : "";
            return word.replaceAll(compiledPattern, replacement);
        }
        else {
            return null;
        }
    }
}
