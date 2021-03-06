/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.CompositeProtocol;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
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

/**
 * Do Batcher Sort on K Jaccard Distances
 *
 * @author keerthanaa
 */
public class BatcherSortKNN extends CompositeProtocol implements Callable<List<List<Integer>>> {

    List<List<Integer>> KJaccardDistances;
    int[] indices;
    int K, decimalTiIndex, binaryTiIndex, comparisonTICount, bitDTICount,
            ccTICount, bitLength, prime, pid;
    List<TripleByte> binaryTiShares;
    List<TripleInteger> decimalTiShares;

    /**
     *
     * @param KJaccardDistances
     * @param asymmetricBit
     * @param decimalTiShares
     * @param binaryTiShares
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param protocolIdQueue
     * @param partyCount
     * @param K
     * @param bitLength
     */
    public BatcherSortKNN(List<List<Integer>> KJaccardDistances, int asymmetricBit,
            List<TripleInteger> decimalTiShares, List<TripleByte> binaryTiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, int clientId, int prime,
            int protocolID, Queue<Integer> protocolIdQueue, int partyCount, int K,
            int bitLength) {
        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId, asymmetricBit, partyCount);

        this.KJaccardDistances = KJaccardDistances;
        this.K = K;
        this.decimalTiShares = decimalTiShares;
        this.binaryTiShares = binaryTiShares;
        this.bitLength = bitLength;
        this.prime = prime;
        indices = new int[K];
        for (int i = 0; i < K; i++) {
            indices[i] = i;
        }
        decimalTiIndex = 0;
        binaryTiIndex = 0;
        comparisonTICount = (2 * bitLength) + ((bitLength * (bitLength - 1)) / 2);
        bitDTICount = bitLength * 3 - 2;
        ccTICount = comparisonTICount + 2 * bitDTICount;
        pid = 0;

    }

    /**
     * Recursive Sort function
     *
     * @param indices
     * @param next
     */
    void Sort(int[] indices, int next) throws InterruptedException, ExecutionException {
        //base case
        int startIndex = 0, endIndex = indices.length - 1;
        if (indices[startIndex] == indices[endIndex]) {
            return;
        }

        if (indices[startIndex] + next == indices[endIndex]) {
            //comparison

            ExecutorService es = Executors.newSingleThreadExecutor();

            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(KJaccardDistances.get(indices[startIndex]).get(1),
                    KJaccardDistances.get(indices[startIndex]).get(0),
                    KJaccardDistances.get(indices[endIndex]).get(1),
                    KJaccardDistances.get(indices[endIndex]).get(0), asymmetricBit,
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 2),
                    binaryTiShares.subList(binaryTiIndex, binaryTiIndex + ccTICount), 
                    pidMapper, senderQueue, clientID, prime, 
                    Constants.BINARY_PRIME, pid, new LinkedList<>(protocolIdQueue), 
                    partyCount, bitLength);

            Future<Integer> resultTask = es.submit(ccModule);
            pid++;
            //decimalTiIndex += 2;
            //binaryTiIndex += ccTICount;
            es.shutdown();
            int comparisonresult = resultTask.get();

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

    /**
     * Merge
     *
     * @param indices
     * @param next
     */
    void Merge(int[] indices, int next) throws InterruptedException, ExecutionException {

        int startIndex = 0;
        int endIndex = indices.length - 1;

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

            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(KJaccardDistances.get(indices[i]).get(1),
                    KJaccardDistances.get(indices[i]).get(0),
                    KJaccardDistances.get(indices[i + 1]).get(1),
                    KJaccardDistances.get(indices[i + 1]).get(0), asymmetricBit,
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 2),
                    binaryTiShares.subList(binaryTiIndex, binaryTiIndex + ccTICount), 
                    pidMapper, senderQueue, clientID, prime, 
                    Constants.BINARY_PRIME, pid, new LinkedList<>(protocolIdQueue), 
                    partyCount, bitLength);

            Future<Integer> resultTask = es.submit(ccModule);
            pid++;
            //decimalTiIndex += 2;
            //binaryTiIndex += ccTICount;
            taskList.add(resultTask);
        }

        es.shutdown();

        int n = taskList.size();
        int[] comparisonResults = new int[n];
        for (int i = 0; i < n; i++) {
            Future<Integer> resultTask = taskList.get(i);
            comparisonResults[i] = resultTask.get();
        }

        j = 0;
        for (int i = startIndex + 1; i < endIndex - 1; i += 2, j++) {
            swapCircuitSorting(indices[i], indices[i + 1], comparisonResults[j]);
        }

    }

    /**
     * Swap circuit to interchange unsorted pairs
     *
     * @param leftIndex
     * @param rightIndex
     * @param comparisonOutput
     */
    void swapCircuitSorting(int leftIndex, int rightIndex, int comparisonOutput) throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);

        //Do xor between comparison results....
        Integer[] c = CompareAndConvertField.changeBinaryToDecimalField(Arrays.asList(comparisonOutput),
                decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 1), pid,
                pidMapper, senderQueue, protocolIdQueue, asymmetricBit, clientID,
                prime, partyCount);

        pid++;
        //decimalTiIndex++;

        List<Integer> C = new ArrayList<>(Collections.nCopies(3, c[0]));
        List<Integer> notC = new ArrayList<>(Collections.nCopies(3, Math.floorMod(asymmetricBit - c[0], prime)));

        //left index position
        BatchMultiplicationInteger batchMult = new BatchMultiplicationInteger(C,
                KJaccardDistances.get(rightIndex), decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3),
                pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                clientID, prime, pid, asymmetricBit, 0, partyCount);
        pid++;
        //decimalTiIndex += 3;

        Future<Integer[]> leftTask1 = es.submit(batchMult);

        BatchMultiplicationInteger batchMult2 = new BatchMultiplicationInteger(notC,
                KJaccardDistances.get(leftIndex), decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3),
                pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                clientID, prime, pid, asymmetricBit, 0, partyCount);
        pid++;
        //decimalTiIndex += 3;

        Future<Integer[]> leftTask2 = es.submit(batchMult2);

        //right index position
        BatchMultiplicationInteger batchMult3 = new BatchMultiplicationInteger(C,
                KJaccardDistances.get(leftIndex), decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3),
                pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                clientID, prime, pid, asymmetricBit, 0, partyCount);
        pid++;
        //decimalTiIndex += 3;

        Future<Integer[]> rightTask1 = es.submit(batchMult3);

        BatchMultiplicationInteger batchMult4 = new BatchMultiplicationInteger(notC,
                KJaccardDistances.get(rightIndex), decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3),
                pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                clientID, prime, pid, asymmetricBit, 0, partyCount);
        pid++;
        //decimalTiIndex += 3;

        Future<Integer[]> rightTask2 = es.submit(batchMult4);

        es.shutdown();

        //get the results
        Integer[] left1 = leftTask1.get();
        Integer[] left2 = leftTask2.get();
        Integer[] right1 = rightTask1.get();
        Integer[] right2 = rightTask2.get();

        for (int i = 0; i < 3; i++) {
            KJaccardDistances.get(leftIndex).set(i, Math.floorMod(left1[i] + left2[i], prime));
            KJaccardDistances.get(rightIndex).set(i, Math.floorMod(right1[i] + right2[i], prime));
        }

    }

    /**
     *
     * @return @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Override
    public List<List<Integer>> call() throws InterruptedException, ExecutionException {
        Sort(indices, 1);

        return KJaccardDistances;
    }
}
