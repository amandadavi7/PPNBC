/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.BitDecomposition;
import Protocol.Comparison;
import Protocol.CompositeProtocol;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
 *
 * @author keerthanaa
 */
public class KNN extends Model {

    List<List<Integer>> trainingShares;
    List<Integer> testShare;
    List<Integer> classLabels;
    List<List<Integer>> jaccardDistances, KjaccardDistances;
    int pid, attrLength, K, decimalTiIndex, binaryTiIndex, trainingSharesCount;
    int ccTICount, prime, bitLength, comparisonTICount, bitDTICount;
    List<TripleInteger> decimalTiShares;
    List<TripleByte> binaryTiShares;

    public KNN(int asymmetricBit, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue, int clientId, List<TripleByte> binaryTriples,
            List<TripleInteger> decimalTriples, int partyCount, String[] args) {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount);
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

        System.out.println("Test Print: training=" + trainingShares + " test=" +
                testShare + " classL=" + classLabels + " K=" + K);
    }

    void swapCircuitSorting(int leftIndex, int rightIndex, int comparisonOutput) {

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);

        //Do xor between comparison results....
        List<Integer> cShares = new ArrayList<>();
        cShares.add(comparisonOutput);

        List<Integer> dummy = new ArrayList<>();
        dummy.add(0);
        OR_XOR xor = null;

        if (clientId == 1) {
            xor = new OR_XOR(cShares, dummy, decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 1),
                    asymmetricBit, 2, pidMapper, commonSender, 
                    new LinkedList<>(protocolIdQueue), clientId, prime, 
                    pid, partyCount);
        } else if (clientId == 2) {
            xor = new OR_XOR(dummy, cShares, decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 1),
                    asymmetricBit, 2, pidMapper, commonSender, 
                    new LinkedList<>(protocolIdQueue), clientId, prime, 
                    pid, partyCount);
        }

        Future<Integer[]> xorTask = es.submit(xor);
        pid++;
        decimalTiIndex++;

        Integer[] c = null;
        try {
            c = xorTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }

        List<Integer> C = new ArrayList<>(Collections.nCopies(3, c[0]));
        List<Integer> notC = new ArrayList<>(Collections.nCopies(3, Math.floorMod(asymmetricBit - c[0], prime)));

        //left index position
        
        BatchMultiplicationInteger batchMult = new BatchMultiplicationInteger(C, 
                KjaccardDistances.get(rightIndex), decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3), 
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                clientId, prime, pid, asymmetricBit, 0, partyCount);
        pid++;
        decimalTiIndex += 3;

        Future<Integer[]> leftTask1 = es.submit(batchMult);

        BatchMultiplicationInteger batchMult2 = new BatchMultiplicationInteger(notC, 
                KjaccardDistances.get(leftIndex), decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3), 
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                clientId, prime, pid, asymmetricBit, 0, partyCount);
        pid++;
        decimalTiIndex += 3;

        Future<Integer[]> leftTask2 = es.submit(batchMult2);

        //right index position
        
        BatchMultiplicationInteger batchMult3 = new BatchMultiplicationInteger(C, 
                KjaccardDistances.get(leftIndex), decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3), 
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                clientId, prime, pid, asymmetricBit, 0, partyCount);
        pid++;
        decimalTiIndex += 3;

        Future<Integer[]> rightTask1 = es.submit(batchMult3);

        BatchMultiplicationInteger batchMult4 = new BatchMultiplicationInteger(notC, 
                KjaccardDistances.get(rightIndex), decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3), 
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                clientId, prime, pid, asymmetricBit, 0, partyCount);
        pid++;
        decimalTiIndex += 3;

        Future<Integer[]> rightTask2 = es.submit(batchMult4);

        es.shutdown();
        System.out.println("before swapping indices:" + leftIndex + "and" + rightIndex);
        System.out.println(KjaccardDistances.get(leftIndex) + " " + KjaccardDistances.get(rightIndex));

        //get the results
        Integer[] left1 = null, left2 = null, right1 = null, right2 = null;

        try {
            left1 = leftTask1.get();
            left2 = leftTask2.get();
            right1 = rightTask1.get();
            right2 = rightTask2.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (int i = 0; i < 3; i++) {
            KjaccardDistances.get(leftIndex).set(i, Math.floorMod(left1[i] + left2[i], prime));
            KjaccardDistances.get(rightIndex).set(i, Math.floorMod(right1[i] + right2[i], prime));
        }

        System.out.println("after swapping indices:" + leftIndex + "and" + rightIndex);
        System.out.println(KjaccardDistances.get(leftIndex) + " " + KjaccardDistances.get(rightIndex));

    }

    void Sort(int[] indices, int next) {
        //base case
        int startIndex = 0, endIndex = indices.length - 1;
        System.out.println("in sort: startIndex=" + indices[0] + ", endIndex=" + 
                indices[endIndex] + ",next:" + next);
        if (indices[startIndex] == indices[endIndex]) {
            return;
        }

        if (indices[startIndex] + next == indices[endIndex]) {
            //comparison

            ExecutorService es = Executors.newSingleThreadExecutor();

            System.out.println("calling crossmultiply and compare");

            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(KjaccardDistances.get(indices[startIndex]).get(1),
                    KjaccardDistances.get(indices[startIndex]).get(0), 
                    KjaccardDistances.get(indices[endIndex]).get(1),
                    KjaccardDistances.get(indices[endIndex]).get(0), asymmetricBit,
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 2),
                    binaryTiShares.subList(binaryTiIndex, binaryTiIndex + ccTICount), 
                    pidMapper, commonSender, clientId, prime, 
                    Constants.binaryPrime, pid, new LinkedList<>(protocolIdQueue), 
                    partyCount, bitLength);

            Future<Integer> resultTask = es.submit(ccModule);
            pid++;
            decimalTiIndex += 2;
            binaryTiIndex += ccTICount;

            int comparisonresult = 0;

            try {
                comparisonresult = resultTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }

            System.out.println("comparing indices " + indices[startIndex] + " " + indices[endIndex] + ", result=" + comparisonresult);

            //circuit to swap
            swapCircuitSorting(indices[startIndex], indices[endIndex], comparisonresult);

            return;
        }

        int mid = (endIndex - startIndex) / 2;
        int[] firstArray = Arrays.copyOfRange(indices, startIndex, mid + 1);
        int[] secondArray = Arrays.copyOfRange(indices, mid + 1, endIndex + 1);
        Sort(firstArray, next);
        Sort(secondArray, next);

        Merge(indices, next);
    }

    void Merge(int[] indices, int next) {

        int startIndex = 0;
        int endIndex = indices.length - 1;
        System.out.println("In merge: startIndex=" + indices[startIndex] + " endIndex=" + indices[endIndex]);

        //Sort even indexed
        int[] evenIndices = new int[indices.length / 2 + indices.length % 2];
        int[] oddIndices = new int[indices.length / 2];

        int j = 0;
        for (int i = startIndex; i < indices.length; i += 2) {
            evenIndices[j] = indices[i];
            oddIndices[j] = indices[i + 1];
            j++;
        }

        Sort(evenIndices, 2 * next);
        Sort(oddIndices, 2 * next);

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> taskList = new ArrayList<>();

        //Compare adjacent numbers
        for (int i = startIndex + 1; i < endIndex - 1; i += 2) {
            //compare and swap jd(i) and jd(i+1)
            System.out.println("calling comparison between adjacent elements: indices - "
                    + indices[i] + " and " + indices[i + 1]);
            
            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(KjaccardDistances.get(indices[i]).get(1),
                    KjaccardDistances.get(indices[i]).get(0), 
                    KjaccardDistances.get(indices[i + 1]).get(1),
                    KjaccardDistances.get(indices[i + 1]).get(0), asymmetricBit,
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 2),
                    binaryTiShares.subList(binaryTiIndex, binaryTiIndex + ccTICount), 
                    pidMapper, commonSender, clientId, prime, 
                    Constants.binaryPrime, pid, new LinkedList<>(protocolIdQueue), 
                    partyCount, bitLength);

            Future<Integer> resultTask = es.submit(ccModule);
            pid++;
            decimalTiIndex += 2;
            binaryTiIndex += ccTICount;
            taskList.add(resultTask);
        }

        es.shutdown();

        int n = taskList.size();
        int[] comparisonResults = new int[n];
        for (int i = 0; i < n; i++) {
            Future<Integer> resultTask = taskList.get(i);
            try {
                comparisonResults[i] = resultTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        j = 0;
        for (int i = startIndex + 1; i < endIndex - 1; i += 2, j++) {
            swapCircuitSorting(indices[i], indices[i + 1], comparisonResults[j]);
        }

    }

    Integer[] getKComparisonResults(int index) {
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
            decimalTiIndex += 2;
            binaryTiIndex += ccTICount;

            Future<Integer> ccTask = es.submit(ccModule);
            taskList.add(ccTask);
        }

        es.shutdown();

        for (int i = 0; i < K; i++) {
            Future<Integer> ccTask = taskList.get(i);
            try {
                comparisonResults[i] = ccTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return comparisonResults;
    }

    Integer[] comparisonMultiplicationResultsSequential(Integer[] comparisonResults) {
        Integer[] comparisonMultiplications = new Integer[K];
        comparisonMultiplications[0] = 0;
        comparisonMultiplications[1] = comparisonResults[0];

        ExecutorService es = Executors.newSingleThreadExecutor();

        for (int i = 1; i < K - 1; i++) {
            MultiplicationByte mult = new MultiplicationByte(comparisonMultiplications[i], 
                    comparisonResults[i], binaryTiShares.get(binaryTiIndex), 
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                    clientId, Constants.binaryPrime, pid, asymmetricBit, 0, partyCount);
            binaryTiIndex++;
            pid++;
            Future<Integer> multTask = es.submit(mult);
            try {
                comparisonMultiplications[i + 1] = multTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
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
    void swapTrainingShares(int index) {

        //get all the K comparison results
        Integer[] comparisonResults = getKComparisonResults(index);
        System.out.println("comparison results:" + Arrays.toString(comparisonResults));
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

            binaryTiIndex++;
            pid++;
            Future<Integer> multTask = es.submit(mult);
            taskList.add(multTask);
        }

        for (int i = 1; i < K; i++) {
            Future<Integer> multTask = taskList.get(i - 1);
            try {
                comparisonMultiplications[i] = multTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        System.out.println("comparison multiplications:" + Arrays.toString(comparisonMultiplications));

        //Do xor between comparison results....
        List<Integer> dummy = new ArrayList<>(Collections.nCopies(K, 0));
        OR_XOR xorComp = null, xorCompMults = null;

        // Convert comparisonResults and comparisonMultiplications from binary prime to decimal prime
        if (clientId == 1) {
            xorComp = new OR_XOR(Arrays.asList(comparisonResults), dummy, 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + K),
                    asymmetricBit, 2, pidMapper, commonSender, 
                    new LinkedList<>(protocolIdQueue), clientId, prime, 
                    pid, partyCount);
            pid++;
            decimalTiIndex += K;
            xorCompMults = new OR_XOR(Arrays.asList(comparisonMultiplications), 
                    dummy, decimalTiShares.subList(decimalTiIndex, decimalTiIndex + K),
                    asymmetricBit, 2, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, 
                    pid, partyCount);
        } else if (clientId == 2) {
            xorComp = new OR_XOR(dummy, Arrays.asList(comparisonResults), 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + K),
                    asymmetricBit, 2, pidMapper, commonSender, 
                    new LinkedList<>(protocolIdQueue), clientId, prime, 
                    pid, partyCount);
            pid++;
            decimalTiIndex += K;
            xorCompMults = new OR_XOR(dummy, Arrays.asList(comparisonMultiplications), 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + K), asymmetricBit, 
                    2, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, partyCount);
        }

        Future<Integer[]> xorTask1 = es.submit(xorComp);
        Future<Integer[]> xorTask2 = es.submit(xorCompMults);
        pid++;
        decimalTiIndex += K;

        try {
            comparisonResults = xorTask1.get();
            comparisonMultiplications = xorTask2.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("comparison results:" + Arrays.toString(comparisonResults));
        System.out.println("comparison mults:" + Arrays.toString(comparisonMultiplications));

        List<Future<Integer[]>> swapTaskList = new ArrayList<>();

        for (int i = 0; i < K; i++) {

            // send null if i = 0 , else send (i - 1)th jaccard distance packet;
            List<Integer> prev = null;

            if (i != 0) {
                prev = KjaccardDistances.get(i - 1);
            }

            SwapCircuitTrainingShares swapModule = new SwapCircuitTrainingShares(index,
                    i, comparisonResults, comparisonMultiplications, pid,
                    new LinkedList<>(protocolIdQueue), prime, asymmetricBit, 
                    pidMapper, commonSender, clientId, 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 9),
                    jaccardDistances.get(index), KjaccardDistances.get(i), prev, partyCount);

            swapTaskList.add(es.submit(swapModule));
            pid++;
            decimalTiIndex += 9;
        }

        es.shutdown();

        // update positions from (kth to 1st) position, as kth position is independent of the rest
        for (int i = K - 1; i >= 0; i--) {
            Future<Integer[]> swapTask = swapTaskList.get(i);
            try {
                Integer[] results = swapTask.get();
                for (int j = 0; j < 3; j++) {
                    KjaccardDistances.get(i).set(j, results[j]);
                }
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        System.out.println("KJD:" + KjaccardDistances);
    }

    int computeMajorityClassLabel() {
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
        binaryTiIndex += bitDTICount;
        Future<List<Integer>> bitTaskOne = es.submit(bitDModuleOne);

        BitDecomposition bitDModuleZero = new BitDecomposition(zeroCount,
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + bitDTICount), 
                asymmetricBit, bitLength, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, 
                pid, partyCount);
        pid++;
        binaryTiIndex += bitDTICount;
        Future<List<Integer>> bitTaskZero = es.submit(bitDModuleZero);

        List<Integer> numOfOnePredictions = null, numOfZeroPredictions = null;

        try {
            numOfOnePredictions = bitTaskOne.get();
            numOfZeroPredictions = bitTaskZero.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }

        int comparisonTiCount = bitLength * 2 + (bitLength * (bitLength - 1)) / 2;
        Comparison compClassLabels = new Comparison(numOfOnePredictions, numOfZeroPredictions, 
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + comparisonTiCount),
                asymmetricBit, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, 
                pid, partyCount);

        Future<Integer> resultTask = es.submit(compClassLabels);
        pid++;
        binaryTiIndex += comparisonTiCount;
        es.shutdown();
        try {
            predictedClassLabel = resultTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }

        return predictedClassLabel;
    }

    public int KNN_Model() {
        //Jaccard Computation
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        long startTime = System.currentTimeMillis();
        
        int decTICount = attrLength * 2 * trainingSharesCount;
        JaccardDistance jdModule = new JaccardDistance(trainingShares, testShare,
                asymmetricBit, decimalTiShares.subList(0, decTICount), pidMapper, commonSender,
                clientId, prime, pid, 
                new LinkedList<>(protocolIdQueue), partyCount);

        Future<List<List<Integer>>> jdTask = es.submit(jdModule);
        pid++;
        decimalTiIndex += decTICount;
        es.shutdown();
        try {
            jaccardDistances = jdTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (int i = 0; i < trainingShares.size(); i++) {
            jaccardDistances.get(i).add(classLabels.get(i));
        }

        System.out.println("jaccarddistances:" + jaccardDistances);

        int[] indices = new int[K];
        for (int i = 0; i < K; i++) {
            indices[i] = i;
            KjaccardDistances.add(new ArrayList<>(jaccardDistances.get(i)));
        }

        //Sorting the first K numbers
        Sort(indices, 1);

        System.out.println("Jaccard Distances:" + jaccardDistances);
        System.out.println("KjaccardDistances:" + KjaccardDistances);

        //Iterator circuit for rest of the training examples
        for (int i = K; i < trainingSharesCount; i++) {
            System.out.println("calling for training example:" + i);
            swapTrainingShares(i);
        }

        System.out.println("KjaccardDistances after iterating all the training examples:"
                + KjaccardDistances);

        int predictedLabel = computeMajorityClassLabel();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        System.out.println("Label:" + predictedLabel);
        System.out.println("Time taken:" + elapsedTime + "ms");
        return predictedLabel;
    }

}

/**
 *
 * @author keerthanaa
 */
class SwapCircuitTrainingShares extends CompositeProtocol implements Callable<Integer[]> {

    int trainingIndex;
    int position;
    Integer[] comparisonResults;
    Integer[] comparisonMultiplications;
    List<TripleInteger> decimalTiShares;
    int prime;
    List<Integer> jaccardDistanceTraining, jaccardDistanceSorted, jaccardDistanceSortedPrevious;

    /**
     *
     * @param trainingIndex
     * @param position
     * @param comparisonResults
     * @param comparisonMultiplications
     * @param protocolID
     * @param protocolIdQueue
     * @param prime
     * @param oneShare
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param tiShares
     * @param jaccardDistanceTraining
     * @param jaccardDistanceSorted
     */
    public SwapCircuitTrainingShares(int trainingIndex, int position,
            Integer[] comparisonResults, Integer[] comparisonMultiplications,
            int protocolID, Queue<Integer> protocolIdQueue, int prime, int oneShare,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, BlockingQueue<Message> senderQueue,
            int clientId, List<TripleInteger> tiShares, List<Integer> jaccardDistanceTraining,
            List<Integer> jaccardDistanceSorted, List<Integer> jaccardDistanceSortedprev, int partyCount) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId, oneShare, partyCount);
        this.trainingIndex = trainingIndex;
        this.position = position;
        this.comparisonResults = comparisonResults;
        this.comparisonMultiplications = comparisonMultiplications;
        this.decimalTiShares = tiShares;
        this.jaccardDistanceSorted = jaccardDistanceSorted;
        this.jaccardDistanceTraining = jaccardDistanceTraining;
        this.jaccardDistanceSortedPrevious = jaccardDistanceSortedprev;
        this.prime = prime;
    }

    @Override
    public Integer[] call() {
        int pid = 0, decimalTiIndex = 0;
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        
        //first mult
        List<Integer> piC = new ArrayList<>(Collections.nCopies(3, comparisonMultiplications[position]));
        BatchMultiplicationInteger mult1 = new BatchMultiplicationInteger(jaccardDistanceTraining,
                piC, decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3), 
                pidMapper, senderQueue,
                new LinkedList<>(protocolIdQueue), clientID, prime,
                pid, asymmetricBit, 0, partyCount);
        Future<Integer[]> multTask1 = es.submit(mult1);
        pid++;
        decimalTiIndex += 3;

        //third mult
        List<Integer> C = new ArrayList<>(Collections.nCopies(3, comparisonResults[position]));
        BatchMultiplicationInteger mult3 = new BatchMultiplicationInteger(jaccardDistanceSorted,
                C, decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3), 
                pidMapper, senderQueue,
                new LinkedList<>(protocolIdQueue), clientID, prime,
                pid, asymmetricBit, 0, partyCount);
        Future<Integer[]> multTask3 = es.submit(mult3);
        pid++;
        decimalTiIndex += 3;

        Integer[] results = new Integer[3];
        Arrays.fill(results, 0);

        if (position != 0) {
            //second mult
            List<Integer> notC = new ArrayList<>(Collections.nCopies(3, 
                    Math.floorMod(asymmetricBit - comparisonResults[position - 1], prime)));
            BatchMultiplicationInteger mult2 = new BatchMultiplicationInteger(jaccardDistanceSortedPrevious,
                    notC, decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3), 
                    pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue), clientID, prime,
                    pid, asymmetricBit, 0, partyCount);
            Future<Integer[]> multTask2 = es.submit(mult2);
            pid++;
            decimalTiIndex += 3;

            try {
                Integer[] Mults = multTask2.get();
                for (int i = 0; i < 3; i++) {
                    results[i] += Mults[i];
                }
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        try {
            Integer[] Mults = multTask1.get();
            for (int i = 0; i < 3; i++) {
                results[i] += Mults[i];
            }
            Mults = multTask3.get();
            for (int i = 0; i < 3; i++) {
                results[i] = Math.floorMod(results[i] + Mults[i], prime);
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("results retuning: " + Arrays.toString(results));
        return results;
    }
}
