/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
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
                    jaccardDistances.get(endIndex).get(0), oneShares, 
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
            initQueueMap(recQueues, sendQueues, pid);
            
            CrossMultiplyCompare ccModule = new CrossMultiplyCompare(jaccardDistances.get(i).get(1), 
                    jaccardDistances.get(i).get(0), jaccardDistances.get(i+1).get(1), 
                    jaccardDistances.get(i+1).get(0), oneShares, 
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
    }
    
    
    public int KNN_Model(){
        //Jaccard Computation
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        
        initQueueMap(recQueues, sendQueues, pid);
        
        int decTICount = attrLength*2*trainingShares.size();
        JaccardDistance jdModule = new JaccardDistance(trainingShares, testShare, 
                oneShares, decimalTiShares.subList(0, decTICount), sendQueues.get(pid), 
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
        
        return 0;
    }
    
}
