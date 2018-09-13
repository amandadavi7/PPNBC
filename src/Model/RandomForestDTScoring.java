/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.Comparison;
import Protocol.CompositeProtocol;
import Protocol.DotProductInteger;
import Protocol.OIS;
import Protocol.OR_XOR;
import Protocol.Utility.BatchMultiplicationByte;
import Protocol.Utility.PolynomialComputing;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
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
import java.util.concurrent.Callable;
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
public class RandomForestDTScoring extends Model implements Callable<Integer[]>{

    int prime, depth, attributeBitLength, attributeCount;//depth d, bitlength for each attribute value, total no. of attributes
    boolean partyHasTree;                         //true if the party has the tree, false if it has the test case
    Integer[][] featureVectors;                   //Shares of features 2^d - 1 vectors for each internal node
    // TODO - modify this as a list instead of list of lists
    List<List<Integer>> testVectorsDecimal;       //Test case - feature vectors as decimal numbers
    List<List<Integer>> testVector;               //Test case - a list of features represented as binary values
    Integer[][] leafToClassIndexMappingTransposed;                //leaf node index to class index mapping (stored by the party that has the tree)
    int[] nodeToAttributeIndexMapping;            //internal node index to the attribute intex mapping (stored by the party that has the tree)
    int[] attributeThresholds;                    //each internal node's attribute threshold
    List<List<Integer>> attributeThresholdsBitShares; //attribute thresholds as bits
    int leafNodes, tiBinaryStartIndex, classLabelCount, pid; //leafNode - no. of leafnodes, classlabelcount - total number of class labels
    int[] comparisonOutputs;
    Integer[] finalOutputs;
    List<TripleByte> binaryTiShares;
    List<TripleInteger> decimaTiShares;

    /**
     * Constructor 
     * 2 party DT scoring: 
     * 
     * one party has the tree, one party has the
     * feature vector In args, 
     * 
     * party1: pass the feature vector as csv file
     * (testCsv) pass the tree properties (depth, attribute count, attribute
     * bitlength, class label count in properties file (treeproperties) 
     * 
     * party2:
     * pass the tree as a properties file (storedtree)
     *
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param binaryTriples
     * @param decimalTriples
     * @param partyCount
     * @param args
     * @param protocolIdQueue
     * @param protocolID
     */
    public RandomForestDTScoring(int asymmetricBit,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, int clientId,
            List<TripleByte> binaryTriples, List<TripleInteger> decimalTriples,
            int partyCount, String[] args, Queue<Integer> protocolIdQueue,
            int protocolID) {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);

        initializeModelVariables(args);
        pid = 0;
        tiBinaryStartIndex = 0;
        this.binaryTiShares = binaryTriples;
        this.decimaTiShares = decimalTriples;
    }

    /**
     * initialize variables
     *
     * @param args
     */
    private void initializeModelVariables(String[] args) {
        leafToClassIndexMappingTransposed = null;
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
                case "prime":
                    prime = Integer.parseInt(value);
                    break;
                case "testCsv":
                    //party has feature vector
                    testVectorsDecimal = FileIO.loadIntListFromFile(value);
                    System.out.println("testcsv:" + testVectorsDecimal);
                    break;
                case "storedtree":
                    //party has the tree
                    partyHasTree = true;
                    Properties prop = new Properties();
                    InputStream input = null;
                    try {
                        input = new FileInputStream(value);
                        prop.load(input);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(DecisionTreeScoring.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(DecisionTreeScoring.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    depth = Integer.parseInt(prop.getProperty("depth"));
                    attributeCount = Integer.parseInt(prop.getProperty("attributecount"));
                    attributeBitLength = Integer.parseInt(prop.getProperty("attributebitlength"));
                    classLabelCount = Integer.parseInt(prop.getProperty("classlabelcount"));
                    String str = prop.getProperty("leaftoclassindexmapping");
                    leafToClassIndexMappingTransposed = new Integer[classLabelCount][(int) Math.pow(2, depth)];
                    String[] vectors = str.split(",");
                    int i = 0;
                    for (String vector : vectors) {
                        int j = 0;
                        String[] nums = vector.split(":");
                        for(String num : nums){
                            leafToClassIndexMappingTransposed[j][i] = (int)(Float.parseFloat(num));
                            j++;
                        }
                        i++;
                    }
                    str = prop.getProperty("nodetoattributeindexmapping");
                    nodeToAttributeIndexMapping = Arrays.stream(str.split(",")).mapToInt(Integer::parseInt).toArray();
                    str = prop.getProperty("attributethresholds");
                    attributeThresholds = Arrays.stream(str.split(",")).mapToInt(Integer::parseInt).toArray();
                    break;
                case "treeproperties":
                    //party has feature vector
                    partyHasTree = false;
                    prop = new Properties();
                    input = null;
                    try {
                        input = new FileInputStream(value);
                        prop.load(input);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(DecisionTreeScoring.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(DecisionTreeScoring.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    depth = Integer.parseInt(prop.getProperty("depth"));
                    attributeCount = Integer.parseInt(prop.getProperty("attributecount"));
                    attributeBitLength = Integer.parseInt(prop.getProperty("attributebitlength"));
                    classLabelCount = Integer.parseInt(prop.getProperty("classlabelcount"));
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
    void init() {

        leafNodes = (int) Math.pow(2, depth);
        featureVectors = new Integer[leafNodes - 1][attributeBitLength];
        attributeThresholdsBitShares = new ArrayList<>();
        comparisonOutputs = new int[leafNodes - 1];
        finalOutputs = new Integer[classLabelCount];
    }

    /**
     * Main method for the DT Scoring algorithm
     */
    @Override
    public Integer[] call() {
        init();

        convertThresholdsToBits();
        
        if (!partyHasTree) {
            convertTestVectorToBits(testVectorsDecimal.get(0));
        }

        getFeatureVectors();

        System.out.println("OIS complete - got feature vectors");
        doThresholdComparisons();
        System.out.println("Comparisons complete");
        computePolynomialEquation();

        System.out.println("the output in bits: " + Arrays.toString(finalOutputs));
        return finalOutputs;
    }

    /**
     * gets the feature vectors for each attribute of internal node using
     * Oblivious Input selection Protocol
     */
    void getFeatureVectors() {

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        if (partyHasTree) {
            for (int i = 0; i < leafNodes - 1; i++) {

                OIS ois = new OIS(null, binaryTiShares.subList(tiBinaryStartIndex,
                        tiBinaryStartIndex + (attributeBitLength * attributeCount)),
                        asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                        clientId, Constants.binaryPrime, pid, attributeBitLength,
                        nodeToAttributeIndexMapping[i], attributeCount, partyCount);
                // TODO - disabled ti increment for testing
                //tiBinaryStartIndex += attributeBitLength * attributeCount;
                pid++;
                Future<Integer[]> task = es.submit(ois);
                taskList.add(task);

            }
        } else {
            for (int i = 0; i < leafNodes - 1; i++) {

                OIS ois = new OIS(testVector, binaryTiShares.subList(tiBinaryStartIndex,
                        tiBinaryStartIndex + (attributeBitLength * attributeCount)),
                        asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                        clientId, Constants.binaryPrime, pid,
                        attributeBitLength, -1, attributeCount, partyCount);
                // TODO - disabled ti increment for testing
                //tiBinaryStartIndex += attributeBitLength * attributeCount;
                pid++;
                Future<Integer[]> task = es.submit(ois);
                taskList.add(task);
            }
        }

        es.shutdown();

        for (int i = 0; i < leafNodes - 1; i++) {
            Future<Integer[]> taskResponse = taskList.get(i);
            try {
                featureVectors[i] = taskResponse.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
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

        //System.out.println("thresholds bits:"+attributeThresholdsBitShares);
    }

    /**
     * compares the attribute threshold with the test vector's attribute value
     *
     * @param startpid
     */
    void doThresholdComparisons() {

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();

        int comparisonTiCount = (2 * attributeBitLength) + ((attributeBitLength * (attributeBitLength - 1)) / 2);
        for (int i = 0; i < leafNodes - 1; i++) {
            Comparison comp = new Comparison(Arrays.asList(featureVectors[i]), attributeThresholdsBitShares.get(i),
                    binaryTiShares.subList(tiBinaryStartIndex, tiBinaryStartIndex + comparisonTiCount),
                    asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, Constants.binaryPrime, pid, partyCount);

            Future<Integer> task = es.submit(comp);
            pid++;
            // TODO - disabled ti increment for testing
            //tiBinaryStartIndex += comparisonTiCount;
            taskList.add(task);
        }

        es.shutdown();

        for (int i = 0; i < leafNodes - 1; i++) {
            Future<Integer> taskResponse = taskList.get(i);
            try {
                comparisonOutputs[i] = taskResponse.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    
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
    
    
    Integer[] oneHotEncoding(int index, int size) {
        Integer[] ohe = new Integer[size];
        for(int i=0;i<size;i++) {
            if(i==index){
                ohe[i] = asymmetricBit;
            } else {
                ohe[i] = 0;
            }
        }
        return ohe;
    }

    /**
     * calls the PolynomialComputing class and gets the final output
     * @param startpid
     */
    void computePolynomialEquation() {

        Integer[][] yShares = new Integer[leafNodes][leafNodes];

        //y[j][r] initialization
        for (int j = 0; j < leafNodes; j++) {
                Integer[] temp = oneHotEncoding(j, leafNodes);
                for (int r = 0; r < leafNodes; r++) {
                    yShares[j][r] = temp[r];
                }
            }

        //Polynomial computation
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        int polynomialComputationTiCount = depth * leafNodes;

        for (int j = 0; j < leafNodes; j++) {
            Integer[] jBinary = convertToBits(j, depth);

            PolynomialComputing pc = new PolynomialComputing(yShares[j], jBinary, leafNodes,
                    depth, comparisonOutputs, binaryTiShares.subList(tiBinaryStartIndex, 
                            tiBinaryStartIndex + polynomialComputationTiCount),
                    new LinkedList<>(protocolIdQueue), pidMapper, commonSender,
                    pid, clientId, asymmetricBit, partyCount);

            pid++;
            //tiBinaryStartIndex += polynomialComputationTiCount;
            Future<Integer[]> task = es.submit(pc);
            taskList.add(task);
        }

        for (int j = 0; j < leafNodes; j++) {
            Future<Integer[]> taskResponse = taskList.get(j);
            try {
                yShares[j] = taskResponse.get();
                //System.out.println("j=" + j + ", y[j]=" + Arrays.toString(yShares[j]));
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        Integer[] result = new Integer[leafNodes];
        for (int i = 0; i < leafNodes; i++) {
            result[i] = 0;
            for (int j = 0; j < leafNodes; j++) {
                result[i] += yShares[j][i];
            }
            result[i] %= Constants.binaryPrime;
        }
            
        List<Integer> dummy = new ArrayList<>(Collections.nCopies(leafNodes, 0));
        Integer[] one_hot_encoding_leaf_predicted = null;
        OR_XOR xorModule;
        
        //asymmetric xor to switch primes
        if(asymmetricBit == 1) {
            xorModule = new OR_XOR(Arrays.asList(result), dummy, decimaTiShares.subList(0, leafNodes), 
                    asymmetricBit, 2, pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                    clientId, prime, pid, partyCount);
        } else {
            xorModule = new OR_XOR(dummy, Arrays.asList(result), decimaTiShares.subList(0, leafNodes), 
                    asymmetricBit, 2, pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                    clientId, prime, pid, partyCount);
        }
        Future<Integer[]> xorTask = es.submit(xorModule);
        try {
            one_hot_encoding_leaf_predicted = xorTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(RandomForestDTScoring.class.getName()).log(Level.SEVERE, null, ex);
        }
        pid++;
           
        List<Future<Integer>> dpTaskList = new ArrayList<>();
        if(leafToClassIndexMappingTransposed == null) {
            for(int i=0;i<classLabelCount;i++) {
                DotProductInteger dpModule = new DotProductInteger(Arrays.asList(one_hot_encoding_leaf_predicted), 
                        dummy, decimaTiShares, pidMapper, commonSender, 
                        new LinkedList<>(protocolIdQueue), clientId, prime, pid, asymmetricBit, partyCount);
                dpTaskList.add(es.submit(dpModule));
                pid++;
            }
        } else {
            for(int i=0;i<classLabelCount;i++) {
                DotProductInteger dpModule = new DotProductInteger(Arrays.asList(one_hot_encoding_leaf_predicted), 
                        Arrays.asList(leafToClassIndexMappingTransposed[i]), decimaTiShares,
                        pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                        clientId, prime, pid, asymmetricBit, partyCount);
                dpTaskList.add(es.submit(dpModule));
                pid++;
            }
        }
        es.shutdown();
        for(int i=0;i<classLabelCount;i++) {
            Future<Integer> dpResult = dpTaskList.get(i);
            try {
                finalOutputs[i] = dpResult.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(RandomForestDTScoring.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
