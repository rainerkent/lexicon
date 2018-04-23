package org.lexicon.data;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.lexicon.Sentiment;
import org.lexicon.process.DataProcessor;

/**
 * Model class for text with classification
 */
public class AnnotatedText implements Serializable {

    private static final long serialVersionUID = 7089133612712575410L;
    private String text;
    private Sentiment category;

    public AnnotatedText(String text, Sentiment sentiment) {
        if (text == null || sentiment == null) { throw new NullPointerException(); }
        this.text = text;
        this.category = sentiment;
    }

    public AnnotatedText(String text, String sentiment) {
        if (text == null || sentiment == null) { throw new NullPointerException(); }
        this.text = text;
        this.category = Sentiment.valueOf(sentiment.toUpperCase());
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (text == null) { throw new NullPointerException(); }
        this.text = text;
    }

    public Sentiment getCategory() {
        return category;
    }

    public void setCategory(Sentiment sentiment) {
        if (sentiment == null) { throw new NullPointerException(); }
        this.category = sentiment;
    }

    public List<AnnotatedText> preprocessIntoWords() {
        List<String> wordList = DataProcessor.preprocess(text);
        List<AnnotatedText> result = new LinkedList<>();
        for (String word : wordList) {
            result.add(new AnnotatedText(word, category));
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        AnnotatedText other = (AnnotatedText) obj;
        return text.equals(other.text) && category.equals(other.category);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        return sb.append("[").append(text).append(": ").append(category).append("]").toString();
    }
    
    @Override
    public int hashCode() {
        return text.hashCode() + category.hashCode();
    }
}
