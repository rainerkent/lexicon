package org.lexicon;

public enum ExtractionScheme {
    TO("TO"), TF("TF"), TFIDF("TFIDF");

    private String feature;

    ExtractionScheme(String feature) {}
}
