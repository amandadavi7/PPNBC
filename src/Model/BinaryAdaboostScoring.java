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
 * Adaboost scoring for binary classification Test feature vector is also binary
 * and shared (addition mod 2) with both parties
 *
 * Uses numFeatures*6 binaryTiShares and decimalTiShares Prime value must be
 * 2^bitLength
 *
 * @author ariel
 */
public class BinaryAdaboostScoring extends Model {

    List<List<Integer>> testVector;
    List<Integer> modifiedTestVector;
    Integer[] testVectorPrime;
    List<Integer> zeroVector;
    List<List<Integer>> modelVectors;
    List<TripleInteger> decimalTiShares;
    List<TripleByte> binaryTiShares;
    int decSharesStartInd, binSharesStartInd;
    int dpResult0, dpResult1, compResult;
    List<Integer> bitShares0;
    List<Integer> bitShares1;
    String[] args;
    boolean hasModel = false;
    int numFeatures;
    int prime;
    int bitLength;
    int pid = 0;
    Logger LOGGER;

    /**
     * Constructor:
     *
     * One party has the model, both parties have shares of the test feature
     * vector
     *
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param binaryTriples
     * @param decimalTriples
     * @param partyCount
     * @param args
     * @param protocolIdQueue
     * @param protocolID
     */
    public BinaryAdaboostScoring(int asymmetricBit, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, int clientId, List<TripleByte> binaryTriples, List<TripleInteger> decimalTriples,
            int partyCount, String[] args, Queue<Integer> protocolIdQueue, int protocolID) {
        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);

        this.args = args;
        decimalTiShares = decimalTriples;
        binaryTiShares = binaryTriples;
        decSharesStartInd = 0;
        binSharesStartInd = 0;
        modifiedTestVector = new ArrayList();
        LOGGER = Logger.getLogger(BinaryAdaboostScoring.class.getName());

    }

    /**
     * Initializes model vector, test vector, and bit length
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
                    //party has feature vector
                    testVector = FileIO.loadIntListFromFile(value);
                    numFeatures = testVector.get(0).size() * 2;
                    zeroVector = new ArrayList<>(Collections.nCopies(numFeatures, 0));
                    break;
                case "storedModel":
                    hasModel = true;
                    modelVectors = FileIO.loadIntListFromFile(value);
                    break;
                case "bitLength":
                    bitLength = Integer.parseInt(value);
                    prime = (int) Math.pow(2, bitLength);
                    break;

            }

        }
        if (!hasModel) {
            modelVectors = new ArrayList();
            modelVectors.add(zeroVector);
            modelVectors.add(zeroVector);
        }
    }

    /**
     * Main method for adaboost scoring
     *
     * modifies shared binary test vector to be shares mod prime
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void scoreAdaboost() throws IOException, InterruptedException, ExecutionException {
        initializeModelVariables(args);

        modifyTestVector();

        long startTime = System.currentTimeMillis();
        
        runXOR(); // convert binary shares to shares over prime field
        runDotProduct();
        runBitDecomp();
        runComparison();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        LOGGER.log(Level.INFO, "The result bit: {0}", compResult);
        LOGGER.log(Level.INFO, "Avg time duration:{0}", elapsedTime);

    }

    /**
     * Modifies the test vector to be double the length, including the negated
     * values [x1, x2, x3] --> [!x1, x1, !x2, x2, !x3, x3]
     */
    private void modifyTestVector() {
        // include in test vector the negated values
        for (int i = 0; i < testVector.get(0).size(); i++) {
            modifiedTestVector.add(Math.floorMod(testVector.get(0).get(i) + asymmetricBit, 2));
            modifiedTestVector.add(testVector.get(0).get(i));
            
        }

    }

    /**
     * Turns the test vector which is initially shared mod 2, to be shared mod
     * prime
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void runXOR() throws InterruptedException, ExecutionException {
        OR_XOR xor;
        if (asymmetricBit == 1) {
            xor = new OR_XOR(modifiedTestVector, zeroVector, 
                    decimalTiShares.subList(decSharesStartInd, decSharesStartInd + numFeatures),
                    asymmetricBit, 2, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
        } else {
            xor = new OR_XOR(zeroVector, modifiedTestVector, 
                    decimalTiShares.subList(decSharesStartInd, decSharesStartInd + numFeatures),
                    asymmetricBit, 2, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
        }
        pid++;
        decSharesStartInd += numFeatures;
        testVectorPrime = xor.call();
    }

    /**
     * Runs the dot product protocol for class label 0 and class label 1
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void runDotProduct() throws InterruptedException, ExecutionException {
        // calculate dot product of model coefficients for class label 0 and test vector
        DotProductInteger dotProduct0 = new DotProductInteger(modelVectors.get(0), Arrays.asList(testVectorPrime), 
                decimalTiShares.subList(decSharesStartInd, decSharesStartInd + numFeatures),
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, prime, pid, asymmetricBit, partyCount);
        pid++;
        decSharesStartInd += numFeatures;
        dpResult0 = dotProduct0.call();

        // calculate dot poduct of model coefficients for class label 1 and test vector
        DotProductInteger dotProduct1 = new DotProductInteger(modelVectors.get(1), Arrays.asList(testVectorPrime), 
                decimalTiShares.subList(decSharesStartInd, decSharesStartInd + numFeatures),
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, prime, pid, asymmetricBit, partyCount);
        pid++;
        decSharesStartInd += numFeatures;
        dpResult1 = dotProduct1.call();
    }

    /**
     * Runs the bit decomposition protocol for both of the dot product results
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void runBitDecomp() throws InterruptedException, ExecutionException {
        BitDecomposition bitDecomp0 = new BitDecomposition(dpResult0,
                binaryTiShares.subList(binSharesStartInd, binSharesStartInd + numFeatures), 
                asymmetricBit, bitLength, pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                clientId, Constants.BINARY_PRIME, pid, partyCount);
        bitShares0 = bitDecomp0.call();
        pid++;
        binSharesStartInd += numFeatures;

        BitDecomposition bitDecomp1 = new BitDecomposition(dpResult1,
                binaryTiShares.subList(binSharesStartInd, binSharesStartInd + numFeatures),
                asymmetricBit, bitLength, pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                clientId, Constants.BINARY_PRIME, pid, partyCount);
        bitShares1 = bitDecomp1.call();
        pid++;
        binSharesStartInd += numFeatures;

    }

    /**
     * Runs the comparison protocol between the results for class label 0 and
     * class label 1
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void runComparison() throws InterruptedException, ExecutionException {
        Comparison comp = new Comparison(bitShares1, bitShares0,
                binaryTiShares.subList(binSharesStartInd, binSharesStartInd + (bitLength*bitLength)),
                asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, Constants.BINARY_PRIME, pid, partyCount);
        compResult = comp.call();
        pid++;
        binSharesStartInd += bitLength*bitLength;
    }

}
