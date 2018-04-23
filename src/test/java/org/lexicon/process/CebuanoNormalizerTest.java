package org.lexicon.process;

import static org.junit.Assert.*;

import org.junit.Test;
import org.lexicon.process.CebuanoNormalizer;

public class CebuanoNormalizerTest {

    @Test
    public void testNormalize() {
        final String input = " Tĥïŝ ĩš â fůňķŷ Šťŕĭńġ,";
        final String expected = "this is a punky string,";

        String output = CebuanoNormalizer.normalize(input);
        assertEquals(expected, output);
    }

}
