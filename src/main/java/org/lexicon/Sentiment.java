package org.lexicon;

public enum Sentiment {
     POSITIVE("POSITIVE"), NEGATIVE("NEGATIVE"), NEUTRAL("NEUTRAL");

    private String sentiment;

    Sentiment(String sentiment) {
    }

    public static Sentiment getValue(String arg) {
        return Sentiment.valueOf(arg.trim().toUpperCase());
    }

    public String getValue() {
        return sentiment;
    }
}
