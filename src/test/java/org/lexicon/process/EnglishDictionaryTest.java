package org.lexicon.process;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.lexicon.process.dictionary.EnglishDictionary;

public class EnglishDictionaryTest {

    @Test
    public void testIsEnglishWord() {
        assertTrue(EnglishDictionary.isEnglishWord("cat"));
        assertFalse(EnglishDictionary.isEnglishWord("hagdanan"));
    }

    @Test
    public void testRemoveEnglishWords() {
        List<String> wordList = new ArrayList<>();
        wordList.add("cat");
        wordList.add("dog");
        wordList.add("lakaw");
        wordList.add("sakay");

        List<String> result = EnglishDictionary.removeEnglishWords(wordList);

        assertFalse(result.contains("cat"));
        assertFalse(result.contains("dog"));
        assertTrue(result.contains("lakaw"));
        assertTrue(result.contains("sakay"));
    }
}
