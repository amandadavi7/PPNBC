/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.BitDecomposition;
import Protocol.Comparison;
import Protocol.MultiplicationByte;
import Protocol.OR_XOR;
import Protocol.Utility.BatcherSortKNN;
import Protocol.Utility.CrossMultiplyCompare;
import Protocol.Utility.JaccardDistance;
import Protocol.Utility.SwapCircuitKNN;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
 * KNN Sort and Swap - Sort K elements and iteratively swap to maintain the top K
 * @author keerthanaa
 */
public class KNNSortAndSwap extends Model {

    List<List<Integer>> trainingShares;
    List<Integer> testShare;
    List<Integer> classLabels;
    List<List<Integer>> jaccardDistances, KjaccardDistances;
    int pid, attrLength, K, decimalTiIndex, binaryTiIndex, trainingSharesCount;
    int ccTICount, prime, bitLength, comparisonTICount, bitDTICount;
    List<TripleInteger> decimalTiShares;
    List<TripleByte> binaryTiShares;
    private static final Logger LOGGER = Logger.getLogger(KNNSortAndSwap.class.getName());

    /**
     * Constructor
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
    public KNNSortAndSwap(int asymmetricBit, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue, int clientId, List<TripleByte> binaryTriples,
            List<TripleInteger> decimalTriples, int partyCount, String[] args, 
            Queue<Integer> protocolIdQueue, int protocolID) {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);
        pid = 0;

        initalizeModelVariables(args);
        
        this.prime = (int) Math.pow(2.0, bitLength);
        comparisonTICount = (2*bitLength) + ((bitLength*(bitLength-1))/2);
        bitDTICount = bitLength*3 - 2;
        ccTICount = comparisonTICount + 2*bitDTICount;

        this.attrLength = testShare.size();
        this.trainingSharesCount = trainingShares.size();
        
        this.binaryTiShares = binaryTriples;
        this.decimalTiShares = decimalTriples;

        this.decimalTiIndex = 0;
        this.binaryTiIndex = 0;

        KjaccardDistances = new ArrayList<>();
    }

    /**
     * Initialize model variables
     * @param args 
     */
    private void initalizeModelVariables(String[] args) {

        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.partyUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];

            switch (command) {
                case "trainingShares":
                    trainingShares = FileIO.loadIntListFromFile(value);
                    break;
                case "testShares":
                    testShare = (FileIO.loadIntListFromFile(value)).get(0);
                    break;
                case "classLabels":
                    classLabels = (FileIO.loadIntListFromFile(value)).get(0);
                    break;
                case "K":
                    K = Integer.parseInt(value);
                    break;
                case "bitLength":
                    bitLength = Integer.parseInt(value);
                    break;

            }

        }

    }

    /**
     * Compare the JD(index) with the first K JDs
     * 
     * @param index
     * @return 
     */
    Integer[] getKComparisonResults(int index) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> taskList = new ArrayList<>();
        //Do all the k comparisons with the training share
        Integer[] comparisonResults = new Integer[K];
        for (int i = 0; i < K; i++) {
            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(jaccardDistances.get(index).get(1),
                    jaccardDistances.get(index).get(0), KjaccardDistances.get(i).get(1), 
                    KjaccardDistances.get(i).get(0), asymmetricBit, 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 2), 
                    binaryTiShares.subList(binaryTiIndex, binaryTiIndex + ccTICount), 
                    pidMapper, commonSender, clientId, prime,
                    Constants.binaryPrime, pid, new LinkedList<>(protocolIdQueue), 
                    partyCount, bitLength);

            pid++;
            //decimalTiIndex += 2;
            //binaryTiIndex += ccTICount;

            Future<Integer> ccTask = es.submit(ccModule);
            taskList.add(ccTask);
        }

        es.shutdown();

        for (int i = 0; i < K; i++) {
            Future<Integer> ccTask = taskList.get(i);
            comparisonResults[i] = ccTask.get();
        }

        return comparisonResults;
    }

    /**
     * Product of comparison outputs sequentially
     * 
     * @param comparisonResults
     * @return 
     */
    Integer[] comparisonMultiplicationResultsSequential(Integer[] comparisonResults) throws InterruptedException {
        Integer[] comparisonMultiplications = new Integer[K];
        comparisonMultiplications[0] = 0;
        comparisonMultiplications[1] = comparisonResults[0];

        for (int i = 1; i < K - 1; i++) {
            MultiplicationByte mult = new MultiplicationByte(comparisonMultiplications[i], 
                    comparisonResults[i], binaryTiShares.get(binaryTiIndex), 
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                    clientId, Constants.binaryPrime, pid, asymmetricBit, 0, partyCount);
            pid++;
            comparisonMultiplications[i + 1] = mult.call();
        }
        return comparisonMultiplications;
    }

    /**
     * Take the index and generate all comparison results, sequential comparison
     * multiplication results, assymetric XOR to change the prime from
     * binaryPrime to decimalPrime and call the swap circuit
     *
     * @param index
     */
    void swapTrainingShares(int index) throws InterruptedException, ExecutionException {

        //get all the K comparison results
        Integer[] comparisonResults = getKComparisonResults(index);
        
        //get all PI(Cj) from j = 0 to j = K-2 index
        Integer[] comparisonMultiplications = comparisonMultiplicationResultsSequential(comparisonResults);

        comparisonMultiplications[0] = Math.floorMod(asymmetricBit - comparisonResults[0], 
                Constants.binaryPrime);
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        
        List<Future<Integer>> taskList = new ArrayList<>();
        
        for (int i = 1; i < K; i++) {
            MultiplicationByte mult = new MultiplicationByte(comparisonMultiplications[i],
                    Math.floorMod(asymmetricBit - comparisonResults[i], Constants.binaryPrime),
                    binaryTiShares.get(binaryTiIndex), pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId,
                    Constants.binaryPrime, pid, asymmetricBit, 0, partyCount);

            //binaryTiIndex++;
            pid++;
            Future<Integer> multTask = es.submit(mult);
            taskList.add(multTask);
        }

        for (int i = 1; i < K; i++) {
            Future<Integer> multTask = taskList.get(i - 1);
            comparisonMultiplications[i] = multTask.get();
        }

        //Do xor between comparison results to change the prime
        List<Integer> dummy = new ArrayList<>(Collections.nCopies(K, 0));
        OR_XOR xorComp = null, xorCompMults = null;

        // Convert comparisonResults and comparisonMultiplications from binary prime to decimal prime
        if (asymmetricBit == 1) {
            xorComp = new OR_XOR(Arrays.asList(comparisonResults), dummy, 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + K),
                    asymmetricBit, 2, pidMapper, commonSender, 
                    new LinkedList<>(protocolIdQueue), clientId, prime, 
                    pid, partyCount);
            pid++;
            //decimalTiIndex += K;
            xorCompMults = new OR_XOR(Arrays.asList(comparisonMultiplications), 
                    dummy, decimalTiShares.subList(decimalTiIndex, decimalTiIndex + K),
                    asymmetricBit, 2, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, 
                    pid, partyCount);
        } else {
            xorComp = new OR_XOR(dummy, Arrays.asList(comparisonResults), 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + K),
                    asymmetricBit, 2, pidMapper, commonSender, 
                    new LinkedList<>(protocolIdQueue), clientId, prime, 
                    pid, partyCount);
            pid++;
            //decimalTiIndex += K;
            xorCompMults = new OR_XOR(dummy, Arrays.asList(comparisonMultiplications), 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + K), asymmetricBit, 
                    2, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, partyCount);
        }

        Future<Integer[]> xorTask1 = es.submit(xorComp);
        Future<Integer[]> xorTask2 = es.submit(xorCompMults);
        pid++;
        //decimalTiIndex += K;

        comparisonResults = xorTask1.get();
            comparisonMultiplications = xorTask2.get();

        List<Future<Integer[]>> swapTaskList = new ArrayList<>();

        for (int i = 0; i < K; i++) {

            // send null if i = 0 , else send (i - 1)th jaccard distance packet;
            List<Integer> prev = null;

            if (i != 0) {
                prev = KjaccardDistances.get(i - 1);
            }

            SwapCircuitKNN swapModule = new SwapCircuitKNN(index,
                    i, comparisonResults, comparisonMultiplications, pid,
                    new LinkedList<>(protocolIdQueue), prime, asymmetricBit, 
                    pidMapper, commonSender, clientId, 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 9),
                    jaccardDistances.get(index), KjaccardDistances.get(i), prev, partyCount);

            swapTaskList.add(es.submit(swapModule));
            pid++;
            //decimalTiIndex += 9;
        }

        es.shutdown();

        // update positions from (kth to 1st) position, as kth position is independent of the rest
        for (int i = K - 1; i >= 0; i--) {
            Future<Integer[]> swapTask = swapTaskList.get(i);
            Integer[] results = swapTask.get();
                for (int j = 0; j < 3; j++) {
                    KjaccardDistances.get(i).set(j, results[j]);
                }
        }

    }

    /**
     * Compute the majority class label and return
     * @return 
     */
    int computeMajorityClassLabel() throws InterruptedException, ExecutionException {
        int classLabelSum = 0;
        int predictedClassLabel = -1;

        //Get the sum of K - class labels
        for (int i = 0; i < K; i++) {
            classLabelSum += KjaccardDistances.get(i).get(2);
        }

        int oneCount = Math.floorMod(classLabelSum, prime);
        // The number of zeroCounts is just K - oneCount
        int zeroCount = Math.floorMod(asymmetricBit * K - classLabelSum, prime);

        ExecutorService es = Executors.newFixedThreadPool(2);

        //Do a comparison between oneCount and zeroCount
        BitDecomposition bitDModuleOne = new BitDecomposition(oneCount,
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + bitDTICount), 
                asymmetricBit, bitLength, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, 
                pid, partyCount);
        pid++;
        //binaryTiIndex += bitDTICount;
        Future<List<Integer>> bitTaskOne = es.submit(bitDModuleOne);

        BitDecomposition bitDModuleZero = new BitDecomposition(zeroCount,
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + bitDTICount), 
                asymmetricBit, bitLength, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, 
                pid, partyCount);
        pid++;
        //binaryTiIndex += bitDTICount;
        Future<List<Integer>> bitTaskZero = es.submit(bitDModuleZero);

        es.shutdown();
        List<Integer> numOfOnePredictions = bitTaskOne.get();
            List<Integer> numOfZeroPredictions = bitTaskZero.get();
        
        Comparison compClassLabels = new Comparison(numOfOnePredictions, numOfZeroPredictions, 
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + comparisonTICount),
                asymmetricBit, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, 
                pid, partyCount);

        predictedClassLabel = compClassLabels.call();
        pid++;
        //binaryTiIndex += comparisonTICount;
        
        return predictedClassLabel;
    }
    
    /**
     * run the KNN model
     * @return 
     * @throws java.lang.InterruptedException 
     * @throws java.util.concurrent.ExecutionException 
     */
    public int runModel() throws InterruptedException, ExecutionException {
        //Jaccard Computation for all the training shares
        long startTime = System.currentTimeMillis();
        
        int decTICount = attrLength * 2 * trainingSharesCount;
        JaccardDistance jdModule = new JaccardDistance(trainingShares, testShare,
                asymmetricBit, decimalTiShares, pidMapper, commonSender,
                clientId, prime, pid, 
                new LinkedList<>(protocolIdQueue), partyCount);

        jaccardDistances = jdModule.call();
        pid++;
        //decimalTiIndex += decTICount;

        //add class labels to JD data structure (contains OR, XOR and Class Labels)
        for (int i = 0; i < trainingShares.size(); i++) {
            jaccardDistances.get(i).add(classLabels.get(i));
        }

        for (int i = 0; i < K; i++) {
            KjaccardDistances.add(new ArrayList<>(jaccardDistances.get(i)));
        }

        //Sorting the first K numbers
        LOGGER.log(Level.FINE, "KjaccardDistances before:{0}", KjaccardDistances);
        
        BatcherSortKNN sortModule = new BatcherSortKNN(KjaccardDistances,
                asymmetricBit, decimalTiShares, binaryTiShares,
                pidMapper, commonSender, clientId, prime, pid,
                new LinkedList<>(protocolIdQueue), partyCount, K, bitLength);

        KjaccardDistances = sortModule.call();

        LOGGER.log(Level.FINE, "KjaccardDistances:{0}", KjaccardDistances);

        //Iterator circuit for rest of the training examples
        for (int i = K; i < trainingSharesCount; i++) {
            LOGGER.log(Level.INFO, "calling for training example:{0}", i);
            swapTrainingShares(i);
        }

        LOGGER.log(Level.FINE, "KjaccardDistances after iterating all the training examples:{0}", KjaccardDistances);

        int predictedLabel = computeMajorityClassLabel();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        LOGGER.log(Level.INFO, "Label:{0}", predictedLabel);
        LOGGER.log(Level.INFO, "Time taken:{0}ms", elapsedTime);
        return predictedLabel;
    }

}
