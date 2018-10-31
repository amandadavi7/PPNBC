/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Model.KNNSortAndSwap;
import Protocol.CompositeProtocol;
import Protocol.OR_XOR;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
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
        for(int i=0;i<K;i++) {
            indices[i] = i;
        }
        decimalTiIndex = 0;
        binaryTiIndex = 0;
        comparisonTICount = (2*bitLength) + ((bitLength*(bitLength-1))/2);
        bitDTICount = bitLength*3 - 2;
        ccTICount = comparisonTICount + 2*bitDTICount;
        pid = 0;
        
    }
    
    void Sort(int[] indices, int next) {
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
                    Constants.binaryPrime, pid, new LinkedList<>(protocolIdQueue), 
                    partyCount, bitLength);

            Future<Integer> resultTask = es.submit(ccModule);
            pid++;
            //decimalTiIndex += 2;
            //binaryTiIndex += ccTICount;
            es.shutdown();
            int comparisonresult = 0;

            try {
                comparisonresult = resultTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNNSortAndSwap.class.getName()).log(Level.SEVERE, null, ex);
            }

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
                    Constants.binaryPrime, pid, new LinkedList<>(protocolIdQueue), 
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
            try {
                comparisonResults[i] = resultTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNNSortAndSwap.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        j = 0;
        for (int i = startIndex + 1; i < endIndex - 1; i += 2, j++) {
            swapCircuitSorting(indices[i], indices[i + 1], comparisonResults[j]);
        }

    }
    
    void swapCircuitSorting(int leftIndex, int rightIndex, int comparisonOutput) {

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);

        //Do xor between comparison results....
        List<Integer> cShares = new ArrayList<>();
        cShares.add(comparisonOutput);

        List<Integer> dummy = new ArrayList<>();
        dummy.add(0);
        OR_XOR xor = null;

        if (clientID == 1) {
            xor = new OR_XOR(cShares, dummy, decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 1),
                    asymmetricBit, 2, pidMapper, senderQueue, 
                    new LinkedList<>(protocolIdQueue), clientID, prime, 
                    pid, partyCount);
        } else if (clientID == 2) {
            xor = new OR_XOR(dummy, cShares, decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 1),
                    asymmetricBit, 2, pidMapper, senderQueue, 
                    new LinkedList<>(protocolIdQueue), clientID, prime, 
                    pid, partyCount);
        }

        Future<Integer[]> xorTask = es.submit(xor);
        pid++;
        //decimalTiIndex++;

        Integer[] c = null;
        try {
            c = xorTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNNSortAndSwap.class.getName()).log(Level.SEVERE, null, ex);
        }

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
        Integer[] left1 = null, left2 = null, right1 = null, right2 = null;

        try {
            left1 = leftTask1.get();
            left2 = leftTask2.get();
            right1 = rightTask1.get();
            right2 = rightTask2.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNNSortAndSwap.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (int i = 0; i < 3; i++) {
            KJaccardDistances.get(leftIndex).set(i, Math.floorMod(left1[i] + left2[i], prime));
            KJaccardDistances.get(rightIndex).set(i, Math.floorMod(right1[i] + right2[i], prime));
        }

    }
    
    @Override
    public List<List<Integer>> call() {
        Sort(indices, 1);
        
        return KJaccardDistances;
    }
}
