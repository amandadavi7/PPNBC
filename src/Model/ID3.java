/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.ArgMax;
import Protocol.BitDecomposition;
import Protocol.DotProduct;
import Protocol.Equality;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.ArrayList;
import java.util.Arrays;
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
public class ID3 extends Model {
    
    //classLabelCount - total no. of class labels, attrCount - total no. of attributes
    //datasetSize - total no. of rows in the DB
    int classLabelCount, attrCount, datasetSize, rCounter;
    int[][] dataset;    //the dataset
    int attrLabelCounts[]; //Mapping between attr index to no. of attr values for each attr (may or may not be needed
    //List<List<Integer>> attrValues; //List i consists of all possible attr values for attr i
    Integer[] classIndexLabelMapping;
    Integer[] subsetTransactionsBitVector; //dataset currently considered
    Integer[] attributeBitVector;
    //probably shared adhering to oneShare rules??
    List<List<Integer[]>> attrValueBitVector; //list(k attr) of list(j values) of arrays[bit representation of ak,j]
    List<Integer[]> classValueBitVector; //same as the above but for class values
    
    int pid, binaryIndex, decimalIndex, equalityIndex; // a global ID series - TODO
    
    //oneshares to equalityShares - common for all the models
    public ID3(int oneShares, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, List<Triple> binaryTriples, 
            List<Triple> decimalTriple, List<Integer> equalityShares, int classesCount, 
            int attrCount, int[][] dataset, List<List<Integer[]>> attrValues, 
            Integer[] classLabels, List<Integer[]> classValues) {
        
        super(senderQueue, receiverQueue, clientId, oneShares, binaryTriples, decimalTriple, equalityShares);
        this.classLabelCount = classesCount;
        this.attrCount = attrCount;
        this.dataset = dataset;
        this.attrValueBitVector = attrValues;
        classIndexLabelMapping = classLabels;
        this.rCounter = attrCount-1;
        this.classValueBitVector = classValues;
        pid = 0;
        
    }
    
    void init() {
        //Initialize the transaction vector to 1 (one or oneshare???)
        subsetTransactionsBitVector = new Integer[dataset.length];
        for(int i=0;i<datasetSize;i++){
            subsetTransactionsBitVector[i] = 1;
        }
        
        //Initially all attributes are available (may need to be changed to oneShares instead of 1
        //To check
        attributeBitVector = new Integer[attrCount];
        for(int i=0;i<attrCount;i++) {
            attributeBitVector[i] = 1;
        }
        
    }
    
    Integer[] findCommonClassIndex(Integer[] subsetTransactions) {
        /// total no. of rows / class label
        int[] s = new int[classLabelCount];
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        
        for(int i=0;i<classLabelCount;i++) {
            
            initQueueMap(recQueues, sendQueues, i+pid);
            
            DotProduct dp = new DotProduct(Arrays.asList(subsetTransactions), 
                    Arrays.asList(classValueBitVector.get(i)), binaryTiShares, 
                    sendQueues.get(i+pid), recQueues.get(i+pid), clientId, 
                    Constants.binaryPrime, i+pid, oneShares);
            
            Future<Integer> dpresult = es.submit(dp);
            taskList.add(dpresult);
        }
        
        for(int i=0;i<classLabelCount;i++){
            try {
                Future<Integer> dpresult = taskList.get(i);
                s[i] = dpresult.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(ID3.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        pid+=classLabelCount;
        ///////////////////////////////////// convert the above results to bits
        List<Future<List<Integer>>> taskList2 = new ArrayList<>();
        
        
        //Need to handle tishares sublist
        if(clientId == 1) {
            for(int i=0;i<classLabelCount;i++) {
                initQueueMap(recQueues, sendQueues, i+pid);
                BitDecomposition bitD = new BitDecomposition(s[i], 0, binaryTiShares, 
                        oneShares, Constants.bitLength, sendQueues.get(i+pid), 
                        recQueues.get(i+pid), clientId, Constants.binaryPrime, i+pid);
                Future<List<Integer>> task = es.submit(bitD);
                taskList2.add(task);
            }
        } else if (clientId==2) {
            for(int i=0;i<classLabelCount;i++) {
                initQueueMap(recQueues, sendQueues, i+pid);
                BitDecomposition bitD = new BitDecomposition(0, s[i], binaryTiShares, 
                        oneShares, Constants.bitLength, sendQueues.get(i+pid), 
                        recQueues.get(i+pid), clientId, Constants.binaryPrime, i+pid);
                Future<List<Integer>> task = es.submit(bitD);
                taskList2.add(task);
            }
        }
        
        pid+=classLabelCount;
        
        List<List<Integer>> bitSharesS = new ArrayList<>();
        for (int i = 0; i < classLabelCount; i++) {
            Future<List<Integer>> taskResponse = taskList2.get(i);
            try {
                bitSharesS.add(taskResponse.get());
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }        
        }
        
        //compute argmax
        Integer[] commonClassLabel = new Integer[classLabelCount];
        initQueueMap(recQueues, sendQueues, pid);
        ArgMax argmax = new ArgMax(bitSharesS, binaryTiShares, oneShares, sendQueues.get(pid), 
                recQueues.get(pid), clientId, Constants.binaryPrime, pid);
        Future<Integer[]> argmaxTask = es.submit(argmax);
        pid++;
        
        try {
            commonClassLabel = argmaxTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(ID3.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //compute the class index using the above result
        initQueueMap(recQueues, sendQueues, pid);
        DotProduct dp = new DotProduct(Arrays.asList(commonClassLabel), Arrays.asList(classIndexLabelMapping), 
                decimalTiShares, sendQueues.get(pid), recQueues.get(pid), 
                clientId, Constants.prime, pid, oneShares);
            
        Future<Integer> ci = es.submit(dp);
        Integer[] commonClass = new Integer[2];
        pid++;
        
        try {
            commonClass[0] = ci.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(ID3.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //compute the si using argmax result
        initQueueMap(recQueues, sendQueues, pid);
        DotProduct dp2 = new DotProduct(Arrays.asList(commonClassLabel), 
                Arrays.asList(classIndexLabelMapping), decimalTiShares, 
                sendQueues.get(pid), recQueues.get(pid), clientId, 
                Constants.prime, pid, oneShares);
        
        Future<Integer> si = es.submit(dp2);
        pid++;
        es.shutdown();
        
        try{
            commonClass[1] = si.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(ID3.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return commonClass;
        
    }
    
    public void Compute() {
        init();
        
        ID3_Model(subsetTransactionsBitVector, attributeBitVector, rCounter);
    }
    
    
    Integer ID3_Model(Integer[] transactions, Integer[] attributes, int r) {
        //Finding the most common class Label
        Integer[] ci = findCommonClassIndex(transactions);
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        //Check the stopping criteria
        if(r==0) {
            return ci[0];
        }
        
        //doing secure equality between |T| and ci[1]
        int transactionCount = 0;
        for(int i=0;i<datasetSize;i++) {
            transactionCount+=subsetTransactionsBitVector[i];
        }
        
        Equality eq = new Equality(transactionCount, ci[1], equalityTiShares.get(0), 
                decimalTiShares.get(0), oneShares, commonSender, commonReceiver, 
                clientId, Constants.prime, pid);
        Future<Integer> eqTask = es.submit(eq);
        pid++;
        try {
            int eqResult = eqTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(ID3.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //U computation
        return 0;
    }
    
    
}
