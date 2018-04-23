package org.lexicon.process;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {

    private Tokenizer() {}

    public static List<String> tokenize(String sentence) {
        String[] words = sentence.replaceAll("-", "")
                .replaceAll("\\p{P}", " ") // remove characters that are not non-alphanumeric and whitespaces(\s)
                .trim()
                .split("\\s+");            // split string in whitespaces

        // Move strings in Array to List
        List<String> wordList = new ArrayList<>();
        for (String word : words) {
            wordList.add(word);
        }
        return wordList;
    }

    public static List<String> tokenize(List<String> sentences) {
        List<String> wordList = new ArrayList<>();
        for (String sentence : sentences) {
            wordList.addAll(tokenize(sentence));
        }
        return wordList;
    }
}
