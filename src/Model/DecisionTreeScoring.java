/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.Comparison;
import Protocol.CompositeProtocol;
import Protocol.OIS;
import Protocol.Utility.BatchMultiplicationByte;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * compute the final tree output by computing the polynomial circuit
 *
 * @author keerthanaa
 */
class PolynomialComputing extends CompositeProtocol implements Callable<Integer[]> {

    int s, u, alpha;
    int[] comparisonOutputs;
    Integer[] y_j, jBinary;
    List<TripleByte> tiShares;

    /**
     * Constructor (takes initial y[j][r] for a given j and computes the final
     * y[j][r] output
     *
     * @param y_j
     * @param jBinary
     * @param alpha
     * @param depth
     * @param zOutputs
     * @param tiShares
     * @param protocolIdQueue
     * @param senderQueue
     * @param receiverQueue
     * @param protocolID
     * @param clientId
     * @param asymmetricBit
     * @param partyCount
     */
    public PolynomialComputing(Integer[] y_j, Integer[] jBinary, int alpha, int depth,
            int[] zOutputs, List<TripleByte> tiShares,
            Queue<Integer> protocolIdQueue,
            BlockingQueue<Message> senderQueue, BlockingQueue<Message> receiverQueue,
            int protocolID, int clientId, int asymmetricBit, int partyCount) {

        super(protocolID, senderQueue, receiverQueue, protocolIdQueue, clientId, asymmetricBit, partyCount);

        this.s = depth;
        u = 1;
        this.alpha = alpha;
        this.y_j = y_j;
        this.comparisonOutputs = zOutputs;
        this.jBinary = jBinary;
        this.tiShares = tiShares;
    }

    /**
     *
     * @return @throws Exception
     */
    @Override
    public Integer[] call() throws Exception {

        startHandlers();
        int pid = 0;

        while (s > 0) {
            ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
            List<Future<Integer[]>> taskList = new ArrayList<>();

            List<Integer> yj = Arrays.asList(y_j);
            List<Integer> z_u = new ArrayList<>(Collections.<Integer>nCopies(Constants.batchSize,
                    (comparisonOutputs[u - 1] + asymmetricBit * jBinary[s - 1]) % Constants.binaryPrime));

            int i = 0;
            do {
                int toIndex = Math.min(i + Constants.batchSize, alpha);
                initQueueMap(recQueues, pid);

                BatchMultiplicationByte mults = new BatchMultiplicationByte(
                        yj.subList(i, toIndex), z_u, tiShares.subList(i, toIndex),
                        senderQueue, recQueues.get(pid),
                        new LinkedList<>(protocolIdQueue), clientID,
                        Constants.binaryPrime, pid, asymmetricBit, protocolId,
                        partyCount);

                Future<Integer[]> task = es.submit(mults);
                taskList.add(task);
                i = toIndex;
                pid++;

            } while (i < alpha);

            int batches = taskList.size();
            int globalIndex = 0;
            for (i = 0; i < batches; i++) {
                Future<Integer[]> taskResponse = taskList.get(i);
                try {
                    Integer[] arr = taskResponse.get();
                    for (int l = 0; l < arr.length; l++) {
                        y_j[globalIndex] = arr[l];
                        globalIndex++;
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            u = 2 * u + jBinary[s - 1];
            s--;
        }

        tearDownHandlers();
        System.out.println("pid:" + protocolId + " returning" + Arrays.toString(y_j));
        return y_j;

    }
}

/**
 * This class takes a row of attributes values and predicts the class label
 * based on the decision tree
 *
 * @author keerthanaa
 */
public class DecisionTreeScoring extends Model implements Callable<List<Integer[]>>{

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
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param binaryTriples
     * @param partyCount
     * @param args
     * @param protocolIdQueue
     */
    public DecisionTreeScoring(int asymmetricBit, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, List<TripleByte> binaryTriples,
            int partyCount, String[] args, LinkedList<Integer> protocolIdQueue) {

        super(senderQueue, receiverQueue, clientId, asymmetricBit, partyCount, protocolIdQueue);

        initializeModelVariables(args);
        pid = 0;
        tiBinaryStartIndex = 0;
        this.binaryTiShares = binaryTriples;
    }

    /**
     * initialize variables
     *
     * @param args
     */
    private void initializeModelVariables(String[] args) {
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
                    leafToClassIndexMapping = new int[(int) Math.pow(2, depth) + 1];
                    String[] nums = str.split(",");
                    int i = 1;
                    for (String num : nums) {
                        leafToClassIndexMapping[i] = Integer.parseInt(num);
                        i++;
                    }
                    //leafToClassIndexMapping = Arrays.stream(str.split(",")).mapToInt(Integer::parseInt).toArray();
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
        alpha = (int) Math.ceil(Math.log(classLabelCount) / Math.log(2.0));
        finalOutputs = new Integer[alpha];
    }

    /**
     * Main method for the DT Scoring algorithm
     */
    public List<Integer[]> call() {
        startModelHandlers();

        init();

        long startTime = System.currentTimeMillis();

        convertThresholdsToBits();
        System.out.println("converting decimal thresholds to bitshares: from " + Arrays.toString(attributeThresholds) + " to " + attributeThresholdsBitShares);

        if (!partyHasTree) {
            convertTestVectorToBits(testVectorsDecimal.get(0));
            System.out.println("converting to bits feature vector: from " + testVectorsDecimal.get(0) + " to " + testVector);
        }

        getFeatureVectors();

        System.out.println("got the feature vectors:" + Arrays.deepToString(featureVectors));

        doThresholdComparisons();

        computePolynomialEquation();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        System.out.println("the output in bits: " + Arrays.toString(finalOutputs));
        System.out.println("Avg time duration:" + elapsedTime);

        teardownModelHandlers();
        
        List<Integer[]> result = new ArrayList<>();
        result.add(finalOutputs);
        return result;
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

                System.out.println("PID:" + pid + " k=" + nodeToAttributeIndexMapping[i]);
                initQueueMap(recQueues, pid);
                OIS ois = new OIS(null, binaryTiShares.subList(tiBinaryStartIndex,
                        tiBinaryStartIndex + (attributeBitLength * attributeCount)),
                        asymmetricBit, commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue),
                        clientId, Constants.binaryPrime, pid, attributeBitLength,
                        nodeToAttributeIndexMapping[i], attributeCount, partyCount);
                tiBinaryStartIndex += attributeBitLength * attributeCount;
                pid++;
                Future<Integer[]> task = es.submit(ois);
                taskList.add(task);

            }
        } else {
            for (int i = 0; i < leafNodes - 1; i++) {

                System.out.println("PID:" + pid + " k=" + testVector);
                initQueueMap(recQueues, pid);
                OIS ois = new OIS(testVector, binaryTiShares.subList(tiBinaryStartIndex,
                        tiBinaryStartIndex + (attributeBitLength * attributeCount)),
                        asymmetricBit, commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue),
                        clientId, Constants.binaryPrime, pid,
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
                attributeThresholdsBitShares.add(new ArrayList<Integer>(Collections.nCopies(attributeBitLength, 0)));
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
            initQueueMap(recQueues, pid);
            Comparison comp = new Comparison(Arrays.asList(featureVectors[i]), attributeThresholdsBitShares.get(i),
                    binaryTiShares.subList(tiBinaryStartIndex, tiBinaryStartIndex + comparisonTiCount),
                    asymmetricBit, commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue),
                    clientId, Constants.binaryPrime, pid, partyCount);

            Future<Integer> task = es.submit(comp);
            pid++;
            tiBinaryStartIndex += comparisonTiCount;
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

        System.out.println("threshold comparison results:" + Arrays.toString(comparisonOutputs));
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

    /**
     * calls the polynomialcomputing class and gets the final output
     *
     * @param startpid
     */
    void computePolynomialEquation() {

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
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        int polynomialComputationTiCount = depth * alpha;

        for (int j = 0; j < leafNodes; j++) {
            Integer[] jBinary = convertToBits(j, depth);

            initQueueMap(recQueues, pid);

            PolynomialComputing pc = new PolynomialComputing(yShares[j], jBinary, alpha,
                    depth, comparisonOutputs, binaryTiShares.subList(tiBinaryStartIndex, tiBinaryStartIndex + polynomialComputationTiCount),
                    new LinkedList<>(protocolIdQueue), commonSender, recQueues.get(pid),
                    pid, clientId, asymmetricBit, partyCount);

            pid++;
            tiBinaryStartIndex += polynomialComputationTiCount;
            Future<Integer[]> task = es.submit(pc);
            taskList.add(task);
        }

        es.shutdown();

        for (int j = 0; j < leafNodes; j++) {
            Future<Integer[]> taskResponse = taskList.get(j);
            try {
                yShares[j] = taskResponse.get();
                System.out.println("j=" + j + ", y[j]=" + Arrays.toString(yShares[j]));
            } catch (InterruptedException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        System.out.println("final:" + Arrays.toString(finalOutputs));
        for (int i = 0; i < alpha; i++) {
            finalOutputs[i] = 0;
            for (int j = 0; j < leafNodes; j++) {
                finalOutputs[i] += yShares[j][i];
            }
            finalOutputs[i] %= Constants.binaryPrime;
        }
    }

}
