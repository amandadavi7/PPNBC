/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.BitDecomposition;
import Protocol.Comparison;
import Protocol.OIS;
import Protocol.Utility.BatchMultiplicationNumber;
import TrustedInitializer.Triple;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author keerthanaa
 */
class PolynomialComputing implements Callable<Integer[]> {
    int s, u, alpha;
    int[] comparisonOutputs;
    Integer[] y_j, jBinary;
    List<Triple> tiShares;
    int startpid, clientId, protocolId, oneShare;
    
    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    protected Queue<Integer> protocolIdQueue;
    
    BlockingQueue<Message> commonSender;
    BlockingQueue<Message> commonReceiver;
    
    public PolynomialComputing(Integer[] y_j, Integer[] jBinary, int alpha, int depth, int[] zOutputs, List<Triple> tiShares, 
            Queue<Integer> protocolIdQueue, 
            ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues,
            BlockingQueue<Message> senderQueue, BlockingQueue<Message> receiverQueue, 
            int startpid, int clientId, int protocolId, int oneShare) {
        this.s = depth;
        u = 1;
        this.alpha = alpha;
        //this.y_j = new Integer[y_j.length];
        //System.arraycopy(y_j, 0, this.y_j, 0, y_j.length);
        this.y_j = y_j;
        this.comparisonOutputs = zOutputs;
        this.jBinary = jBinary;
        this.tiShares = tiShares;
        this.protocolIdQueue = protocolIdQueue;
        this.recQueues = recQueues;
        this.commonReceiver = receiverQueue;
        this.commonSender = senderQueue;
        this.startpid = startpid;
        this.clientId = clientId;
        this.protocolId = protocolId;
        this.oneShare = oneShare;
    }

    @Override
    public Integer[] call() throws Exception {
        
        while(s>0) {
            ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
            List<Future<Integer[]>> taskList = new ArrayList<>();
        
            List<Integer> yj = Arrays.asList(y_j);
            List<Integer> z_u = new ArrayList<>(Collections.<Integer>nCopies(Constants.batchSize, 
                    (comparisonOutputs[u-1] + oneShare*jBinary[s-1])%Constants.binaryPrime));
                        
            int i=0;
            do {
                int toIndex = Math.min(i+Constants.batchSize, alpha);
                //System.out.println("toIndex="+toIndex+", comp"+Arrays.toString(comparisonOutputs)+"jbin"+Arrays.toString(jBinary));
                
                //System.out.println("j="+Arrays.toString(jBinary)+"batchmults between "+yj+" and "+z_u);
                
                recQueues.putIfAbsent(startpid, new LinkedBlockingQueue<>());
                
                BatchMultiplicationNumber mults = new BatchMultiplicationNumber(yj.subList(i, toIndex), z_u, 
                        tiShares.subList(i, toIndex), commonSender, recQueues.get(startpid), new LinkedList<>(protocolIdQueue),clientId, 
                        Constants.binaryPrime, startpid, oneShare, protocolId);

                Future<Integer[]> task = es.submit(mults);
                taskList.add(task);
                i = toIndex;
                startpid++;
                
            } while(i<alpha);
            
            int batches = taskList.size();
            int globalIndex = 0;
            for(i=0;i<batches;i++) {
                Future<Integer[]> taskResponse = taskList.get(i);
                try {
                    Integer[] arr = taskResponse.get();
                    //System.out.println("jBin="+Arrays.toString(jBinary)+"arr="+Arrays.toString(arr));
                    for(int l=0;l<arr.length;l++) {
                        y_j[globalIndex] = arr[l];
                        globalIndex++;
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
                }                       
            }
            
            System.out.println("u="+u+",s="+s+",jbin="+Arrays.toString(jBinary)+",y_j="+Arrays.toString(y_j));
            u = 2*u + jBinary[s-1];
            s--;
        }
        
        System.out.println("returning" + Arrays.toString(y_j));
        return y_j;
        
    }
}

/**
 * This class takes a row of attributes values and predicts the class label based on the decision tree
 * @author keerthanaa
 */
public class DecisionTreeScoring extends Model {
    
    int depth, attributeBitLength, attributeCount;//depth d, bitlength for each attribute value, total no. of attributes
    boolean partyHasTree;                       //true if the party has the tree, false if it has the test case
    Integer[][] featureVectors;                 //Shares of features 2^d - 1 vectors for each internal node
    List<List<Integer>> testVector;             //Test case - a list of features represented as binary values
    int[] leafToClassIndexMapping;              //leaf node index to class index mapping (stored by the party that has the tree)
    int[] nodeToAttributeIndexMapping;          //internal node index to the attribute intex mapping (stored by the party that has the tree)
    int[] attributeThresholds;                  //each internal node's attribute threshold
    List<List<Integer>> attributeThresholdsBitShares;
    int leafNodes, tiBinaryStartIndex, tiDecimalStartIndex, classValueCount, alpha;
    int[] comparisonOutputs, finalOutputs;
    
    /**
     * Constructor if the party has the tree
     * 
     * @param oneShare
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param binaryTriples
     * @param decimalTriple
     * @param depth
     * @param attributeCount
     * @param bitLength
     * @param leafToClassIndexMapping
     * @param nodeToAttributeIndexMapping
     * @param attributeThresholds
     * @param classValueCount 
     */
    public DecisionTreeScoring(int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, List<Triple> binaryTriples, List<Triple> decimalTriple, 
            int depth, int attributeCount, int bitLength, int[] leafToClassIndexMapping, 
            int[] nodeToAttributeIndexMapping, int[] attributeThresholds, int classValueCount) {
        

        super(senderQueue, receiverQueue, clientId, oneShare, binaryTriples, decimalTriple);
        
        this.depth = depth;
        partyHasTree = true;
        testVector = null;
        this.leafToClassIndexMapping = leafToClassIndexMapping;
        this.nodeToAttributeIndexMapping = nodeToAttributeIndexMapping;
        this.attributeThresholds = attributeThresholds;
        this.attributeCount = attributeCount;
        attributeBitLength = bitLength;
        this.classValueCount = classValueCount;
    }
    
    
    /**
     * Constructor if the party has the feature vector
     * 
     * @param oneShares
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param binaryTriples
     * @param decimalTriples
     * @param depth
     * @param attributeCount
     * @param bitLength
     * @param testVector
     * @param classValueCount 
     */
    public DecisionTreeScoring(int oneShares, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, List<Triple> binaryTriples, List<Triple> decimalTriples,
            int depth, int attributeCount, int bitLength, List<List<Integer>> testVector, int classValueCount){
        
        super(senderQueue, receiverQueue, clientId, oneShares, binaryTriples, decimalTriples);
        
        partyHasTree = false;
        this.depth = depth;
        attributeBitLength = bitLength;
        this.attributeCount = attributeCount;
        this.testVector = testVector;
        this.leafToClassIndexMapping = null;
        this.nodeToAttributeIndexMapping = null;
        this.attributeThresholds = null;
        this.classValueCount = classValueCount;
    }
    
    
    /**
     * Doing common initializations for both parties here
     */
    void init() {
        
        leafNodes = (int) Math.pow(2, depth);
        featureVectors = new Integer[leafNodes-1][attributeBitLength];
        attributeThresholdsBitShares = new ArrayList<>();
        comparisonOutputs = new int[leafNodes-1];
        tiBinaryStartIndex = 0;
        tiDecimalStartIndex = 0;
        alpha = (int) Math.ceil(Math.log(classValueCount)/Math.log(2.0));
        finalOutputs = new int[alpha];
    }
    
    
    /**
     * Main method for the DT Scoring algorithm 
     */
    public void ScoreDecisionTree(){
        
        startModelHandlers();
        
        init();
        
        long startTime = System.currentTimeMillis();
        
        //Protocol IDs from 0 to leafNodes-2
        getFeatureVectors();
        System.out.println("got the feature vectors");
        
        //Convert Threshold to bit shares leafNodes-1 to 2(leafNodes)-3
        convertThresholdsToBits(leafNodes - 1);
        
        //Protocol IDs from 2(leafNodes)- 2 to 3(leafNodes) - 4
        doThresholdComparisons(2*leafNodes - 1);
        
        //
        computePolynomialEquation(3*leafNodes - 4);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
                
        System.out.println("the output in bits: " + Arrays.toString(finalOutputs));
        System.out.println("Avg time duration:" + elapsedTime);
        
        teardownModelHandlers();
    }
    
    /**
     * gets the feature vectors for each attribute of 
     * internal node using Oblivious Input selection Protocol
     */
    void getFeatureVectors(){
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();
        
        if(partyHasTree){
            for(int i=0;i<leafNodes-1;i++){
                
                initQueueMap(recQueues, i);
                OIS ois = new OIS(null, binaryTiShares.subList(tiBinaryStartIndex, 
                        tiBinaryStartIndex+(attributeBitLength*attributeCount)), 
                        oneShare, commonSender, recQueues.get(i), new LinkedList<>(protocolIdQueue),clientId, Constants.binaryPrime, i, 
                        attributeBitLength, nodeToAttributeIndexMapping[i], attributeCount);
                tiBinaryStartIndex += attributeBitLength*attributeCount;                
                Future<Integer[]> task = es.submit(ois);
                taskList.add(task);
                
            }
        } else {
            for(int i=0;i<leafNodes-1;i++) {
                
                initQueueMap(recQueues, i);
                OIS ois = new OIS(testVector, binaryTiShares.subList(tiBinaryStartIndex, 
                        tiBinaryStartIndex+(attributeBitLength*attributeCount)), 
                        oneShare, commonSender, recQueues.get(i), new LinkedList<>(protocolIdQueue),clientId, Constants.binaryPrime, i, 
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
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
       
    }
    
    /**
     * Converts all the attribute thresholds to bits using bitdecomposition protocol
     * @param startpid 
     */
    void convertThresholdsToBits(int startpid) {
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<List<Integer>>> taskList = new ArrayList<>();
        
        
        //Need to handle tishares sublist TODO - bitdecomposition change w.r.t protocol
        if(partyHasTree) {
            for(int i=0;i<leafNodes-1;i++) {
                initQueueMap(recQueues, i+startpid);
                BitDecomposition bitD = new BitDecomposition(attributeThresholds[i], binaryTiShares, oneShare, Constants.bitLength,
                        commonSender, recQueues.get(i+startpid), new LinkedList<>(protocolIdQueue),clientId, Constants.binaryPrime, i+startpid);
                Future<List<Integer>> task = es.submit(bitD);
                taskList.add(task);
            }
        } else {
            for(int i=0;i<leafNodes-1;i++) {
                initQueueMap(recQueues, i+startpid);
                BitDecomposition bitD = new BitDecomposition(0, binaryTiShares, oneShare, Constants.bitLength,
                        commonSender, recQueues.get(i+startpid), new LinkedList<>(protocolIdQueue),clientId, Constants.binaryPrime, i+startpid);
                Future<List<Integer>> task = es.submit(bitD);
                taskList.add(task);
            }
        }
        
        es.shutdown();
        
        for (int i = 0; i < leafNodes-1; i++) {
            Future<List<Integer>> taskResponse = taskList.get(i);
            try {
                attributeThresholdsBitShares.add(taskResponse.get());
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }        
        }
        
    }
    
    /**
     * compares the attribute threshold with the test vector's attribute value
     * @param startpid 
     */
    void doThresholdComparisons(int startpid) {
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        
        // TODO - Need to handle tishares sublist
        for(int i=0;i<leafNodes-1;i++){
            initQueueMap(recQueues, i+startpid);
            Comparison comp = new Comparison(Arrays.asList(featureVectors[i]), attributeThresholdsBitShares.get(i), binaryTiShares,
                    oneShare, commonSender, recQueues.get(i+startpid), new LinkedList<>(protocolIdQueue),clientId, Constants.binaryPrime, i+startpid);
            
            Future<Integer> task = es.submit(comp);
            taskList.add(task);
        }
        
        es.shutdown();
        
        for (int i = 0; i < leafNodes-1; i++) {
            Future<Integer> taskResponse = taskList.get(i);
            try {
                comparisonOutputs[i] = taskResponse.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    Integer[] convertToBits(int decimal, int size) {
        Integer[] binaryNum = new Integer[size];
        int i = 0;
        while (decimal > 0) 
        {
            binaryNum[i] = decimal % 2;
            decimal = decimal / 2;
            i++;
        }
        
        while(i<size) {
            binaryNum[i] = 0;
            i++;
        }
        
        return binaryNum;
    }
    
    /**
     * 
     * @param startpid 
     */
    void computePolynomialEquation(int startpid){
        
        Integer[][] yShares = new Integer[leafNodes][alpha];
        
        //y[j][r] initialization
        if(partyHasTree) {
            for(int j=0;j<leafNodes;j++) {
                Integer[] temp = convertToBits(leafToClassIndexMapping[j+1]-1, alpha);
                for(int r=0;r<alpha;r++) {
                    yShares[j][r] = temp[r];
                }
            }
        } else {
            for (int j=0;j<leafNodes;j++){
                for(int r=0;r<alpha;r++) {
                    yShares[j][r] = 0;
                }
            }
        }
        
        //Polynomial computation
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();
        
        for(int j=0;j<leafNodes;j++) {
            Integer[] jBinary = convertToBits(j, depth);
            
            PolynomialComputing pc = new PolynomialComputing(yShares[j], jBinary, alpha, depth, comparisonOutputs, binaryTiShares, 
                    new LinkedList<>(protocolIdQueue), recQueues, commonSender, commonReceiver, startpid, clientId, 0, oneShare);
            
            startpid += depth * (alpha/Constants.batchSize + 1);
            Future<Integer[]> task = es.submit(pc);
            taskList.add(task);
        }
        
        es.shutdown();
        
        for (int j = 0; j < leafNodes; j++) {
            Future<Integer[]> taskResponse = taskList.get(j);
            try {
                yShares[j] = taskResponse.get();
                System.out.println("j="+j+", y[j]="+Arrays.toString(yShares[j]));
                //Integer[] temp = taskResponse.get();
                //System.out.println("temp="+Arrays.toString(temp));
                //for(int r=0;r<alpha;r++) {
                //    yShares[j][r] = temp[r];
                //}
            } catch (InterruptedException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        for(int i=0;i<alpha;i++) {
            for(int j=0;j<leafNodes;j++) {
                finalOutputs[i] += yShares[j][i];
            }
            finalOutputs[i]%=Constants.binaryPrime;
        }        
    }
}
