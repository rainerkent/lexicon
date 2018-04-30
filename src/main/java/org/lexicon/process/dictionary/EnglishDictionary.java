package org.lexicon.process.dictionary;

import static org.lexicon.util.ResourceUtil.ENGLISH_WORDS_JSON;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lexicon.process.GsonHelper;
import org.lexicon.util.ResourceUtil;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

public class EnglishDictionary {

    private static EnglishDictionary instance;

    private Set<String> englishWords;

    private EnglishDictionary() {
        Gson gson = GsonHelper.createGson();
        JsonElement wordsJson = ResourceUtil.parseJson(ENGLISH_WORDS_JSON);
        englishWords = gson.fromJson(wordsJson, new TypeToken<HashSet<String>>() {}.getType());
    }

    private static EnglishDictionary getInstance() {
        if (instance == null) {
            instance = new EnglishDictionary();
        }
        return instance;
    }

    public static boolean isEnglishWord(String word) {
        return getInstance().englishWords.contains(word);
    }

    public static List<String> removeEnglishWords(List<String> wordList) {
        List<String> result = new ArrayList<>();

        for (String word : wordList) {
            if (!isEnglishWord(word)) {
                result.add(word);
            }
        }
        return result;
    }
}
