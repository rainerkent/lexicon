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
            if (word.length() == 0 || !word.matches("^[a-zA-Z]*$") || EnglishDictionary.isEnglishWord(word))
                continue;

            word = CebuanoNormalizer.normalize(word);
            // word = CebuanoStemmer.getRootWord(word);

            if (removeStopWords && StopWords.isStopWord(word))
                continue;

            result.add(word);
        }
        // Remove English words
        // wordList = EnglishDictionary.removeEnglishWords(wordList);

        // // Normalize words
        // wordList = CebuanoNormalizer.normalize(wordList);

        // // Stemmer
        // wordList = CebuanoStemmer.stemWords(wordList);

        // // Remove stop words
        // if (removeStopWords) {
        //     wordList = StopWords.removeStopWords(wordList);
        // }

        return result;
    }
}
