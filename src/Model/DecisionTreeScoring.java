/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.Comparison;
import Protocol.OIS;
import Protocol.Utility.PolynomialComputing;
import TrustedInitializer.TripleByte;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class takes a row of attributes values and predicts the class label
 * based on the decision tree
 *
 * @author keerthanaa
 */
public class DecisionTreeScoring extends Model {

    int depth, attributeBitLength, attributeCount;//depth d, bitlength for each attribute value, total no. of attributes
    boolean partyHasTree;                         //true if the party has the tree, false if it has the test case
    Integer[][] featureVectors;                   //Shares of features 2^d - 1 vectors for each internal node
    // TODO - modify this as a list instead of list of lists
    List<List<Integer>> testVectorsDecimal;       //Test case - feature vectors as decimal numbers
    List<List<Integer>> testVector;               //Test case - a list of features represented as binary values
    int[] leafToClassIndexMapping;                //leaf node index to class index mapping (stored by the party that has the tree)
    int[] nodeToAttributeIndexMapping;            //internal node index to the attribute intex mapping (stored by the party that has the tree)
    int[] attributeThresholds;                    //each internal node's attribute threshold
    List<List<Integer>> attributeThresholdsBitShares; //attribute thresholds as bits
    int leafNodes, tiBinaryStartIndex, classLabelCount, alpha, pid; //leafNode - no. of leafnodes, classlabelcount - total number of class labels
    int[] comparisonOutputs;
    Integer[] finalOutputs;
    List<TripleByte> binaryTiShares;
    String[] args;
    Logger LOGGER;

    /**
     * Constructor 2 party DT scoring:
     *
     * one party has the tree, one party has the feature vector In args,
     *
     * party1: pass the feature vector as csv file (testCsv) pass the tree
     * properties (depth, attribute count, attribute bitlength, class label
     * count in properties file (treeproperties)
     *
     * party2: pass the tree as a properties file (storedtree)
     *
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param binaryTriples
     * @param partyCount
     * @param args
     * @param protocolIdQueue
     * @param protocolID
     */
    public DecisionTreeScoring(int asymmetricBit, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, int clientId, List<TripleByte> binaryTriples,
            int partyCount, String[] args, Queue<Integer> protocolIdQueue, int protocolID) {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);

        this.args = args;
        pid = 0;
        tiBinaryStartIndex = 0;
        this.binaryTiShares = binaryTriples;
        LOGGER = Logger.getLogger(DecisionTreeScoring.class.getName());
    }

    /**
     * initialize variables
     *
     * @param args
     */
    private void initializeModelVariables(String[] args) throws FileNotFoundException, IOException {
        leafToClassIndexMapping = null;
        nodeToAttributeIndexMapping = null;
        attributeThresholds = null;
        testVector = null;
        testVectorsDecimal = null;

        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.partyUsage();
                System.exit(0);
            }

            String command = currInput[0];
            String value = currInput[1];

            switch (command) {
                case "testCsv":
                    //party has feature vector
                    testVectorsDecimal = FileIO.loadIntListFromFile(value);
                    break;
                case "storedtree":
                    //party has the tree
                    partyHasTree = true;
                    Properties prop = new Properties();
                    InputStream input = new FileInputStream(value);
                    prop.load(input);
                    depth = Integer.parseInt(prop.getProperty("depth"));
                    attributeCount = Integer.parseInt(prop.getProperty("attribute.count"));
                    attributeBitLength = Integer.parseInt(prop.getProperty("attribute.bitlength"));
                    classLabelCount = Integer.parseInt(prop.getProperty("classlabel.count"));
                    String str = prop.getProperty("leaf.to.class.index.mapping");
                    leafToClassIndexMapping = new int[(int) Math.pow(2, depth) + 1];
                    String[] nums = str.split(",");
                    int i = 1;
                    for (String num : nums) {
                        leafToClassIndexMapping[i] = Integer.parseInt(num);
                        i++;
                    }
                    //leafToClassIndexMapping = Arrays.stream(str.split(",")).mapToInt(Integer::parseInt).toArray();
                    str = prop.getProperty("node.to.attribute.index.mapping");
                    nodeToAttributeIndexMapping = Arrays.stream(str.split(",")).mapToInt(Integer::parseInt).toArray();
                    str = prop.getProperty("attribute.thresholds");
                    attributeThresholds = Arrays.stream(str.split(",")).mapToInt(Integer::parseInt).toArray();
                    break;
                case "treeproperties":
                    //party has feature vector
                    partyHasTree = false;
                    prop = new Properties();
                    input = new FileInputStream(value);
                    prop.load(input);
                    depth = Integer.parseInt(prop.getProperty("depth"));
                    attributeCount = Integer.parseInt(prop.getProperty("attribute.count"));
                    attributeBitLength = Integer.parseInt(prop.getProperty("attribute.bitlength"));
                    classLabelCount = Integer.parseInt(prop.getProperty("classlabel.count"));
                    break;
            }
        }
    }

    /**
     * Convert the feature values from decimal to bits
     *
     * @param testCase
     */
    void convertTestVectorToBits(List<Integer> testCase) {
        testVector = new ArrayList<>();
        for (int i : testCase) {
            testVector.add(Arrays.asList(convertToBits(i, attributeBitLength)));
        }
    }

    /**
     * Doing common initializations for both parties here
     */
    void init() throws IOException {
        initializeModelVariables(args);
        leafNodes = (int) Math.pow(2, depth);
        featureVectors = new Integer[leafNodes - 1][attributeBitLength];
        attributeThresholdsBitShares = new ArrayList<>();
        comparisonOutputs = new int[leafNodes - 1];
        alpha = (int) Math.ceil(Math.log(classLabelCount) / Math.log(2.0));
        finalOutputs = new Integer[alpha];
    }

    /**
     * Main method for the DT Scoring algorithm
     *
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public void scoreDecisionTree() throws IOException, InterruptedException, ExecutionException {

        init();

        long startTime = System.currentTimeMillis();

        convertThresholdsToBits();
        LOGGER.fine("Converted Thresholds to Bits");

        if (!partyHasTree) {
            convertTestVectorToBits(testVectorsDecimal.get(0));
            LOGGER.fine("Converted feature vector to bits");
        }

        getFeatureVectors();

        LOGGER.log(Level.FINE, "got the feature vectors:{0}", Arrays.deepToString(featureVectors));

        doThresholdComparisons();

        computePolynomialEquation();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        LOGGER.log(Level.INFO, "the output in bits: {0}", Arrays.toString(finalOutputs));
        LOGGER.log(Level.INFO, "Avg time duration:{0}", elapsedTime);

    }

    /**
     * gets the feature vectors for each attribute of internal node using
     * Oblivious Input selection Protocol
     */
    void getFeatureVectors() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        if (partyHasTree) {
            for (int i = 0; i < leafNodes - 1; i++) {

                OIS ois = new OIS(null, binaryTiShares.subList(tiBinaryStartIndex,
                        tiBinaryStartIndex + (attributeBitLength * attributeCount)),
                        asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                        clientId, Constants.BINARY_PRIME, pid, attributeBitLength,
                        nodeToAttributeIndexMapping[i], attributeCount, partyCount);
                tiBinaryStartIndex += attributeBitLength * attributeCount;
                pid++;
                Future<Integer[]> task = es.submit(ois);
                taskList.add(task);

            }
        } else {
            for (int i = 0; i < leafNodes - 1; i++) {

                OIS ois = new OIS(testVector, binaryTiShares.subList(tiBinaryStartIndex,
                        tiBinaryStartIndex + (attributeBitLength * attributeCount)),
                        asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                        clientId, Constants.BINARY_PRIME, pid,
                        attributeBitLength, -1, attributeCount, partyCount);
                tiBinaryStartIndex += attributeBitLength * attributeCount;
                pid++;
                Future<Integer[]> task = es.submit(ois);
                taskList.add(task);
            }
        }

        es.shutdown();

        for (int i = 0; i < leafNodes - 1; i++) {
            Future<Integer[]> taskResponse = taskList.get(i);
            featureVectors[i] = taskResponse.get();
        }

    }

    /**
     * Converts all the attribute thresholds to bits protocol
     *
     * @param startpid
     */
    void convertThresholdsToBits() {

        if (partyHasTree) {
            for (int i = 0; i < leafNodes - 1; i++) {
                attributeThresholdsBitShares.add(Arrays.asList(convertToBits(attributeThresholds[i], attributeBitLength)));
            }
        } else {
            for (int i = 0; i < leafNodes - 1; i++) {
                attributeThresholdsBitShares.add(new ArrayList<>(Collections.nCopies(attributeBitLength, 0)));
            }
        }

    }

    /**
     * compares the attribute threshold with the test vector's attribute value
     *
     * @param startpid
     */
    void doThresholdComparisons() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> taskList = new ArrayList<>();

        int comparisonTiCount = (2 * attributeBitLength) + ((attributeBitLength * (attributeBitLength - 1)) / 2);
        for (int i = 0; i < leafNodes - 1; i++) {
            Comparison comp = new Comparison(Arrays.asList(featureVectors[i]), attributeThresholdsBitShares.get(i),
                    binaryTiShares.subList(tiBinaryStartIndex, tiBinaryStartIndex + comparisonTiCount),
                    asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, Constants.BINARY_PRIME, pid, partyCount);

            Future<Integer> task = es.submit(comp);
            pid++;
            tiBinaryStartIndex += comparisonTiCount;
            taskList.add(task);
        }

        es.shutdown();

        for (int i = 0; i < leafNodes - 1; i++) {
            Future<Integer> taskResponse = taskList.get(i);
            comparisonOutputs[i] = taskResponse.get();
        }

        LOGGER.log(Level.FINE, "threshold comparison results:{0}", Arrays.toString(comparisonOutputs));
    }

    /**
     * convert decimal number to bit shares of length size
     *
     * @param decimal
     * @param size
     * @return
     */
    Integer[] convertToBits(int decimal, int size) {
        Integer[] binaryNum = new Integer[size];
        int i = 0;
        while (decimal > 0) {
            binaryNum[i] = decimal % 2;
            decimal = decimal / 2;
            i++;
        }

        while (i < size) {
            binaryNum[i] = 0;
            i++;
        }

        return binaryNum;
    }

    /**
     * calls the PolynomialComputing class and gets the final output
     *
     * @param startpid
     */
    void computePolynomialEquation() throws InterruptedException, ExecutionException {

        Integer[][] yShares = new Integer[leafNodes][alpha];

        //y[j][r] initialization
        if (partyHasTree) {
            for (int j = 0; j < leafNodes; j++) {
                Integer[] temp = convertToBits(leafToClassIndexMapping[j + 1] - 1, alpha);
                for (int r = 0; r < alpha; r++) {
                    yShares[j][r] = temp[r];
                }
            }
        } else {
            for (int j = 0; j < leafNodes; j++) {
                for (int r = 0; r < alpha; r++) {
                    yShares[j][r] = 0;
                }
            }
        }

        //Polynomial computation
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        int polynomialComputationTiCount = depth * alpha;

        for (int j = 0; j < leafNodes; j++) {
            Integer[] jBinary = convertToBits(j, depth);

            PolynomialComputing pc = new PolynomialComputing(yShares[j], jBinary, alpha,
                    depth, comparisonOutputs, binaryTiShares.subList(tiBinaryStartIndex,
                            tiBinaryStartIndex + polynomialComputationTiCount),
                    new LinkedList<>(protocolIdQueue), pidMapper, commonSender,
                    pid, clientId, asymmetricBit, partyCount);

            pid++;
            tiBinaryStartIndex += polynomialComputationTiCount;
            Future<Integer[]> task = es.submit(pc);
            taskList.add(task);
        }

        es.shutdown();

        for (int j = 0; j < leafNodes; j++) {
            Future<Integer[]> taskResponse = taskList.get(j);
            yShares[j] = taskResponse.get();
        }

        for (int i = 0; i < alpha; i++) {
            finalOutputs[i] = 0;
            for (int j = 0; j < leafNodes; j++) {
                finalOutputs[i] += yShares[j][i];
            }
            finalOutputs[i] %= Constants.BINARY_PRIME;
        }
    }

}
