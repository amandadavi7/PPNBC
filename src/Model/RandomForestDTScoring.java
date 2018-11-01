/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.DotProductInteger;
import Protocol.OR_XOR;
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
public class RandomForestDTScoring extends DecisionTreeScoring implements Callable<Integer[]>{

    Integer[][] leafToClassIndexMappingTransposed;                //leaf node index to class index mapping (stored by the party that has the tree)
    List<TripleInteger> decimalTiShares;
    Logger LOGGER;
    
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

        super(asymmetricBit, pidMapper, senderQueue, clientId, binaryTriples, partyCount, args, protocolIdQueue, protocolID);
        
        pid = 0;
        tiBinaryStartIndex = 0;
        this.decimalTiShares = decimalTriples;
        LOGGER = Logger.getLogger(RandomForestDTScoring.class.getName());
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
                case "testCsv":
                    //party has feature vector
                    testVectorsDecimal = FileIO.loadIntListFromFile(value);
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
                        LOGGER.log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                    depth = Integer.parseInt(prop.getProperty("depth"));
                    attributeCount = Integer.parseInt(prop.getProperty("attribute.count"));
                    attributeBitLength = Integer.parseInt(prop.getProperty("attribute.bitlength"));
                    classLabelCount = Integer.parseInt(prop.getProperty("classlabel.count"));
                    String str = prop.getProperty("leaf.to.class.index.mapping");
                    leafToClassIndexMappingTransposed = new Integer[classLabelCount][(int) Math.pow(2, depth)];
                    String[] vectors = str.split(",");
                    int i = 0;
                    for (String vector : vectors) {
                        int j = 0;
                        String[] nums = vector.split(":");
                        for(String num : nums){
                            leafToClassIndexMappingTransposed[j][i] = Integer.parseInt(num);
                            j++;
                        }
                        i++;
                    }
                    str = prop.getProperty("node.to.attribute.index.mapping");
                    nodeToAttributeIndexMapping = Arrays.stream(str.split(",")).mapToInt(Integer::parseInt).toArray();
                    str = prop.getProperty("attribute.thresholds");
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
                        LOGGER.log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                    depth = Integer.parseInt(prop.getProperty("depth"));
                    attributeCount = Integer.parseInt(prop.getProperty("attribute.count"));
                    attributeBitLength = Integer.parseInt(prop.getProperty("attribute.bitlength"));
                    classLabelCount = Integer.parseInt(prop.getProperty("classlabel.count"));
                    break;
            }
        }
    }

    /**
     * Doing common initializations for both parties here
     */
    void init() {
        initializeModelVariables(args);
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
        LOGGER.fine("Converted Thresholds to Bits");
        
        if (!partyHasTree) {
            convertTestVectorToBits(testVectorsDecimal.get(0));
        }

        getFeatureVectors();
        LOGGER.fine("got the feature vectors:" + Arrays.deepToString(featureVectors));
        
        doThresholdComparisons();
        
        computePolynomialEquation();

        LOGGER.info(modelProtocolId + "-the output in bits: " + Arrays.toString(finalOutputs));
        return finalOutputs;
    }
    
    /**
     * 
     * @param index
     * @param size
     * @return 
     */
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
    @Override
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
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
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
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        Integer[] result = new Integer[leafNodes];
        for (int i = 0; i < leafNodes; i++) {
            result[i] = 0;
            for (int j = 0; j < leafNodes; j++) {
                result[i] += yShares[j][i];
            }
            result[i] %= Constants.BINARY_PRIME;
        }
            
        List<Integer> dummy = new ArrayList<>(Collections.nCopies(leafNodes, 0));
        Integer[] one_hot_encoding_leaf_predicted = null;
        OR_XOR xorModule;
        
        //asymmetric xor to switch primes
        if(asymmetricBit == 1) {
            xorModule = new OR_XOR(Arrays.asList(result), dummy, decimalTiShares.subList(0, leafNodes), 
                    asymmetricBit, 2, pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                    clientId, Constants.PRIME, pid, partyCount);
        } else {
            xorModule = new OR_XOR(dummy, Arrays.asList(result), decimalTiShares.subList(0, leafNodes), 
                    asymmetricBit, 2, pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                    clientId, Constants.PRIME, pid, partyCount);
        }
        Future<Integer[]> xorTask = es.submit(xorModule);
        try {
            one_hot_encoding_leaf_predicted = xorTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        pid++;
           
        List<Future<Integer>> dpTaskList = new ArrayList<>();
        if(leafToClassIndexMappingTransposed == null) {
            for(int i=0;i<classLabelCount;i++) {
                DotProductInteger dpModule = new DotProductInteger(Arrays.asList(one_hot_encoding_leaf_predicted), 
                        dummy, decimalTiShares, pidMapper, commonSender, 
                        new LinkedList<>(protocolIdQueue), clientId, Constants.PRIME, pid, asymmetricBit, partyCount);
                dpTaskList.add(es.submit(dpModule));
                pid++;
            }
        } else {
            for(int i=0;i<classLabelCount;i++) {
                DotProductInteger dpModule = new DotProductInteger(Arrays.asList(one_hot_encoding_leaf_predicted), 
                        Arrays.asList(leafToClassIndexMappingTransposed[i]), decimalTiShares,
                        pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                        clientId, Constants.PRIME, pid, asymmetricBit, partyCount);
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
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

}
