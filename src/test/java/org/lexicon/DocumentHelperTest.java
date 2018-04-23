package org.lexicon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.lexicon.data.AnnotatedText;
import org.lexicon.data.Document;
import org.lexicon.process.DocumentHelper;

public class DocumentHelperTest {

    private static final String INVALID_FILE_NAME = "./documents/asd";
    private static final String VALID_FILE_NAME = "./files/sentences_document.xlsx";

    @Test
    public void testLoadDocument() {
        Map<Sentiment, List<AnnotatedText>> nullDocument = DocumentHelper.loadDocument(INVALID_FILE_NAME);
        assertNull(nullDocument);

        Map<Sentiment, List<AnnotatedText>> myDocument = DocumentHelper.loadDocument(VALID_FILE_NAME);
        assertNotNull(myDocument);
        assertEquals(Sentiment.values().length, myDocument.size());
    }

    @Test
    public void testLoadTrainingDocument() {
        Document invalidList = DocumentHelper.loadTrainingDocument(INVALID_FILE_NAME);
        assertNull(invalidList);

        Document validList = DocumentHelper.loadTrainingDocument(VALID_FILE_NAME);
        assertNotNull(validList);
    }

    @Test
    public void testLoadTestingDocument() {
        Document invalidList = DocumentHelper.loadTestingDocument(INVALID_FILE_NAME);
        assertNull(invalidList);

        Document validList = DocumentHelper.loadTestingDocument(VALID_FILE_NAME);
        assertNotNull(validList);
    }

}
