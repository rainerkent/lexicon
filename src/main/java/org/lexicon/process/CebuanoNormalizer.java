package org.lexicon.process;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CebuanoNormalizer {

    // static methods only
    private CebuanoNormalizer() {}

    public static String normalize(String word) {
        return removeDiacritics(word).trim().toLowerCase()
                .replaceAll("z", "s") // Old orthography stuff
                .replaceAll("ce", "si")
                .replaceAll("ci", "si")
                .replaceAll("gui", "gi")
                .replaceAll("qui", "ki")
                .replaceAll("c", "k")
                .replaceAll("j", "h")

                .replaceAll("f", "p")
                .replaceAll("v", "b")
                .replaceAll("o", "u")
                .replaceAll("e", "i");
    }

    private static String removeDiacritics(String word) {
        return Normalizer.normalize(word, Normalizer.Form.NFD) // removes diacritics
                .replaceAll("[^\\p{ASCII}]", ""); // removes non-ASCII characters?
    }

    public static List<String> normalize(List<String> list) {
        List<String> result = new ArrayList<>();
        for (String word : list) {
            result.add(normalize(word));
        }
        return result;
    };

    public static Set<String> normalize(Set<String> set) {
        Set<String> result = new HashSet<>();
        for (String word : set) {
            result.add(normalize(word));
        }
        return result;
    }
}
