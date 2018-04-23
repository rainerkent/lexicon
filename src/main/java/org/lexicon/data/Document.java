package org.lexicon.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lexicon.Sentiment;
import org.lexicon.process.DataProcessor;

public class Document implements Serializable {

    private static final long serialVersionUID = -4335694861861622949L;

    private List<AnnotatedText> sentences;
    
    private Map<Sentiment, Integer> sentenceCountMapCache;
    
    private Map<Sentiment, List<String>> wordListMapCache;
    
    private Set<String> vocabularySetCache;
    
    private boolean cacheValid = false;

    public Document(List<AnnotatedText> sentences) {
        if (sentences == null) {
            throw new NullPointerException();    
        }
        this.sentences = sentences;
    }
    
    public List<AnnotatedText> getData() {
        return sentences;
    }

    public Map<Sentiment, Integer> getSentenceCountMap() {
        if (!cacheValid) {
            generateCache();
        }
        return sentenceCountMapCache;
    }

    public Map<Sentiment, List<String>> getWordListMap() {
        if (!cacheValid) {
            generateCache();
        }
        return wordListMapCache;
    }
    
    public Set<String> getVocabulary() {
        if (!cacheValid) {
            generateCache();
        }
        return vocabularySetCache;
    }
    
    private void generateCache() {
        initializeCache();
        for (AnnotatedText sentence : sentences) {
            Sentiment sentiment = sentence.getCategory();
            
            // sentence count
            int count = sentenceCountMapCache.get(sentiment);
            sentenceCountMapCache.put(sentiment, count + 1);
            
            // word list
            List<String> words = DataProcessor.preprocess(sentence.getText());
            wordListMapCache.get(sentiment).addAll(words);
            vocabularySetCache.addAll(words);
        }
        cacheValid = true;
    }
    
    private void initializeCache() {
        sentenceCountMapCache = new HashMap<>();
        wordListMapCache = new HashMap<>();
        vocabularySetCache = new HashSet<>(5000);
        
        for (Sentiment sentiment : Sentiment.values()) {
            sentenceCountMapCache.put(sentiment, 0);
            wordListMapCache.put(sentiment, new ArrayList<>(5000));
        }
    }
}
