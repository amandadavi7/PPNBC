/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.Multiplication;
import Protocol.OR_XOR;
import Protocol.Utility.CrossMultiplyCompare;
import Protocol.Utility.JaccardDistance;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
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
    List<List<Integer>> jaccardDistances;
    int pid, attrLength, K, decimalTiIndex, binaryTiIndex;
    
    
    public KNN(int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, List<Triple> binaryTriples, 
            List<Triple> decimalTriples, List<List<Integer>> trainingShares, List<Integer> testShare, 
            List<Integer> classLabels, int K) {
        
        super(senderQueue, receiverQueue, clientId, oneShare, binaryTriples, decimalTriples);
        this.trainingShares = trainingShares;
        this.testShare = testShare;
        this.classLabels = classLabels;
        pid = 0;
        this.attrLength = testShare.size();
        this.K = K; //K is not shares
        this.decimalTiIndex = 0;
        this.binaryTiIndex = 0;
    }
    
    void swapCircuit(int leftIndex, int rightIndex, int comparisonOutput) {
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> leftTaskList = new ArrayList<>();
        List<Future<Integer>> rightTaskList = new ArrayList<>();
        
        
        //trial and error .........................................................................
        initQueueMap(recQueues, pid);
        List<Integer> cShares = new ArrayList<>();
        cShares.add(comparisonOutput);
        
        List<Integer> dummy = new ArrayList<>();
        dummy.add(0);
        OR_XOR xor = null;
        
        
        if(clientId==1) {
            xor = new OR_XOR(cShares, dummy, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+1), 
                oneShare, 2, commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                clientId, Constants.prime, pid);
        } else if(clientId==2) {
            xor = new OR_XOR(dummy, cShares, decimalTiShares.subList(decimalTiIndex, decimalTiIndex+1), 
                oneShare, 2, commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                clientId, Constants.prime, pid);
        }
        
        Future<Integer[]> xorTask = es.submit(xor);
        pid++; decimalTiIndex++;
        
        Integer[] c = null;
        try {
            c = xorTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }
        //trial and error..........................................................................
        
        //left index position
        for(int i=0;i<3;i++) {
            initQueueMap(recQueues, pid);
            System.out.println("multiplying.."+c[0]+" and "+ jaccardDistances.get(rightIndex).get(i));
            Multiplication multModule = new Multiplication(c[0], jaccardDistances.get(rightIndex).get(i), 
                decimalTiShares.get(decimalTiIndex), commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                clientId, Constants.prime, pid, oneShare, 0);
            
            pid++;
            decimalTiIndex++;
            Future<Integer> task = es.submit(multModule);
            leftTaskList.add(task);
            
            initQueueMap(recQueues, pid);
            System.out.println("multiplying.."+Math.floorMod(oneShare - c[0], Constants.prime)+" and "+
                    jaccardDistances.get(leftIndex).get(i));
            Multiplication multModule2 = new Multiplication(
                    Math.floorMod(oneShare - c[0], Constants.prime), 
                    jaccardDistances.get(leftIndex).get(i), decimalTiShares.get(decimalTiIndex), 
                    commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), clientId, 
                    Constants.prime, pid, oneShare, 0);
            
            pid++;
            decimalTiIndex++;
            Future<Integer> task2 = es.submit(multModule2);
            leftTaskList.add(task2);
        }
        
        //right index position
        for(int i=0;i<3;i++) {
            initQueueMap(recQueues, pid);
            Multiplication multModule = new Multiplication(c[0], jaccardDistances.get(leftIndex).get(i), 
                decimalTiShares.get(decimalTiIndex), commonSender, recQueues.get(pid), 
                new LinkedList<>(protocolIdQueue), 
                clientId, Constants.prime, pid, oneShare, 0);
            
            pid++;
            decimalTiIndex++;
            Future<Integer> task = es.submit(multModule);
            rightTaskList.add(task);
            
            initQueueMap(recQueues, pid);
            Multiplication multModule2 = new Multiplication(
                    Math.floorMod(oneShare - c[0], Constants.prime), 
                    jaccardDistances.get(rightIndex).get(i), decimalTiShares.get(decimalTiIndex), 
                    commonSender, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                    clientId, Constants.prime, pid, oneShare, 0);
            
            pid++;
            decimalTiIndex++;
            Future<Integer> task2 = es.submit(multModule2);
            rightTaskList.add(task2);
        }
        es.shutdown();
        System.out.println("before swapping indices:"+leftIndex+"and"+rightIndex);
        System.out.println(jaccardDistances.get(leftIndex)+" "+ jaccardDistances.get(rightIndex));
        
        for(int i=0;i<6;i+=2) {
            try {
                Future<Integer> leftTask1 = leftTaskList.get(i);
                Future<Integer> leftTask2 = leftTaskList.get(i+1);
                int test1, test2;
                test1 = leftTask1.get(); test2 = leftTask2.get();
                System.out.println("c.d:"+test1+",(1-c).d:"+test2);
                int newLeftVal = Math.floorMod(test1+test2, Constants.prime);
                jaccardDistances.get(leftIndex).set(i/2, newLeftVal);
                
                Future<Integer> rightTask1 = rightTaskList.get(i);
                Future<Integer> rightTask2 = rightTaskList.get(i+1);
                test1 = rightTask1.get(); test2 = rightTask2.get();
                System.out.println("c.d:"+test1+",(1-c).d:"+test2);
                int newRightVal = Math.floorMod(test1+test2, Constants.prime);
                jaccardDistances.get(rightIndex).set(i/2, newRightVal);
                
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        System.out.println("after swapping indices:"+leftIndex+"and"+rightIndex);
        System.out.println(jaccardDistances.get(leftIndex)+" "+ jaccardDistances.get(rightIndex));
        
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
            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(jaccardDistances.get(indices[startIndex]).get(1), 
                    jaccardDistances.get(indices[startIndex]).get(0), jaccardDistances.get(indices[endIndex]).get(1), 
                    jaccardDistances.get(indices[endIndex]).get(0), oneShare, 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex+2), 
                    binaryTiShares, commonSender, recQueues.get(pid), 
                    clientId, Constants.prime, Constants.binaryPrime, pid, new LinkedList<>(protocolIdQueue));
            
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
            swapCircuit(indices[startIndex], indices[endIndex], comparisonresult);
            
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
        
        int startIndex = 0, endIndex = indices.length-1;
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
            System.out.println("calling comparison between adjacent elements: indices - "+indices[i]+" and "+ indices[i+1]);
            initQueueMap(recQueues, pid);
            
            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(jaccardDistances.get(indices[i]).get(1), 
                    jaccardDistances.get(indices[i]).get(0), jaccardDistances.get(indices[i+1]).get(1), 
                    jaccardDistances.get(indices[i+1]).get(0), oneShare, 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex+2), 
                    binaryTiShares, commonSender, recQueues.get(pid), 
                    clientId, Constants.prime, Constants.binaryPrime, pid, new LinkedList<>(protocolIdQueue));
            
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
            swapCircuit(indices[i], indices[i+1], comparisonResults[j]);
        }
        
    }
    
    
    public int KNN_Model(){
        //Jaccard Computation
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        startModelHandlers();
        long startTime = System.currentTimeMillis();
        initQueueMap(recQueues, pid);
        
        int decTICount = attrLength*2*trainingShares.size();
        JaccardDistance jdModule = new JaccardDistance(trainingShares, testShare, 
                oneShare, decimalTiShares.subList(0, decTICount), commonSender, 
                recQueues.get(pid), clientId, Constants.prime, pid, new LinkedList<>(protocolIdQueue));
        
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
        }
        Sort(indices, 1);
        
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        
        System.out.println("jaccarddistances:" + jaccardDistances);
        System.out.println("Time taken:" + elapsedTime + "ms");
        teardownModelHandlers();
        return 0;
    }
    
}
