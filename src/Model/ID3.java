/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.ArgMax;
import Protocol.BitDecomposition;
import Protocol.DotProductByte;
import Protocol.DotProductInteger;
import Protocol.Equality;
import Protocol.MultiplicationInteger;
import Protocol.Utility.BatchMultiplicationInteger;
import TrustedInitializer.Triple;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
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
public class ID3 extends Model {
    
    //classLabelCount - total no. of class labels, attrCount - total no. of attributes
    //datasetSize - total no. of rows in the DB, rCounter - the counter to decrement no. of attributes left
    //attrValueCount - total no. of attribute values per attribute (make it constant by adding dummy variable)
    //For now, input accordingly, TODO -  how to manage this dummy value addition??
    int classLabelCount, attrCount, datasetSize, rCounter, attrValueCount;
    int[][] dataset;    //the dataset
    int attrLabelCounts[]; //Mapping between attr index to no. of attr values for each attr (may or may not be needed
    Integer[] classIndexLabelMapping;
    Integer[] subsetTransactionsBitVector; //dataset currently considered
    Integer[] attributeBitVector;
    List<Integer> equalityTiShares;
    List<TripleByte> binaryTiShares;
    List<TripleInteger> decimalTiShares;
    
    int alpha; //the corrective factor to multiply (value is 8 in the paper)
    
    //probably shared adhering to oneShare rules??
    List<List<Integer[]>> attrValueBitVector; //list(k attr) of list(j values) of arrays[bit representation of ak,j]
    List<Integer[]> classValueBitVector; //same as the above but for class values
    
    int pid, binaryTiIndex, decimalTiIndex, equalityTiIndex; // a global ID series - TODO
    
    //oneshares to equalityShares - common for all the models
    public ID3(int asymmetricBit, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue, int clientId, List<TripleByte> binaryTriples, 
            List<TripleInteger> decimalTriple, List<Integer> equalityShares, int classesCount, 
            int attrCount, int attrValueCount, int[][] dataset, List<List<Integer[]>> attrValues, 
            Integer[] classLabels, List<Integer[]> classValues, int partyCount,
            Queue<Integer> protocolIdQueue, int protocolID) {
        
        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);
        
        this.equalityTiShares = equalityShares;
        this.classLabelCount = classesCount;
        this.attrCount = attrCount;
        this.attrValueCount = attrValueCount;
        this.dataset = dataset;
        this.attrValueBitVector = attrValues;
        classIndexLabelMapping = classLabels;
        this.rCounter = attrCount-1;
        this.classValueBitVector = classValues;
        this.decimalTiShares = decimalTriple;
        this.binaryTiShares = binaryTriples;
        this.alpha = 8;
        pid = 0;
        decimalTiIndex = 0;
        binaryTiIndex = 0;
        equalityTiIndex = 0;
        
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
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> DPtaskList = new ArrayList<>();
        
        for(int i=0;i<classLabelCount;i++) {
            
            DotProductByte dp = new DotProductByte(Arrays.asList(subsetTransactions), 
                    Arrays.asList(classValueBitVector.get(i)), binaryTiShares, pidMapper, 
                    commonSender, new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime,
                    pid, asymmetricBit, partyCount);
            
            Future<Integer> dpresult = es.submit(dp);
            pid++;
            DPtaskList.add(dpresult);
        }
        
        for(int i=0;i<classLabelCount;i++){
            try {
                Future<Integer> dpresult = DPtaskList.get(i);
                s[i] = dpresult.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(ID3.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        pid+=classLabelCount;
        ///////////////////////////////////// convert the above results to bits
        List<Future<List<Integer>>> BDtaskList = new ArrayList<>();
        
        
        //Need to handle tishares sublist
        for(int i=0;i<classLabelCount;i++) {
            BitDecomposition bitD = new BitDecomposition(s[i], binaryTiShares, 
                    asymmetricBit, Constants.bitLength, pidMapper, commonSender, 
                    new LinkedList<>(protocolIdQueue), clientId, 
                    Constants.binaryPrime, pid, partyCount);
            pid++;
            Future<List<Integer>> task = es.submit(bitD);
            BDtaskList.add(task);
        }
        
        pid+=classLabelCount;
        
        List<List<Integer>> bitSharesS = new ArrayList<>();
        for (int i = 0; i < classLabelCount; i++) {
            Future<List<Integer>> taskResponse = BDtaskList.get(i);
            try {
                bitSharesS.add(taskResponse.get());
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }        
        }
        
        //compute argmax
        Integer[] commonClassLabel = new Integer[classLabelCount];
        ArgMax argmax = new ArgMax(bitSharesS, binaryTiShares, asymmetricBit,
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, Constants.binaryPrime, pid, partyCount);
        Future<Integer[]> argmaxTask = es.submit(argmax);
        pid++;
        
        try {
            commonClassLabel = argmaxTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(ID3.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //compute the class index using the above result
        DotProductInteger dp = new DotProductInteger(Arrays.asList(commonClassLabel), Arrays.asList(classIndexLabelMapping),
                decimalTiShares, pidMapper, commonSender, 
                new LinkedList<>(protocolIdQueue),
                clientId, Constants.prime, pid, asymmetricBit, partyCount);
            
        Future<Integer> ci = es.submit(dp);
        Integer[] commonClass = new Integer[2];
        pid++;
        
        try {
            commonClass[0] = ci.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(ID3.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //compute the si using argmax result
        DotProductInteger dp2 = new DotProductInteger(Arrays.asList(commonClassLabel), 
                Arrays.asList(classIndexLabelMapping), decimalTiShares, 
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue), clientId, 
                Constants.prime, pid, asymmetricBit, partyCount);
        
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
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
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
                decimalTiShares.get(0), asymmetricBit, pidMapper, commonSender, 
                clientId, Constants.prime, pid, new LinkedList<>(protocolIdQueue), partyCount);
        Future<Integer> eqTask = es.submit(eq);
        pid++;
        int eqResult = 0;
        try {
            eqResult = eqTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(ID3.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //TODO another stopping criteria and return class label
        
        //U computation starts.......................................
        List<Integer[]> U = new ArrayList<>();
        List<Future<Integer[]>> UtaskList = new ArrayList<>();
        
        for(int i=0;i<classLabelCount;i++) {
            //TODO - regulate the batch size and call in iterations
            BatchMultiplicationInteger batchMult = new BatchMultiplicationInteger(Arrays.asList(subsetTransactionsBitVector),
                    Arrays.asList(classValueBitVector.get(i)), decimalTiShares,
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, Constants.prime, pid, asymmetricBit, modelProtocolId, partyCount);
            pid++;    
                   
            Future<Integer[]> batchMultTask = es.submit(batchMult);
            UtaskList.add(batchMultTask);
        }
        pid+=classLabelCount;
        
        for (int i = 0; i < classLabelCount; i++) {
            Future<Integer[]> taskResponse = UtaskList.get(i);
            try {
                U.add(taskResponse.get());
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }        
        }
        //U computation ends.......................................
        
        //Computing Gini Gain and argmax
        Integer[][][] X = new Integer[attrCount][classLabelCount][attrValueCount]; //xij for each attribute
        Integer[][][] X2 = new Integer[attrCount][classLabelCount][attrValueCount]; //x^2
        Integer[][] Y = new Integer[attrCount][attrValueCount]; //yj for each attribute
        Integer[] Denominator = new Integer[attrCount]; //denominator for all attributes
        List<Future<Integer>> xTasks = new ArrayList<>();
        
        for(int k=0;k<attrCount;k++) { //For each attribute
            for(int j=0;j<attrValueCount;j++) { //For each possible value attribute k can take
                xTasks.clear();
                Y[k][j] = 0;
                for(int i=0;i<classLabelCount;i++) { //For each class value
                    //compute xij
                    DotProductByte dp = new DotProductByte(Arrays.asList(U.get(i)), 
                        Arrays.asList(attrValueBitVector.get(k).get(j)), binaryTiShares, 
                        pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                        clientId, Constants.binaryPrime, pid, asymmetricBit, partyCount);
                    Future<Integer> dpTask = es.submit(dp);
                    xTasks.add(dpTask);
                    
                }
                pid+=classLabelCount;
                //compute yj
                for(int i=0;i<classLabelCount;i++) {
                    Future<Integer> dpTask = xTasks.get(i);
                    try {
                        X[k][i][j] = dpTask.get();
                        Y[k][j] += X[k][i][j];
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(ID3.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                // TODO - verify if floor mod on prime is needed here
                Y[k][j] = Math.floorMod(alpha*Y[k][j] + 1, Constants.prime);
            }
            //compute Gk
            
            //Compute x^2
            List<Future<Integer[]>> batchMultTasks = new ArrayList<>();
            for(int i=0;i<classLabelCount;i++) {
                BatchMultiplicationInteger batchMult = new BatchMultiplicationInteger(Arrays.asList(X[k][i]), 
                        Arrays.asList(X[k][i]), decimalTiShares, pidMapper, commonSender, 
                        new LinkedList<>(protocolIdQueue), 
                        clientId, Constants.prime, pid, asymmetricBit, modelProtocolId, partyCount);
                Future<Integer[]> bmTask = es.submit(batchMult);
                batchMultTasks.add(bmTask);
            }
            pid+=classLabelCount;
            for(int i=0;i<classLabelCount;i++) {
                Future<Integer[]> bmTask = batchMultTasks.get(i);
                try {
                    X2[k][i] = bmTask.get();
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(ID3.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            Integer numerator = 0;
            for(int i=0;i<classLabelCount;i++) {
                for(int j=0;j<attrValueCount;j++) {
                    numerator += X2[k][i][j];
                }
            }
            
            Integer YProd = Y[k][0];
            for(int j=1;j<attrValueCount;j++) {
                MultiplicationInteger mult = new MultiplicationInteger(YProd, Y[k][j], decimalTiShares.get(0), 
                        pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                        clientId, Constants.prime, pid, asymmetricBit, 0, partyCount);
                pid++;
                Future<Integer> multTask = es.submit(mult);
                try {
                    YProd = multTask.get();
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(ID3.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            Integer denominator = YProd;
           
        }
        
        //do argmax of Gk and return
        return 0;
    }
       
}