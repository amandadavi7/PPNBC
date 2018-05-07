/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.Utility.JaccardDistance;
import TrustedInitializer.Triple;
import Utility.Constants;
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
    int pid, attrLength;
    
    
    public KNN(int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, List<Triple> binaryTriples, 
            List<Triple> decimalTriples, List<List<Integer>> trainingShares, List<Integer> testShare, 
            List<Integer> classLabels
            ) {
        
        super(senderQueue, receiverQueue, clientId, oneShare, binaryTriples, decimalTriples);
        this.trainingShares = trainingShares;
        this.testShare = testShare;
        this.classLabels = classLabels;
        pid = 0;
        this.attrLength = testShare.size();
    }
    
    int KNN_Model(){
        //Jaccard Computation
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        
        initQueueMap(recQueues, sendQueues, pid);
        
        int decTICount = attrLength*2*trainingShares.size();
        JaccardDistance jdModule = new JaccardDistance(trainingShares, testShare, 
                oneShares, decimalTiShares.subList(0, decTICount), sendQueues.get(pid), 
                recQueues.get(pid), clientId, Constants.prime, pid);
        
        Future<List<List<Integer>>> jdTask = es.submit(jdModule);
        
        try {
            jaccardDistances = jdTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for(int i=0;i<trainingShares.size();i++) {
            jaccardDistances.get(i).add(classLabels.get(i));
        }
        return 0;
    }
    
}
