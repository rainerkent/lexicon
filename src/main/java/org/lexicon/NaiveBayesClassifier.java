package org.lexicon;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lexicon.data.AnnotatedText;
import org.lexicon.data.Document;
import org.lexicon.process.DataProcessor;

public class NaiveBayesClassifier implements Serializable {

    private static final long serialVersionUID = 8707384910244268938L;
    private static final Sentiment[] SENTIMENT_VALUES = { Sentiment.NEGATIVE, Sentiment.NEUTRAL, Sentiment.POSITIVE };

    public static final String DEFAULT_MODEL_FILE = "./files/classifier.model";

    private Map<Sentiment, Double> priorMap;

    private Map<AnnotatedText, Double> condProbMap;

    private Set<String> vocabulary;

    public NaiveBayesClassifier() {
        priorMap = new HashMap<>();
        condProbMap = new HashMap<>();
        vocabulary = new HashSet<>();
    }

    public Map<Sentiment, Double> getPriorMap() {
        return priorMap;
    }

    public Map<AnnotatedText, Double> getCondProbMap() {
        return condProbMap;
    }

    public Set<String> getVocabulary() {
        return vocabulary;
    }

    public static NaiveBayesClassifier loadModel(String file) {
        NaiveBayesClassifier model = null;
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            model = (NaiveBayesClassifier) in.readObject();
            in.close();
        }
        catch (Exception e) {
            return null;
        }
        return model;
    }

    public boolean writeModel(String file) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(this);
            out.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void train(Document trainingDocument) {
        int docCount = trainingDocument.getData().size();
        System.out.println("Building vocabulary list... \nThis may take a while...");
        vocabulary = trainingDocument.getVocabulary();

        System.out.println("Building knowledge base...");
        for (Sentiment sentiment : SENTIMENT_VALUES) {
            int docCountForClass = trainingDocument.getSentenceCountMap().get(sentiment);
            double prior = (double) docCountForClass / docCount;
            priorMap.put(sentiment, prior);

            List<String> wordsForClass = trainingDocument.getWordListMap().get(sentiment);
            for (String word : vocabulary) {
                int wordCount = countWord(wordsForClass, word);
                double condProb = ((double) wordCount + 1) / (wordsForClass.size() + vocabulary.size());
                condProbMap.put(new AnnotatedText(word, sentiment), condProb);
            }
        }
    }

    public Sentiment predict(String sentence) {
        List<String> tokens = DataProcessor.preprocess(sentence);

        Sentiment maxSentiment = null;
        double maxScore = Double.NEGATIVE_INFINITY;

        for (Sentiment sentiment : SENTIMENT_VALUES) {
            double score = Math.log(priorMap.get(sentiment));
            for (String token : tokens) {
                if (vocabulary.contains(token)) {
                    Double condProb = condProbMap.get(new AnnotatedText(token, sentiment));
                    score += Math.log(condProb);
                }
            }

            if (maxScore < score) {
                maxSentiment = sentiment;
                maxScore = score;
            }
        }

        return maxSentiment;
    }

    public Map<AnnotatedText, Sentiment> test(Document testDocument) {
        Map<AnnotatedText, Sentiment> result = new LinkedHashMap<>();
        for (AnnotatedText sentence : testDocument.getData()) {
            Sentiment prediction = predict(sentence.getText());
            result.put(sentence, prediction);
        }
        return result;
    }

    private int countWord(List<String> wordList, String wordToFind) {
        int count = 0;
        for (String word : wordList) {
            if (word.equals(wordToFind)) {
                count++;
            }
        }
        return count;
    }
}
