package org.lexicon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.lexicon.data.AnnotatedText;
import org.lexicon.data.Document;
import org.lexicon.process.ChiSquare;
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
        CommandHappinessTest cHappinessTest = new CommandHappinessTest();
        CommandChiSquareFeatureSelection cChiSquareFeatureSelection = new CommandChiSquareFeatureSelection();
        CommandIntersectionCount cIntersectionCount = new CommandIntersectionCount();
        JCommander jc = JCommander
            .newBuilder()
            .addObject(app)
            .addCommand(cTrain)
            .addCommand(cTest)
            .addCommand(cStats)
            .addCommand(cPopulate)
            .addCommand(cTopWords)
            .addCommand(cHappinessTest)
            .addCommand(cChiSquareFeatureSelection)
            .addCommand(cIntersectionCount)
            .build();
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
            case "happiness-index-test":
                happinessIndexTest(cHappinessTest);
                break;
            case "cs-feature-select":
                chiSquareFeatureSelect(cChiSquareFeatureSelection);
                break;
            case "intersection-count":
                intersectionCount(cIntersectionCount);
                break;
            default:
                jc.usage();
                break;
        }

        long end = System.currentTimeMillis();
        System.out.printf("Program execution: %.2f secs\n", (end - start) / 1000.0);
    }

    private static void train(CommandTrain args) {
        Document trainingDocument = DocumentHelper.loadTrainingDocument(args.trainDocFile, false);
        if (args.useFeatureSelection) {
            System.out.println("Selecting Features: ");
            trainingDocument.getVocabulary();
            trainingDocument.useFeatureSelection = true;
            trainingDocument.invalidateCache();
        }
        if (trainingDocument == null) {
            System.err.println("Problem found when reading: " + args.trainDocFile);
            return;
        }

        BagOfWords bow = new BagOfWords(trainingDocument);
        NaiveBayesClassifier classifier = new NaiveBayesClassifier();
        classifier.train(trainingDocument, args.extractionScheme);
        System.out.println(classifier.getPriorMap());

        System.out.println("Saving model file...");
        if (classifier.writeModel(args.modelFile)) {
            System.out.println("Model file saved in: " + args.modelFile);
        }
        else {
            System.err.println("Problem found when writing: " + args.modelFile);
        }

        System.out.println("Saving bag-of-words file...");
        if (bow.writeFile(args.bowFile)) {
            System.out.println("Bag-of-words file saved in: " + args.bowFile);
        }
        else {
            System.err.println("Problem found when writing: " + args.bowFile);
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
        TestResult testResult = classifier.test(testingDocument);


        System.out.println("Writing result file...");
        if (DocumentHelper.writeTestResult(testResult, args.resultDocFile)) {
            System.out.println("Test result file saved in: " + args.resultDocFile);
        }
        else {
            System.err.println("Problem found when writing: " + args.resultDocFile);
        }
    }

    private static void showTopWords(CommandTopWords args) {
        FeatureSelection.displayTopWords(args.lexiconFile, args.numOfWords, args.sentiment, !args.includeStopWords);
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

    private static void happinessIndexTest(CommandHappinessTest args) {
        System.out.println("Loading testing data...");
        Document testingDocument = DocumentHelper.loadTestingDocument(args.testDocFile);
        if (testingDocument == null) {
            System.err.println("Problem found when reading: " + args.testDocFile);
            return;
        }

        System.out.println("Loading lexicon...");
        HappinessIndex hi = new HappinessIndex();
        hi.load(args.lexiconFile);

         System.out.println("Classifying data...");
         TestResult testResult = hi.test(testingDocument, args.levels);

         System.out.println("Writing result file...");
         if (DocumentHelper.writeTestResult(testResult, args.resultDocFile)) {
             System.out.println("Test result file saved in: " + args.resultDocFile);
         }
         else {
             System.err.println("Problem found when writing: " + args.resultDocFile);
         }
    }

    private static void chiSquareFeatureSelect(CommandChiSquareFeatureSelection args) {

        System.out.println("Loading model...");
        NaiveBayesClassifier classifier = NaiveBayesClassifier.loadModel(args.modelFile);
        if (classifier == null) {
            System.err.println("Problem found when reading: " + args.modelFile);
            return;
        }

        ChiSquare cs = new ChiSquare();
        Map<String, Double> selectedFeatures = cs.selectFeatures(classifier);
        Set<String> newVocabulary = new HashSet<>(selectedFeatures.keySet());
        Map<AnnotatedText, Double> newLikelihoodMap = new HashMap<>();

        for (Map.Entry<AnnotatedText, Double> entry : classifier.getLikelihoodMap().entrySet()) {
            String word = entry.getKey().getText();
            if (selectedFeatures.containsKey(word)) {
                newLikelihoodMap.put(entry.getKey(), entry.getValue());
            }
        }
        classifier.setVocabulary(newVocabulary);
        classifier.setLikelihoodMap(newLikelihoodMap);

        System.out.println("Saving model file...");
        if (classifier.writeModel(args.resultModelFile)) {
            System.out.println("Model file saved in: " + args.resultModelFile);
        }
        else {
            System.err.println("Problem found when writing: " + args.resultModelFile);
        }
    }

    private static void intersectionCount(CommandIntersectionCount args) {
        Document document = DocumentHelper.loadTrainingDocument(args.docFile, false);
        if (args.useFeatureSelection) {
            System.out.println("Selecting Features: ");
            document.getVocabulary();
            document.useFeatureSelection = true;
            document.invalidateCache();
        }

        document.exportIntersectionData(args.resultFile);
    }

    @Parameters(commandNames = "train", commandDescription = "Train a classifier model")
    private static class CommandTrain {

        @Parameter(names = { "--document", "-d" }, description = "Training document to use")
        private String trainDocFile = DocumentHelper.DEFAULT_DOCUMENT_FILE;

        @Parameter(names = { "--bow"}, description = "Bag-of-words model file path output")
        private String bowFile = BagOfWords.DEFAULT_EXCEL_NAME;

        @Parameter(names = { "--model", "-m" }, description = "Classifier model file output")
        private String modelFile = NaiveBayesClassifier.DEFAULT_MODEL_FILE;

        @Parameter(names = { "--feature", "-f" }, description = "Method to use for feature extraction")
        private ExtractionScheme extractionScheme = ExtractionScheme.TO;

        @Parameter(names = { "--feature-selection", "-s" }, description = "Use feature selection", arity = 1)
        private boolean useFeatureSelection = true;
    }

    @Parameters(commandNames = "test", commandDescription = "Test created model and writes results to an excel file")
    private static class CommandTest {

        @Parameter(names = { "--document", "-d" }, description = "Testing document")
        private String testDocFile = DocumentHelper.DEFAULT_DOCUMENT_FILE;

        @Parameter(names = { "--model", "-m" }, description = "Classifier model file to use; uses this as default")
        private String modelFile = NaiveBayesClassifier.DEFAULT_MODEL_FILE;

        @Parameter(names = { "--bow" }, description = "Bag-of-words model file path; uses this when specified")
        private String bowFile = null;

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

        @Parameter(names = { "--class", "-c" }, description = "Sentiment classification")
        private Sentiment sentiment;

        @Parameter(names = "--include-stopwords", description = "Include stop words from frequencies")
        private boolean includeStopWords = false;
    }

    @Parameters(commandNames = "happiness-index-test", commandDescription = "Test using Happiness Index / Corpus-based method")
    private static class CommandHappinessTest {

        @Parameter(names = { "--document", "-d" }, description = "Testing document")
        private String testDocFile = DocumentHelper.DEFAULT_DOCUMENT_FILE;

        @Parameter(names = { "--level" }, description = "Level", listConverter=IntegerListConverter.class)
        private List<Integer> levels = new ArrayList<>();
        {
            levels.add(1);
            levels.add(2);
            levels.add(3);
        }

        @Parameter(names = { "--lexicon", "-x" }, description = "Lexicon File")
        private String lexiconFile = "./files/Bisaya Lexicon.xls";

        @Parameter(names = { "--result", "-r" }, description = "Path for the result file")
        private String resultDocFile = DocumentHelper.DEFAULT_TEST_RESULT_FILE;

        @Parameter(names = "--include-stopwords", description = "Include stop words from frequencies")
        private boolean includeStopWords = false;
    }

    @Parameters(commandNames = "cs-feature-select", commandDescription = "Use ChiSquare method to select features from a model")
    private static class CommandChiSquareFeatureSelection {

        @Parameter(names = { "--model", "-m" }, description = "Classifier model file to use; uses this as default")
        private String modelFile = NaiveBayesClassifier.DEFAULT_MODEL_FILE;

        @Parameter(names = { "--result", "-r" }, description = "Output for result model")
        private String resultModelFile = "./files/classifies-cs-selected.model";

    }

    @Parameters(commandNames = "intersection-count")
    private static class CommandIntersectionCount {

        @Parameter(names = { "--document", "-d" }, description = "Training document to use")
        private String docFile = DocumentHelper.DEFAULT_DOCUMENT_FILE;

        @Parameter(names = { "--feature-selection", "-s" }, description = "Use feature selection", arity = 1)
        private boolean useFeatureSelection = true;

        @Parameter(names = { "--result", "-r" }, description = "Path for the result file")
        private String resultFile = "./files/IntersectionData.xlsx";

    }

    public static class IntegerListConverter implements IStringConverter<List<Integer>> {
        @Override
        public List<Integer> convert(String str) {
            String [] numbers = str.split(",");
            List<Integer> numList = new ArrayList<>();
            for(String num : numbers){
                numList.add(Integer.parseInt(num));
            }
            return numList;
        }
    }
}
