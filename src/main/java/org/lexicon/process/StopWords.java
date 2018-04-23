package org.lexicon.process;

import static org.lexicon.util.ResourceUtil.CEBUANO_STOP_JSON;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.lexicon.util.ResourceUtil;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

public class StopWords {

    private Set<String> stopWords;

    private static StopWords instance;

    private StopWords() {
        Gson gson = GsonHelper.createGson();
        JsonElement wordsJson = ResourceUtil.parseJson(CEBUANO_STOP_JSON);
        stopWords = gson.fromJson(wordsJson, new TypeToken<HashSet<String>>() {}.getType());
        stopWords = CebuanoNormalizer.normalize(stopWords);
    }

    public static boolean isStopWord(String word) {
        return getInstance().stopWords.contains(word);
    }

    private static StopWords getInstance() {
        if (instance == null) {
            instance = new StopWords();
        }
        return instance;
    }

    public static List<String> removeStopWords(List<String> words) {
        List<String> result = new LinkedList<>();
        for (String word : words) {
            if (!isStopWord(word) && !hasDigits(word)) {
                result.add(word);
            }
        }
        return result;
    }
    
    private static boolean hasDigits(String word) {
        return word.matches(".*\\d.*");
    }
}
