/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.BitDecomposition;
import Protocol.Comparison;
import Protocol.DotProductInteger;
import Protocol.OR_XOR;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logistic Regression Scoring (binary classification)
 * 
 * This class takes a test vector (attribute values) and predicts the class
 * label using the logistic regression model (model vectors)
 * 
 * @author ariel
 */
public class LogisticRegressionScoring extends Model {

    List<List<Integer>> testVector, modelVector, zeroList;
    int intercept;
    int vectorSize;
    int numTests;
    String[] args;
    boolean hasModel;
    List<TripleInteger> intTriples;
    List<TripleByte> binaryTriples;
    int intSharesStartInd, binSharesStartInd;
    int prime;
    int pid = 0;
    int dpResult;
    List<Integer> bitShares;
    List<Integer> primeBitShares;
    Integer [] testVectorPrime;
    int compResult;
    int bitLength;
    boolean sharesMod2;
    Logger LOGGER;

    
    /**
     * Constructor for 2 party Logistic Regression scoring:
     *
     * One party has the model vectors, one or both parties have the test vector in args
     * If test vector is shared over mod 2, both parties must enter parameter mod=2
     *
     * party1: pass the test vector as csv file (testCsv), pass the bit length
     * as an integer (bitLength)
     *
     * party2: pass the model as a csv file (storedModel), pass the intercept as
     * a csv file (intercept), if applicable pass shares of test vector as csv file (testCsv)
     *
     * @param intTriples
     * @param binaryTriples
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param asymmetricBit
     * @param partyCount
     * @param args
     * @param protocolIdQueue
     * @param protocolID
     */
    public LogisticRegressionScoring(List<TripleInteger> intTriples, List<TripleByte> binaryTriples,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, int clientId, int asymmetricBit, 
            int partyCount, String[] args, Queue<Integer> protocolIdQueue, int protocolID) {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);

        this.args = args;
        this.intTriples = intTriples;
        this.binaryTriples = binaryTriples;
        intSharesStartInd = 0;
        binSharesStartInd = 0;
        primeBitShares = new ArrayList();
        testVector = new ArrayList();
        zeroList = new ArrayList();
        hasModel = false;
        sharesMod2 = false;
        LOGGER = Logger.getLogger(LogisticRegressionScoring.class.getName());
        
    }

    /**
     * The main method for scoring logistic regression
     * 
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    public void scoreLogisticRegression() throws IOException, InterruptedException, ExecutionException {

        initializeModelVariables(args);

        dpResult = 0;
        
        long startTime = System.currentTimeMillis();
        if(sharesMod2){
            runXOR();
        }
        
        runDotProduct();
        runBitDecomp();
        runComparison();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        LOGGER.log(Level.INFO, "The output share: {0}", compResult);
        LOGGER.log(Level.INFO, "Avg time duration:{0}", elapsedTime);

    }

    /**
     * Initialize the model variables, test vector, and bit length
     * 
     * @param args
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private void initializeModelVariables(String[] args) throws FileNotFoundException, IOException {

        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.partyUsage();
                System.exit(0);
            }

            String command = currInput[0];
            String value = currInput[1];

            switch (command) {
                case "testCsv":
                    // both parties have shares of the test vector
                    testVector = FileIO.loadIntListFromFile(value);
                    testVectorPrime = testVector.get(0).toArray(new Integer[0]);
                    vectorSize = testVector.get(0).size();
                    break;
                case "storedModel":
                    // party that has the model
                    modelVector = FileIO.loadIntListFromFile(value);
                    vectorSize = modelVector.get(0).size();
                    hasModel = true;
                    break;
                case "intercept":
                    // party has the intercept for the model vector
                    intercept = FileIO.loadIntListFromFile(value).get(0).get(0);
                    break;
                case "bitLength":
                    // bit length used for BitDecomposition (both parties)
                    bitLength = Integer.parseInt(value);
                    prime = (int) Math.pow(2, bitLength);
                    break;
                case "mod":
                    if(Integer.parseInt(value) == 2){
                        sharesMod2 = true;
                    }

            }
        }
        
        zeroList.add(new ArrayList(Collections.nCopies(vectorSize, 0)));
        if (testVector.isEmpty()){
            testVector = zeroList;
            testVectorPrime = testVector.get(0).toArray(new Integer[0]);
        }
    }
    
    private void runXOR() throws InterruptedException, ExecutionException{
        OR_XOR xor;
        if (asymmetricBit ==1){
            xor = new OR_XOR(testVector.get(0), zeroList.get(0), 
                    intTriples.subList(intSharesStartInd, intSharesStartInd + vectorSize),
                    asymmetricBit, 2, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
        } else {
            xor = new OR_XOR(zeroList.get(0), testVector.get(0), 
                    intTriples.subList(intSharesStartInd, intSharesStartInd + vectorSize),
                    asymmetricBit, 2, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
        }
        pid++;
        intSharesStartInd += vectorSize;
        testVectorPrime = xor.call();

    }

    /**
     * Run the DotProductInteger protocol
     * 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void runDotProduct() throws InterruptedException, ExecutionException {
        
        DotProductInteger dotProduct;

        if (hasModel) {
            dotProduct = new DotProductInteger(modelVector.get(0), Arrays.asList(testVectorPrime), 
                    intTriples.subList(intSharesStartInd, intSharesStartInd + vectorSize),
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, asymmetricBit, partyCount);
            dpResult = intercept;
        } else {
            dotProduct = new DotProductInteger(zeroList.get(0), Arrays.asList(testVectorPrime), 
                    intTriples.subList(intSharesStartInd, intSharesStartInd + vectorSize),
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, asymmetricBit, partyCount);
        }
        dpResult = Math.floorMod(dpResult + dotProduct.call(), prime);

        pid++;
        intSharesStartInd += vectorSize;

    }

    /**
     * Run the BitDecomposition protocol with result from the dot product,
     * convert (prime/2)-1 to bits
     * 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void runBitDecomp() throws InterruptedException, ExecutionException {
        BitDecomposition bitDecomp = new BitDecomposition(dpResult,
                binaryTriples.subList(binSharesStartInd, binSharesStartInd + vectorSize), 
                asymmetricBit, bitLength, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME, pid, partyCount);
        bitShares = bitDecomp.call();
        pid++;
        binSharesStartInd += vectorSize;
        
        int middle = (prime / 2) - 1; // comparison point for negative or positive number

        intToBinary(middle * asymmetricBit, bitLength);

    }

    /**
     * Run the comparison protocol with bit shares of (prime/2)-1 and bit shares
     * of the dot product
     * 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void runComparison() throws InterruptedException, ExecutionException {
        Comparison comp = new Comparison(primeBitShares, bitShares,
                binaryTriples.subList(binSharesStartInd, binSharesStartInd + vectorSize), 
                asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, Constants.BINARY_PRIME, pid, partyCount);
        compResult = comp.call();
        pid++;
        binSharesStartInd += vectorSize;
    }

    /**
     * Converts num to a list of bits of length len
     * 
     * @param num
     * @param len 
     */
    private void intToBinary(int num, int len) {
        for (int i = 0; i < len; i++) {
            int n = num % 2;
            primeBitShares.add(n);
            num /= 2;
        }
    }

}
