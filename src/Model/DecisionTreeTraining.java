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
import Protocol.Utility.ParallelMultiplication;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class DecisionTreeTraining extends Model {

    /* Share types needed for DT Learn */
    List<Integer> equalityTiShares;
    List<TripleByte> binaryTiShares;
    List<TripleInteger> decimalTiShares;

    /* Constants related to cutoff size TODO: should be in config file */
    int alpha;
    double epsilon;
    int cutoffTransactionSetSize;

    /* Input size parameters */
    int attrCount;
    int datasetSize;
    int attrValueCount;
    int classValueCount;

    /* Data set prime field representations */
    Integer[][][] attrValues;
    Integer[][] classValues;
    List<List<List<Integer>>> attributeValueTransactionVector;
    List<List<List<Integer>>> attributeValueTransactionVectorDecimal;
    List<List<Integer>> classValueTransactionVector;
    List<List<Integer>> classValueTransactionVectorDecimal;

    /* Recursive protocol paramters */
    int levelCounter;
    Integer[] subsetTransactionBitVector;
    Integer[] attributeBitVector;

    // a global ID series - TODO
    int pid, binaryTiIndex, decimalTiIndex, equalityTiIndex;
    int bitDTiCount, comparisonTiCount, bitLength;
    int prime;

    // TODO: replace with tree
    List<String> decisionTreeNodes;

    /**
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param binaryTriples
     * @param decimalTriple
     * @param equalityShares
     * @param args
     * @param partyCount
     * @param protocolIdQueue
     * @param protocolID
     */
    public DecisionTreeTraining(int asymmetricBit,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, int clientId,
            List<TripleByte> binaryTriples, List<TripleInteger> decimalTriple,
            List<Integer> equalityShares, String[] args, int partyCount,
            Queue<Integer> protocolIdQueue, int protocolID) {

        /* Initialization of generic Model type */
        super(pidMapper, senderQueue, clientId, asymmetricBit,
                partyCount, protocolIdQueue, protocolID);

        /* Initialization of input parameters */
        initalizeModelVariables(args);

        this.decimalTiShares = decimalTriple;
        this.binaryTiShares = binaryTriples;
        this.equalityTiShares = equalityShares;

        this.levelCounter = attrCount;

        /* TODO: initiate asymmetric bit holder from config file */
        this.alpha = 8;
        this.epsilon = 0.1;
        this.cutoffTransactionSetSize = (int) (epsilon * (double) datasetSize);

        /* Initialization of architectural indeces */
        pid = 0;
        decimalTiIndex = 0;
        binaryTiIndex = 0;
        equalityTiIndex = 0;


        /* Currently need to change prime in config file to same value as prime here */
        int baseBitLength = (int) Math.ceil(Math.log(datasetSize) / Math.log(2));
        bitLength
                = /* attrValueCount*(baseBitLength + 4)+*/ (attrValueCount - 1) * (baseBitLength + 4)
                + (int) Math.ceil(Math.log(datasetSize) / Math.log(2))
                + (int) Math.ceil(Math.log(datasetSize) / Math.log(2))
                + 2 * baseBitLength - 2; // -2 added just so int will work

        prime = (int) Math.pow(2, bitLength);

        System.out.println("Prime: " + prime);
        System.out.println("Bitlength: " + bitLength);

        /* Increment for TI Shares */
        bitDTiCount = bitLength * 3 - 2;
        comparisonTiCount = (2 * bitLength) + ((bitLength * (bitLength - 1)) / 2);

        // TODO replace with tree
        decisionTreeNodes = new ArrayList<>();
    }
    
    public DecisionTreeTraining(int asymmetricBit,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, int clientId,
            List<TripleByte> binaryTriples, List<TripleInteger> decimalTriple,
            List<Integer> equalityShares, int partyCount,
            Queue<Integer> protocolIdQueue, int protocolID, int classValueCount,
            int attrCount, int attrValueCount, int datasetSize,
            Integer[][][] attrValues,Integer[][] classValues) {

        /* Initialization of generic Model type */
        super(pidMapper, senderQueue, clientId, asymmetricBit,
                partyCount, protocolIdQueue, protocolID);

        /* Initialization of input parameters */
        this.classValueCount = classValueCount;
        this.attrCount = attrCount;
        this.attrValueCount = attrValueCount;
        this.datasetSize = datasetSize;
        this.attrValues = attrValues;
        this.classValues = classValues;
        //initalizeModelVariables(args);

        this.decimalTiShares = decimalTriple;
        this.binaryTiShares = binaryTriples;
        this.equalityTiShares = equalityShares;

        this.levelCounter = attrCount;

        /* TODO: initiate asymmetric bit holder from config file */
        this.alpha = 8;
        this.epsilon = 0.1;
        this.cutoffTransactionSetSize = (int) (epsilon * (double) datasetSize);

        /* Initialization of architectural indeces */
        pid = 0;
        decimalTiIndex = 0;
        binaryTiIndex = 0;
        equalityTiIndex = 0;


        /* Currently need to change prime in config file to same value as prime here */
        int baseBitLength = (int) Math.ceil(Math.log(datasetSize) / Math.log(2));
        bitLength
                = /* attrValueCount*(baseBitLength + 4)+*/ (attrValueCount - 1) * (baseBitLength + 4)
                + (int) Math.ceil(Math.log(datasetSize) / Math.log(2))
                + (int) Math.ceil(Math.log(datasetSize) / Math.log(2))
                + 2 * baseBitLength - 2; // -2 added just so int will work

        prime = (int) Math.pow(2, bitLength);

        System.out.println("Prime: " + prime);
        System.out.println("Bitlength: " + bitLength);

        /* Increment for TI Shares */
        bitDTiCount = bitLength * 3 - 2;
        comparisonTiCount = (2 * bitLength) + ((bitLength * (bitLength - 1)) / 2);

        // TODO replace with tree
        decisionTreeNodes = new ArrayList<>();
    }

    public void trainDecisionTree()
            throws InterruptedException, ExecutionException {

        final long startTime = System.currentTimeMillis();

        init();
        System.out.println(this);
        System.out.println("\nBeginning Model...\n");
        ID3Model(subsetTransactionBitVector, attributeBitVector, levelCounter);

        final long endTime = System.currentTimeMillis();

        System.out.println("______________________________________");
        System.out.println("AttrCnt=" + attrCount + " TransactionCnt=" + datasetSize
                + " Runtime=" + String.valueOf(endTime - startTime) + "ms");
        System.out.println("\nDecision Tree:");
        for (int i = 0; i < decisionTreeNodes.size(); i++) {
            System.out.println(decisionTreeNodes.get(i));
        }
        System.out.println("\n");

    }

    void ID3Model(Integer[] transactions, Integer[] attributes, int r)
            throws InterruptedException, ExecutionException {

        System.out.println("______________________________________");
        System.out.println("Recursion Level (public): " + r);
        System.out.print("Attribute Subset (public): ");
        for (int el : attributes) {
            System.out.print(el /*+ " "*/);
        }
        System.out.println();

        System.out.print("Transaction Subset: ");
        for (int el : transactions) {
            System.out.print(el /*+ " "*/);
        }
        System.out.println();

        System.out.println("______________________________________");

        // Get majority cass index one-hot encoding
        Integer[] majorityClassIndex = findCommonClassIndex(transactions);

        // Make majority class index one-hot encoding public
        Integer[] majorityClassIndexShared = new Integer[classValueCount];
        for (int i = 0; i < classValueCount; i++) {

            Message senderMessage = new Message(majorityClassIndex[i],
                    clientId, protocolIdQueue);
            Integer majorityClassIndex2 = 0;
            commonSender.put(senderMessage);
            Message receivedMessage = pidMapper.get(protocolIdQueue).take();
            majorityClassIndex2 = (Integer) receivedMessage.getValue();
            majorityClassIndexShared[i] = Math.floorMod(
                    majorityClassIndex[i] + majorityClassIndex2, 2);

        }

        // Find the integer version of the majority class index
        int majorityIndexAsInt = 0;
        for (int i = 0; i < classValueCount; i++) {
            if (majorityClassIndexShared[i] == 1) {
                majorityIndexAsInt = i;
                break;
            }
        }
        // Correct one-hot encoding to take only the first instance of a maximum
        for (int i = majorityIndexAsInt + 1; i < classValueCount; i++) {
            majorityClassIndexShared[i] = 0;
        }

        System.out.println("Majority Class Index (public): " + majorityIndexAsInt);
        System.out.print("Maj Index One-Hot (public): ");
        for (int el : majorityClassIndexShared) {
            System.out.print(el/* + " "*/);
        }
        System.out.println();

        // quit if no attributes left in subset
        if (r == 0) {
            System.out.println("Exited on base case: Recursion Level == 0");
            decisionTreeNodes.add("class=" + majorityIndexAsInt);
            return;
        }

        // create shares of one-hot majority index over decimal field
        Integer[] majorityClassIndexDecimal = asymmetricBit == 1
                ? CompareAndConvertField.changeBinaryToDecimalField(Arrays.asList(
                        majorityClassIndexShared), decimalTiShares, pid, pidMapper, commonSender,
                        protocolIdQueue, asymmetricBit, clientId, prime, partyCount)
                : CompareAndConvertField.changeBinaryToDecimalField(Collections.nCopies(
                        classValueCount, 0), decimalTiShares, pid, pidMapper, commonSender,
                        protocolIdQueue, asymmetricBit, clientId, prime, partyCount);
        pid++;

        // Convert transactions to decimal field
        Integer[] transactionsDecimal
                = CompareAndConvertField.changeBinaryToDecimalField(Arrays.asList(
                        transactions), decimalTiShares, pid, pidMapper, commonSender,
                        protocolIdQueue, asymmetricBit, clientId, prime, partyCount);
        pid++;

        // Generate a vector for each class value that contains:
        // - all 0's if i is not the majority class index
        // - the current subset of transactions if i is the majority class index  
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer[]>> batchMultTasks1 = new ArrayList<>();
        for (int i = 0; i < classValueCount; i++) {
            BatchMultiplicationInteger bm = new BatchMultiplicationInteger(
                    Arrays.asList(transactionsDecimal), Collections.nCopies(datasetSize, majorityClassIndexDecimal[i]),
                    decimalTiShares, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId,
                    prime, pid, asymmetricBit, modelProtocolId,
                    partyCount);
            pid++;
            batchMultTasks1.add(es.submit(bm));
        }
        List<Integer[]> majClassOfSubsetTransactions = new ArrayList<>();
        for (int i = 0; i < classValueCount; i++) {
            Future<Integer[]> bmTask1 = batchMultTasks1.get(i);
            majClassOfSubsetTransactions.add(bmTask1.get());
        }

        // Determine the number of transactions in the subset that predict the
        // majority class value
        int majorityClassTransactionCount = 0;

        List<Future<Integer>> dpTasks = new ArrayList<>();
        for (int i = 0; i < classValueCount; i++) {
            DotProductInteger dpModule = new DotProductInteger(
                    /*Collections.nCopies(datasetSize, majorityClassIndexDecimal[i])*/
                    Arrays.asList(majClassOfSubsetTransactions.get(i)),
                    classValueTransactionVectorDecimal.get(i), decimalTiShares,
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, asymmetricBit, partyCount);
            Future<Integer> dpResult = es.submit(dpModule);
            pid++;
            dpTasks.add(dpResult);
        }
        for (int i = 0; i < classValueCount; i++) {
            Future<Integer> dpResult = dpTasks.get(i);
            majorityClassTransactionCount
                    = Math.floorMod(majorityClassTransactionCount + dpResult.get(), prime);
        }

        System.out.println("Majority Class Transaction Count: "
                + majorityClassTransactionCount);

        // Convert subset of transactions to decimal field (HAS THIS ALREADY BEEN DONE?)
        Integer[] subsetTransactionsDecimal
                = CompareAndConvertField.changeBinaryToDecimalField(
                        Arrays.asList(transactions), decimalTiShares,
                        pid, pidMapper, commonSender, protocolIdQueue, asymmetricBit,
                        clientId, prime, partyCount);
        pid++;

        // Count the total number of transactions in the current subset
        int transactionCount = 0;
        DotProductInteger dpm = new DotProductInteger(
                Arrays.asList(subsetTransactionsDecimal),
                Collections.nCopies(transactions.length, asymmetricBit), decimalTiShares,
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
                pid, asymmetricBit, partyCount);
        pid++;
        transactionCount = dpm.call();

        System.out.println("Transactions in current subset: " + transactionCount);

        // Determine if the entire subset of transactions predicts the majority class
        // attribute
        Equality eq = new Equality(transactionCount,
                majorityClassTransactionCount, equalityTiShares.get(0),
                decimalTiShares.get(0), asymmetricBit, pidMapper, commonSender,
                clientId, prime, pid, new LinkedList<>(protocolIdQueue),
                partyCount);
        pid++;
        int eqResult = eq.call();

        System.out.println("MajClassTrans = SubsetTrans? (Non-zero -> not equal): "
                + eqResult);

        // Determine if cutoff transactions size has been reached TODO
        int compResult = 1; // non-zero means |T| is NOT <= e|t|

        // Determine if either base case condition has been reached with
        // NOT(NOT x AND NOT y) in place of x OR y
        MultiplicationInteger mult = new MultiplicationInteger(eqResult,
                compResult, decimalTiShares.get(decimalTiIndex), pidMapper,
                commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
                pid, asymmetricBit, modelProtocolId, partyCount);
        pid++;
        int stoppingBit = mult.call();

        // Make result public: if x OR y is true, exit on base case
        Message senderMessage = new Message(stoppingBit, clientId, protocolIdQueue);
        Integer stoppingBit2 = 0;
        commonSender.put(senderMessage);
        Message receivedMessage = pidMapper.get(protocolIdQueue).take();
        stoppingBit2 = (Integer) receivedMessage.getValue();
        if (Math.floorMod(stoppingBit + stoppingBit2, prime) == 0) {
            System.out.println("Exited on base case: All transactions predict same outcome");
            decisionTreeNodes.add("class=" + majorityIndexAsInt);
            es.shutdown();
            return;
        }
        System.out.println("Base case not reached. Continuing.");

        List<List<Integer>> UDecimal = new ArrayList<>();
        List<Future<Integer[]>> UtaskList = new ArrayList<>();

        // Generate each subset of transactions within the current subset
        // that predict the i-th class value	
        for (int i = 0; i < classValueCount; i++) {

            BatchMultiplicationByte batchMult = new BatchMultiplicationByte(
                    Arrays.asList(transactions), classValueTransactionVector.get(i),
                    binaryTiShares, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId,
                    Constants.BINARY_PRIME, pid, asymmetricBit, modelProtocolId,
                    partyCount);
            pid++;
            UtaskList.add(es.submit(batchMult));
        }
        pid += classValueCount;

        // Convert binary shares to the decimal field
        for (int i = 0; i < classValueCount; i++) {
            Future<Integer[]> taskResponse = UtaskList.get(i);
            UDecimal.add(Arrays.asList(
                    CompareAndConvertField.changeBinaryToDecimalField(
                            Arrays.asList(taskResponse.get()), decimalTiShares,
                            pid, pidMapper, commonSender, protocolIdQueue, asymmetricBit,
                            clientId, prime, partyCount)));
            pid++;
        }

        Integer[][][] X = new Integer[attrCount][classValueCount][attrValueCount];
        Integer[][][] X2 = new Integer[attrCount][classValueCount][attrValueCount];
        Integer[][] Y = new Integer[attrCount][attrValueCount];
        Integer[] giniNumerators = new Integer[attrCount];
        Integer[] giniDenominators = new Integer[attrCount];

        List<Future<Integer>> xTasks = new ArrayList<>();

        // For every attribute in the current subset
        for (int k = 0; k < attrCount; k++) {
            if (attributes[k] != 0) {
                // System.out.println("______________________________________");
                // System.out.println("Attribute ["+k+"]:");

                // For every value taken by the k-th attribute
                for (int j = 0; j < attrValueCount; j++) {
                    xTasks.clear();
                    Y[k][j] = 0;

                    // Determine the number of transactions that are:
                    // 1. in the current subset
                    // 2. predict the i-th class value
                    // 3. and have the j-th value of the k-th attribute
                    for (int i = 0; i < classValueCount; i++) {
                        DotProductInteger dp = new DotProductInteger(UDecimal.get(i),
                                attributeValueTransactionVectorDecimal.get(k).get(j),
                                decimalTiShares, pidMapper, commonSender,
                                new LinkedList<>(protocolIdQueue), clientId, prime,
                                pid, asymmetricBit, partyCount);
                        pid++;
                        Future<Integer> dpTask = es.submit(dp);
                        xTasks.add(dpTask);
                    }

                    for (int i = 0; i < classValueCount; i++) {
                        Future<Integer> dpResult = xTasks.get(i);
                        X[k][i][j] = dpResult.get();
                        // System.out.println("X["+k+"]["+j+"]["+i+"]: " + X[k][i][j]);
                        Y[k][j] = Math.floorMod(Y[k][j] + X[k][i][j], prime);
                    }
                    // System.out.println("Y["+k+"]["+j+"]:" + Y[k][j]);
                    // System.out.println();

                    // Y contains the total number of transactions in the current subset 
                    // in which the j-th value of the k-th attribute is present
                    // The max bitLength of Y is bitLength
                    // this line makes bitlength need to be bitLength + 4 (for alpha=8) 
                    Y[k][j] = Math.floorMod(
                            alpha * Y[k][j] + asymmetricBit, prime);

                }

                /* Compute X^2 for every X
			   this makes the bitLength need to be 2*bitLength */
                List<Future<Integer[]>> batchMultTasks = new ArrayList<>();
                for (int i = 0; i < classValueCount; i++) {
                    BatchMultiplicationInteger batchMult
                            = new BatchMultiplicationInteger(Arrays.asList(X[k][i]),
                                    Arrays.asList(X[k][i]), decimalTiShares, pidMapper,
                                    commonSender, new LinkedList<>(protocolIdQueue),
                                    clientId, prime, pid, asymmetricBit,
                                    modelProtocolId, partyCount);
                    pid++;
                    Future<Integer[]> bmTask = es.submit(batchMult);
                    batchMultTasks.add(bmTask);
                }
                for (int i = 0; i < classValueCount; i++) {
                    Future<Integer[]> bmTask = batchMultTasks.get(i);
                    X2[k][i] = bmTask.get();
                }

                /* Compute the product of all Y's : These are the Gini Gain denominators.
			   the max bitLength of Y can be (bitLength + 4), so bitlength 
			   should be attrValCount * (bitLength + 4)                       */
                ParallelMultiplication pMult = new ParallelMultiplication(
                        Arrays.asList(Y[k]), binaryTiShares, clientId, prime, pid,
                        asymmetricBit, pidMapper, commonSender,
                        new LinkedList<>(protocolIdQueue), partyCount);
                pid++;
                giniDenominators[k] = pMult.call();

                /* Compute the Gini Gain numerator as the product of each Y w/o j
			   multiplied by the sum over Xij^2's.
			- Y w/o J needs bitLength (attrVaCount - 1)*(bitLength + 4)
			- X^2 has bitlength of 2*bitLength, so the sum over Xij^2's needs
			  ceil[ log(attrValCount) ] + ceil[ log(classValCount) ] + 2*bitLength
			- Then, the entire Gini Numerator needs a bit length of:
				(attrValCount-1)*(bitlength + 4) + 
				ceil[ log(classValCount) ] + 
				ceil[ log(attrValCount) ] + 2*bitLength                       */
                giniNumerators[k] = 0;
                for (int j = 0; j < attrValueCount; j++) {
                    List<Integer> YwithoutJ = new ArrayList<>();
                    for (int l = 0; l < Y[k].length; l++) {
                        if (j != l) {
                            YwithoutJ.add(Y[k][l]);
                        }
                    }
                    int YprodWithoutJ = 0;
                    ParallelMultiplication pMult0 = new ParallelMultiplication(
                            YwithoutJ, binaryTiShares, clientId, prime, pid,
                            asymmetricBit, pidMapper, commonSender,
                            new LinkedList<>(protocolIdQueue), partyCount);
                    pid++;

                    YprodWithoutJ = pMult0.call();

                    Integer sumX2 = 0;
                    for (int i = 0; i < classValueCount; i++) {
                        sumX2 = Math.floorMod(sumX2 + X2[k][i][j], prime);
                    }

                    MultiplicationInteger mult0 = new MultiplicationInteger(sumX2,
                            YprodWithoutJ, decimalTiShares.get(decimalTiIndex), pidMapper,
                            commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
                            pid, asymmetricBit, modelProtocolId, partyCount);

                    pid++;

                    giniNumerators[k] = Math.floorMod(mult0.call() + giniNumerators[k], prime);
                }
            }
        }

        for (int k = 0; k < attrCount; k++) {
            System.out.println("Gini_Gain[" + k + "]: num=" + giniNumerators[k]
                    + ", denom=" + giniDenominators[k]);
        }

        int k = 0;
        while (attributes[k] == 0) {
            k++;
        }

        /* The argmax of Gini Gains over k is computed by performing pairwise
		   comparisons between the current max Gini Gain and the next Gini Gain.
		   This is done by multiplying each denominator by the other Numerator.
		   A denominator has max bitlength : attrValCount * (bitLength + 4) 
		   A numerator has max bitlength: 

				(attrValCount-1)*(bitlength + 4) + 
				ceil[ log(classValCount) ] + 
				ceil[ log(attrValCount) ] + 2*bitLength

			So the total bitLength needed is the sum of the two:

				attrValCount*(bitLength + 4) +
 				(attrValCount-1)*(bitlength + 4) + 
				ceil[ log(classValCount) ] + 
				ceil[ log(attrValCount) ] + 2*bitLength

         */
        Integer giniMaxNumerator = giniNumerators[k];
        Integer giniMaxDenominator = giniDenominators[k];
        int giniArgmax = k;

        k++;
        for (; k < giniNumerators.length; k++) {
            if (attributes[k] == 1) {

                Message senderMessage1 = new Message(giniNumerators[k],
                        clientId, protocolIdQueue);
                Integer giniNumerator2 = 0;
                commonSender.put(senderMessage1);
                Message receivedMessage1 = pidMapper.get(protocolIdQueue).take();
                giniNumerator2 = (Integer) receivedMessage1.getValue();
                giniNumerators[k] = asymmetricBit == 1 ? Math.floorMod(
                        giniNumerators[k] + giniNumerator2, prime) : 0;

                Message senderMessage2 = new Message(giniDenominators[k],
                        clientId, protocolIdQueue);
                Integer giniDenominators2 = 0;
                commonSender.put(senderMessage2);
                Message receivedMessage2 = pidMapper.get(protocolIdQueue).take();
                giniDenominators2 = (Integer) receivedMessage2.getValue();
                giniDenominators[k] = asymmetricBit == 1 ? Math.floorMod(
                        giniDenominators[k] + giniDenominators2, prime) : 0;

                Message senderMessage3 = new Message(giniMaxNumerator,
                        clientId, protocolIdQueue);
                Integer giniMaxNumerator2 = 0;
                commonSender.put(senderMessage3);
                Message receivedMessage3 = pidMapper.get(protocolIdQueue).take();
                giniMaxNumerator2 = (Integer) receivedMessage3.getValue();
                giniMaxNumerator = asymmetricBit == 1 ? Math.floorMod(
                        giniMaxNumerator + giniMaxNumerator2, prime) : 0;

                Message senderMessage4 = new Message(giniMaxDenominator,
                        clientId, protocolIdQueue);
                Integer giniMaxDenominator2 = 0;
                commonSender.put(senderMessage4);
                Message receivedMessage4 = pidMapper.get(protocolIdQueue).take();
                giniMaxDenominator2 = (Integer) receivedMessage4.getValue();
                giniMaxDenominator = asymmetricBit == 1 ? Math.floorMod(
                        giniMaxDenominator + giniMaxDenominator2, prime) : 0;

                int leftOperand = giniNumerators[k] * giniMaxDenominator;
                int rightOperand = giniMaxNumerator * giniDenominators[k];

                int comparisonResult = 0;
                int compResultInverse = 0;
                if (asymmetricBit == 1) {

                    comparisonResult = (leftOperand >= rightOperand) ? 1 : 0;
                    compResultInverse = Math.floorMod(comparisonResult + 1, 2);
                }
                //   System.out.println("GiniMaxNum=" + giniMaxNumerator + ", giniMaxDenominator=" + giniMaxDenominator);
                //   System.out.println("GiniNums[k]=" + giniNumerators[k] + ", GiniDenoms[k]=" + giniDenominators[k]);
                // int leftOperand=0;

                // MultiplicationInteger mult0 = new MultiplicationInteger(giniNumerators[k], 
                // 				giniMaxDenominator, decimalTiShares.get(decimalTiIndex), pidMapper,
                // 				commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
                // 				pid, asymmetricBit, modelProtocolId, partyCount);
                // 			pid++;
                // leftOperand = mult0.call();
                // System.out.println("New Num * Old Denom: " + leftOperand);
                // int rightOperand=0;
                // MultiplicationInteger mult1 = new MultiplicationInteger(giniMaxNumerator, 
                // 				giniDenominators[k], decimalTiShares.get(decimalTiIndex), pidMapper,
                // 				commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
                // 				pid, asymmetricBit, modelProtocolId, partyCount);
                // 			pid++;
                // rightOperand = mult1.call();
                // System.out.println("Old Num * New Denom: " + rightOperand);
                int placeheHolder = CompareAndConvertField.compareIntegers(
                        1, 1, binaryTiShares,
                        asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                        clientId, prime, bitLength, partyCount, pid,
                        false, decimalTiShares);
                pid++;

                //         	int compResultInverse = comparisonResult;
                //         	if(asymmetricBit==1) {
                //         		compResultInverse = Math.floorMod(comparisonResult+1, 2);
                //         	}
                //    	System.out.println("CompResult: "+comparisonResult + ", CompResultInv: " + compResultInverse);
                //         	// convert to decimal field compResult and inverse to decimal
                //         	//Integer[] newAssignmentsBin = {compResultInverse, comparisonResult};
                Integer[] newAssignmentsBin = {comparisonResult, compResultInverse};

                Integer[] newAssignments
                        = CompareAndConvertField.changeBinaryToDecimalField(
                                Arrays.asList(newAssignmentsBin), decimalTiShares,
                                pid, pidMapper, commonSender, protocolIdQueue,
                                asymmetricBit, clientId, prime, partyCount);
                pid++;

                Integer[] giniArgmaxes = {k, giniArgmax};
                if (asymmetricBit == 0) {
                    giniArgmaxes[0] = 0;
                }
                Integer[] numerators = {giniNumerators[k], giniMaxNumerator};
                Integer[] denominators = {giniDenominators[k], giniMaxDenominator};

                DotProductInteger dp1 = new DotProductInteger(Arrays.asList(newAssignments),
                        Arrays.asList(giniArgmaxes), decimalTiShares, pidMapper, commonSender,
                        new LinkedList<>(protocolIdQueue), clientId, prime,
                        pid, asymmetricBit, partyCount);
                pid++;
                giniArgmax = dp1.call();

                DotProductInteger dp2 = new DotProductInteger(Arrays.asList(newAssignments),
                        Arrays.asList(numerators), decimalTiShares, pidMapper, commonSender,
                        new LinkedList<>(protocolIdQueue), clientId, prime,
                        pid, asymmetricBit, partyCount);
                pid++;
                giniMaxNumerator = dp2.call();

                DotProductInteger dp3 = new DotProductInteger(Arrays.asList(newAssignments),
                        Arrays.asList(denominators), decimalTiShares, pidMapper, commonSender,
                        new LinkedList<>(protocolIdQueue), clientId, prime,
                        pid, asymmetricBit, partyCount);
                pid++;
                giniMaxDenominator = dp3.call();

            }
        }

        Message sendMessage = new Message(giniArgmax, clientId, protocolIdQueue);
        Integer giniArgmax2 = 0;
        commonSender.put(sendMessage);
        Message receiveMessage = pidMapper.get(protocolIdQueue).take();
        giniArgmax2 = (Integer) receiveMessage.getValue();
        Integer sharedGiniArgmax = Math.floorMod(giniArgmax + giniArgmax2, prime);
        System.out.println("Gini Gain Argmax (public): " + sharedGiniArgmax);

        attributes[sharedGiniArgmax] = 0;

        decisionTreeNodes.add("attr=" + sharedGiniArgmax);

        // for each j in attrValue, batchmult transactions (*) subsetTransactionBitVector[giniArgmax][j]
        // dtLearn(transactions', attributes', --r);
        List<Future<Integer[]>> batchMultTasks0 = new ArrayList<>();
        List<Integer[]> updatedTransactions = new ArrayList<>();

        for (int j = 0; j < attrValueCount; j++) {

            BatchMultiplicationByte batchMult
                    = new BatchMultiplicationByte(Arrays.asList(transactions),
                            attributeValueTransactionVector.get(sharedGiniArgmax).get(j),
                            binaryTiShares, pidMapper, commonSender,
                            new LinkedList<>(protocolIdQueue), clientId,
                            Constants.BINARY_PRIME, pid, asymmetricBit, modelProtocolId, partyCount);
            pid++;
            Future<Integer[]> bmTask = es.submit(batchMult);
            batchMultTasks0.add(bmTask);
        }
        pid += attrValueCount;

        for (int j = 0; j < attrValueCount; j++) {
            Future<Integer[]> bmTask = batchMultTasks0.get(j);
            updatedTransactions.add(bmTask.get());
        }

        for (int j = 0; j < attrValueCount; j++) {
            ID3Model(updatedTransactions.get(j), attributes, r - 1);
        }
        es.shutdown();
    }

    Integer[] findCommonClassIndex(Integer[] subsetTransactions)
            throws InterruptedException, ExecutionException {

        int[] s = new int[classValueCount];
        ExecutorService es
                = Executors.newFixedThreadPool(Constants.THREAD_COUNT);

        List<Future<Integer>> DPtaskList = new ArrayList<>();

        Integer[] subsetTransactionsDecimal
                = CompareAndConvertField.changeBinaryToDecimalField(
                        Arrays.asList(subsetTransactions), decimalTiShares,
                        pid, pidMapper, commonSender, protocolIdQueue, asymmetricBit,
                        clientId, prime, partyCount);
        pid++;

        // System.out.println("Subset Transactions (bin): ");
        // for(Integer i : subsetTransactions)
        // 	System.out.print(i + " ");
        // System.out.println();
        // System.out.println("Subset Transactions (dec): ");
        // for(Integer i : subsetTransactionsDecimal)
        // 	System.out.print(i + " ");
        // System.out.println();
        for (int i = 0; i < classValueCount; i++) {

            DotProductInteger dp = new DotProductInteger(
                    Arrays.asList(subsetTransactionsDecimal),
                    classValueTransactionVectorDecimal.get(i), decimalTiShares,
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, asymmetricBit, partyCount);

            Future<Integer> dpresult = es.submit(dp);
            pid++;

            DPtaskList.add(dpresult);
        }

        for (int i = 0; i < classValueCount; i++) {
            Future<Integer> dpresult = DPtaskList.get(i);
            s[i] = dpresult.get();
        }

        for (int i = 0; i < classValueCount; i++) {
            System.out.println("Tranactions w/ c.v. [" + i + "]: " + s[i]);
        }

        List<Future<List<Integer>>> BDtaskList = new ArrayList<>();
        for (int i = 0; i < classValueCount; i++) {
            BitDecomposition bitD = new BitDecomposition(s[i], binaryTiShares,
                    asymmetricBit, bitLength, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId,
                    Constants.BINARY_PRIME, pid, partyCount);
            BDtaskList.add(es.submit(bitD));
            pid++;
        }

        List<List<Integer>> bitSharesS = new ArrayList<>();
        for (int i = 0; i < classValueCount; i++) {
            Future<List<Integer>> taskResponse = BDtaskList.get(i);
            bitSharesS.add(taskResponse.get());
        }

        for (int i = 0; i < classValueCount; i++) {
            System.out.print("Bit decomp of c.v. [" + i + "] count: ");
            for (int j = 0; j < bitLength; j++) {
                System.out.print(bitSharesS.get(i).get(j) /* + " "*/);
            }
            System.out.println();
        }

        ArgMax argmax = new ArgMax(bitSharesS, binaryTiShares, asymmetricBit,
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, Constants.BINARY_PRIME, pid, partyCount);
        pid++;
        Integer[] majorityClassIndex = argmax.call();

        System.out.print("One Hot Common Class Index: ");
        for (Integer i : majorityClassIndex) {
            System.out.print(i /*+ " "*/);
        }
        System.out.println();

        es.shutdown();

        return majorityClassIndex;
    }

    void init() throws InterruptedException, ExecutionException {

        subsetTransactionBitVector = new Integer[datasetSize];

        for (int i = 0; i < datasetSize; i++) {
            subsetTransactionBitVector[i] = asymmetricBit;
        }

        attributeBitVector = new Integer[attrCount];

        for (int i = 0; i < attrCount; i++) {
            attributeBitVector[i] = 1;
        }

        attributeValueTransactionVector = new ArrayList<>();

        for (int i = 0; i < attrValues.length; i++) {
            attributeValueTransactionVector.add(new ArrayList<>());
            for (int j = 0; j < attrValues[0].length; j++) {
                attributeValueTransactionVector.get(i).add(
                        Arrays.asList(attrValues[i][j]));
            }
        }

        classValueTransactionVector = new ArrayList<>();

        for (int i = 0; i < classValues.length; i++) {
            classValueTransactionVector.add(Arrays.asList(classValues[i]));
        }

        attributeValueTransactionVectorDecimal = new ArrayList<>();
        for (int i = 0; i < attrCount; i++) {
            attributeValueTransactionVectorDecimal.add(new ArrayList<>());
            for (int j = 0; j < attrValueCount; j++) {
                attributeValueTransactionVectorDecimal.get(i).add(Arrays.asList(
                        CompareAndConvertField.changeBinaryToDecimalField(
                                attributeValueTransactionVector.get(i).get(j),
                                decimalTiShares, pid, pidMapper, commonSender,
                                protocolIdQueue, asymmetricBit, clientId, prime,
                                partyCount)));
                pid++;
            }
        }

        classValueTransactionVectorDecimal = new ArrayList<>();
        for (int i = 0; i < classValueCount; i++) {
            classValueTransactionVectorDecimal.add(Arrays.asList(
                    CompareAndConvertField.changeBinaryToDecimalField(
                            classValueTransactionVector.get(i), decimalTiShares,
                            pid, pidMapper, commonSender, protocolIdQueue,
                            asymmetricBit, clientId, prime, partyCount)));
            pid++;
        }
    }

    void initalizeModelVariables(String[] args) {

        for (String arg : args) {

            String[] currInput = arg.split("=");
            String command = currInput[0];
            String value = currInput[1];

            switch (command) {

                case "datasetShare":
                    try {
                        Scanner s = new Scanner(new File(value));

                        int[] dimensions = new int[4];
                        for (int i = 0; i < 4; i++) {
                            dimensions[i] = s.nextInt();
                        }
                        s.nextLine();

                        this.classValueCount = dimensions[0];
                        this.attrCount = dimensions[1];
                        this.attrValueCount = dimensions[2];
                        this.datasetSize = dimensions[3];

                        this.attrValues
                                = new Integer[attrCount][attrValueCount][datasetSize];

                        for (int i = 0; i < attrCount; i++) {
                            for (int j = 0; j < attrValueCount; j++) {
                                String[] temp = new String[datasetSize];
                                temp = s.nextLine().split(",");
                                for (int k = 0; k < datasetSize; k++) {
                                    attrValues[i][j][k] = Integer.parseInt(temp[k]);
                                }
                            }
                        }

                        this.classValues = new Integer[classValueCount][datasetSize];

                        for (int i = 0; i < classValueCount; i++) {
                            String[] temp = new String[datasetSize];
                            temp = s.nextLine().split(",");
                            for (int j = 0; j < datasetSize; j++) {
                                classValues[i][j] = Integer.parseInt(temp[j]);
                            }
                        }

                        s.close();
                    } catch (FileNotFoundException fnfe) {
                        System.out.println(fnfe);
                    }

                    break;

                case "output":
                    //TODO: add output path
                    break;
            }
        }

    }

    public String toString() {

        String ret = "attrCount=" + attrCount + ", attrValueCount=" + attrValueCount
                + ", classValueCount=" + classValueCount + ", datasetSize="
                + datasetSize + "\n\nattrValues:\n";

        for (int i = 0; i < attrCount; i++) {
            for (int j = 0; j < attrValueCount; j++) {
                for (int k = 0; k < datasetSize; k++) {
                    ret += attributeValueTransactionVector.get(i).get(j).get(k) /*+ " "*/;
                }
                if (j < attrValueCount - 1) {
                    ret += "; ";
                }
            }
            ret += "\n";
        }
        ret += "\nclassValues:\n";
        for (int i = 0; i < classValueCount; i++) {
            for (int j = 0; j < datasetSize; j++) {
                ret += classValueTransactionVector.get(i).get(j) /*+ " "*/;
            }
            if (i < classValueCount - 1) {
                ret += "; ";
            };
        }
        // ret += "\n\nattrValues(dec):\n";
        // for(int i=0; i<attrCount; i++) {
        // 	for(int j=0; j<attrValueCount; j++) {
        // 		for(int k=0; k<datasetSize; k++) {
        // 			ret+=attributeValueTransactionVectorDecimal.get(i).get(j).get(k) + " ";
        // 		}
        // 		if(j < attrValueCount-1) ret += "; ";
        // 	}
        // 	ret += "\n";
        // }
        // ret += "\nclassValues(dec):\n";
        // for(int i=0; i<classValueCount; i++) {
        // 	for(int j=0; j<datasetSize; j++) {
        // 		ret+=classValueTransactionVectorDecimal.get(i).get(j) + " ";
        // 	}
        // 	if(i < classValueCount-1) ret += "; ";
        // }

        return ret + "\n";
    }

}
