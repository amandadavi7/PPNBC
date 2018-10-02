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
import Protocol.Utility.BatchMultiplicationInteger;
import Protocol.Utility.CrossMultiplyCompare;
import Protocol.Utility.JaccardDistance;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;
import Utility.ThreadPoolManager;
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
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author keerthanaa
 */
public class KNNBinarySearch extends Model {

    List<List<Integer>> trainingShares;
    List<Integer> testShare;
    List<Integer> classLabels;
    List<List<Integer>> jaccardDistances;
    int pid, attrLength, K, decimalTiIndex, binaryTiIndex, trainingSharesCount;
    int ccTICount, prime, bitLength, comparisonTICount, bitDTICount;
    List<TripleInteger> decimalTiShares;
    List<TripleByte> binaryTiShares;
    ExecutorService es;

    public KNNBinarySearch(int asymmetricBit,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, int clientId, List<TripleByte> binaryTriples,
            List<TripleInteger> decimalTriples, int partyCount, String[] args) {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount);

        pid = 0;
        initalizeModelVariables(args);

        this.prime = (int) Math.pow(2.0, bitLength);
        comparisonTICount = (2 * bitLength) + ((bitLength * (bitLength - 1)) / 2);
        bitDTICount = bitLength * 3 - 2;
        ccTICount = comparisonTICount + 2 * bitDTICount;

        this.attrLength = testShare.size();
        this.trainingSharesCount = trainingShares.size();

        this.binaryTiShares = binaryTriples;
        this.decimalTiShares = decimalTriples;

        this.decimalTiIndex = 0;
        this.binaryTiIndex = 0;

        es = ThreadPoolManager.getInstance();
    }

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

        //System.out.println("Test Print: training=" + trainingShares + " test=" +
        //        testShare + " classL=" + classLabels + " K=" + K);
    }

    /**
     * returns comparison results of all training examples: 
     * 1 if <= Threshold, 0 otherwise
     * converted comparison output to decimal prime
     * @param threshold
     * @return 
     */
    public Integer[] getComparisonResults(int threshold) {
        Integer[] comparisonResults = new Integer[trainingSharesCount];
        List<Future<Integer>> ccTaskList = new ArrayList<>();

        for (int i = 0; i < trainingSharesCount; i++) {
            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(threshold, asymmetricBit,
                    jaccardDistances.get(i).get(1), jaccardDistances.get(i).get(0), asymmetricBit,
                    decimalTiShares, binaryTiShares, pidMapper, commonSender,
                    clientId, prime, Constants.binaryPrime, pid,
                    new LinkedList<>(protocolIdQueue), partyCount, bitLength);
            Future<Integer> task = es.submit(ccModule);
            ccTaskList.add(task);
            pid++;
        }

        for (int i = 0; i < trainingSharesCount; i++) {
            Future<Integer> task = ccTaskList.get(i);
            try {
                comparisonResults[i] = task.get();
                System.out.println(threshold+"/"+asymmetricBit+" >= " + jaccardDistances.get(i).get(1)+"/"+jaccardDistances.get(i).get(0)+" => "+comparisonResults[i]);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNNBinarySearch.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        List<Integer> dummy = new ArrayList<>(Collections.nCopies(trainingSharesCount, 0));
        OR_XOR xorModule;
        // Binary to decimal prime conversion
        if (clientId == 1) {
            xorModule = new OR_XOR(Arrays.asList(comparisonResults),
                    dummy, decimalTiShares, asymmetricBit, 2, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);

        } else {
            xorModule = new OR_XOR(dummy, Arrays.asList(comparisonResults),
                    decimalTiShares, asymmetricBit, 2, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
        }
        pid++;
        comparisonResults = xorModule.call();

        return comparisonResults;
    }

    public int binarySearch(int lbound, int ubound) {
        int threshold = asymmetricBit * (lbound + ubound) / 2;
        int stoppingBit = 0;
        //Integer[] comparisonResults = new Integer[trainingSharesCount];
        Integer[] comparisonResults;
        while (stoppingBit == 0) {
            //compute no. of elements lesser than threshold
            System.out.println("threshold: " + threshold + ", ubound: " + ubound + ", lbound: " + lbound);
            comparisonResults = getComparisonResults(threshold);
            int elementsLesser = 0;
            System.out.println(Arrays.toString(comparisonResults));
            for (int i : comparisonResults) {
                elementsLesser += i;
            }
            BitDecomposition bitD1 = new BitDecomposition(elementsLesser,
                    binaryTiShares, asymmetricBit, bitLength, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
            pid++;
            BitDecomposition bitD2 = new BitDecomposition(K, binaryTiShares,
                    asymmetricBit, bitLength, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
            pid++;
            Future<List<Integer>> x, y;
            x = es.submit(bitD1);
            y = es.submit(bitD2);

            List<Integer> lessThanBitShares = null, KBitShares = null;
            try {
                lessThanBitShares = x.get();
                KBitShares = y.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNNBinarySearch.class.getName()).log(Level.SEVERE, null, ex);
            }

            Comparison greaterThanModule = new Comparison(lessThanBitShares,
                    KBitShares, binaryTiShares, asymmetricBit, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
            pid++;

            Comparison lessThanModule = new Comparison(KBitShares, lessThanBitShares,
                    binaryTiShares, asymmetricBit, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
            pid++;

            Future<Integer> gtTask = es.submit(greaterThanModule);
            Future<Integer> ltTask = es.submit(lessThanModule);
            int gt = 0, lt = 0;
            try {
                gt = gtTask.get();
                lt = ltTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNNBinarySearch.class.getName()).log(Level.SEVERE, null, ex);
            }

            MultiplicationByte multTask = new MultiplicationByte(gt, lt,
                    binaryTiShares.get(binaryTiIndex), pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid,
                    asymmetricBit, 0, partyCount);
            pid++;

            int ltgt = multTask.call();
            Message senderMessage = new Message(ltgt, clientId, protocolIdQueue);
            Message receivedMessage = null;
            int ltgt_party = 0;
            try {
                commonSender.put(senderMessage);
                receivedMessage = pidMapper.get(protocolIdQueue).take();
                ltgt_party = (int) receivedMessage.getValue();
            } catch (InterruptedException ex) {
                Logger.getLogger(KNNBinarySearch.class.getName()).log(Level.SEVERE, null, ex);
            }
            stoppingBit = (ltgt + ltgt_party) % 2;
            if(stoppingBit == 1) {
                break;
            }

            //reduce threshold if no. of elements less than threshold > k (gt == 1),
            // ubound = threshold => ubound = threshold*gt + ubound*lt
            //Similarly, lbound = threshold if lt == 1 and lbound remains the same otherwise
            //lbound = threshold*lt + lbound*gt
            //convert primes of lt and gt from 2 to prime
            OR_XOR xorModule = new OR_XOR(Arrays.asList(lt, gt), Arrays.asList(0, 0),
                    decimalTiShares, asymmetricBit, 2, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
            pid++;
            Integer[] xorOutputs = xorModule.call();
            
            BatchMultiplicationInteger bmInteger = new BatchMultiplicationInteger(
                    Arrays.asList(xorOutputs[0], xorOutputs[1], xorOutputs[0], xorOutputs[1]),
                    Arrays.asList(ubound, threshold, threshold, lbound), decimalTiShares,
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, asymmetricBit, 0, partyCount);
            pid++;
            Integer[] bmOutputs = bmInteger.call();

            lbound = Math.floorMod(bmOutputs[2] + bmOutputs[3], prime);
            ubound = Math.floorMod(bmOutputs[0] + bmOutputs[1], prime);
            threshold = (lbound + ubound)/2;

        }

        return threshold;
    }

    public int computeMajorityClassLabel(int threshold) {
        // get class labels of JDs that are lesser than threshold
        int classLabelSum = 0;
        int predictedClassLabel = -1;

        List<Integer> comparisonResults = Arrays.asList(getComparisonResults(threshold));
        List<Future<Integer[]>> taskList = new ArrayList<>();
        int i=0;
        while(i<trainingSharesCount) {
            int toIndex = Math.min(trainingSharesCount, i+Constants.BATCH_SIZE);
            BatchMultiplicationInteger bmModule = new BatchMultiplicationInteger(comparisonResults.subList(i, toIndex),
                    classLabels.subList(i, toIndex), decimalTiShares, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, asymmetricBit, 0, partyCount);
            pid++;
            i = toIndex;
            Future<Integer[]> task = es.submit(bmModule);
            taskList.add(task);
        }

        for(i=0;i<taskList.size();i++) {
            Future<Integer[]> task = taskList.get(i);
            try {
                Integer[] products = task.get();
                for(int p: products) {
                    classLabelSum += p;
                }
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNNBinarySearch.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
        int oneCount = Math.floorMod(classLabelSum, prime);
        // The number of zeroCounts is just K - oneCount
        int zeroCount = Math.floorMod(asymmetricBit * K - classLabelSum, prime);

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
        List<Integer> numOfOnePredictions = null, numOfZeroPredictions = null;

        try {
            numOfOnePredictions = bitTaskOne.get();
            numOfZeroPredictions = bitTaskZero.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }

        Comparison compClassLabels = new Comparison(numOfOnePredictions, numOfZeroPredictions,
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + comparisonTICount),
                asymmetricBit, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime,
                pid, partyCount);

        predictedClassLabel = compClassLabels.call();
        pid++;

        return predictedClassLabel;
    }

    public int KNN_Model() {
        //Jaccard Computation for all the training shares
        //ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        long startTime = System.currentTimeMillis();

        int decTICount = attrLength * 2 * trainingSharesCount;
        JaccardDistance jdModule = new JaccardDistance(trainingShares, testShare,
                asymmetricBit, decimalTiShares, pidMapper, commonSender,
                clientId, prime, pid,
                new LinkedList<>(protocolIdQueue), partyCount);

        Future<List<List<Integer>>> jdTask = es.submit(jdModule);
        pid++;
        //decimalTiIndex += decTICount;
        //es.shutdown();
        try {
            jaccardDistances = jdTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }

        //add class labels to JD data structure (contains OR, XOR and Class Labels)
        for (int i = 0; i < trainingShares.size(); i++) {
            jaccardDistances.get(i).add(classLabels.get(i));
        }

        //System.out.println("jaccarddistances:" + jaccardDistances);
        int threshold = binarySearch(0, prime);

        int classLabel = computeMajorityClassLabel(threshold);

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        ThreadPoolManager.shutDownThreadService();
        System.out.println("Label:" + classLabel);
        System.out.println("Time taken:" + elapsedTime + "ms");
        return 0;
    }

}
