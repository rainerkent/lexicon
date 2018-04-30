package org.lexicon.process.stemmer;

import static org.lexicon.util.ResourceUtil.CEBUANO_STEMMER_JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.lexicon.process.CebuanoNormalizer;
import org.lexicon.process.GsonHelper;
import org.lexicon.process.dictionary.CebuanoDictionary;
import org.lexicon.util.ResourceUtil;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

/**
 * Singleton class
 *
 */
public class CebuanoStemmer {

    private static CebuanoStemmer instance;

    @Expose
    private List<AffixGroup> groups;

    @Expose
    private Map<String, String> constants;

    @Expose
    private String language;

    private static Map<String, String> rootCache = new HashMap<>();

    private CebuanoStemmer() {}

    private static CebuanoStemmer getInstance() {
        if (instance == null) {
            JsonElement stemmerJson = ResourceUtil.parseJson(CEBUANO_STEMMER_JSON);
            Gson gson = GsonHelper.createGson();
            instance = gson.fromJson(stemmerJson, new TypeToken<CebuanoStemmer>() {}.getType());
        }
        return instance;
    }

    public static List<AffixGroup> getGroups() {
        return getInstance().groups;
    }

    public static Map<String, String> getConstants() {
        return getInstance().constants;
    }

    public static String getLanguage() {
        return getInstance().language;
    }

    public static String getRootWord(String word) {
        String cachedResult = rootCache.get(word);
        if (cachedResult != null) {
            return cachedResult;
        }
        else {
            List<Derivation> derivations = findDerivations(word);
            if (derivations.size() == 0) { return word; }
            String rootWord = derivations.get(derivations.size() - 1).root;
            // System.out.println(word + " -> " + rootWord);
            rootCache.put(word, rootWord);
            return rootWord;
        }
    }

    public static List<Derivation> findDerivations(String word) {
        String normalizedWord = CebuanoNormalizer.normalize(word);
        return findDerivations(getInstance(), normalizedWord, new HashSet<>(), 0);
    }

    private static List<Derivation> findDerivations(CebuanoStemmer stemmer, String word, HashSet<String> handledRoots,
            int level) {
        List<Derivation> derivations;

        if (stemmer.groups.size() <= level) {
            derivations = new ArrayList<>();
            if (!handledRoots.contains(word) && CebuanoDictionary.isRootWord(word)) {
                ArrayList<Affix> affixes = new ArrayList<>();
                Derivation derivation = new Derivation(word, affixes);
                derivations.add(derivation);
                handledRoots.add(word);
            }
            return derivations;
        }
        else {
            derivations = findDerivations(stemmer, word, handledRoots, level + 1);
        }

        // AffixGroup group = stemmer.groups.get(level);
        for (AffixGroup group : stemmer.groups) {
            List<Affix> affixes = group.affixes;
            for (Affix affix : affixes) {
                List<String> rootCandidates = affix.getRootCandidates(word);
                for (String root : rootCandidates) {
                    if (!handledRoots.contains(root) && CebuanoDictionary.isRootWord(root)) {
                        ArrayList<Affix> affixes2 = new ArrayList<>();
                        Derivation derivation = new Derivation(root, affixes2);
                        derivation.affixes.add(affix);
                        derivations.add(derivation);
                        handledRoots.add(root);
                    }

                    List<Derivation> innerDerivations = findDerivations(stemmer, root, handledRoots, level + 1);

                    // Copy the found derivations to the result list with the current affix as
                    // additional affix:
                    for (Derivation derivation : innerDerivations) {
                        derivation.affixes.add(affix);
                        derivations.add(derivation);
                    }
                }
            }
        }
        return derivations;
    }

    public static List<String> stemWords(List<String> wordList) {
        List<String> result = new ArrayList<>();
        for (String word : wordList) {
            String root = getRootWord(word);
            // System.out.printf("%s -> %s\n", word, root);
            result.add(root);
        }
        return result;
    }
}
