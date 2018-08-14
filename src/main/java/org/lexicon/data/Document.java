package org.lexicon.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lexicon.Sentiment;
import org.lexicon.process.ChiSquare;
import org.lexicon.process.DataProcessor;
import org.lexicon.util.ProgressBar;

public class Document implements Serializable {

    private static final long serialVersionUID = -4335694861861622949L;

    private List<AnnotatedText> sentences;

    // Sentence count per sentiment
    private Map<Sentiment, Integer> sentenceCountMapCache;

    // List of words found in document grouped by sentiment
    private Map<Sentiment, List<String>> wordListMapCache;

    // Set of words found in the vocabulary
    public Set<String> vocabularySetCache;

    public boolean useFeatureSelection;

    private boolean cacheValid = false;

    public Document(List<AnnotatedText> sentences, boolean useChiSquare) {
        if (sentences == null) {
            throw new NullPointerException();
        }
        this.sentences = sentences;
        this.useFeatureSelection = useChiSquare;
    }

    public List<AnnotatedText> getData() {
        return sentences;
    }

    public void invalidateCache() {
        cacheValid = false;
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
        ProgressBar bar = new ProgressBar(sentences.size());
        Map<String, Double> selectedFeatures = new HashMap<>();
        if (useFeatureSelection) {
            ChiSquare cs = new ChiSquare();
            selectedFeatures = cs.selectFeatures(this);
        }
        initializeCache();
        for (AnnotatedText sentence : sentences) {
            Sentiment sentiment = sentence.getCategory();

            // sentence count
            int count = sentenceCountMapCache.get(sentiment);
            sentenceCountMapCache.put(sentiment, count + 1);

            // word list
            List<String> words = DataProcessor.preprocess(sentence.getText());
            for (String word : words) {
                if (!useFeatureSelection || selectedFeatures.get(word) != null) {
                    wordListMapCache.get(sentiment).add(word);
                    vocabularySetCache.add(word);
                }
            }
            bar.step();
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
