package org.lexicon.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.lexicon.process.dictionary.CebuanoDictionary;
import org.lexicon.process.stemmer.CebuanoStemmer;
import org.lexicon.process.stemmer.Derivation;

public class StemmerTest {

    @Test
    public void testGetGroups() {
        assertNotNull(CebuanoStemmer.getGroups());
    }

    @Test
    public void testGetConstants() {
        assertNotNull(CebuanoStemmer.getConstants());
    }

    @Test
    public void testGetLanguage() {
        assertNotNull(CebuanoStemmer.getLanguage());
    }

    @Test
    public void testFindDerivations() {
        List<Derivation> derivations = CebuanoStemmer.findDerivations("kamatuoran");
        for (Derivation derivation : derivations) {
            assertTrue(CebuanoDictionary.isRootWord(derivation.root));
        }

        derivations = CebuanoStemmer.findDerivations("makatarunganun");
        for (Derivation derivation : derivations) {
            assertTrue(CebuanoDictionary.isRootWord(derivation.root));
        }
    }

    @Test
    public void testFindRootWord() {
        String rootWord = CebuanoStemmer.getRootWord("kamatuoran");
        assertEquals("tuud", rootWord);

        rootWord = CebuanoStemmer.getRootWord("makatarunganun");
        assertEquals("tarung", rootWord);
    }
}
