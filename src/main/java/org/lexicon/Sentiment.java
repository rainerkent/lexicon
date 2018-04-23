package org.lexicon;

public enum Sentiment {
    NEGATIVE("NEGATIVE"), NEUTRAL("NEUTRAL"), POSITIVE("POSITIVE");

    private String sentiment;

    Sentiment(String sentiment) {
        sentiment = sentiment.toUpperCase();
    }

    public static Sentiment getValue(String arg) {
        return Sentiment.valueOf(arg.trim().toUpperCase());
    }

    public String getValue() {
        return sentiment;
    }
}
