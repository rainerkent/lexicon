package org.lexicon.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.lexicon.Sentiment;

public class AnnotatedTextTest {

    @Test
    public void testAnnotatedTextStringSentiment() {
        AnnotatedText text = new AnnotatedText("Hey", Sentiment.NEUTRAL);
        assertNotNull(text);
        assertEquals("H" + "ey", text.getText());
        assertEquals(Sentiment.NEUTRAL, text.getCategory());
    }

    @Test
    public void testAnnotatedTextStringString() {
        AnnotatedText text = new AnnotatedText("Hey", "neutral");
        assertNotNull(text);
        assertEquals("H" + "ey", text.getText());
        assertEquals(Sentiment.NEUTRAL, text.getCategory());
    }

    @Test
    public void testSetText() {
        AnnotatedText text = new AnnotatedText("Hey", "neutral");
        text.setText("Hello");
        assertEquals("Hello", text.getText());
    }

    @Test
    public void testSetCategory() {
        AnnotatedText text = new AnnotatedText("Hey", Sentiment.NEUTRAL);
        text.setCategory(Sentiment.POSITIVE);
        assertEquals(Sentiment.POSITIVE, text.getCategory());
    }

    @Test
    public void testPreprocessIntoWords() {
        AnnotatedText text = new AnnotatedText("Ang babayi nga gwapahon", Sentiment.NEUTRAL);
        List<AnnotatedText> list = text.preprocessIntoWords();
        assertNotEquals(0, list.size());
    }

    @Test
    public void testEqualsObject() {
        AnnotatedText text1 = new AnnotatedText("Hey", Sentiment.NEUTRAL);
        AnnotatedText text2 = new AnnotatedText("He" + "y", Sentiment.NEUTRAL);
        assertEquals(text1, text2);

        AnnotatedText text3 = new AnnotatedText("Hey", Sentiment.NEGATIVE);
        AnnotatedText text4 = new AnnotatedText("Hey", Sentiment.NEUTRAL);
        assertNotEquals(text3, text4);
        
        assertEquals(text1.getText().hashCode(), text2.getText().hashCode());
        assertEquals(text1.getCategory().hashCode(), text2.getCategory().hashCode());
        assertEquals(text1.hashCode(), text2.hashCode());
    }

}
