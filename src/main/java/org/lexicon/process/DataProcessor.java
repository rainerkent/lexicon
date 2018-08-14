package org.lexicon.process;

import java.util.ArrayList;
import java.util.List;

import org.lexicon.process.dictionary.EnglishDictionary;
import org.lexicon.process.stemmer.CebuanoStemmer;

public class DataProcessor {

    // static methods only
    private DataProcessor() {}

    public static String clean(String word) {
        return CebuanoNormalizer.normalize(word);
    }

    public static List<String> preprocess(String sentence) {
        return preprocess(sentence, true);
    }

    public static List<String> preprocess(String sentence, boolean removeStopWords) {
        List<String> wordList = new ArrayList<>(50);

        // Split into words
        wordList.addAll(Tokenizer.tokenize(sentence.toLowerCase()));

        List<String> result = new ArrayList<>(50);
        for (String word : wordList) {
            if (word.length() != 0 && word.matches("^[a-zA-Z]*$") && !EnglishDictionary.isEnglishWord(word)) {
                word = CebuanoNormalizer.normalize(word);

                if (!removeStopWords || !StopWords.isStopWord(word)) {
                    // word = CebuanoStemmer.getRootWord(word);
                    result.add(word);
                }
            }

        }

        return result;
    }
}
