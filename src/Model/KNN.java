/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.Multiplication;
import Protocol.Utility.CrossMultiplyCompare;
import Protocol.Utility.JaccardDistance;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.ArrayList;
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
        
        
        //left index position
        for(int i=0;i<3;i++) {
            initQueueMap(recQueues, sendQueues, pid);
            System.out.println("multiplying.."+comparisonOutput+" and "+ jaccardDistances.get(rightIndex).get(i));
            Multiplication multModule = new Multiplication(comparisonOutput, jaccardDistances.get(rightIndex).get(i), 
                decimalTiShares.get(decimalTiIndex), sendQueues.get(pid), recQueues.get(pid), 
                clientId, Constants.prime, pid, oneShare, 0);
            
            pid++;
            decimalTiIndex++;
            Future<Integer> task = es.submit(multModule);
            leftTaskList.add(task);
            
            initQueueMap(recQueues, sendQueues, pid);
            System.out.println("multiplying.."+Math.floorMod(oneShare - comparisonOutput, Constants.binaryPrime)+" and "+
                    jaccardDistances.get(leftIndex).get(i));
            Multiplication multModule2 = new Multiplication(
                    Math.floorMod(oneShare - comparisonOutput, Constants.binaryPrime), 
                    jaccardDistances.get(leftIndex).get(i), decimalTiShares.get(decimalTiIndex), 
                    sendQueues.get(pid), recQueues.get(pid), clientId, 
                    Constants.prime, pid, oneShare, 0);
            
            pid++;
            decimalTiIndex++;
            Future<Integer> task2 = es.submit(multModule2);
            leftTaskList.add(task2);
        }
        
        //right index position
        for(int i=0;i<3;i++) {
            initQueueMap(recQueues, sendQueues, pid);
            Multiplication multModule = new Multiplication(comparisonOutput, jaccardDistances.get(leftIndex).get(i), 
                decimalTiShares.get(decimalTiIndex), sendQueues.get(pid), recQueues.get(pid), 
                clientId, Constants.prime, pid, oneShare, 0);
            
            pid++;
            decimalTiIndex++;
            Future<Integer> task = es.submit(multModule);
            rightTaskList.add(task);
            
            initQueueMap(recQueues, sendQueues, pid);
            Multiplication multModule2 = new Multiplication(
                    Math.floorMod(oneShare - comparisonOutput, Constants.binaryPrime), 
                    jaccardDistances.get(rightIndex).get(i), decimalTiShares.get(decimalTiIndex), 
                    sendQueues.get(pid), recQueues.get(pid), 
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
    
    void Sort(int startIndex, int endIndex, int next) {
        //base case
        System.out.println("in sort: startIndex="+startIndex+", endIndex="+endIndex+", next="+next);
        if(startIndex == endIndex) {
            return;
        }
        
        if(startIndex+next==endIndex) {
            //comparison
            
            ExecutorService es = Executors.newSingleThreadExecutor();
            
            initQueueMap(recQueues, sendQueues, pid);
            System.out.println("calling crossmultiply and compare");
            //TODO binary shares sublist handling here
            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(jaccardDistances.get(startIndex).get(1), 
                    jaccardDistances.get(startIndex).get(0), jaccardDistances.get(endIndex).get(1), 
                    jaccardDistances.get(endIndex).get(0), oneShare, 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex+2), 
                    binaryTiShares, sendQueues.get(pid), recQueues.get(pid), 
                    clientId, Constants.prime, pid);
            
            Future<Integer> resultTask = es.submit(ccModule);
            pid++;
            decimalTiIndex+=2;
            
            int comparisonresult = 0;
            
            try {
                comparisonresult = resultTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            System.out.println("comparing indices" + startIndex + " " + endIndex + ", result=" +comparisonresult);
            
            //circuit to swap
            swapCircuit(startIndex, endIndex, comparisonresult);
            
            return;
        }
        
        Sort(startIndex, (endIndex-startIndex)/2, 1);
        Sort((endIndex-startIndex)/2 + 1, endIndex, 1);
        
        Merge(startIndex, endIndex);
    }
    
    void Merge(int startIndex, int endIndex) {
        
        System.out.println("In merge: startIndex=" + startIndex + " endIndex=" + endIndex);
        //Sort even indexed
        if(endIndex%2==0){
            Sort(startIndex, endIndex, 2);
        } else {
            Sort(startIndex, endIndex-1, 2);
        }
        
        //Sort odd indexed
        if(endIndex%2==0){
            Sort(startIndex+1, endIndex-1, 2);
        } else {
            Sort(startIndex+1, endIndex, 2);
        }
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        
        //Compare adjacent numbers TODO - handle binaryTiShares sublist
        for(int i=startIndex+1;i<endIndex-1;i+=2){
            //compare and swap jd(i) and jd(i+1)
            System.out.println("calling comparison between adjacent elements: indices - "+i+" and "+ (i+1));
            initQueueMap(recQueues, sendQueues, pid);
            
            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(jaccardDistances.get(i).get(1), 
                    jaccardDistances.get(i).get(0), jaccardDistances.get(i+1).get(1), 
                    jaccardDistances.get(i+1).get(0), oneShare, 
                    decimalTiShares.subList(decimalTiIndex, decimalTiIndex+2), 
                    binaryTiShares, sendQueues.get(pid), recQueues.get(pid), 
                    clientId, Constants.prime, pid);
            
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
        
        for(int i=0;i<n;i++) {
            swapCircuit(startIndex+2*i+1, startIndex+2*i+2, comparisonResults[i]);
        }
    }
    
    
    public int KNN_Model(){
        //Jaccard Computation
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        
        initQueueMap(recQueues, sendQueues, pid);
        
        int decTICount = attrLength*2*trainingShares.size();
        JaccardDistance jdModule = new JaccardDistance(trainingShares, testShare, 
                oneShare, decimalTiShares.subList(0, decTICount), sendQueues.get(pid), 
                recQueues.get(pid), clientId, Constants.prime, pid);
        
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
        
        Sort(0, K-1, 1);
        
        System.out.println("jaccarddistances:" + jaccardDistances);
        
        return 0;
    }
    
}
