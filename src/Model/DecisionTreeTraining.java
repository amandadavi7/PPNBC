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

    //datasetSize - total no. of rows in the DB, levelCounter - the counter to decrement no. of attributes left
    //classLabelCount - total no. of class labels, attributeCount - total no. of attributes
    //attrValueCount - total no. of attribute values per attribute (make it constant by adding dummy variable)
    //For now, input accordingly, TODO -  how to manage this dummy value addition??
    int classLabelCount, attributeCount, datasetSize, levelCounter, attrValueCount;
    int[][] dataset;    //the dataset (DO WE NEED IT??)(REMOVE IF NOT USED)
    //int attrLabelCounts[]; //Mapping between attr index to no. of attr values for each attr (may or may not be needed
    
    Integer[] classIndexLabelMapping;
    Integer[] subsetTransactionsBitVector; //dataset currently considered
    Integer[] attributeBitVector;
    
    List<Integer> equalityTiShares;
    List<TripleByte> binaryTiShares;
    List<TripleInteger> decimalTiShares;
    
    int alpha; //the corrective factor to multiply (value is 8 in the paper)
    double epsilon; // DAVIS
    int cutoffTransactionSetSize; // DAVIS

    //probably shared adhering to asymmetricBit rules??
    List<List<List<Integer>>> attributeValueTransactionVector; //list(k attr) of list(j values) of arrays[bit representation of ak,j] Sk,j - each Integer array - bit vector
    List<List<List<Integer>>> attributeValueTransactionVectorDecimal;
    List<List<Integer>> classValueTransactionVector; //same as the above but for class values
    List<List<Integer>> classValueTransactionVectorDecimal; //same as the above but for class values in decimal field
    
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
        
        this.attributeValueTransactionVector = attrValues;
        this.attrValueCount = attrValues.get(0).size();
        
        //classIndexLabelMapping = classLabels;
        
        this.classValueTransactionVector = classValues;
        this.classLabelCount = classValues.size();
        
        this.decimalTiShares = decimalTriple;
        this.binaryTiShares = binaryTriples;
        this.equalityTiShares = equalityShares;
        
        this.levelCounter = attrCount-1;
        this.alpha = 8;

        this.epsilon = 0.1;                                             // DAVIS: Testing value for epsilon
        this.cutoffTransactionSetSize = (int) (epsilon * (double)datasetSize);    // precomputes cutoff size to use for comparison base case

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
            subsetTransactionsBitVector[i] = asymmetricBit;         // initialize set of transactions to be held only by the asymmetric bit holder
        }
        
        //Initially all attributes are available
        attributeBitVector = new Integer[attributeCount];
        for(int i=0;i<attributeCount;i++) {
            attributeBitVector[i] = asymmetricBit;          // initialize set of attributes to be full and held only by the asymmetric bit holder
        }
                                                                                // classValueTransactionVector is the set of S_o,j
        for(int i=0;i<classLabelCount;i++){                                     // generates decimal vectors for classValueTransactionVector
            classValueTransactionVectorDecimal.add(Arrays.asList(
                    CompareAndConvertField.changeBinaryToDecimalField(classValueTransactionVector.get(i),
                            decimalTiShares, pid, pidMapper, commonSender, protocolIdQueue,
                            asymmetricBit, clientId, prime, partyCount)));
            pid++;                                                                 // a protocol has been performed, so increment pid
            // TODO - decimalTiShares increment
        }
        
        // TODO - handle decimal ti shares sublist and increment
        for(int i=0;i<attributeCount;i++) {                                     // generates decimal vectors for attributeValueTransactionVector
            attributeValueTransactionVectorDecimal.add(new ArrayList());        // attributeValueTransactionVector is the set of S_k,j
            for(int j=0;j<attrValueCount;j++) {
                attributeValueTransactionVectorDecimal.get(i).add(Arrays.asList(
                        CompareAndConvertField.changeBinaryToDecimalField(attributeValueTransactionVector.get(i).get(j),
                                decimalTiShares, pid, pidMapper, commonSender, protocolIdQueue,
                                asymmetricBit, clientId, prime, partyCount)));
                pid++;                                                          // a protocol has been performed, so increment pid
            }
        }
        
    }
    
    /**
     * Find the most common class label in subsetTransactions       This method performs lines 1-3 of SID3(T,R), de'Hoog
     * Returns one hot encoding share of majority class label
     * @param subsetTransactions
     * @return 
     */
    Integer[] findCommonClassIndex(Integer[] subsetTransactions) throws InterruptedException, ExecutionException {
        
        int[] s = new int[classLabelCount];                                                         // array of possible most common class labels
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> DPtaskList = new ArrayList<>();                                       // hold returns of all dot products computed next. will have same length as classLabelCount
        
        //subsetTransaction is over field 2, change to prime before doing argmax
        Integer[] subsetTransactionsDecimal = CompareAndConvertField.changeBinaryToDecimalField(    // converts bit vectors to decimal field
                Arrays.asList(subsetTransactions), decimalTiShares, pid, pidMapper,
                commonSender, protocolIdQueue, asymmetricBit, clientId, prime, partyCount);
        pid++;                                                                                      // one protocol has been performed, so increment PID
        
        // for each class label do a Dot Product with the input transactions
        // outputs shares of number of transactions holding every class label
        // TODO - handle TI Shares
        for(int i=0;i<classLabelCount;i++) {                                                        // for every i-th class value
            
            DotProductInteger dp = new DotProductInteger(Arrays.asList(subsetTransactionsDecimal),  // set up instance of callable to perform a dot product
                    classValueTransactionVectorDecimal.get(i), decimalTiShares, pidMapper,          // between (decimal) transaction vector and S_0,i
                    commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
                    pid, asymmetricBit, partyCount);
            
            Future<Integer> dpresult = es.submit(dp);                                               // call dot product and store result in dpresult
            pid++;                                                                                  // one protocol has been performed, so increment PID
            DPtaskList.add(dpresult);                                                               // store dpresult in DPtaskList.
        }                                                                                           // DPtaskList now contains all dot products for every i-th class value, indexed the same way
        
        for(int i=0;i<classLabelCount;i++){                     // when DPTask list is populated,
            Future<Integer> dpresult = DPtaskList.get(i);       // move results to array s[]. get() is blocking
            s[i] = dpresult.get();
        }
        
        // convert the above results to bits so that we can do ArgMax
        List<Future<List<Integer>>> BDtaskList = new ArrayList<>();     // will hold the results of callable BitComposition protocols
        
        // TODO handle tishares sublist
        for(int i=0;i<classLabelCount;i++) {                                        // for every value in s[]
            BitDecomposition bitD = new BitDecomposition(s[i], binaryTiShares,      // create a new instance of callable BitComposition protocol
                    asymmetricBit, bitLength, pidMapper, commonSender, 
                    new LinkedList<>(protocolIdQueue), clientId, 
                    Constants.BINARY_PRIME, pid, partyCount);
            pid++;                                                  // a protocol has been performed, so increment pid
            Future<List<Integer>> task = es.submit(bitD);       // pass instance of callable to executor service to schedule
            BDtaskList.add(task);                               // add future return of callable to BDtaskList
        }
        
        List<List<Integer>> bitSharesS = new ArrayList<>();         // wait for all bit decomposition computations to complete and copy their results
        for (int i = 0; i < classLabelCount; i++) {                 // into bitSharesS - a list of bit decomposed dot products between |T| and |S_o,i|
            Future<List<Integer>> taskResponse = BDtaskList.get(i);
            bitSharesS.add(taskResponse.get());        
        }
        
        //compute argmax - will hold one hot encoding of majority class label
        // TODO - handle tishares
        ArgMax argmax = new ArgMax(bitSharesS, binaryTiShares, asymmetricBit,   // create a new argmax protocol instance for bitSharesS
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, Constants.BINARY_PRIME, pid, partyCount);
        pid++;                                                                  // a protocol has been performed, so increment pid
        
        Integer[] commonClassLabel = argmax.call();                              // we now have the one hot encoding of majority class label
        
        return commonClassLabel;                                                 // return this value to ID3Model
        
    }

    /**
     * recursive helper function -- starts ID3Model with entire transaction set (bit array of all 1's), attribute
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void trainDecisionTree() throws InterruptedException, ExecutionException {
        init();
        ArrayList<Integer> decisionTreeNodes = new ArrayList<>();

        /*        all 1's of asymmetric bit party and all 0's for other parties                  */
        /*                   T                    R              decrements remaining attributes (cardinality of R)*/
        ID3Model(subsetTransactionsBitVector, attributeBitVector, levelCounter ,decisionTreeNodes);
    }
    
    
    void ID3Model(Integer[] transactions, Integer[] attributes, int r, ArrayList<Integer> DTnodes)
            throws InterruptedException, ExecutionException {
        
        //Finding the most common class Label
        Integer[] majorityClassIndex = findCommonClassIndex(transactions);


        Integer majorityIndexAsInt = 0;
        for(int i=0; i<majorityClassIndex.length; i++) {
            if(majorityClassIndex[i]==1) {
                majorityIndexAsInt = i;
                break;
            }
        }

        //Check the stopping criteria
        // 1. no more attributes left, just return i_star label
        if(r==0) {
            DTnodes.add(majorityIndexAsInt);
            return;
        }
        
        // 2. |T| <= e|T'| or 3. Si* = |T|
        //Step 1: convert fields of argmax output
        //Step 2: do Dot Product
        
        Integer[] majorityClassIndexDecimal = CompareAndConvertField.changeBinaryToDecimalField(    // takes the majority class index from binary representation to decimal
                Arrays.asList(majorityClassIndex), decimalTiShares, pid, pidMapper, commonSender,
                protocolIdQueue, asymmetricBit, clientId, prime, partyCount);
        pid++;                                                                                      //  a protocol has been performed, so increment pid


        // gets s_i* -- the cardinality of the subset of the current subset of transactions with class value i*.
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> dpTasks = new ArrayList<>();                              // will contain reseult of each ith dot product between one-hot encoding of i* and S_o,i
        for(int i=0;i<classLabelCount;i++){
            DotProductInteger dpModule = new DotProductInteger(Collections.nCopies(datasetSize, majorityClassIndexDecimal[i]), // computes the ith dot product between one-hot encoding of i* and S_o,i
                    classValueTransactionVectorDecimal.get(i), decimalTiShares, pidMapper,
                    commonSender, new LinkedList<>(protocolIdQueue), clientId,
                    prime, pid, asymmetricBit, partyCount);
            pid++;                                                                                                             //  a protocol has been performed, so increment pid
            dpTasks.add(es.submit(dpModule));       // hand callable dot product instance to executor service for scheduling
        }
        
        int majorityClassTransactionCount = 0;                  // once all dot products have been performed,
        for(int i=0;i<classLabelCount;i++) {
            Future<Integer> dpResult = dpTasks.get(i);          // sum them up into an int
            majorityClassTransactionCount += dpResult.get();
        }                                                        // AT THIS POINT, we now have s_i*
        
        // do a security equality between the two numbers
        int transactionCount = 0;                               // calculates |T|
        for(int i=0;i<datasetSize;i++) {
            transactionCount += transactions[i];
        }                                                          // AT THIS POINT, we now have |T|

        // determines if s_i* = |T| -- the second base case condition
        Equality eq = new Equality(transactionCount, majorityClassTransactionCount,     // create callable equality test instance between s_i* and |T|
                equalityTiShares.get(0), decimalTiShares.get(0), asymmetricBit,
                pidMapper, commonSender, clientId, prime, pid,
                new LinkedList<>(protocolIdQueue), partyCount);
        pid++;                                                                          //  a protocol has been performed, so increment pid
        int eqResult = eq.call(); /*change to complement*/                              // performs equality check (non-zero if equal, 0 if not equal)



        // TENTATIVELY COMPLETE:TODO another stopping criteria and return class label after doing or between the 2
        // Comparison     // |T| =< e|t|
        // int compResult = 0;                                                              // using the place holder 0 will cause the following multiplication to assume the criteria has failed
                                                                                            // (basically doesn't exist) but without breaking anything

        // DAVIS: compute comparison between current transaction count and cutoffTransactionSetSize
        int compResult = CompareAndConvertField.compareIntegers(cutoffTransactionSetSize, transactionCount,
                                binaryTiShares, asymmetricBit, pidMapper, commonSender,
                                new LinkedList<>(protocolIdQueue), clientId,
                                Constants.BINARY_PRIME, 2, partyCount,
                                pid,false, decimalTiShares);
        pid++;

        // DAVIS: Is this still needed if we convert and compare in the same function call? Why an array?
        Integer[] compResultDec = CompareAndConvertField.changeBinaryToDecimalField(Arrays.asList(compResult),
                decimalTiShares, pid, pidMapper, commonSender, protocolIdQueue,
                asymmetricBit, clientId, prime, partyCount);
        pid++;


        // Reveal the output of the product to both sides and if product is 0, exit with leaf node
        // Doing AND of converse instead of doing an OR (check again)
        
        MultiplicationInteger mult = new MultiplicationInteger(eqResult, compResult,        // performs multiplication (AND) between equality result and comparison result
                decimalTiShares.get(decimalTiIndex), pidMapper, commonSender,               // DAVIS: Inputs need to be inverted for this to work properly, I think.
                new LinkedList<>(protocolIdQueue), clientId, prime, pid, asymmetricBit,
                modelProtocolId, partyCount);
        pid++;
        int stoppingBit = mult.call();                                                       // multiplication result saved in stopping bit
        
        //share the results with each other
        Message senderMessage = new Message(stoppingBit, clientId, protocolIdQueue);         // broadcast first component
        int stoppingBit2 = 0;
        commonSender.put(senderMessage);
        Message receivedMessage = pidMapper.get(protocolIdQueue).take();
        stoppingBit2 = (int) receivedMessage.getValue();                                      // receive other component
        if(Math.floorMod(stoppingBit+stoppingBit2,prime)==0){                                 // if the sum of both bits is zero
            DTnodes.add(majorityIndexAsInt);                                                        // one or both tests passed, so exit recursion
            return;
        }
        
        // Find the best splitting attribute based on the GINI Gain
        // Step 1. Get the set of transactions with each class label (U)
        
        List<List<Integer>> UDecimal = new ArrayList<>();
        List<Future<Integer[]>> UtaskList = new ArrayList<>();
        
        for(int i=0;i<classLabelCount;i++) {
            //TODO - regulate the batch size and call in iterations, handle Ti Shares
            BatchMultiplicationByte batchMult = new BatchMultiplicationByte(
                    Arrays.asList(transactions), classValueTransactionVector.get(i), binaryTiShares,
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
        List<Integer[]> giniGains = new ArrayList<>(); // will hold k [ num, denom ] pairs

        List<Future<Integer>> xTasks = new ArrayList<>();
        
        for(int k=0;k<attributeCount;k++) { //For each attribute
            if(attributes[k] != 0) { // we can do this because attributes is public
                for (int j = 0; j < attrValueCount; j++) { //For each possible value attribute k can take
                    xTasks.clear();
                    Y[k][j] = 0;
                    for (int i = 0; i < classLabelCount; i++) { //For each class value
                        //compute xij
                        DotProductInteger dp = new DotProductInteger(UDecimal.get(i),
                                attributeValueTransactionVectorDecimal.get(k).get(j), decimalTiShares,
                                pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                                clientId, prime, pid, asymmetricBit, partyCount);
                        pid++;
                        Future<Integer> dpTask = es.submit(dp);
                        xTasks.add(dpTask);
                    }

                    //compute yj
                    for (int i = 0; i < classLabelCount; i++) {
                        Future<Integer> dpTask = xTasks.get(i);
                        X[k][i][j] = dpTask.get();
                        Y[k][j] += X[k][i][j];
                    }

                    // TODO - verify if floor mod on prime is needed here what if value becomes 0 after floormod
                    Y[k][j] = Math.floorMod(alpha * Y[k][j] + 1, prime);    // according to the paper, the prime must be large enough to where this is not possible
                }
                //compute Gk

                //Compute x^2
                List<Future<Integer[]>> batchMultTasks = new ArrayList<>();
                for (int i = 0; i < classLabelCount; i++) {
                    BatchMultiplicationInteger batchMult = new BatchMultiplicationInteger(Arrays.asList(X[k][i]),
                            Arrays.asList(X[k][i]), decimalTiShares, pidMapper, commonSender,
                            new LinkedList<>(protocolIdQueue),
                            clientId, prime, pid, asymmetricBit, modelProtocolId, partyCount);
                    pid++;
                    Future<Integer[]> bmTask = es.submit(batchMult);
                    batchMultTasks.add(bmTask);
                }

                for (int i = 0; i < classLabelCount; i++) {
                    Future<Integer[]> bmTask = batchMultTasks.get(i);
                    X2[k][i] = bmTask.get();
                }

                Integer numerator = 0;
                Integer xSquaredij = 0;
                for (int j = 0; j < attrValueCount; j++) {
                    xSquaredij = 0;
                    for (int i = 0; i < classLabelCount; i++) {
                        xSquaredij += X2[k][i][j];
                    }

                    // find product of yk w/o yj
                    Integer YProdWithoutJ = j == 0 ? Y[k][1] : Y[k][0];
                    for (int l = 0; l < attrValueCount; l++) {
                        if (l != j) {
                            mult = new MultiplicationInteger(YProdWithoutJ, Y[k][l], decimalTiShares.get(0),
                                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                                    clientId, prime, pid, asymmetricBit, 0, partyCount);
                            pid++;
                            YProdWithoutJ = mult.call();
                        }
                    }

                    // multiply that product with xij^2
                    mult = new MultiplicationInteger(YProdWithoutJ, xSquaredij, decimalTiShares.get(0),
                            pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                            clientId, prime, pid, asymmetricBit, 0, partyCount);
                    pid++;
                    numerator += mult.call();
                }
                // AT THIS POINT: We have the numerator
                Integer YProd = Y[k][0];
                for (int j = 1; j < attrValueCount; j++) {
                    mult = new MultiplicationInteger(YProd, Y[k][j], decimalTiShares.get(0),
                            pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                            clientId, prime, pid, asymmetricBit, 0, partyCount);
                    pid++;
                    YProd = mult.call();
                }

                // AT THIS POINT: We have the denominator

                Integer denominator = YProd;                    // save num, denom pair of kth Gini Gain into GiniGains
                Integer[] kthGini = {numerator, denominator};
                giniGains.add(kthGini);
            }

        }
        //keep track of max gini gain and the corresponding k???
        Integer[] giniMax = giniGains.get(0);
        int k_star = 0;                         // start by assuming gini gain at 0th index is max
        Integer compRes=0;

        for(int k=1; k<giniGains.size(); k++) {

            // computes dot product (x,y) . (y', -x'). If the result is less than 0, the x',y' num-denom pair is the new max gini gain
            DotProductInteger dpModule = new DotProductInteger(Arrays.asList(giniMax), Arrays.asList(giniGains.get(k)[1], -giniGains.get(k)[0]),
                     decimalTiShares, pidMapper,
                    commonSender, new LinkedList<>(protocolIdQueue), clientId,
                    prime, pid, asymmetricBit, partyCount);
            pid++;
            compRes = dpModule.call();
            if(compRes < 0) {                        // updates the current max
                giniMax[0] = giniGains.get(k)[0];
                giniMax[1] = giniGains.get(k)[1];
                k_star = k;

            }

        }


        // 1.add A-kstar to the array of children
        DTnodes.add(attributes[k_star]);

        // 3.remove A-kstar from R
        attributes[k_star] = 0;


        // 2.decrement attribute count
        r--;


        // 4.for each j, computs the vector (entrywise product) T*Skstar,j -- do this completely before recursion ???

        // 5. for each j, return T*SK*,j, R\{Ak*}

        return;
    }
       
}
