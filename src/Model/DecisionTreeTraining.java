/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.ArgMax;
import Protocol.BitDecompositionBigInteger;
import Protocol.BitDecomposition;
import Protocol.DotProductByte;
import Protocol.DotProductBigInteger;
import Protocol.DotProductInteger;
import Protocol.EqualityBigInteger;
import Protocol.MultiplicationBigInteger;
import Protocol.Utility.BatchMultiplicationByte;
import Protocol.Utility.BatchMultiplicationBigInteger;
import Protocol.Utility.CompareAndConvertField;
import Protocol.Utility.ParallelMultiplication;
import Protocol.Utility.ParallelMultiplicationBigInteger;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import TrustedInitializer.TripleBigInteger;
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
import java.math.BigInteger;
import java.io.File; /* TODO: Mpve parsing to FileIO */
import java.io.FileNotFoundException;
import java.util.Scanner;


public class DecisionTreeTraining extends Model {

	/* Share types needed for DT Learn */
    List<BigInteger> equalityTiShares;
    List<TripleByte> binaryTiShares;
    List<TripleBigInteger> bigIntTiShares;
    List<TripleInteger> decimalTiShares;
    /* Constants related to cutoff size TODO: should be in config file */
    BigInteger alpha;
    double epsilon; 
    int cutoffTransactionSetSize; 

    /* Input size parameters */
	int attrCount;
	int datasetSize;
	int attrValueCount;
	int classValueCount;

	/* Data set prime field representations */
	Byte[][][] attrValues;
	Byte[][] classValues;
    
    List<List<List<Byte>>> attributeValueTransactionVector;
    List<List<Byte>> classValueTransactionVector; 
    
    List<List<List<Integer>>> attributeValueTransactionVectorInteger;
    List<List<Integer>> classValueTransactionVectorInteger; 
    
    List<List<List<BigInteger>>> attributeValueTransactionVectorBigInteger;
    List<List<BigInteger>> classValueTransactionVectorBigInteger; 
    
    
    /* Recursive protocol paramters */
	int levelCounter;
	Byte[] subsetTransactionBitVector;
	Byte[] attributeBitVector;
	
    // a global ID series - TODO
    int pid, binaryTiIndex, bigIntTiIndex, equalityTiIndex; 
    int bitDTiCount, comparisonTiCount, bitLength;
    BigInteger prime;
    int datasetSizePrime;
    int datasetSizeBitLength;

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
    	List<TripleByte> binaryTriples, List<TripleBigInteger> bigIntTriples, 
    	List<TripleInteger> decTriples, List<BigInteger> equalityShares, String[] args, int partyCount,
    	Queue<Integer> protocolIdQueue, int protocolID) {
        
        /* Initialization of generic Model type */
        super(pidMapper, senderQueue, clientId, asymmetricBit, 
        	partyCount, protocolIdQueue, protocolID);

        /* Initialization of input parameters */
		initializeModelVariables(args);
        
        this.bigIntTiShares  = bigIntTriples;
        this.binaryTiShares   = binaryTriples;
        this.equalityTiShares = equalityShares;
        this.decimalTiShares = decTriples;

        this.levelCounter = attrCount;

        /* TODO: initiate asymmetric bit holder from config file */
        this.alpha = BigInteger.valueOf(8);
        this.epsilon = 0.1;                                             
        this.cutoffTransactionSetSize = (int) (epsilon * (double)datasetSize);  

        /* Initialization of architectural indeces */
        pid = 0;
        bigIntTiIndex = 0;
        binaryTiIndex = 0;
        equalityTiIndex = 0;

        /* Currently need to change prime in config file to same value as prime here */
        //int baseBitLength = (int) Math.ceil(Math.log(datasetSize)/Math.log(2));
        //bitLength = /* attrValueCount*(baseBitLength + 4)+*/
        // 			(attrValueCount-1)*(baseBitLength+4) + 
        // 			(int) Math.ceil(Math.log(datasetSize)/Math.log(2)) + 
        // 			(int) Math.ceil(Math.log(datasetSize)/Math.log(2)) + 
        // 			2*baseBitLength - 2; // -2 added just so int will work

        //prime = (int) Math.pow(2, bitLength);
        this.bitLength = Constants.BIT_LENGTH;
        this.prime = Constants.BIG_INT_PRIME;

        System.out.println("Prime: " + prime);
        System.out.println("Bitlength: " + bitLength);
        
        /* Increment for TI Shares */ 
        bitDTiCount = bitLength * 3 - 2;
        comparisonTiCount = (2*bitLength) + ((bitLength*(bitLength-1)) / 2);
        
        this.datasetSizeBitLength = (int) Math.ceil(Math.log(datasetSize)/Math.log(2));
        this.datasetSizePrime = (int) Math.pow(2, datasetSizeBitLength);

        System.out.println("DatasetSize Prime: " + datasetSizePrime);
        System.out.println("DatasetSize Bitlength: " + datasetSizeBitLength);

        // TODO replace with tree
        decisionTreeNodes = new ArrayList<>(); 
    }

    public void trainDecisionTree() 
    	throws InterruptedException, ExecutionException {
        
        final long startTime = System.currentTimeMillis();
        
    	init();
    	System.out.println(this);
{
        BigInteger i = BigInteger.ZERO;
        for(;;) {
            if(i.compareTo( new BigInteger("10000000", 10)) == 0)
                break;
            i = i.add(BigInteger.ONE);
        }
}
        System.out.println("\nBeginning Model...\n");
		ID3Model(subsetTransactionBitVector, attributeBitVector,levelCounter);

        final long endTime = System.currentTimeMillis();
        
        System.out.println("______________________________________");
        System.out.println("AttrCnt=" + attrCount + " TransactionCnt=" + datasetSize + 
        				   " Runtime=" + String.valueOf(endTime - startTime) + "ms");
		System.out.println("\nDecision Tree:");

		for(int i=0; i<decisionTreeNodes.size(); i++)
			System.out.println(decisionTreeNodes.get(i));
		System.out.println("\n");
    }
    
    void ID3Model(Byte[] transactions, Byte[] attributes, int r) 
    	throws InterruptedException, ExecutionException {
    
    	System.out.println("______________________________________");
    	System.out.println("Recursion Level (public): " + r);
    	System.out.print("Attribute Subset (public): ");
    	for(int el : attributes) System.out.print(el /*+ " "*/);
    	System.out.println();
    	System.out.print("Transaction Subset: ");
		for(int el : transactions) System.out.print(el /*+ " "*/);
    	System.out.println();
		System.out.println("______________________________________");
    	
    	// Get majority cass index one-hot encoding
    	Byte[] majorityClassIndex = findCommonClassIndex(transactions);
    	
    	// Make majority class index one-hot encoding public
    	Byte[] majorityClassIndexShared = new Byte[classValueCount];
		for(int i=0; i<classValueCount; i++) {
		
			Message senderMessage = new Message(majorityClassIndex[i],
				clientId, protocolIdQueue);
			Byte majorityClassIndex2 = 0;
			commonSender.put(senderMessage);
			Message receivedMessage = pidMapper.get(protocolIdQueue).take();
			majorityClassIndex2 = (Byte) receivedMessage.getValue();
			majorityClassIndexShared[i] = (byte) Math.floorMod(
				majorityClassIndex[i]+majorityClassIndex2, Constants.BINARY_PRIME);
		}
    	
    	// Find the integer version of the majority class index
    	int majorityIndexAsInt = 0;
    	for(int i=0; i<classValueCount; i++) {
    		if(majorityClassIndexShared[i] == 1) {
    			majorityIndexAsInt = i;
    			break;
            }
    	}
    	// Correct one-hot encoding to take only the first instance of a maximum
    	for(int i=majorityIndexAsInt+1; i<classValueCount; i++) {
    		majorityClassIndexShared[i] = 0;
    	}
    	
    	System.out.println("Majority Class Index (public): " + majorityIndexAsInt);
    	System.out.print("Maj Index One-Hot (public): ");
		for(int el : majorityClassIndexShared) {
			System.out.print(el/* + " "*/);
		} System.out.println();
	

		// quit if no attributes left in subset
    	if(r == 0) {
    		System.out.println("Exited on base case: Recursion Level == 0");
    		decisionTreeNodes.add("class=" + majorityIndexAsInt);
    		return;
    	}
    	
    	// create shares of one-hot majority index over decimal field
        BigInteger[] majorityClassIndexDecimal = asymmetricBit==1 ?

            CompareAndConvertField.changeBinaryToBigIntegerField(Arrays.asList(
            majorityClassIndexShared), bigIntTiShares, pid, pidMapper, commonSender, 
            protocolIdQueue, asymmetricBit, clientId, prime, partyCount) :

			CompareAndConvertField.changeBinaryToBigIntegerField(Collections.nCopies(
			classValueCount, new Byte("0")), bigIntTiShares, pid, pidMapper, commonSender, 
            protocolIdQueue, asymmetricBit, clientId, prime, partyCount);
        pid++;
        
        // Convert transactions to decimal field
        BigInteger[] transactionsDecimal = 
            CompareAndConvertField.changeBinaryToBigIntegerField(Arrays.asList(
            transactions), bigIntTiShares, pid, pidMapper, commonSender, 
            protocolIdQueue, asymmetricBit, clientId, prime, partyCount);
        pid++;

        // Generate a vector for each class value that contains:
        // - all 0's if i is not the majority class index
        // - the current subset of transactions if i is the majority class index  
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<BigInteger[]>> batchMultTasks1 = new ArrayList<>(); 
        for(int i=0; i<classValueCount; i++) {
            BatchMultiplicationBigInteger bm = new BatchMultiplicationBigInteger(
                Arrays.asList(transactionsDecimal), 
                Collections.nCopies(datasetSize, majorityClassIndexDecimal[i]),
                bigIntTiShares, pidMapper, commonSender, 
                new LinkedList<>(protocolIdQueue), clientId, 
                prime, pid, asymmetricBit, modelProtocolId, 
                partyCount);
            pid++;
            batchMultTasks1.add(es.submit(bm));
        }
        List<BigInteger[]> majClassOfSubsetTransactions = new ArrayList<>();
        for(int i=0; i<classValueCount; i++) {
            Future<BigInteger[]> bmTask1 = batchMultTasks1.get(i);
            majClassOfSubsetTransactions.add(bmTask1.get());
        }
        
        // Determine the number of transactions in the subset that predict the
        // majority class value
    	BigInteger majorityClassTransactionCount = BigInteger.ZERO;

    	List<Future<BigInteger>> dpTasks = new ArrayList<>();
    	for(int i=0; i<classValueCount; i++) {
    		DotProductBigInteger dpModule = new DotProductBigInteger(
    			/*Collections.nCopies(datasetSize, majorityClassIndexDecimal[i])*/ 
    			Arrays.asList(majClassOfSubsetTransactions.get(i)),
    			classValueTransactionVectorBigInteger.get(i), bigIntTiShares,
    			pidMapper,commonSender, new LinkedList<>(protocolIdQueue), 
    			clientId, prime, pid, asymmetricBit, partyCount);    			
    		Future<BigInteger> dpResult = es.submit(dpModule);
    		pid++;
    		dpTasks.add(dpResult);
    	} 
    	for(int i=0; i<classValueCount; i++) {
    		Future<BigInteger> dpResult = dpTasks.get(i);
    		majorityClassTransactionCount = 
                (majorityClassTransactionCount.add(dpResult.get())).mod(prime);
    	}

    	System.out.println("Majority Class Transaction Count: " +
    		majorityClassTransactionCount);

    	// Convert subset of transactions to decimal field (HAS THIS ALREADY BEEN DONE?)
        BigInteger[] subsetTransactionsDecimal = 
            CompareAndConvertField.changeBinaryToBigIntegerField(
                Arrays.asList(transactions), bigIntTiShares,
                pid, pidMapper, commonSender, protocolIdQueue, asymmetricBit,
                clientId, prime, partyCount);
        pid++;

        // Count the total number of transactions in the current subset
    	BigInteger transactionCount = BigInteger.ZERO;
        DotProductBigInteger dpm = new DotProductBigInteger(
        	Arrays.asList(subsetTransactionsDecimal),
            Collections.nCopies(datasetSize, asymmetricBit==1 ? 
            BigInteger.ONE : BigInteger.ZERO), bigIntTiShares, pidMapper, 
            commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
            pid, asymmetricBit, partyCount);
        pid++;
        transactionCount = dpm.call();
    	
    	System.out.println("Transactions in current subset: " + transactionCount);
    	
    	// Determine if the entire subset of transactions predicts the majority class
    	// attribute -- TODO: Can't be 0 index every time
    	EqualityBigInteger eq = new EqualityBigInteger(transactionCount, 
    		majorityClassTransactionCount, equalityTiShares.get(0), 
    		bigIntTiShares.get(0), asymmetricBit, pidMapper, commonSender,
    		clientId, prime, pid, new LinkedList<>(protocolIdQueue), 
    		partyCount);
    	pid++;
    	BigInteger eqResult = eq.call();
    	
    	System.out.println("MajClassTrans = SubsetTrans? (Non-zero -> not equal): "
    					   + eqResult);
    	
    	// Determine if cutoff transactions size has been reached TODO
    	BigInteger compResult=BigInteger.ONE; // non-zero means |T| is NOT <= e|t|
    	
    	// Determine if either base case condition has been reached with
    	// NOT(NOT x AND NOT y) in place of x OR y
    	MultiplicationBigInteger mult = new MultiplicationBigInteger(eqResult, 
    		compResult, bigIntTiShares.get(bigIntTiIndex), pidMapper,
    		commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
    		pid, asymmetricBit, partyCount);
    	pid++;
    	BigInteger stoppingBit = mult.call();
    	
    	// Make result public: if x OR y is true, exit on base case
    	Message senderMessage = new Message(stoppingBit, clientId, protocolIdQueue);
		BigInteger stoppingBit2 = BigInteger.ZERO;
		commonSender.put(senderMessage);
		Message receivedMessage = pidMapper.get(protocolIdQueue).take();
		stoppingBit2 = (BigInteger) receivedMessage.getValue();
        /* TODO: Check compareTo return value */
		if( ((stoppingBit.add(stoppingBit2)).mod(prime)).compareTo(BigInteger.ZERO) == 0) { //Math.floorMod(stoppingBit+stoppingBit2, prime)==0) {
			System.out.println("Exited on base case: All transactions predict same outcome");
			decisionTreeNodes.add("class=" + majorityIndexAsInt);
			es.shutdown();
			return;
		}
		System.out.println("Base case not reached. Continuing.");
    	
    	List<List<BigInteger>> UDecimal = new ArrayList<>();
    	List<Future<Integer[]>> UtaskList = new ArrayList<>();
    	
    	// Generate each subset of transactions within the current subset
    	// that predict the i-th class value	

    	for(int i=0; i<classValueCount; i++) {
    	
    		BatchMultiplicationByte batchMult = new BatchMultiplicationByte(
    			convertToIntList(transactions, null), 
                convertToIntList(null, classValueTransactionVector.get(i)),
    			binaryTiShares, pidMapper, commonSender, 
    			new LinkedList<>(protocolIdQueue), clientId, 
    			Constants.BINARY_PRIME, pid, asymmetricBit, modelProtocolId, 
    			partyCount);
    		pid++;
    		UtaskList.add(es.submit(batchMult));
    	}
    	pid += classValueCount;

    	// Convert binary shares to the decimal field
    	for(int i=0; i<classValueCount; i++) {
    		Future<Integer[]> taskResponse = UtaskList.get(i);
    		UDecimal.add(Arrays.asList(
    			CompareAndConvertField.changeBinaryToBigIntegerField(
    				convertToByteList(taskResponse.get() , null), bigIntTiShares,
    			pid, pidMapper, commonSender, protocolIdQueue, asymmetricBit,
    			clientId, prime, partyCount)));
    		pid++;
    	}

		BigInteger[][][] X = new BigInteger[attrCount][classValueCount][attrValueCount];
		BigInteger[][][] X2 = new BigInteger[attrCount][classValueCount][attrValueCount];
		BigInteger[][] Y = new BigInteger[attrCount][attrValueCount];
		BigInteger[] giniNumerators = new BigInteger[attrCount];
		BigInteger[] giniDenominators = new BigInteger[attrCount];
		
		List<Future<BigInteger>> xTasks = new ArrayList<>();
		
		// For every attribute in the current subset
		for(int k=0; k<attrCount; k++) {
		if(attributes[k] != 0) {
			// System.out.println("______________________________________");
			// System.out.println("Attribute ["+k+"]:");

			// For every value taken by the k-th attribute
			for(int j=0; j<attrValueCount; j++) {
				xTasks.clear();
				Y[k][j] = BigInteger.ZERO;

				// Determine the number of transactions that are:
				// 1. in the current subset
				// 2. predict the i-th class value
				// 3. and have the j-th value of the k-th attribute
				for(int i=0; i<classValueCount; i++) {
					DotProductBigInteger dp = new DotProductBigInteger(UDecimal.get(i),
						attributeValueTransactionVectorBigInteger.get(k).get(j),
						bigIntTiShares, pidMapper, commonSender,
						new LinkedList<>(protocolIdQueue), clientId, prime,
						pid, asymmetricBit, partyCount);
					pid++;
					Future<BigInteger> dpTask = es.submit(dp);
					xTasks.add(dpTask);
				}		
				
				for(int i=0; i<classValueCount; i++) {
					Future<BigInteger> dpResult = xTasks.get(i);
					X[k][i][j] = dpResult.get();
					// System.out.println("X["+k+"]["+j+"]["+i+"]: " + X[k][i][j]);
					Y[k][j] = (Y[k][j].add(X[k][i][j])).mod(prime);
				}
				// System.out.println("Y["+k+"]["+j+"]:" + Y[k][j]);
				// System.out.println();

				// Y contains the total number of transactions in the current subset 
				// in which the j-th value of the k-th attribute is present
				// The max bitLength of Y is bitLength
				// this line makes bitlength need to be bitLength + 4 (for alpha=8) 
				Y[k][j] = ((alpha.multiply(Y[k][j]))
                        .add( asymmetricBit == 1 ? BigInteger.ONE : BigInteger.ZERO ))
                        .mod(prime);
			}
			
			/* Compute X^2 for every X
			   this makes the bitLength need to be 2*bitLength */
			List<Future<BigInteger[]>> batchMultTasks = new ArrayList<>();
			for(int i=0; i<classValueCount; i++) {
				BatchMultiplicationBigInteger batchMult = 
					new BatchMultiplicationBigInteger(Arrays.asList(X[k][i]), 
						Arrays.asList(X[k][i]), bigIntTiShares, pidMapper,
						commonSender, new LinkedList<>(protocolIdQueue), 
						clientId, prime, pid, asymmetricBit, 
						modelProtocolId, partyCount);
				pid++;
				Future<BigInteger[]> bmTask = es.submit(batchMult);
				batchMultTasks.add(bmTask);
			}
			for(int i=0; i<classValueCount; i++) {
				Future<BigInteger[]> bmTask = batchMultTasks.get(i);
				X2[k][i] = bmTask.get();
			}
			
			/* Compute the product of all Y's : These are the Gini Gain denominators.
			   the max bitLength of Y can be (bitLength + 4), so bitlength 
			   should be attrValCount * (bitLength + 4)                       */
			ParallelMultiplicationBigInteger pMult = new ParallelMultiplicationBigInteger( 
				Arrays.asList(Y[k]), bigIntTiShares/*binaryTiShares*/, clientId, prime, pid, 
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
			giniNumerators[k] = BigInteger.ZERO;
			for(int j=0; j<attrValueCount; j++) {
				List<BigInteger> YwithoutJ = new ArrayList<>();
				for(int l=0; l<Y[k].length; l++) {
					if(j != l)
						YwithoutJ.add(Y[k][l]);
				}	
				BigInteger YprodWithoutJ = BigInteger.ZERO;
				ParallelMultiplicationBigInteger pMult0 = new ParallelMultiplicationBigInteger( 
					YwithoutJ, bigIntTiShares, clientId, prime, pid, 
					asymmetricBit, pidMapper, commonSender,
            		new LinkedList<>(protocolIdQueue), partyCount); 	
				pid++;
				
				YprodWithoutJ = pMult0.call();	
				
				BigInteger sumX2 = BigInteger.ZERO;
				for(int i=0; i<classValueCount; i++) {
					sumX2 = (sumX2.add(X2[k][i][j])).mod(prime); 
				}
				
				MultiplicationBigInteger mult0 = new MultiplicationBigInteger(sumX2, 
    				YprodWithoutJ, bigIntTiShares.get(bigIntTiIndex), pidMapper,
    				commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
    				pid, asymmetricBit, partyCount);
    			
    			pid++;
    			
    			giniNumerators[k] = (mult0.call().add(giniNumerators[k])).mod(prime); 
			}
		}
		}
		
		for(int k=0; k<attrCount; k++) {
			System.out.println("Gini_Gain["+k+"]: num=" + giniNumerators[k] +
				", denom=" + giniDenominators[k]);
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
		
		int k = 0;
		while(attributes[k] == 0) k++;
		
		BigInteger giniMaxNumerator   = giniNumerators[k];
		BigInteger giniMaxDenominator = giniDenominators[k];
	    BigInteger giniArgmax 		  = BigInteger.valueOf(k);

		k++;
		for(; k<attrCount; k++) {
		if(attributes[k]==1) {		

            BigInteger leftOperand, rightOperand;
            BigInteger[] giniArgmaxes 		= {
                BigInteger.valueOf(asymmetricBit == 1 ? k : 0), giniArgmax};
			BigInteger[] numerators   		= {giniNumerators[k], giniMaxNumerator};
			BigInteger[] denominators 		= {giniDenominators[k], giniMaxDenominator};
			BigInteger[] newAssignmentsBin  = new BigInteger[2];
			BigInteger[] newAssignments 	= new BigInteger[2]; 

			MultiplicationBigInteger mult0 = new MultiplicationBigInteger(numerators[0], 
				denominators[1], bigIntTiShares.get(bigIntTiIndex), pidMapper,
				commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
				pid, asymmetricBit, partyCount);
			pid++;
			leftOperand = mult0.call();

			MultiplicationBigInteger mult1 = new MultiplicationBigInteger(numerators[1], 
				denominators[0], bigIntTiShares.get(bigIntTiIndex), pidMapper,
				commonSender, new LinkedList<>(protocolIdQueue), clientId, prime,
				pid, asymmetricBit, partyCount);
			pid++;
			rightOperand = mult1.call();

			newAssignmentsBin[0] = CompareAndConvertField.compareBigIntegers(
				leftOperand, rightOperand, binaryTiShares,
        		asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
       			clientId, prime, bitLength, partyCount, pid,
        		false,  bigIntTiShares);
        	pid++;
    
        	newAssignmentsBin[1] = (newAssignmentsBin[0]
                .add(asymmetricBit == 1 ? BigInteger.ONE : BigInteger.ZERO))
                .mod(BigInteger.valueOf(Constants.BINARY_PRIME));

    		newAssignments = 
				CompareAndConvertField.changeBinaryToBigIntegerField(
					convertBigIntToByteList(newAssignmentsBin, null), bigIntTiShares,
					pid, pidMapper, commonSender, protocolIdQueue, 
					asymmetricBit, clientId, prime, partyCount);
			pid++;
				
			DotProductBigInteger dp1 = new DotProductBigInteger(Arrays.asList(newAssignments),
				Arrays.asList(giniArgmaxes), bigIntTiShares, pidMapper, commonSender,
				new LinkedList<>(protocolIdQueue), clientId, prime,
				pid, asymmetricBit, partyCount);
         	pid++;
         	giniArgmax = dp1.call();
         						
			DotProductBigInteger dp2 = new DotProductBigInteger(Arrays.asList(newAssignments),
				Arrays.asList(numerators), bigIntTiShares, pidMapper, commonSender,
				new LinkedList<>(protocolIdQueue), clientId, prime,
				pid, asymmetricBit, partyCount);
         	pid++;
         	giniMaxNumerator = dp2.call();
     	
     					
			DotProductBigInteger dp3 = new DotProductBigInteger(Arrays.asList(newAssignments),
				Arrays.asList(denominators), bigIntTiShares, pidMapper, commonSender,
				new LinkedList<>(protocolIdQueue), clientId, prime,
				pid, asymmetricBit, partyCount);
         	pid++;
         	giniMaxDenominator = dp3.call();

         	System.out.println("k = " + k);
         	System.out.println("New Num x Old Denom : " + leftOperand);
         	System.out.println("Old Num x New Denom : " + rightOperand);
         	System.out.println("New >= Old		    : " + newAssignmentsBin[0]);
         	System.out.println("New >= Old (dec)    : " + newAssignments[0]);
         	System.out.println("New <  Old		    : " + newAssignmentsBin[1]);
         	System.out.println("New <  Old (dec)    : " + newAssignments[1]);
         	System.out.println("Updated argmax      : " + giniArgmax);
         	System.out.println("Updated numerator   : " + giniMaxNumerator);
         	System.out.println("Updated denominator : " + giniMaxDenominator);
         	
		}
		}
		
		Message sendMessage = new Message(giniArgmax, clientId, protocolIdQueue);
		commonSender.put(sendMessage);
		Message receiveMessage = pidMapper.get(protocolIdQueue).take();
		BigInteger giniArgmax2 = (BigInteger) receiveMessage.getValue();
		int sharedGiniArgmax = (int)((giniArgmax.add(giniArgmax2)).mod(prime).longValue());
		System.out.println("Gini Gain Argmax (public): " + sharedGiniArgmax);
		
		attributes[sharedGiniArgmax] = 0;
		
		decisionTreeNodes.add("attr=" + sharedGiniArgmax);
			
		// Create transaction subsets for each of the following recursive calls.
		// One for each subset where the k*-th attribute takes the j-th value
		List<Future<Integer[]>> batchMultTasks0 = new ArrayList<>();
		List<Integer[]> updatedTransactions     = new ArrayList<>();

		for(int j=0; j<attrValueCount; j++) {
		
			BatchMultiplicationByte batchMult = 
				new BatchMultiplicationByte(convertToIntList(transactions, null), 
				convertToIntList(null, attributeValueTransactionVector.get(sharedGiniArgmax).get(j)),
    			binaryTiShares, pidMapper, commonSender, 
    			new LinkedList<>(protocolIdQueue), clientId, 
    			Constants.BINARY_PRIME, pid, asymmetricBit, modelProtocolId, partyCount);
			pid++;
			Future<Integer[]> bmTask = es.submit(batchMult);
			batchMultTasks0.add(bmTask);
		}
		pid += attrValueCount;

		for(int j=0; j<attrValueCount; j++) {
			Future<Integer[]> bmTask = batchMultTasks0.get(j);
			updatedTransactions.add(bmTask.get());
		}

		// Make a recursive call to ID3 for each new subset of transactions w/o A_k*
		for(int j=0; j<attrValueCount; j++) {
			ID3Model(convertToByteArray(updatedTransactions.get(j), null), attributes, r-1);
		}	
    	es.shutdown();
    }
   
    Byte[] findCommonClassIndex(Byte[] subsetTransactionsByte)
        throws InterruptedException, ExecutionException {
        
        Integer[] subsetTransactions = convertToIntArr(subsetTransactionsByte, null);

        int[] s = new int[classValueCount];
        ExecutorService es = 
            Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        
        List<Future<Integer>> DPtaskList = new ArrayList<>();
        
        Integer[] subsetTransactionsDecimal = 
            CompareAndConvertField.changeBinaryToDecimalField(
                Arrays.asList(subsetTransactions), decimalTiShares,
                pid, pidMapper, commonSender, protocolIdQueue, asymmetricBit,
                clientId, datasetSizePrime, partyCount);
        pid++;  
        
        // System.out.println("Subset Transactions (bin): ");
        // for(Integer i : subsetTransactions)
        //  System.out.print(i + " ");
        // System.out.println();
        
        // System.out.println("Subset Transactions (dec): ");
        // for(Integer i : subsetTransactionsDecimal)
        //  System.out.print(i + " ");
        // System.out.println();
            
                
        for(int i=0; i<classValueCount; i++) {
        
            DotProductInteger dp = new DotProductInteger(
                Arrays.asList(subsetTransactionsDecimal), 
                classValueTransactionVectorInteger.get(i), decimalTiShares,
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, datasetSizePrime, pid, asymmetricBit, partyCount);
        
            Future<Integer> dpresult = es.submit(dp);
            pid++;
            
            DPtaskList.add(dpresult);
        }
        
        for(int i=0; i<classValueCount; i++) {
            Future<Integer> dpresult = DPtaskList.get(i);
            s[i] = dpresult.get();
        }

        for(int i=0; i<classValueCount; i++) {
            System.out.println("Tranactions w/ c.v. ["+i+"]: "+s[i]);
        }

        List<Future<List<Integer>>> BDtaskList = new ArrayList<>();
        for(int i=0; i<classValueCount; i++) {
            BitDecomposition bitD = new BitDecomposition(s[i], binaryTiShares,
                asymmetricBit, datasetSizeBitLength, pidMapper, commonSender, 
                new LinkedList<>(protocolIdQueue), clientId, 
                Constants.BINARY_PRIME, pid, partyCount);
            BDtaskList.add(es.submit(bitD));
            pid++;
        }   
        
        List<List<Integer>> bitSharesS = new ArrayList<>();
        for(int i=0; i<classValueCount; i++) {
            Future<List<Integer>> taskResponse = BDtaskList.get(i);
            bitSharesS.add(taskResponse.get());
        }

        for(int i=0; i<classValueCount; i++) {
            System.out.print("Bit decomp of c.v. ["+i+"] count: ");
            for(int j=0; j<datasetSizeBitLength; j++) {
                System.out.print(bitSharesS.get(i).get(j) /* + " "*/);
            } System.out.println();
        }

        ArgMax argmax = new ArgMax(bitSharesS, binaryTiShares, asymmetricBit, 
            pidMapper, commonSender, new LinkedList<>(protocolIdQueue),     
            clientId, Constants.BINARY_PRIME, pid, partyCount);
        pid++;
        Integer[] majorityClassIndex = argmax.call();

        System.out.print("One Hot Common Class Index: ");
        for(Integer i : majorityClassIndex)
            System.out.print(i /*+ " "*/);
        System.out.println();   
        
        es.shutdown();  
            
        return convertToByteArray(majorityClassIndex, null);
    }   
  //   Byte[] findCommonClassIndex(Byte[] subsetTransactions)
  //   	throws InterruptedException, ExecutionException {
    	
  //   	BigInteger[] s = new BigInteger[classValueCount];

  //   	BigInteger[] subsetTransactionsDecimal = 
  //   		CompareAndConvertField.changeBinaryToBigIntegerField(
  //   			Arrays.asList(subsetTransactions), bigIntTiShares,
  //   			pid, pidMapper, commonSender, protocolIdQueue, asymmetricBit,
  //   			clientId, prime, partyCount);
  //   	pid++;	
    	
  //   	System.out.println("Subset Transactions (bin): ");
  //   	for(Byte i : subsetTransactions) System.out.print(i + " ");
  //   	System.out.println();
  //   	System.out.println("Subset Transactions (dec): ");
  //   	for(BigInteger i : subsetTransactionsDecimal) System.out.print(i + " ");
  //   	System.out.println();

  //       ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);    	
  //       List<Future<BigInteger>> DPtaskList = new ArrayList<>();	
    			
  //   	for(int i=0; i<classValueCount; i++) {
    	
  //   		DotProductBigInteger dp = new DotProductBigInteger(
  //   			Arrays.asList(subsetTransactionsDecimal), 
  //   			classValueTransactionVectorBigInteger.get(i), bigIntTiShares,
  //   			pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
  //   			clientId, prime, pid, asymmetricBit, partyCount);
    	
  //   		Future<BigInteger> dpresult = es.submit(dp);
  //   		pid++;
    		
  //   		DPtaskList.add(dpresult);
  //   	}
    	
  //   	for(int i=0; i<classValueCount; i++) {
  //   		Future<BigInteger> dpresult = DPtaskList.get(i);
  //   		s[i] = dpresult.get();
  //   	}

  //   	for(int i=0; i<classValueCount; i++)
  //        System.out.println("Tranactions w/ c.v. ["+i+"]: "+s[i]);
    	
		// List<Future<List<Integer>>> BDtaskList = new ArrayList<>();
		// for(int i=0; i<classValueCount; i++) {
		// 	BitDecompositionBigInteger bitD = new BitDecompositionBigInteger(
  //               s[i], binaryTiShares, asymmetricBit, bitLength, pidMapper, 
  //               commonSender, new LinkedList<>(protocolIdQueue), clientId, 
		// 		Constants.BINARY_PRIME, pid, partyCount);
  //           BDtaskList.add(es.submit(bitD));
		// 	pid++;
		// }	
		
		// List<List<Integer>> bitSharesS = new ArrayList<>();
		// for(int i=0; i<classValueCount; i++) {
		// 	Future<List<Integer>> taskResponse = BDtaskList.get(i);
		// 	bitSharesS.add(taskResponse.get());
		// }

		// for(int i=0; i<classValueCount; i++) {
		// 	System.out.print("Bit decomp of c.v. ["+i+"] count: ");
		// 	for(int j=0; j<bitLength; j++) {
		// 		System.out.print(bitSharesS.get(i).get(j) /* + " "*/);
		// 	} System.out.println();
		// }
        
  //       ArgMax argmax = new ArgMax(bitSharesS, binaryTiShares, asymmetricBit, 
		// 	pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 	
		// 	clientId, Constants.BINARY_PRIME, pid, partyCount);
		// pid++;
		// Integer[] majorityClassIndex = argmax.call();

		// System.out.print("One Hot Common Class Index: ");
		// for(Integer i : majorityClassIndex)
		// 	System.out.print(i /*+ " "*/);
		// System.out.println();	
		
  //   	es.shutdown();	
			
  //       Byte[] majorityClassIndexByte = new Byte[classValueCount];
  //       for(int i=0; i<classValueCount; i++) {
  //           majorityClassIndexByte[i] = majorityClassIndex[i].byteValue();
  //       }

  //   	return majorityClassIndexByte;
  //   }
   
    void init() throws InterruptedException, ExecutionException {
    
    	this.subsetTransactionBitVector = new Byte[datasetSize];
    	
    	for(int i=0; i<datasetSize; i++) 
    		subsetTransactionBitVector[i] = Byte.valueOf((byte)asymmetricBit);
    	
    	this.attributeBitVector = new Byte[attrCount];
    	
    	for(int i=0; i<attrCount; i++) 
    		attributeBitVector[i] = 1;
    	
    	attributeValueTransactionVector = new ArrayList<>();
    	
    	for(int i=0; i<attrValues.length; i++) {
    		attributeValueTransactionVector.add(new ArrayList<>());
    		for(int j=0; j<attrValues[0].length; j++) {
    			attributeValueTransactionVector.get(i).add(
    				Arrays.asList(attrValues[i][j]));
    		}
    	}
    	
    	this.classValueTransactionVector = new ArrayList<>();
    	
    	for(int i=0; i<classValues.length; i++) {
    		classValueTransactionVector.add(Arrays.asList(classValues[i]));
    	} 
    	
        this.attributeValueTransactionVectorInteger = new ArrayList<>();
    	this.attributeValueTransactionVectorBigInteger = new ArrayList<>();
    	
        for(int i=0; i<attrCount; i++) {
    		
            attributeValueTransactionVectorInteger.add(new ArrayList<>());
            attributeValueTransactionVectorBigInteger.add(new ArrayList<>());
    		
            for(int j=0; j<attrValueCount; j++) {


                attributeValueTransactionVectorInteger.get(i).add(Arrays.asList(
                    CompareAndConvertField.changeBinaryToDecimalField(
                        convertToIntList(null, attributeValueTransactionVector.get(i).get(j)),
                        decimalTiShares, pid, pidMapper, commonSender, 
                        protocolIdQueue, asymmetricBit, clientId, datasetSizePrime, 
                        partyCount)));
                pid++;

    			attributeValueTransactionVectorBigInteger.get(i).add(Arrays.asList(
    				CompareAndConvertField.changeBinaryToBigIntegerField(
    					attributeValueTransactionVector.get(i).get(j),
    					bigIntTiShares, pid, pidMapper, commonSender, 
    					protocolIdQueue, asymmetricBit, clientId, prime, 
    					partyCount)));
    			pid++; 
    		}
    	}
    	
        classValueTransactionVectorInteger = new ArrayList<>();
    	classValueTransactionVectorBigInteger = new ArrayList<>();

        System.out.println("DatasetSize prime: " + datasetSizePrime);
    	for(int i=0; i<classValueCount; i++) {

            System.out.print("Class Value ["+i+"] (Byte): ");
            for(int j=0; j<datasetSize; j++) {
                System.out.print(classValueTransactionVector.get(i).get(j));
            } System.out.println();
            System.out.print("Class Value ["+i+"]  (Int): ");
            List<Integer> temp = convertToIntList(null, classValueTransactionVector.get(i));
            for(int j=0; j<datasetSize; j++) {
                System.out.print(temp.get(j));
            } System.out.println();
            

           classValueTransactionVectorInteger.add(Arrays.asList(
                CompareAndConvertField.changeBinaryToDecimalField(
                    convertToIntList(null, classValueTransactionVector.get(i)),
                    decimalTiShares, pid, pidMapper, commonSender, 
                    protocolIdQueue, asymmetricBit, clientId, datasetSizePrime, 
                    partyCount)));
            pid++;

    		classValueTransactionVectorBigInteger.add(Arrays.asList(
    			CompareAndConvertField.changeBinaryToBigIntegerField(
    				classValueTransactionVector.get(i), bigIntTiShares,
    				pid, pidMapper, commonSender, protocolIdQueue, 
    				asymmetricBit, clientId, prime, partyCount)));
    		pid++;
    	}
    }
		
	void initializeModelVariables(String[] args) {

        for(String arg : args) {
			
			String[] currInput = arg.split("=");
			String command = currInput[0];
			String value = currInput[1];
			
			switch(command) {
			
			case "datasetShare":
			try {
				Scanner s = new Scanner(new File(value));
				
				int[] dimensions = new int[4];
				for(int i=0; i<4; i++)  {
					dimensions[i] = s.nextInt();
				}
				s.nextLine();
				
				this.classValueCount = dimensions[0];
				this.attrCount       = dimensions[1];
				this.attrValueCount  = dimensions[2];
				this.datasetSize     = dimensions[3];
				
				this.attrValues = 
					new Byte[attrCount][attrValueCount][datasetSize];
				
				for(int i=0; i<attrCount; i++) {
					for(int j=0; j<attrValueCount; j++) {
						String[] temp = new String[datasetSize];
						temp = s.nextLine().split(",");
						for(int k=0; k<datasetSize; k++) {
							attrValues[i][j][k] = Byte.valueOf(temp[k]);
						}
					}
				}
				
				this.classValues = new Byte[classValueCount][datasetSize];
				
				for(int i=0; i<classValueCount; i++) {
					String[] temp = new String[datasetSize];
					temp = s.nextLine().split(",");
					for(int j=0; j<datasetSize; j++) {
						classValues[i][j] = Byte.valueOf(temp[j]);
					}
				}
				
				s.close();
				}catch(FileNotFoundException fnfe) { System.out.println(fnfe); }
				
				break;
			
			case "output":
			//TODO: add output path
			break;
			}
		}

	}
	
	public String toString() {
	
		String ret="attrCount="+attrCount+", attrValueCount="+attrValueCount 
				  +", classValueCount="+classValueCount+", datasetSize=" +
				  datasetSize +"\n\nattrValues:\n";

		for(int i=0; i<attrCount; i++) {
			for(int j=0; j<attrValueCount; j++) {
				for(int k=0; k<datasetSize; k++) {
					ret+=attributeValueTransactionVector.get(i).get(j).get(k) /*+ " "*/;
				}
				if(j < attrValueCount-1) ret += "; ";
			}
			ret += "\n";
		}
		ret += "\nclassValues:\n";
		for(int i=0; i<classValueCount; i++) {
			for(int j=0; j<datasetSize; j++) {
				ret+=classValueTransactionVector.get(i).get(j) /*+ " "*/;
			}
			if(i < classValueCount-1) ret += "; ";;
		}
		ret += "\n\nattrValues(dec):\n";
		for(int i=0; i<attrCount; i++) {
			for(int j=0; j<attrValueCount; j++) {
				for(int k=0; k<datasetSize; k++) {
					ret+=attributeValueTransactionVectorInteger.get(i).get(j).get(k) + " ";
				}
				if(j < attrValueCount-1) ret += "; ";
			}
			ret += "\n";
		}
		ret += "\nclassValues(dec):\n";
		for(int i=0; i<classValueCount; i++) {
			for(int j=0; j<datasetSize; j++) {
				ret+=classValueTransactionVectorInteger.get(i).get(j) + " ";
			}
			if(i < classValueCount-1) ret += "; ";
		}
		
		return ret + "\n";
	}
    

    private static List<Integer> convertToIntList(Byte[] arr, List<Byte> list) {

        List<Integer> ret = new ArrayList<>();

        if(arr != null && list == null) {
            
            for(Byte el : arr)
                ret.add(el.intValue());
            
        } else if(arr == null && list != null) {
            
            for(int i=0; i<list.size(); i++) 
                ret.add(list.get(i).intValue());

        } else {
        
            System.out.println("CONVERT_TO_INT_LIST: Bad parameters");
        }    
        return ret;
        
    }

    private static Integer[] convertToIntArr(Byte[] arr, List<Byte> list) {

        Integer[] ret = null;

        if(arr != null && list == null) {
            
            ret = new Integer[arr.length];

            for(int i=0; i<arr.length; i++)
                ret[i] = arr[i].intValue();
            
        } else if(arr == null && list != null) {
            
            ret = new Integer[list.size()];

            for(int i=0; i<list.size(); i++) 
                ret[i] = list.get(i).intValue();

        } else {
        
            System.out.println("CONVERT_TO_INT_LIST: Bad parameters");
        }    
        return ret;
        
    }

    private static List<Byte> convertToByteList(Integer[] arr, List<Integer> list) {

        List<Byte> ret = new ArrayList<>();

        if(arr != null && list == null) {
            
            for(Integer el : arr)
                ret.add(el.byteValue());
            
        } else if(arr == null && list != null) {
            
            for(int i=0; i<list.size(); i++) 
                ret.add(list.get(i).byteValue());

        } else {
        
            System.out.println("CONVERT_TO_BYTE_LIST: Bad parameters");
        }    
        return ret;
        
    }

    private static Byte[] convertToByteArray(Integer[] arr, List<Integer> list) {

        Byte[] ret = null;

        if(arr != null && list == null) {
            
            ret = new Byte[arr.length];

            for(int i=0; i<arr.length; i++)
                ret[i] = arr[i].byteValue();
            
        } else if(arr == null && list != null) {
            
            ret = new Byte[list.size()];

            for(int i=0; i<list.size(); i++) 
                ret[i] = list.get(i).byteValue();

        } else {
        
            System.out.println("CONVERT_TO_BYTE_ARR: Bad parameters");
        }    
        return ret;
        
    }
    

    private static List<Byte> convertBigIntToByteList(BigInteger[] arr, List<BigInteger> list) {

        List<Byte> ret = new ArrayList<>();

        if(arr != null && list == null) {
            
            for(BigInteger el : arr)
                ret.add(el.byteValue());
            
        } else if(arr == null && list != null) {
            
            for(int i=0; i<list.size(); i++) 
                ret.add(list.get(i).byteValue());

        } else {
        
            System.out.println("CONVERT_TO_BYTE_LIST: Bad parameters");
        }    
        return ret;
        
    }
  
}
