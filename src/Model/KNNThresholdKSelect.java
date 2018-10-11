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
import Protocol.MultiplicationInteger;
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
public class KNNThresholdKSelect extends Model {

    List<List<Integer>> trainingShares;
    List<Integer> testShare;
    List<Integer> classLabels;
    List<Integer> KBitShares;
    List<List<Integer>> jaccardDistances;
    int pid, attrLength, K, decimalTiIndex, binaryTiIndex, trainingSharesCount;
    int ccTICount, prime, bitLength, comparisonTICount, bitDTICount;
    List<TripleInteger> decimalTiShares;
    List<TripleByte> binaryTiShares;
    ExecutorService es;

    /**
     * 
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param binaryTriples
     * @param decimalTriples
     * @param partyCount
     * @param args 
     */
    public KNNThresholdKSelect(int asymmetricBit,
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
        
        //Bit Shares of K - used in multiple places
        BitDecomposition bitD = new BitDecomposition(asymmetricBit*K, binaryTiShares,
                    asymmetricBit, bitLength, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, pid, partyCount);
        pid++;
        KBitShares = bitD.call();
    }

    /**
     * 
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
     * returns comparison results of all training examples: 
     * 1 if <= Threshold, 0 otherwise
     * converted comparison output to decimal prime
     * @param thresholds
     * @return 
     */
    public Integer[] getComparisonResults(int[] thresholds) {
        Integer[] comparisonResults = new Integer[trainingSharesCount];
        List<Future<Integer>> ccTaskList = new ArrayList<>();

        for (int i = 0; i < trainingSharesCount; i++) {
            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(thresholds[0], thresholds[1],
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
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNNThresholdKSelect.class.getName()).log(Level.SEVERE, null, ex);
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

    /**
     * compute threshold (e/f) from lbound (a/b) and ubound(c/d)
     * e/f = ((a*d) + (b*c))/(2*(b*d))
     * @param lbound_numerator
     * @param lbound_denominator
     * @param ubound_numerator
     * @param ubound_denominator
     * @return 
     */
    public int[] getThreshold(int lbound_numerator, int lbound_denominator,
            int ubound_numerator, int ubound_denominator) {
        MultiplicationInteger mult1 = new MultiplicationInteger(lbound_numerator,
                ubound_denominator, decimalTiShares.get(decimalTiIndex), pidMapper,
                commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
                pid, asymmetricBit, 0, partyCount);
        pid++;
        Future<Integer> task1 = es.submit(mult1);
        MultiplicationInteger mult2 = new MultiplicationInteger(lbound_denominator,
                ubound_numerator, decimalTiShares.get(decimalTiIndex), pidMapper,
                commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
                pid, asymmetricBit, 0, partyCount);
        pid++;
        Future<Integer> task2 = es.submit(mult2);
        MultiplicationInteger mult3 = new MultiplicationInteger(lbound_denominator,
                ubound_denominator, decimalTiShares.get(decimalTiIndex), pidMapper,
                commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
                pid, asymmetricBit, 0, partyCount);
        pid++;
        Future<Integer> task3 = es.submit(mult3);
        
        int[] thresholds = new int[2];
        try {
            thresholds[0] = Math.floorMod(task1.get() + task2.get(), prime);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNNThresholdKSelect.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        int p = 0;
        try {
            p = task3.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNNThresholdKSelect.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        thresholds[1] = Math.floorMod(2*p, prime);
        
        return thresholds;
    }
    
    /**
     * 
     * @param lbound
     * @param ubound
     * @return 
     */
    public int[] binarySearch(int lbound, int ubound) {
        int lbound_numerator = lbound;
        int lbound_denominator = asymmetricBit;
        int ubound_numerator = ubound;
        int ubound_denominator = asymmetricBit;
        int[] thresholds = null;
        int stoppingBit = 0;
        Integer[] comparisonResults;
        int maxIterations = (int) Math.ceil(Math.log(trainingSharesCount)/Math.log(2.0));
        
        while (stoppingBit == 0 && maxIterations >= 0) {
            System.out.println("iteration countdown:" + maxIterations);
            thresholds = getThreshold(lbound_numerator,
                lbound_denominator, ubound_numerator, ubound_denominator);
            /*System.out.println("threshold: " + thresholds[0] +"/"+thresholds[1]
                    + ", ubound: " + ubound_numerator + "/" + ubound_denominator + ", lbound: "
                    + lbound_numerator + "/" + lbound_denominator);*/
            
            //compute no. of elements lesser than threshold
            comparisonResults = getComparisonResults(thresholds);
            int elementsLesser = 0;
            for (int i : comparisonResults) {
                elementsLesser += i;
            }
            elementsLesser %= prime;
            //System.out.println("elements lesser:" + elementsLesser);
            
            BitDecomposition bitD = new BitDecomposition(elementsLesser,
                    binaryTiShares, asymmetricBit, bitLength, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, pid, partyCount);
            pid++;
            List<Integer> lessThanBitShares = bitD.call();

            Comparison greaterThanModule = new Comparison(lessThanBitShares,
                    KBitShares, binaryTiShares, asymmetricBit, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, pid, partyCount);
            Future<Integer> gtTask = es.submit(greaterThanModule);
            pid++;
            
            Comparison lessThanModule = new Comparison(KBitShares, lessThanBitShares,
                    binaryTiShares, asymmetricBit, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, pid, partyCount);
            Future<Integer> ltTask = es.submit(lessThanModule);
            pid++;
            
            int gt = 0, lt = 0;
            try {
                gt = gtTask.get();
                lt = ltTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNNThresholdKSelect.class.getName()).log(Level.SEVERE, null, ex);
            }

            //System.out.println("lt: " + lt + " gt: " + gt);
            
            MultiplicationByte multTask = new MultiplicationByte(gt, lt,
                    binaryTiShares.get(binaryTiIndex), pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, pid,
                    asymmetricBit, 0, partyCount);
            pid++;

            int ltgt = multTask.call();
            
            //Share the lt*gt shares with each other
            Message senderMessage = new Message(ltgt, clientId, protocolIdQueue);
            Message receivedMessage = null;
            int ltgt_party = 0;
            try {
                commonSender.put(senderMessage);
                receivedMessage = pidMapper.get(protocolIdQueue).take();
                ltgt_party = (int) receivedMessage.getValue();
            } catch (InterruptedException ex) {
                Logger.getLogger(KNNThresholdKSelect.class.getName()).log(Level.SEVERE, null, ex);
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
            OR_XOR xorModule;
            if(asymmetricBit == 1) {
                xorModule = new OR_XOR(Arrays.asList(lt, gt), Arrays.asList(0, 0),
                        decimalTiShares, asymmetricBit, 2, pidMapper, commonSender,
                        new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
            } else {
                xorModule = new OR_XOR(Arrays.asList(0, 0), Arrays.asList(lt, gt),
                        decimalTiShares, asymmetricBit, 2, pidMapper, commonSender,
                        new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
            }
            
            pid++;
            Integer[] xorOutputs = xorModule.call();
            
            BatchMultiplicationInteger bmInteger = new BatchMultiplicationInteger(
                    Arrays.asList(xorOutputs[0], xorOutputs[1], xorOutputs[0], 
                        xorOutputs[1], xorOutputs[0], xorOutputs[1], xorOutputs[0],
                        xorOutputs[1]),
                    Arrays.asList(ubound_numerator, thresholds[0], thresholds[0],
                        lbound_numerator, ubound_denominator, thresholds[1],
                        thresholds[1], lbound_denominator),
                    decimalTiShares, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, asymmetricBit, 0, partyCount);
            pid++;
            Integer[] bmOutputs = bmInteger.call();

            lbound_numerator = Math.floorMod(bmOutputs[2] + bmOutputs[3], prime);
            ubound_numerator = Math.floorMod(bmOutputs[0] + bmOutputs[1], prime);
            lbound_denominator = Math.floorMod(bmOutputs[6] + bmOutputs[7], prime);
            ubound_denominator = Math.floorMod(bmOutputs[4] + bmOutputs[5], prime);
            maxIterations--;
        }

        return thresholds;
    }

    
    /**
     * 
     * @param thresholds
     * @return 
     */
    public int computeMajorityClassLabel(int[] thresholds) {
        // get class labels of JDs that are lesser than threshold
        int classLabelSum = 0;
        int predictedClassLabel = -1;
        System.out.println("computing class label");
        List<Integer> comparisonResults = Arrays.asList(getComparisonResults(thresholds));
        List<Future<Integer[]>> taskList = new ArrayList<>();
        int endIndex = K;
        int comparisonSum = 0;
        for(int i=0;i<endIndex;i++){
            comparisonSum += comparisonResults.get(i);
        }
        comparisonSum = Math.floorMod(comparisonSum, prime);
        boolean stoppingCriteria = false;
        while(!stoppingCriteria) {
            BitDecomposition bitDmodule = new BitDecomposition(comparisonSum, 
                    binaryTiShares, asymmetricBit, bitLength, pidMapper, 
                    commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, Constants.binaryPrime, pid, partyCount);
            pid++;
            List<Integer> comparisonSumShares = bitDmodule.call();
            Comparison compModule = new Comparison(comparisonSumShares, KBitShares, binaryTiShares,
                    asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, partyCount);
            pid++;
            int compResult = compModule.call();
            
            Message senderMessage = new Message(compResult, clientId, protocolIdQueue);
            Message receivedMessage = null;
            int compResult_party = 0;
            try {
                commonSender.put(senderMessage);
                receivedMessage = pidMapper.get(protocolIdQueue).take();
                compResult_party = (int) receivedMessage.getValue();
            } catch (InterruptedException ex) {
                Logger.getLogger(KNNThresholdKSelect.class.getName()).log(Level.SEVERE, null, ex);
            }
            if((compResult+compResult_party)%2 == 1){
                stoppingCriteria = true;
                break;
            } else {
                comparisonSum += comparisonResults.get(endIndex);
                comparisonSum = Math.floorMod(comparisonSum, prime);
                endIndex++;
            }
        }
        
        int i=0;
        while(i<endIndex) {
            int toIndex = Math.min(endIndex, i+Constants.BATCH_SIZE);
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
                Logger.getLogger(KNNThresholdKSelect.class.getName()).log(Level.SEVERE, null, ex);
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

    /**
     * 
     * @return 
     */
    public int KNN_Model() {
        //Jaccard Computation for all the training shares
        long startTime = System.currentTimeMillis();

        int decTICount = attrLength * 2 * trainingSharesCount;
        JaccardDistance jdModule = new JaccardDistance(trainingShares, testShare,
                asymmetricBit, decimalTiShares, pidMapper, commonSender,
                clientId, prime, pid,
                new LinkedList<>(protocolIdQueue), partyCount);

        Future<List<List<Integer>>> jdTask = es.submit(jdModule);
        pid++;
        //decimalTiIndex += decTICount;
        try {
            jaccardDistances = jdTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }

        int[] thresholds = binarySearch(0*asymmetricBit, asymmetricBit);

        int classLabel = computeMajorityClassLabel(thresholds);

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        
        ThreadPoolManager.shutDownThreadService();
        
        System.out.println("Label:" + classLabel);
        System.out.println("Time taken:" + elapsedTime + "ms");
        
        return 0;
    }

}
