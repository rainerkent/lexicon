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
import org.lexicon.util.ProgressBar;

public class NaiveBayesClassifier implements Serializable {

    private static final long serialVersionUID = 8707384910244268938L;

    public static final String DEFAULT_MODEL_FILE = "./files/classifier.model";

    private Map<Sentiment, Double> priorMap;

    private Map<AnnotatedText, Double> likelihoodMap;

    private Set<String> vocabulary;

    public NaiveBayesClassifier() {
        priorMap = new HashMap<>();
        likelihoodMap = new HashMap<>();
        vocabulary = new HashSet<>();
    }

    public Map<Sentiment, Double> getPriorMap() {
        return priorMap;
    }

    public Map<AnnotatedText, Double> getLikelihoodMap() {
        return likelihoodMap;
    }

    public Set<String> getVocabulary() {
        return vocabulary;
    }

    public static NaiveBayesClassifier loadModel(String file) {
        NaiveBayesClassifier model;
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            model = (NaiveBayesClassifier) in.readObject();
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
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

    public void train(Document trainingDocument, ExtractionScheme featureMethod) {
        int docCount = trainingDocument.getData().size();
        System.out.println("Building vocabulary list...");
        vocabulary = trainingDocument.getVocabulary();

        System.out.println("Building knowledge base...");
        for (Sentiment sentiment : Sentiment.values()) {
            int docCountForClass = trainingDocument.getSentenceCountMap().get(sentiment);
            double prior = (double) docCountForClass / docCount;
            priorMap.put(sentiment, prior);
        }

        if (featureMethod == ExtractionScheme.TO) {
            ProgressBar bar = new ProgressBar(vocabulary.size() * Sentiment.values().length);

            for (Sentiment sentiment : Sentiment.values()) {
                List<String> wordsByClass = trainingDocument.getWordListMap().get(sentiment);
                for (String word : vocabulary) {
                    int wordCount = countWord(wordsByClass, word);
                    double condProb = calculateCondProb(wordCount, wordsByClass.size());
                    likelihoodMap.put(new AnnotatedText(word, sentiment), condProb);
                    bar.step();
                }
            }
        }
        else if (featureMethod == ExtractionScheme.TF) {
            ProgressBar bar = new ProgressBar(vocabulary.size() * Sentiment.values().length + trainingDocument.getData().size());

            Map<String, Map<Sentiment, Double>> tfBySentimentMap = new HashMap<>();
            Map<Sentiment, Double> totalTfBySentimentMap = new HashMap<>();
            totalTfBySentimentMap.put(Sentiment.POSITIVE, 0.0);
            totalTfBySentimentMap.put(Sentiment.NEGATIVE, 0.0);
            totalTfBySentimentMap.put(Sentiment.NEUTRAL, 0.0);
            for (AnnotatedText text : trainingDocument.getData()) {
                List<String> words = DataProcessor.preprocess(text.getText(), true);
                Set<String> uniqueWords = new HashSet<>(words);
                Sentiment sentiment = text.getCategory();

                for (String word : uniqueWords) {
                    if (!tfBySentimentMap.containsKey(word)) {
                        Map<Sentiment, Double> tfMap = new HashMap<>();
                        tfMap.put(Sentiment.POSITIVE, 0.0);
                        tfMap.put(Sentiment.NEGATIVE, 0.0);
                        tfMap.put(Sentiment.NEUTRAL, 0.0);
                        tfBySentimentMap.put(word, tfMap);
                    }

                    double tf = tfBySentimentMap.get(word).get(sentiment);
                    tf += calculateTermFrequency(word, words);
                    // System.out.println(tf);
                    tfBySentimentMap.get(word).put(sentiment, tf);

                    double totalTf = totalTfBySentimentMap.get(sentiment);
                    totalTfBySentimentMap.put(sentiment, totalTf + tf);
                }
                bar.step();
            }
            for (Sentiment sentiment : Sentiment.values()) {
                for (String word : vocabulary) {
                    double wordTf = tfBySentimentMap.get(word).get(sentiment);
                    double condProb = calculateCondProb(wordTf, totalTfBySentimentMap.get(sentiment));
                    likelihoodMap.put(new AnnotatedText(word, sentiment), condProb);
                    bar.step();
                }
            }
        }
        else if (featureMethod == ExtractionScheme.TFIDF) {
            ProgressBar bar = new ProgressBar(vocabulary.size() * Sentiment.values().length + trainingDocument.getData().size());

            // Count each word on documents, to be used for getting idf
            Map<String, Map<Sentiment, Integer>> wordCountBySentimentMap = new HashMap<>();
            for (AnnotatedText text : trainingDocument.getData()) {
                List<String> words = DataProcessor.preprocess(text.getText(), true);
                Set<String> uniqueWords = new HashSet<>(words);
                Sentiment sentiment = text.getCategory();

                for (String word : uniqueWords) {
                    if (!wordCountBySentimentMap.containsKey(word)) {
                        Map<Sentiment, Integer> countMap = new HashMap<>();
                        countMap.put(Sentiment.POSITIVE, 0);
                        countMap.put(Sentiment.NEGATIVE, 0);
                        countMap.put(Sentiment.NEUTRAL, 0);
                        wordCountBySentimentMap.put(word, countMap);
                    }

                    int count = wordCountBySentimentMap.get(word).get(sentiment) + 1;
                    wordCountBySentimentMap.get(word).put(sentiment, count);
                }
            }

            // Get total TF-IDFs for word and for class
            Map<String, Map<Sentiment, Double>> totalTfidfByWordMap = new HashMap<>();
            Map<Sentiment, Double> totalTfidfBySentimentMap = new HashMap<>();
            totalTfidfBySentimentMap.put(Sentiment.POSITIVE, 0.0);
            totalTfidfBySentimentMap.put(Sentiment.NEGATIVE, 0.0);
            totalTfidfBySentimentMap.put(Sentiment.NEUTRAL, 0.0);
            for (AnnotatedText text : trainingDocument.getData()) {
                List<String> words = DataProcessor.preprocess(text.getText(), true);
                Set<String> uniqueWords = new HashSet<>(words);
                Sentiment sentiment = text.getCategory();

                for (String word : uniqueWords) {
                    if (!totalTfidfByWordMap.containsKey(word)) {
                        Map<Sentiment, Double> tfidfMap = new HashMap<>();
                        tfidfMap.put(Sentiment.POSITIVE, 0.0);
                        tfidfMap.put(Sentiment.NEGATIVE, 0.0);
                        tfidfMap.put(Sentiment.NEUTRAL, 0.0);
                        totalTfidfByWordMap.put(word, tfidfMap);
                    }
                    int sentenceCountInClass = trainingDocument.getSentenceCountMap().get(sentiment);

                    double tfidf = totalTfidfByWordMap.get(word).get(sentiment);
                    tfidf += calculateTermFrequency(word, words) * calculateIDF(sentenceCountInClass, wordCountBySentimentMap.get(word).get(sentiment));
                    // System.out.println(tf);
                    totalTfidfByWordMap.get(word).put(sentiment, tfidf);

                    double totalTfidf = totalTfidfBySentimentMap.get(sentiment);
                    totalTfidfBySentimentMap.put(sentiment, totalTfidf + tfidf);
                }
                bar.step();
            }
            for (Sentiment sentiment : Sentiment.values()) {
                for (String word : vocabulary) {
                    double wordTf = totalTfidfByWordMap.get(word).get(sentiment);
                    double condProb = calculateCondProb(wordTf, totalTfidfBySentimentMap.get(sentiment));
                    likelihoodMap.put(new AnnotatedText(word, sentiment), condProb);
                    bar.step();
                }
            }
        }
    }

    private double calculateTermFrequency(String word, List<String> document) {
        int termCount = 0;
        for (String term : document) {
            if (word.equals(term)) {
                termCount++;
            }
        }
        return (double) (termCount + 1) / document.size();
    }

    private double calculateIDF(int documentSize, int termCount ) {
        return Math.log((double) documentSize / termCount);
    }

    private double calculateCondProb(double wordCount, double wordsByClassCount) {
        return Math.log((wordCount + 1) / (wordsByClassCount + vocabulary.size()));
    }

    public Sentiment predict(String sentence) {
        List<String> tokens = DataProcessor.preprocess(sentence);

        Sentiment maxSentiment = null;
        double maxScore = Double.NEGATIVE_INFINITY;

        for (Sentiment sentiment : Sentiment.values()) {
            double score = Math.log(priorMap.get(sentiment));
            for (String token : tokens) {
                if (vocabulary.contains(token)) {
                    Double likelihood = likelihoodMap.get(new AnnotatedText(token, sentiment));
                    score += likelihood;
                }
            }

            if (maxScore < score) {
                maxSentiment = sentiment;
                maxScore = score;
            }
        }

        return maxSentiment;
    }

    public TestResult test(Document testDocument) {
        Map<AnnotatedText, Sentiment> result = new LinkedHashMap<>();
        ProgressBar bar = new ProgressBar(testDocument.getData().size());
        for (AnnotatedText sentence : testDocument.getData()) {
            Sentiment prediction = predict(sentence.getText());
            result.put(sentence, prediction);
            bar.step();
        }
        return new TestResult(result);
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
