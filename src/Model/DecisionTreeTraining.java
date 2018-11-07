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
import Protocol.Utility.BatchMultiplicationByte;
import Protocol.Utility.BatchMultiplicationInteger;
import Protocol.Utility.CompareAndConvertField;
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
public class DecisionTreeTraining extends Model {
    
    //classLabelCount - total no. of class labels, attributeCount - total no. of attributes
    //datasetSize - total no. of rows in the DB, levelCounter - the counter to decrement no. of attributes left
    //attrValueCount - total no. of attribute values per attribute (make it constant by adding dummy variable)
    //For now, input accordingly, TODO -  how to manage this dummy value addition??
    int classLabelCount, attributeCount, datasetSize, levelCounter, attrValueCount;
    int[][] dataset;    //the dataset
    //int attrLabelCounts[]; //Mapping between attr index to no. of attr values for each attr (may or may not be needed
    
    Integer[] classIndexLabelMapping;
    Integer[] subsetTransactionsBitVector; //dataset currently considered
    Integer[] attributeBitVector;
    
    List<Integer> equalityTiShares;
    List<TripleByte> binaryTiShares;
    List<TripleInteger> decimalTiShares;
    
    int alpha; //the corrective factor to multiply (value is 8 in the paper)
    
    //probably shared adhering to asymmetricBit rules??
    List<List<List<Integer>>> attrValueBitVector; //list(k attr) of list(j values) of arrays[bit representation of ak,j] Sk,j - each Integer array - bit vector
    List<List<List<Integer>>> attrValueBitVectorDecimal;
    List<List<Integer>> classValueBitVector; //same as the above but for class values
    List<List<Integer>> classValueBitVectorDecimal; //same as the above but for class values in decimal field
    
    int pid, binaryTiIndex, decimalTiIndex, equalityTiIndex; // a global ID series - TODO
    int bitDTiCount, comparisonTiCount, bitLength, prime;
    
    /**
     * 
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param binaryTriples
     * @param decimalTriple
     * @param equalityShares
     * @param classLabelCount
     * @param attrCount
     * @param attrValueCount
     * @param datasetSize
     * @param dataset
     * @param attrValues
     * @param classLabels
     * @param classValues
     * @param partyCount
     * @param protocolIdQueue
     * @param protocolID 
     */
    public DecisionTreeTraining(int asymmetricBit, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue, int clientId, List<TripleByte> binaryTriples, 
            List<TripleInteger> decimalTriple, List<Integer> equalityShares, int classLabelCount, 
            int attrCount, int attrValueCount, int datasetSize, int[][] dataset, List<List<List<Integer>>> attrValues, 
            Integer[] classLabels, List<List<Integer>> classValues, int partyCount,
            Queue<Integer> protocolIdQueue, int protocolID) {
        
        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);
        
        this.attributeCount = attrCount;
        this.dataset = dataset;
        this.datasetSize = datasetSize;
        
        this.attrValueBitVector = attrValues;
        this.attrValueCount = attrValues.get(0).size();
        
        //classIndexLabelMapping = classLabels;
        
        this.classValueBitVector = classValues;
        this.classLabelCount = classValues.size();
        
        this.decimalTiShares = decimalTriple;
        this.binaryTiShares = binaryTriples;
        this.equalityTiShares = equalityShares;
        
        this.levelCounter = attrCount-1;
        this.alpha = 8;
        pid = 0;
        decimalTiIndex = 0;
        binaryTiIndex = 0;
        equalityTiIndex = 0;
        bitLength = (int) Math.ceil(Math.log(datasetSize)/Math.log(2));
        prime = (int) Math.pow(2, bitLength);
        bitDTiCount = bitLength * 3 - 2;
        comparisonTiCount = (2 * bitLength) + ((bitLength * (bitLength - 1)) / 2);
        
    }
    
    /**
     * 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    void init() throws InterruptedException, ExecutionException {
        //Initialize the set of input transactions to asymmetricBit
        subsetTransactionsBitVector = new Integer[datasetSize];
        for(int i=0;i<datasetSize;i++){
            subsetTransactionsBitVector[i] = asymmetricBit;
        }
        
        //Initially all attributes are available
        attributeBitVector = new Integer[attributeCount];
        for(int i=0;i<attributeCount;i++) {
            attributeBitVector[i] = asymmetricBit;
        }
        
        for(int i=0;i<classLabelCount;i++){
            classValueBitVectorDecimal.add(Arrays.asList(CompareAndConvertField.changeBinaryToDecimalField(
                    classValueBitVector.get(i), decimalTiShares,
                    pid, pidMapper, commonSender, protocolIdQueue, asymmetricBit,
                    clientId, prime, partyCount)));
            pid++;
            // TODO - decimalTiShares increment
        }
        
        // TODO - handle decimal ti shares sublist and increment
        for(int i=0;i<attributeCount;i++) {
            attrValueBitVectorDecimal.add(new ArrayList());
            for(int j=0;j<attrValueCount;j++) {
                attrValueBitVectorDecimal.get(i).add(Arrays.asList(CompareAndConvertField.changeBinaryToDecimalField(
                        attrValueBitVector.get(i).get(j), decimalTiShares, pid, pidMapper,
                        commonSender, protocolIdQueue, asymmetricBit, clientId, prime, partyCount)));
                pid++;
            }
        }
        
    }
    
    /**
     * Find the most common class label in subsetTransactions
     * Returns one hot encoding share of majority class label
     * @param subsetTransactions
     * @return 
     */
    Integer[] findCommonClassIndex(Integer[] subsetTransactions) throws InterruptedException, ExecutionException {
        
        int[] s = new int[classLabelCount];
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> DPtaskList = new ArrayList<>();
        
        //subsetTransaction is over field 2, change to prime before doing argmax
        Integer[] subsetTransactionsDecimal = CompareAndConvertField.changeBinaryToDecimalField(
                Arrays.asList(subsetTransactions), decimalTiShares, pid, pidMapper,
                commonSender, protocolIdQueue, asymmetricBit, clientId, prime, partyCount);
        pid++;
        
        // for each class label do a Dot Product with the input transactions
        // outputs shares of number of transactions holding every class label
        // TODO - handle TI Shares
        for(int i=0;i<classLabelCount;i++) {
            
            DotProductInteger dp = new DotProductInteger(Arrays.asList(subsetTransactionsDecimal), 
                    classValueBitVectorDecimal.get(i), decimalTiShares, pidMapper, 
                    commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
                    pid, asymmetricBit, partyCount);
            
            Future<Integer> dpresult = es.submit(dp);
            pid++;
            DPtaskList.add(dpresult);
        }
        
        for(int i=0;i<classLabelCount;i++){
            Future<Integer> dpresult = DPtaskList.get(i);
            s[i] = dpresult.get();
        }
        
        // convert the above results to bits so that we can do ArgMax
        List<Future<List<Integer>>> BDtaskList = new ArrayList<>();
        
        // TODO handle tishares sublist
        for(int i=0;i<classLabelCount;i++) {
            BitDecomposition bitD = new BitDecomposition(s[i], binaryTiShares, 
                    asymmetricBit, bitLength, pidMapper, commonSender, 
                    new LinkedList<>(protocolIdQueue), clientId, 
                    Constants.BINARY_PRIME, pid, partyCount);
            pid++;
            Future<List<Integer>> task = es.submit(bitD);
            BDtaskList.add(task);
        }
        
        List<List<Integer>> bitSharesS = new ArrayList<>();
        for (int i = 0; i < classLabelCount; i++) {
            Future<List<Integer>> taskResponse = BDtaskList.get(i);
            bitSharesS.add(taskResponse.get());        
        }
        
        //compute argmax - will hold one hot encoding of majority class label
        // TODO - handle tishares
        ArgMax argmax = new ArgMax(bitSharesS, binaryTiShares, asymmetricBit,
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, Constants.BINARY_PRIME, pid, partyCount);
        pid++;
        
        Integer[] commonClassLabel = argmax.call();
        
        /*
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
        }*/
        
        return commonClassLabel;
        
    }
    
    public void trainDecisionTree() throws InterruptedException, ExecutionException {
        init();
        
        ID3_Model(subsetTransactionsBitVector, attributeBitVector, levelCounter);
    }
    
    
    Integer[] ID3_Model(Integer[] transactions, Integer[] attributes, int r) 
            throws InterruptedException, ExecutionException {
        
        //Finding the most common class Label
        Integer[] majorityClassIndex = findCommonClassIndex(transactions);
        
        //Check the stopping criteria
        // 1. no more attributes left, just return i_star label
        if(r==0) {
            return majorityClassIndex;
        }
        
        // 2. |T| <= e|T'| or 3. Si* = |T|
        //Step 1: convert fields of argmax output
        //Step 2: do Dot Product
        
        Integer[] majorityClassIndexDecimal = CompareAndConvertField.changeBinaryToDecimalField(
                Arrays.asList(majorityClassIndex), decimalTiShares, pid, pidMapper, commonSender,
                protocolIdQueue, asymmetricBit, clientId, prime, partyCount);
        pid++;
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> dpTasks = new ArrayList<>();
        for(int i=0;i<classLabelCount;i++){
            DotProductInteger dpModule = new DotProductInteger(Collections.nCopies(datasetSize, majorityClassIndexDecimal[i]),
                    classValueBitVectorDecimal.get(i), decimalTiShares, pidMapper,
                    commonSender, new LinkedList<>(protocolIdQueue), clientId,
                    prime, pid, asymmetricBit, partyCount);
            pid++;
            dpTasks.add(es.submit(dpModule));
        }
        
        int majorityClassTransactionCount = 0;
        for(int i=0;i<classLabelCount;i++) {
            Future<Integer> dpResult = dpTasks.get(i);
            majorityClassTransactionCount += dpResult.get();
        }
        
        // do a security equality between the two numbers
        int transactionCount = 0;
        for(int i=0;i<datasetSize;i++) {
            transactionCount += transactions[i];
        }
        
        Equality eq = new Equality(transactionCount, majorityClassTransactionCount,
                equalityTiShares.get(0), decimalTiShares.get(0), asymmetricBit,
                pidMapper, commonSender, clientId, prime, pid,
                new LinkedList<>(protocolIdQueue), partyCount);
        pid++;
        int eqResult = eq.call();
        
        //TODO another stopping criteria and return class label after doing or between the 2
        
        
        
        
        
        
        // Find the best splitting attribute based on the GINI Gain
        // Step 1. Get the set of transactions with each class label (U)
        
        List<List<Integer>> UDecimal = new ArrayList<>();
        List<Future<Integer[]>> UtaskList = new ArrayList<>();
        
        for(int i=0;i<classLabelCount;i++) {
            //TODO - regulate the batch size and call in iterations, handle Ti Shares
            BatchMultiplicationByte batchMult = new BatchMultiplicationByte(
                    Arrays.asList(transactions), classValueBitVector.get(i), binaryTiShares,
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, Constants.BINARY_PRIME, pid, asymmetricBit, modelProtocolId, partyCount);
            pid++;    
            UtaskList.add(es.submit(batchMult));
        }
        pid+=classLabelCount;
        // We use U only in Dot products in the future, which means we need to do a field conversion now
        for (int i = 0; i < classLabelCount; i++) {
            Future<Integer[]> taskResponse = UtaskList.get(i);
            UDecimal.add(Arrays.asList(CompareAndConvertField.changeBinaryToDecimalField(
                    Arrays.asList(taskResponse.get()), decimalTiShares, pid, pidMapper,
                    commonSender, protocolIdQueue, asymmetricBit, clientId, prime, partyCount)));      
            pid++;
        }
        
        //U computation ends.......................................
        
        //Computing Gini Gain and argmax for every attribute k
        Integer[][][] X = new Integer[attributeCount][classLabelCount][attrValueCount]; //xij for each attribute k*i*j
        Integer[][][] X2 = new Integer[attributeCount][classLabelCount][attrValueCount]; //x^2 of the x above
        Integer[][] Y = new Integer[attributeCount][attrValueCount]; //yj for each attribute k*j
        Integer[] Denominator = new Integer[attributeCount]; //denominator for all attributes k
        
        List<Future<Integer>> xTasks = new ArrayList<>();
        
        for(int k=0;k<attributeCount;k++) { //For each attribute
            for(int j=0;j<attrValueCount;j++) { //For each possible value attribute k can take
                xTasks.clear();
                Y[k][j] = 0;
                for(int i=0;i<classLabelCount;i++) { //For each class value
                    //compute xij
                    DotProductInteger dp = new DotProductInteger(UDecimal.get(i), 
                        attrValueBitVectorDecimal.get(k).get(j), decimalTiShares, 
                        pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                        clientId, prime, pid, asymmetricBit, partyCount);
                    pid++;
                    Future<Integer> dpTask = es.submit(dp);
                    xTasks.add(dpTask);
                }

                //compute yj
                for(int i=0;i<classLabelCount;i++) {
                    Future<Integer> dpTask = xTasks.get(i);
                    X[k][i][j] = dpTask.get();
                    Y[k][j] += X[k][i][j];
                }
                
                // TODO - verify if floor mod on prime is needed here what if value becomes 0 after floormod
                Y[k][j] = Math.floorMod(alpha*Y[k][j] + 1, prime);
            }
            //compute Gk
            
            //Compute x^2
            List<Future<Integer[]>> batchMultTasks = new ArrayList<>();
            for(int i=0;i<classLabelCount;i++) {
                BatchMultiplicationInteger batchMult = new BatchMultiplicationInteger(Arrays.asList(X[k][i]), 
                        Arrays.asList(X[k][i]), decimalTiShares, pidMapper, commonSender, 
                        new LinkedList<>(protocolIdQueue), 
                        clientId, prime, pid, asymmetricBit, modelProtocolId, partyCount);
                pid++;
                Future<Integer[]> bmTask = es.submit(batchMult);
                batchMultTasks.add(bmTask);
            }
            
            for(int i=0;i<classLabelCount;i++) {
                Future<Integer[]> bmTask = batchMultTasks.get(i);
                X2[k][i] = bmTask.get();
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
                        clientId, prime, pid, asymmetricBit, 0, partyCount);
                pid++;
                YProd = mult.call();
            }
            
            Integer denominator = YProd;
            
            //keep track of max gini gain and the corresponding k???
           
        }
        
        //do argmax of Gk and return
        return null;
    }
       
}