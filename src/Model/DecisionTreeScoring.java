/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Communication.ReceiverQueueHandler;
import Communication.SenderQueueHandler;
import Protocol.Comparison;
import Protocol.OIS;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author keerthanaa
 */
public class DecisionTreeScoring extends Model {
    
    int depth, attributeBitLength, attributeCount;//depth d
    boolean partyHasTree;                       //true if the party has the tree, false if it has the test case
    Integer[][] featureVectors;                 //Shares of features 2^d - 1 vectors for each internal node
    List<List<Integer>> testVector;             //Test case - a list of features represented as binary values
    int[] leafToClassIndexMapping;
    int[] nodeToAttributeIndexMapping;
    int[] attributeThresholds;
    int leafNodes, tiBinaryStartIndex, tiDecimalStartIndex;
    int[] comparisonOutputs;
    
    public DecisionTreeScoring(int oneShares, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, List<Triple> binaryTriples, List<Triple> decimalTriple, 
            int depth, int attributeCount, int bitLength, int[] leafToClassIndexMapping, 
            int[] nodeToAttributeIndexMapping, int[] attributeThresholds) {
        
        super(senderQueue, receiverQueue, clientId, oneShares, binaryTriples, decimalTriple);
        
        this.depth = depth;
        partyHasTree = true;
        testVector = null;
        this.leafToClassIndexMapping = leafToClassIndexMapping;
        this.nodeToAttributeIndexMapping = nodeToAttributeIndexMapping;
        this.attributeThresholds = attributeThresholds;
        this.attributeCount = attributeCount;
        attributeBitLength = bitLength;
    }
    
    public DecisionTreeScoring(int oneShares, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, List<Triple> binaryTriples, List<Triple> decimalTriple,
            int depth, int attributeCount, int bitLength, List<List<Integer>> testVector){
        
        super(senderQueue, receiverQueue, clientId, oneShares, binaryTriples, decimalTriple);
        
        partyHasTree = false;
        this.depth = depth;
        attributeBitLength = bitLength;
        this.attributeCount = attributeCount;
        this.testVector = testVector;
        this.leafToClassIndexMapping = null;
        this.nodeToAttributeIndexMapping = null;
        this.attributeThresholds = null;
    }
    
    int ScoreDecisionTree(){
        
        startModelHandlers();
        
        //Doing common initializations for both parties here
        leafNodes = (int) Math.pow(2, depth);
        featureVectors = new Integer[leafNodes-1][attributeBitLength];
        tiBinaryStartIndex = 0;
        tiDecimalStartIndex = 0;
                
        //Protocol IDs from 0 to leafNodes-2
        getFeatureVectors();
        
        //Protocol IDs from leaftNodes-1 to 2(leafNodes)- 3
        doThresholdComparisons();
        
        teardownModelHandlers();
        return 0;
    }
    
    
    void getFeatureVectors(){
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();
        
        
        if(partyHasTree){
            for(int i=0;i<leafNodes-1;i++){
                
                initQueueMap(recQueues, sendQueues, i);
                OIS ois = new OIS(null, binaryTiShares.subList(tiBinaryStartIndex, 
                        tiBinaryStartIndex+(attributeBitLength*attributeCount)), 
                        oneShares, sendQueues.get(i), recQueues.get(i), clientId, Constants.binaryPrime, i, 
                        attributeBitLength, nodeToAttributeIndexMapping[i], attributeCount);
                tiBinaryStartIndex += attributeBitLength*attributeCount;                
                Future<Integer[]> task = es.submit(ois);
                taskList.add(task);
                
            }
        } else {
            for(int i=0;i<leafNodes-1;i++) {
                
                initQueueMap(recQueues, sendQueues, i);
                OIS ois = new OIS(testVector, binaryTiShares.subList(tiBinaryStartIndex, 
                        tiBinaryStartIndex+(attributeBitLength*attributeCount)), 
                        oneShares, sendQueues.get(i), recQueues.get(i), clientId, Constants.binaryPrime, i, 
                        attributeBitLength, -1, attributeCount);
                tiBinaryStartIndex += attributeBitLength*attributeCount;                
                Future<Integer[]> task = es.submit(ois);
                taskList.add(task);
            }
        }
        
        es.shutdown();
                
        for (int i = 0; i < leafNodes-1; i++) {
            Future<Integer[]> taskResponse = taskList.get(i);
            try {
                featureVectors[i] = taskResponse.get();
            } catch (InterruptedException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
       
    }
    
    void doThresholdComparisons() {
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        
        if(partyHasTree) {
            for(int i=0;i<leafNodes-1;i++){
//                initQueueMap(recQueues, sendQueues, i+leafNodes-1);
//                Comparison comp = new Comparison(Arrays.asList(featureVectors[i]));
//            
//                Future<Integer[]> task = es.submit(comp);
//                taskList.add(task);
            
            }
        } else {
            
        }
        
        
        
        
        
    }
}
