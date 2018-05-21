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
import Protocol.MultiplicationInteger;
import Protocol.OR_XOR;
import Protocol.Utility.BatchMultiplicationInteger;
import Protocol.Utility.CrossMultiplyCompare;
import Protocol.Utility.JaccardDistance;
import TrustedInitializer.Triple;
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
    
    
    public KNN(int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, List<TripleByte> binaryTriples, 
            List<TripleInteger> decimalTriples, List<List<Integer>> trainingShares, List<Integer> testShare, 
            List<Integer> classLabels, int K, int partyCount) {
        
        super(senderQueue, receiverQueue, clientId, oneShare, binaryTriples, decimalTriples, null, partyCount);
        this.trainingShares = trainingShares;
        this.testShare = testShare;
        this.classLabels = classLabels;
        pid = 0;
        this.attrLength = testShare.size();
        this.K = K; //K is not shares
        this.decimalTiIndex = 0;
        this.binaryTiIndex = 0;
        this.trainingSharesCount = trainingShares.size();
        KjaccardDistances = new ArrayList<>();
        
    }
    
    void swapCircuitSorting(int leftIndex, int rightIndex, int comparisonOutput) {
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> leftTaskList = new ArrayList<>();
        List<Future<Integer>> rightTaskList = new ArrayList<>();
        
        
        //Do xor between comparison results....
        initQueueMap(recQueues, pid);
        List<Integer> cShares = new ArrayList<>();
        cShares.add(comparisonOutput);
        
        List<Integer> dummy = new ArrayList<>();
        dummy.add(0);
        OR_XOR xor = null;
        
        
        if(clientId==1) {
            xor = new OR_XOR(cShares, dummy, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+1), 
                oneShare, 2, commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                clientId, Constants.prime, pid, partyCount);
        } else if(clientId==2) {
            xor = new OR_XOR(dummy, cShares, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+1), 
                oneShare, 2, commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                clientId, Constants.prime, pid, partyCount);
        }
        
        Future<Integer[]> xorTask = es.submit(xor);
        pid++; decimalTiIndex++;
        
        Integer[] c = null;
        try {
            c = xorTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //left index position
        for(int i=0;i<3;i++) {
            initQueueMap(recQueues, pid);

            //System.out.println("multiplying.."+c[0]+" and "+ KjaccardDistances.get(rightIndex).get(i));
            MultiplicationInteger multModule = new MultiplicationInteger(c[0], KjaccardDistances.get(rightIndex).get(i), 
                decimalTiShares.get(decimalTiIndex), commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                clientId, Constants.prime, pid, oneShare, 0, partyCount);
            
            pid++;
            decimalTiIndex++;
            Future<Integer> task = es.submit(multModule);
            leftTaskList.add(task);
            
            initQueueMap(recQueues, pid);

            //System.out.println("multiplying.."+Math.floorMod(oneShare - c[0], Constants.prime)+" and "+
            //      KjaccardDistances.get(leftIndex).get(i));
            MultiplicationInteger multModule2 = new MultiplicationInteger(
                    Math.floorMod(oneShare - c[0], Constants.prime), 
                    KjaccardDistances.get(leftIndex).get(i), decimalTiShares.get(decimalTiIndex), 
                    commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), clientId, 
                    Constants.prime, pid, oneShare, 0, partyCount);
            
            pid++;
            decimalTiIndex++;
            Future<Integer> task2 = es.submit(multModule2);
            leftTaskList.add(task2);
        }
        
        //right index position
        for(int i=0;i<3;i++) {
            initQueueMap(recQueues, pid);
            MultiplicationInteger multModule = new MultiplicationInteger(c[0], KjaccardDistances.get(leftIndex).get(i), 
                decimalTiShares.get(decimalTiIndex), commonSender, recQueues.get(pid), 
                new LinkedList<>(protocolIdQueue), 
                clientId, Constants.prime, pid, oneShare, 0, partyCount);
            
            pid++;
            decimalTiIndex++;
            Future<Integer> task = es.submit(multModule);
            rightTaskList.add(task);
            
            initQueueMap(recQueues, pid);
            MultiplicationInteger multModule2 = new MultiplicationInteger(
                    Math.floorMod(oneShare - c[0], Constants.prime), 
                    KjaccardDistances.get(rightIndex).get(i), decimalTiShares.get(decimalTiIndex), 
                    commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                    clientId, Constants.prime, pid, oneShare, 0, partyCount);
            
            pid++;
            decimalTiIndex++;
            Future<Integer> task2 = es.submit(multModule2);
            rightTaskList.add(task2);
        }
        es.shutdown();
        System.out.println("before swapping indices:"+leftIndex+"and"+rightIndex);
        System.out.println(KjaccardDistances.get(leftIndex)+" "+ KjaccardDistances.get(rightIndex));
        
        for(int i=0;i<6;i+=2) {
            try {
                Future<Integer> leftTask1 = leftTaskList.get(i);
                Future<Integer> leftTask2 = leftTaskList.get(i+1);
                int test1, test2;
                test1 = leftTask1.get(); test2 = leftTask2.get();
                //System.out.println("c.d:"+test1+",(1-c).d:"+test2);
                int newLeftVal = Math.floorMod(test1+test2, Constants.prime);
                KjaccardDistances.get(leftIndex).set(i/2, newLeftVal);
                
                Future<Integer> rightTask1 = rightTaskList.get(i);
                Future<Integer> rightTask2 = rightTaskList.get(i+1);
                test1 = rightTask1.get(); test2 = rightTask2.get();
                //System.out.println("c.d:"+test1+",(1-c).d:"+test2);
                int newRightVal = Math.floorMod(test1+test2, Constants.prime);
                KjaccardDistances.get(rightIndex).set(i/2, newRightVal);
                
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        System.out.println("after swapping indices:"+leftIndex+"and"+rightIndex);
        System.out.println(KjaccardDistances.get(leftIndex)+" "+ KjaccardDistances.get(rightIndex));
        
    }
    
    void Sort(int[] indices, int next) {
        //base case
        int startIndex = 0, endIndex = indices.length - 1;
        System.out.println("in sort: startIndex="+indices[0]+", endIndex="+indices[endIndex] + ",next:" + next);
        if(indices[startIndex] == indices[endIndex]) {
            return;
        }
        
        if(indices[startIndex] + next == indices[endIndex]) {
            //comparison
            
            ExecutorService es = Executors.newSingleThreadExecutor();
            
            initQueueMap(recQueues, pid);
            System.out.println("calling crossmultiply and compare");
            //TODO binary shares sublist handling here
            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(KjaccardDistances.get(indices[startIndex]).get(1), 
                    KjaccardDistances.get(indices[startIndex]).get(0), KjaccardDistances.get(indices[endIndex]).get(1), 
                    KjaccardDistances.get(indices[endIndex]).get(0), oneShare, 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex+2), 
                    binaryTiShares, commonSender, recQueues.get(pid), 
                    clientId, Constants.prime, Constants.binaryPrime, pid, new LinkedList<>(protocolIdQueue),partyCount);
            
            Future<Integer> resultTask = es.submit(ccModule);
            pid++;
            decimalTiIndex+=2;
            
            int comparisonresult = 0;
            
            try {
                comparisonresult = resultTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            System.out.println("comparing indices " + indices[startIndex] + " " + indices[endIndex] + ", result=" +comparisonresult);
            
            //circuit to swap
            swapCircuitSorting(indices[startIndex], indices[endIndex], comparisonresult);
            
            return;
        }
        
        int mid = (endIndex - startIndex)/2;
        int[] firstArray = Arrays.copyOfRange(indices, startIndex, mid+1);
        int[] secondArray = Arrays.copyOfRange(indices, mid+1, endIndex+1);
        Sort(firstArray, next);
        Sort(secondArray, next);
        
        Merge(indices, next);
    }
    
    void Merge(int[] indices, int next) {
        
        int startIndex = 0;
        int endIndex = indices.length - 1;
        System.out.println("In merge: startIndex=" + indices[startIndex] + " endIndex=" + indices[endIndex]);
        
        //Sort even indexed
        int[] evenIndices = new int[indices.length/2 + indices.length%2];
        int[] oddIndices = new int[indices.length/2];
        
        int j=0;
        for(int i=startIndex;i<indices.length;i+=2) {
            evenIndices[j] = indices[i];
            oddIndices[j] = indices[i+1];
            j++;
        }
        
        Sort(evenIndices, 2*next);
        Sort(oddIndices, 2*next);
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        
        //Compare adjacent numbers TODO - handle binaryTiShares sublist
        for(int i=startIndex+1;i<endIndex-1;i+=2){
            //compare and swap jd(i) and jd(i+1)
            System.out.println("calling comparison between adjacent elements: indices - "
                                +indices[i]+" and "+ indices[i+1]);
            initQueueMap(recQueues, pid);
            
            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(KjaccardDistances.get(indices[i]).get(1), 
                    KjaccardDistances.get(indices[i]).get(0), KjaccardDistances.get(indices[i+1]).get(1), 
                    KjaccardDistances.get(indices[i+1]).get(0), oneShare, 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex+2), 
                    binaryTiShares, commonSender, recQueues.get(pid), 
                    clientId, Constants.prime, Constants.binaryPrime, pid, new LinkedList<>(protocolIdQueue),partyCount);
            
            Future<Integer> resultTask = es.submit(ccModule);
            pid++;
            decimalTiIndex+=2;
            taskList.add(resultTask);
        }
        
        es.shutdown();
        
        int n = taskList.size();
        int[] comparisonResults = new int[n];
        for(int i=0;i<n;i++) {
            Future<Integer> resultTask = taskList.get(i);
            try {
                comparisonResults[i] = resultTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        j=0;
        for(int i=startIndex+1;i<endIndex-1;i+=2,j++){
            swapCircuitSorting(indices[i], indices[i+1], comparisonResults[j]);
        }
        
    }
    
    Integer[] getKComparisonResults(int index) {
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        //Do all the k comparisons with the training share
        Integer[] comparisonResults = new Integer[K];
        for(int i=0;i<K;i++) {
            initQueueMap(recQueues, pid);
            
            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(jaccardDistances.get(index).get(1),
                    jaccardDistances.get(index).get(0), KjaccardDistances.get(i).get(1), KjaccardDistances.get(i).get(0),
                    oneShare, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+2), binaryTiShares, commonSender, recQueues.get(pid), clientId, Constants.prime,
                    Constants.binaryPrime, pid, new LinkedList<>(protocolIdQueue), partyCount);
            
            pid++;
            decimalTiIndex += 2;
            
            Future<Integer> ccTask = es.submit(ccModule);
            taskList.add(ccTask);
        }
        
        es.shutdown();
        
        for(int i=0;i<K;i++) {
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
        
        for(int i=1;i<K-1;i++) {
            initQueueMap(recQueues, pid);
            
            MultiplicationByte mult = new MultiplicationByte(comparisonMultiplications[i], comparisonResults[i],
                    binaryTiShares.get(binaryTiIndex), commonSender, recQueues.get(pid),
                    new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, pid, oneShare, 0, partyCount);
            binaryTiIndex++;
            pid++;
            Future<Integer> multTask = es.submit(mult);
            try {
                comparisonMultiplications[i+1] = multTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return comparisonMultiplications;
    }
    
//    int[] swapCircuitTrainingShares(int trainingIndex, int position, 
//            Integer[] comparisonResults, Integer[] comparisonMultiplications) {
//        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
//        //first mult
//        initQueueMap(recQueues, pid);
//        List<Integer> piC = new ArrayList<>(Collections.nCopies(3, comparisonMultiplications[position]));
//        BatchMultiplicationNumber mult1 = new BatchMultiplicationNumber(jaccardDistances.get(trainingIndex), 
//                piC, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+3), commonSender, 
//                recQueues.get(pid), new LinkedList<>(protocolIdQueue), clientId, Constants.prime, pid, oneShare, 0);
//        Future<Integer[]> multTask1 = es.submit(mult1);
//        pid++;
//        decimalTiIndex+=3;
//        
//        //third mult
//        initQueueMap(recQueues, pid);
//        List<Integer> C = new ArrayList<>(Collections.nCopies(3, comparisonResults[position]));
//        BatchMultiplicationNumber mult3 = new BatchMultiplicationNumber(KjaccardDistances.get(position), 
//                C, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+3), commonSender, 
//                recQueues.get(pid), new LinkedList<>(protocolIdQueue), clientId, Constants.prime, pid, oneShare, 0);
//        Future<Integer[]> multTask3 = es.submit(mult3);
//        pid++;
//        decimalTiIndex+=3;
//        
//        int[] results = new int[3];
//        
//        if(position != 0) {
//            //second mult
//            initQueueMap(recQueues, pid);
//            List<Integer> notC = new ArrayList<>(Collections.nCopies(3, Math.floorMod(oneShare-comparisonResults[position-1], Constants.prime)));
//            BatchMultiplicationNumber mult2 = new BatchMultiplicationNumber(KjaccardDistances.get(position-1), 
//                    notC, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+3), commonSender, 
//                    recQueues.get(pid), new LinkedList<>(protocolIdQueue), clientId, Constants.prime, pid, oneShare, 0);
//            Future<Integer[]> multTask2 = es.submit(mult2);
//            pid++;
//            decimalTiIndex+=3;
//            
//            try {
//                Integer[] Mults = multTask2.get();
//                results[0] += Mults[0];
//                results[1] += Mults[1];
//                results[2] += Mults[2];
//            } catch (InterruptedException | ExecutionException ex) {
//                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        
//        try {
//            Integer[] Mults = multTask1.get();
//            results[0] += Mults[0];
//            results[1] += Mults[1];
//            results[2] += Mults[2];
//            Mults = multTask3.get();
//            results[0] = Math.floorMod(results[0] + Mults[0], Constants.prime);
//            results[1] = Math.floorMod(results[1] + Mults[1], Constants.prime);
//            results[2] = Math.floorMod(results[2] + Mults[2], Constants.prime);
//        } catch (InterruptedException | ExecutionException ex) {
//            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        return results;
//    }
    
    
    /**
     * Take the index and generate all comparison results,
     * sequential comparison multiplication results,
     * assymetric XOR to change the prime from binaryPrime to decimalPrime
     * and call the swap circuit
     * 
     * @param index 
     */
    void swapTrainingShares(int index) {
        
        //get all the K comparison results
        Integer[] comparisonResults = getKComparisonResults(index);
        System.out.println("comparison results:" + Arrays.toString(comparisonResults));
        //get all PI(Cj) from j = 0 to j = K-2 index
        Integer[] comparisonMultiplications = comparisonMultiplicationResultsSequential(comparisonResults);
        
        comparisonMultiplications[0] = Math.floorMod(oneShare-comparisonResults[0], Constants.binaryPrime);
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        for(int i=1;i<K;i++) {
            initQueueMap(recQueues, pid);
            
            MultiplicationByte mult = new MultiplicationByte(comparisonMultiplications[i], 
                    Math.floorMod(oneShare-comparisonResults[i], Constants.binaryPrime), 
                    binaryTiShares.get(binaryTiIndex), commonSender, recQueues.get(pid), 
                    new LinkedList<>(protocolIdQueue), clientId, 
                    Constants.binaryPrime, pid, oneShare, 0, partyCount);
            
            binaryTiIndex++;
            pid++;
            Future<Integer> multTask = es.submit(mult);
            taskList.add(multTask);
        }
        
        for(int i=1;i<K;i++) {
            Future<Integer> multTask = taskList.get(i-1);
            try {
                comparisonMultiplications[i] = multTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        System.out.println("comparison multiplications:" + Arrays.toString(comparisonMultiplications));
        
        //Do xor between comparison results....
        initQueueMap(recQueues, pid);
        initQueueMap(recQueues, pid+1);
        
        List<Integer> dummy = new ArrayList<>(Collections.nCopies(K, 0));
        OR_XOR xorComp = null, xorCompMults = null;
        
        // Convert comparisonResults and comparisonMultiplications from binary prime to decimal prime
        if(clientId==1) {
            xorComp = new OR_XOR(Arrays.asList(comparisonResults), dummy, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+K), 
                oneShare, 2, commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                clientId, Constants.prime, pid, partyCount);
            pid++;
            decimalTiIndex += K;
            xorCompMults = new OR_XOR(Arrays.asList(comparisonMultiplications), dummy, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+K), 
                oneShare, 2, commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                clientId, Constants.prime, pid, partyCount);
        } else if(clientId==2) {
            xorComp = new OR_XOR(dummy, Arrays.asList(comparisonResults), decimalTiShares.subList(decimalTiIndex, decimalTiIndex+K), 
                oneShare, 2, commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                clientId, Constants.prime, pid, partyCount);
            pid++;
            decimalTiIndex += K;
            xorCompMults = new OR_XOR(dummy, Arrays.asList(comparisonMultiplications), decimalTiShares.subList(decimalTiIndex, decimalTiIndex+K), 
                oneShare, 2, commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                clientId, Constants.prime, pid, partyCount);
        }
        
        Future<Integer[]> xorTask1 = es.submit(xorComp);
        Future<Integer[]> xorTask2 = es.submit(xorCompMults);
        pid++;
        decimalTiIndex+=K;
        
        try {
            comparisonResults = xorTask1.get();
            comparisonMultiplications = xorTask2.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("comparison results:"+Arrays.toString(comparisonResults));
        System.out.println("comparison mults:"+Arrays.toString(comparisonMultiplications));
        
        List<Future<Integer[]>> swapTaskList = new ArrayList<>();
        
        for(int i=0;i<K;i++) {
            
            initQueueMap(recQueues, pid);
            // send null if i = 0 , else send (i - 1)th jaccard distance packet;
            List<Integer> prev = null;
            
            if(i!=0) {
                prev = KjaccardDistances.get(i-1);
            }
            
            SwapCircuitTrainingShares swapModule = new SwapCircuitTrainingShares(index, 
                    i, comparisonResults, comparisonMultiplications, pid, 
                    new LinkedList<>(protocolIdQueue), Constants.prime, oneShare, commonSender, 
                    recQueues.get(pid), clientId, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+9), 
                    jaccardDistances.get(index), KjaccardDistances.get(i), prev, partyCount);
            
            swapTaskList.add(es.submit(swapModule));
            pid++;
            decimalTiIndex += 9;
        }
        
        es.shutdown();
        
        // update positions from (kth to 1st) position, as kth position is independent of the rest
        for(int i=K-1;i>=0;i--) {
            Future<Integer[]> swapTask = swapTaskList.get(i);
            try {
                Integer[] results = swapTask.get();
                for(int j=0;j<3;j++) {
                    KjaccardDistances.get(i).set(j, results[j]);
                }
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        System.out.println("KJD:"+KjaccardDistances);
    }
    
    
    int computeMajorityClassLabel() {
        int classLabelSum = 0;
        int predictedClassLabel = -1;
        
        //Get the sum of K - class labels
        for(int i=0;i<K;i++) {
            classLabelSum += KjaccardDistances.get(i).get(2);
        }
        
        int oneCount = Math.floorMod(classLabelSum, Constants.prime);
        // The number of zeroCounts is just K - oneCount
        int zeroCount = Math.floorMod(oneShare*K - classLabelSum, Constants.prime);
        
        ExecutorService es = Executors.newFixedThreadPool(2);
        
        //Do a comparison between oneCount and zeroCount
        initQueueMap(recQueues, pid);

        BitDecomposition bitDModuleOne = new BitDecomposition(oneCount,
                                binaryTiShares, oneShare, Constants.bitLength, 
                                commonSender, recQueues.get(pid), 
                                new LinkedList<>(protocolIdQueue),
                                clientId, Constants.binaryPrime, pid, partyCount);
        pid++;
        Future<List<Integer>> bitTaskOne = es.submit(bitDModuleOne);
        
        initQueueMap(recQueues, pid);

        BitDecomposition bitDModuleZero = new BitDecomposition(zeroCount, 
                            binaryTiShares, oneShare, Constants.bitLength, 
                            commonSender, recQueues.get(pid), 
                            new LinkedList<>(protocolIdQueue),
                            clientId, Constants.binaryPrime, pid, partyCount);
        pid++;
        Future<List<Integer>> bitTaskZero = es.submit(bitDModuleZero);
        
        
        List<Integer> numOfOnePredictions = null, numOfZeroPredictions = null;
        
        try {
            numOfOnePredictions = bitTaskOne.get();
            numOfZeroPredictions = bitTaskZero.get();
        } catch (InterruptedException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        initQueueMap(recQueues, pid);

        Comparison compClassLabels = new Comparison(numOfOnePredictions,
                                     numOfZeroPredictions, binaryTiShares, 
                                     oneShare,commonSender, recQueues.get(pid), 
                                     new LinkedList<>(protocolIdQueue), 
                                     clientId, Constants.binaryPrime, pid, partyCount);
        
        Future<Integer> resultTask = es.submit(compClassLabels);
        pid++;
        es.shutdown();
        try {
            predictedClassLabel = resultTask.get();
        } catch (InterruptedException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return predictedClassLabel;
    }
    
    
    public int KNN_Model(){
        //Jaccard Computation
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        startModelHandlers();
        long startTime = System.currentTimeMillis();
        initQueueMap(recQueues, pid);
        
        int decTICount = attrLength*2*trainingSharesCount;
        JaccardDistance jdModule = new JaccardDistance(trainingShares, testShare, 
                oneShare, decimalTiShares.subList(0, decTICount), commonSender, 
                recQueues.get(pid), clientId, Constants.prime, pid, new LinkedList<>(protocolIdQueue),partyCount);
        
        Future<List<List<Integer>>> jdTask = es.submit(jdModule);
        pid++;
        decimalTiIndex += decTICount;
        es.shutdown();
        try {
            jaccardDistances = jdTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for(int i=0;i<trainingShares.size();i++) {
            jaccardDistances.get(i).add(classLabels.get(i));
        }
        
        System.out.println("jaccarddistances:" + jaccardDistances);
        
        int[] indices = new int[K];
        for(int i=0;i<K;i++){
            indices[i] = i;
            KjaccardDistances.add(new ArrayList<>(jaccardDistances.get(i)));
        }
        
        //Sorting the first K numbers
        Sort(indices, 1);
        
        System.out.println("Jaccard Distances:" + jaccardDistances);
        System.out.println("KjaccardDistances:" + KjaccardDistances);
        
        
        //Iterator circuit for rest of the training examples
        for(int i=K;i<trainingSharesCount;i++) {
            System.out.println("calling for training example:"+i);
            swapTrainingShares(i);
        }
        
        System.out.println("KjaccardDistances after iterating all the training examples:"
                            + KjaccardDistances);
        
        int predictedLabel = computeMajorityClassLabel();
        
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        
        System.out.println("Label:"+predictedLabel);
        System.out.println("Time taken:" + elapsedTime + "ms");
        teardownModelHandlers();
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
            BlockingQueue<Message> senderQueue, BlockingQueue<Message> receiverQueue, 
            int clientId, List<TripleInteger> tiShares, List<Integer> jaccardDistanceTraining, 
            List<Integer> jaccardDistanceSorted, List<Integer> jaccardDistanceSortedprev, int partyCount) {
        
        super(protocolID, senderQueue, receiverQueue, protocolIdQueue, clientId, oneShare, partyCount);
        this.trainingIndex = trainingIndex;
        this.position = position;
        this.comparisonResults = comparisonResults;
        this.comparisonMultiplications = comparisonMultiplications;
        this.decimalTiShares = tiShares;
        this.jaccardDistanceSorted = jaccardDistanceSorted;
        this.jaccardDistanceTraining = jaccardDistanceTraining;
        this.jaccardDistanceSortedPrevious = jaccardDistanceSortedprev;
    }
    
    @Override
    public Integer[] call() {
        int pid = 0, decimalTiIndex = 0;
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        startHandlers();
        
        //first mult
        initQueueMap(recQueues, pid);
        List<Integer> piC = new ArrayList<>(Collections.nCopies(3, comparisonMultiplications[position]));
        BatchMultiplicationInteger mult1 = new BatchMultiplicationInteger(jaccardDistanceTraining, 
                piC, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+3), senderQueue, 
                recQueues.get(pid), new LinkedList<>(protocolIdQueue), clientID, Constants.prime, 
                pid, oneShare, 0, partyCount);
        Future<Integer[]> multTask1 = es.submit(mult1);
        pid++;
        decimalTiIndex+=3;
        
        //third mult
        initQueueMap(recQueues, pid);
        List<Integer> C = new ArrayList<>(Collections.nCopies(3, comparisonResults[position]));
        BatchMultiplicationInteger mult3 = new BatchMultiplicationInteger(jaccardDistanceSorted, 
                C, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+3), senderQueue, 
                recQueues.get(pid), new LinkedList<>(protocolIdQueue), clientID, Constants.prime, pid, oneShare, 0, partyCount);
        Future<Integer[]> multTask3 = es.submit(mult3);
        pid++;
        decimalTiIndex+=3;
        
        Integer[] results = new Integer[3];
        Arrays.fill(results, 0);
        
        if(position != 0) {
            //second mult
            initQueueMap(recQueues, pid);
            List<Integer> notC = new ArrayList<>(Collections.nCopies(3, Math.floorMod(oneShare-comparisonResults[position-1], Constants.prime)));
            BatchMultiplicationInteger mult2 = new BatchMultiplicationInteger(jaccardDistanceSortedPrevious, 
                    notC, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+3), senderQueue, 
                    recQueues.get(pid), new LinkedList<>(protocolIdQueue), clientID, Constants.prime, pid, oneShare, 0, partyCount);
            Future<Integer[]> multTask2 = es.submit(mult2);
            pid++;
            decimalTiIndex+=3;
            
            try {
                Integer[] Mults = multTask2.get();
                for(int i=0;i<3;i++) {
                    results[i] += Mults[i];
                }
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        try {
            Integer[] Mults = multTask1.get();
            for(int i=0;i<3;i++) {
                results[i] += Mults[i];
            }
            Mults = multTask3.get();
            for(int i=0;i<3;i++) {
                results[i] = Math.floorMod(results[i] + Mults[i], Constants.prime);
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        tearDownHandlers();
        System.out.println("results retuning: "+Arrays.toString(results));
        return results;
    }
}
