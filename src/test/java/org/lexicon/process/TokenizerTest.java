package org.lexicon.process;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.lexicon.process.Tokenizer;

public class TokenizerTest {

    @Test
    public void testTokenizeString() {
        final String sentence = "The quick  brown\tfox jumps \n over the lazy dog ";

        List<String> tokens = Tokenizer.tokenize(sentence);
        assertSame(9, tokens.size());

        // String pattern = "\\w+";
        // assertFalse(sentence.matches(pattern));
        // assertTrue("Chrysanthemum".matches(pattern));
        // assertTrue("a".matches(pattern));
        // assertFalse("a.".matches(pattern));
    }

    @Test
    public void testTokenizeListOfString() {
        final List<String> sentences = new ArrayList<>();
        sentences.add(" The quick  brown    fox jumps \n over the lazy dog");
        sentences.add(" It was a bright cold day in April, and the clocks were striking thirteen. ");

        List<String> tokens = Tokenizer.tokenize(sentences);
        assertSame(23, tokens.size());
    }

}
