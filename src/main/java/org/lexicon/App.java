package org.lexicon;

import java.io.IOException;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.lexicon.data.AnnotatedText;
import org.lexicon.data.Document;
import org.lexicon.process.DocumentHelper;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

public class App {

    @Parameter(names = { "--help", "-h" }, help = true)
    private boolean help;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        App app = new App();
        CommandPopulate cPopulate = new CommandPopulate();
        CommandTrain cTrain = new App.CommandTrain();
        CommandTest cTest = new CommandTest();
        CommandStats cStats = new CommandStats();
        CommandTopWords cTopWords = new CommandTopWords();
        JCommander jc = JCommander.newBuilder().addObject(app).addCommand(cTrain).addCommand(cTest).addCommand(cStats)
                .addCommand(cPopulate).addCommand(cTopWords).build();
        jc.parse(args);

        if (app.help || jc.getParsedCommand() == null) {
            jc.usage();
        }
        else switch (jc.getParsedCommand()) {
            case "train":
                train(cTrain);
                break;
            case "test":
                test(cTest);
                break;
            case "stats":
                showStats(cStats);
                break;
            case "populate":
                populate(cPopulate);
                break;
            case "top-words":
                showTopWords(cTopWords);
                break;
        }
        long end = System.currentTimeMillis();
        System.out.println(String.format("Program execution: %.2f secs", (end - start) / 1000.0));
    }

    private static void train(CommandTrain args) {
        Document trainingDocument = DocumentHelper.loadTrainingDocument(args.trainDocFile);
        if (trainingDocument == null) {
            System.err.println("Problem found when reading: " + args.trainDocFile);
            return;
        }

        NaiveBayesClassifier classifier = new NaiveBayesClassifier();
        classifier.train(trainingDocument);

        System.out.println("Saving model file...");
        if (classifier.writeModel(args.modelFile)) {
            System.out.println("Model file saved in: " + args.modelFile);
        }
        else {
            System.err.println("Problem found when writing: " + args.modelFile);
        }
    }

    private static void test(CommandTest args) {
        System.out.println("Loading testing data...");
        Document testingDocument = DocumentHelper.loadTestingDocument(args.testDocFile);
        if (testingDocument == null) {
            System.err.println("Problem found when reading: " + args.testDocFile);
            return;
        }

        System.out.println("Loading model...");
        NaiveBayesClassifier classifier = NaiveBayesClassifier.loadModel(args.modelFile);
        if (classifier == null) {
            System.err.println("Problem found when reading: " + args.modelFile);
            return;
        }

        System.out.println("Classifying data...");
        Map<AnnotatedText, Sentiment> testResult = classifier.test(testingDocument);
        

        System.out.println("Writing result file...");
        if (DocumentHelper.writeTestResult(testResult, args.resultDocFile)) {
            System.out.println("Test result file saved in: " + args.resultDocFile);
        }
        else {
            System.err.println("Problem found when writing: " + args.resultDocFile);
        }
    }

    private static void showTopWords(CommandTopWords args) {
        FeatureSelection.displayTopWords(args.lexiconFile, args.numOfWords, args.sentiment, args.removeStopWords);
    }

    private static void showStats(CommandStats args) {
        try {
            Lexicon lexicon = new Lexicon(args.lexiconFile);
            System.out.println("No translations: " + lexicon.countWordsWithNoTranslation());
            System.out.println("Single translation: " + lexicon.countWordsWithSingleTranslation());
            System.out.println("Multiple translations: " + lexicon.countWordsWithMultipleTranslation());
        }
        catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
            System.out.println("Problem found when reading: " + args.lexiconFile);
        }
    }

    private static void populate(CommandPopulate args) {

        if (LexiconPopulator.populate(args.baseFile, args.resultFile)) {
            System.out.println("Workbook successfully saved as " + args.resultFile);
        }
        else {
            System.out.println("Something went wrong when saving " + args.resultFile);
        }
    }

    @Parameters(commandNames = "train", commandDescription = "Train a classifier model")
    private static class CommandTrain {

        @Parameter(names = { "--document", "-d" }, description = "Training document")
        private String trainDocFile = DocumentHelper.DEFAULT_DOCUMENT_FILE;

        @Parameter(names = { "--model", "-m" }, description = "Classifier model file")
        private String modelFile = NaiveBayesClassifier.DEFAULT_MODEL_FILE;
    }

    @Parameters(commandNames = "test", commandDescription = "Test created model and outputs a results excelfile")
    private static class CommandTest {

        @Parameter(names = { "--document", "-d" }, description = "Testing document")
        private String testDocFile = DocumentHelper.DEFAULT_DOCUMENT_FILE;

        @Parameter(names = { "--model", "-m" }, description = "Classifier model file to use")
        private String modelFile = NaiveBayesClassifier.DEFAULT_MODEL_FILE;

        @Parameter(names = { "--result", "-r" }, description = "Path for the result file")
        private String resultDocFile = DocumentHelper.DEFAULT_TEST_RESULT_FILE;
    }

    @Parameters(commandNames = "populate", commandDescription = "Populates a Lexicon document using translations file")
    private static class CommandPopulate {

        @Parameter(names = { "--document", "-d" }, description = "Base excel file")
        private String baseFile = LexiconPopulator.BASE_EXCEL_FILE;

        @Parameter(names = { "--result", "-r" }, description = "Path for result file")
        private String resultFile = LexiconPopulator.OUTPUT_EXCEL_FILE;
    }

    @Parameters(commandNames = "stats", commandDescription = "Show the statistics for a Lexicon document")
    private static class CommandStats {

        @Parameter(names = { "--document", "-d" }, description = "Lexicon document")
        private String lexiconFile = LexiconPopulator.BASE_EXCEL_FILE;
    }

    @Parameters(commandNames = "top-words", commandDescription = "Show top occuring words in a document")
    private static class CommandTopWords {

        @Parameter(names = { "--document", "-d" }, description = "Document to use")
        private String lexiconFile = FeatureSelection.DEFAULT_DOCUMENT_FILE;

        @Parameter(names = "-n", description = "Number of words to show")
        private int numOfWords = 20;

        @Parameter(names = { "--class",
                "-c" }, description = "Sentiment classification", converter = SentimentConverter.class)
        private Sentiment sentiment;

        @Parameter(names = "--remove-stopwords", description = "Exclude stop words from frequencies")
        private boolean removeStopWords = true;
    }

    private static class SentimentConverter implements IStringConverter<Sentiment> {
        @Override
        public Sentiment convert(String value) {
            return Sentiment.valueOf(value.toUpperCase());
        }
    }
}
