/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.BitDecompositionBigInteger;
import Protocol.Comparison;
import Protocol.DotProductBigInteger;
import Protocol.MultiplicationBigInteger;
import Protocol.MultiplicationByte;
import Protocol.OR_XOR;
import Protocol.Truncation;
import Protocol.Utility.BatchMultiplicationBigInteger;
import Protocol.Utility.BatchTruncation;
import Protocol.Utility.CompareAndConvertField;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import TrustedInitializer.TripleBigInteger;
import TrustedInitializer.TruncationPair;
import Utility.Constants;
import Utility.FileIO;
import Utility.LocalMath;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.*;

/**
 * Constraints: Decimal Accuracy > 0
                Ring size > Subring size + 1
 * @author ariel, davis
 */
public class LogisticRegressionTraining extends Model {

    String[] args;

    BigInteger prime;
    int bitlength;
    int decimal_acc;
    int integer_acc;
    BigInteger decimal_shift;
    int subringBitlength;
    BigInteger subringPrime;

    List<List<BigInteger>> x;
    List<BigInteger> y;
    List<BigInteger> weights, zeros;
    BigInteger z;
    BigInteger lowThreshold;
    BigInteger highThreshold;
    BigInteger middle;
    BigInteger ONE;
    BigInteger ONE_HALF;
    BigInteger TWO;
    int iterations;
    BigInteger learningRate;
    int pid;

    List<TripleBigInteger> bigIntTiShares;
    List<TripleInteger> intTriples;
    List<TripleByte> binTriples;

    int binTiIndex;
    int bigIntTiIndex;

    public LogisticRegressionTraining(List<TripleBigInteger> bigIntTiShares,
        List<TripleInteger> intTriples, List<TripleByte> binTriples,
        ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
        BlockingQueue<Message> commonSender, int clientId, int asymmetricBit,
        int partyCount, String[] args, Queue<Integer> protocolIdQueue, int protocolID, int threadID) {

        super(pidMapper, commonSender, clientId, asymmetricBit, partyCount,
            protocolIdQueue, protocolID, threadID);

      //  this.pid = 0;
        this.TWO = BigInteger.valueOf(2);

        this.args = args;
        this.bigIntTiShares = bigIntTiShares;
        this.intTriples = intTriples;
        this.binTriples = binTriples;

        this.binTiIndex = 0;
        this.bigIntTiIndex = 0;

        this.bitlength = Constants.BIT_LENGTH; // ring size
        this.prime = TWO.pow(bitlength); // ring modulus

        this.decimal_acc = Constants.DECIMAL_PRECISION;
        this.integer_acc = Constants.INTEGER_PRECISION;
        this.decimal_shift = TWO.pow(decimal_acc);

        this.subringBitlength = decimal_acc + integer_acc;
        this.subringPrime = TWO.pow(subringBitlength);

        // highThreshold needs to be converted value of 0.5
        this.highThreshold = asymmetricBit == 1 ?
            (BigInteger.ONE).shiftLeft(decimal_acc - 1) : BigInteger.ZERO;

        // lowThreshold needs to be converted value of -0.5
        this.lowThreshold = asymmetricBit == 1 ?
            prime.subtract(highThreshold) : BigInteger.ZERO;

        // midpoint of ring floor| prime / 2 |
        this.middle = asymmetricBit == 1 ?
            prime.shiftRight(1) : BigInteger.ZERO;



        // ONE needs to be the converted value of 1
        this.ONE = (BigInteger.ONE).shiftLeft(decimal_acc);
        this.ONE_HALF = (this.ONE).shiftRight(1);

        // learningRate needs to be converted value of the real learning rate
        this.learningRate = ONE_HALF;

        this.zeros = Collections.nCopies(bitlength, BigInteger.ZERO);

    }

    public void trainLogisticRegression() throws InterruptedException, ExecutionException {


        System.out.println("\nBeginning Model...\n");

        System.out.println("TI shares (bin) : " + binTriples.size());
        System.out.println("TI shares (big) : " + bigIntTiShares.size());

        initializeModelVariables(args);
        int datasetLength = x.size();
        int attributeCount = x.get(0).size();

        // initialize weights to be all zero (NOTE: could be random, too)
        this.weights = new ArrayList<>(Collections.nCopies(attributeCount, BigInteger.ZERO));

        for (int i = 0; i < iterations; i++) {

            System.out.println("[iter = " + i + "]");

            // sum holds the accumulated weights of all the training examples for each iteration
            List<BigInteger> sum = new ArrayList<>(Collections.nCopies(attributeCount, BigInteger.ZERO));

            for (int k = 0; k < datasetLength; k++) {        // for each training example
                System.out.println("[iter = " + i + "][testcase = "+ k +"]");

                DotProductBigInteger dp = new DotProductBigInteger(x.get(k), weights, bigIntTiShares,
                        pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                        clientId, prime, pid, asymmetricBit, partyCount, threadID);
                pid++;
                z = dp.call();

                System.out.println("\tAttr . weights COMPLETE");

                z = LocalMath.truncate(z, prime, decimal_acc, asymmetricBit);
                System.out.println("\tTruncation COMPLETE");


                BigInteger o = activation(z);
                System.out.println("\tActivation COMPLETE, o: " + o);

                // diffArray holds copies of the value (y-o)
                List<BigInteger> diffArray = new ArrayList<>(Collections.nCopies(attributeCount,
                                                (y.get(k).subtract(o)).mod(prime)));

                System.out.println("\tY - activation COMPLETE");
                // compute partial derivative w.r.t. each x_k,i
                BatchMultiplicationBigInteger batchMult = new BatchMultiplicationBigInteger(
                        diffArray, x.get(k), bigIntTiShares, pidMapper, commonSender,
                        new LinkedList<>(protocolIdQueue), clientId, prime, pid,
                        asymmetricBit, 0, partyCount, threadID);
                pid++;
                BigInteger[] gradient = batchMult.call();
                System.out.println("\tGradient COMPLETE");

                // update sum with truncated gradient
                for(int j = 0; j < attributeCount; j++) {
                    sum.set(j, (sum.get(j).add(
                        LocalMath.truncate(gradient[j], prime, decimal_acc, asymmetricBit)))
                        .mod(prime));
                }

                System.out.println("\tSum update COMPLETE");
            }

            // scale sum of gradients by learning rate and update weights
            List<BigInteger> scaledSum = new ArrayList<>();
            for(int j=0; j<attributeCount; j++) {
                scaledSum.add( (learningRate.multiply(sum.get(j))).mod(prime) );
                LocalMath.truncate(scaledSum.get(j), prime, decimal_acc, asymmetricBit);
                weights.set(j, (weights.get(j).add(scaledSum.get(j))).mod(prime) );
            }

            System.out.println("[iter = " + i + "] weights: " + weights.toString());
        }
    }

    private void initializeModelVariables(String[] args) {
        for (String arg : args) {
            String[] currInput = arg.split("=");

            String command = currInput[0];
            String value = currInput[1];

            // these need to be loaded as matrices
            switch (command) {
                case "xCsv":
                    x = FileIO.loadMatrixFromFile(value);
                    // need to add an additional column of ones for the intercept value
                    break;
                case "yCsv":
                    y = FileIO.loadListFromFile(value);

                    break;
                case "iterations":
                    iterations = Integer.parseInt(value);
                    break;
                default:
                    break;
            }
        }
    }

    private BigInteger activation(BigInteger z) throws InterruptedException, ExecutionException {

        // Compute MSB of z locally
        BigInteger msb = z.shiftRight(bitlength - 1);

        // get inverse of msb
        BigInteger msbNegated = asymmetricBit == 1 ?
            msb.add(BigInteger.ONE).mod(TWO) : msb;

        // Compute 1/2 in ring
        // it's ONE_HALF

        // Compute z with sign flipped
        BigInteger zNegated = prime.subtract(z);

        // 0 if msb is 1, otherwise z
        MultiplicationBigInteger mult0 = new MultiplicationBigInteger(
            msbNegated, z, bigIntTiShares.get(bigIntTiIndex), pidMapper,
            commonSender, new LinkedList<>(protocolIdQueue), clientId,
            prime, pid, asymmetricBit, partyCount, threadID);
        pid++;
        BigInteger zUnchanged = mult0.call();

        // 0 if msb is 0, otherwise (modulus - z)
        MultiplicationBigInteger mult1 = new MultiplicationBigInteger(
            msb, zNegated, bigIntTiShares.get(bigIntTiIndex), pidMapper,
            commonSender, new LinkedList<>(protocolIdQueue), clientId,
            prime, pid, asymmetricBit, partyCount, threadID);
        pid++;
        BigInteger zRectified = mult1.call();

        // Convert z to positive w/ cond. asgn. of zUnchanged, zRectified
        BigInteger zPos = (zUnchanged.add(zRectified)).mod(prime);

        // Store the values less than than 1/2 - TODO make bitmask global
        BigInteger bitMask0 = TWO.pow(decimal_acc-1).subtract(BigInteger.ONE);
        BigInteger zFraction = zPos.and(bitMask0);

        // Store the values geq 1/2
        BigInteger bitMask1 = TWO.pow(integer_acc+1).subtract(BigInteger.ONE);
        BigInteger zInteger = (zPos.shiftRight(decimal_acc-1)).and(bitMask1);

        // Bit decompose z integer component and 1/2-th place
        BitDecompositionBigInteger bitD0 = new BitDecompositionBigInteger(
            zInteger, binTriples, asymmetricBit, integer_acc+1, pidMapper, commonSender,
            new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME,
            pid, partyCount, threadID);
        pid++;
        List<Integer> zIntBits = bitD0.call();

        // share bits over subring
//        CompareAndConvertField.changeBinaryToDecimalField( );


        // Compute OR of all zIntBits
        List<Integer> subresult = new ArrayList<>();
        subresult.add(zIntBits.get(0));

// List<Integer> x, List<Integer> y, List<TripleInteger> tiShares,
//             int asymmetricBit, int constantMultiplier,
//             ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
//             BlockingQueue<Message> senderQueue,
//             Queue<Integer> protocolIdQueue,
//             int clientId, int prime, int protocolID, int partyCount

        for(int i=1; i<integer_acc+1; i++) {

//          OR_XOR orModule0 = new OR_XOR( subresult, zIntBits.subList(i, i + 1),
//                  binTriples, asymmetricBit, 1, pidMapper, commonSender,
//                  new LinkedList<>(protocolIdQueue), clientId,
//                  Constants.BINARY_PRIME, pid, partyCount );
//          pid++;
//          subresult.set(0, orModule0.call()[0]);
        }

        // assign 1/2 if the OR is true, else the linear


        // flip by negative and shift by 1/2
        return ONE_HALF;
    }


    // might be in wrong order -- check
    private List<Integer> intToBinary(BigInteger num, int len) {
        List<Integer> bits = new ArrayList();
        for (int i = 0; i < len; i++) {
            int n = (int)num.mod(TWO).longValue();
            bits.add(n);
            num.divide(TWO);
        }
        return bits;
    }


    // private BigInteger activation(BigInteger z) throws InterruptedException, ExecutionException {

    //     BitDecompositionBigInteger bitDecomp = new BitDecompositionBigInteger(
    //      z, binTriples, asymmetricBit, bitlength, pidMapper, commonSender,
    //      new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME,
    //      pid, partyCount, threadID);
    //     pid++;
    //     List<Integer> zBits = bitDecomp.call();

    //     List<Integer> negThreshold = asymmetricBit == 1 ?
       //       intToBinary(lowThreshold, bitlength) :
       //       Collections.nCopies(bitlength, 0);

    //     List<Integer> posThreshold = asymmetricBit == 1 ?
    //          intToBinary(highThreshold, bitlength) :
                // Collections.nCopies(bitlength, 0);

    //     // z >= - 0.5
    //     System.out.println("\tSizeof zBits: "+ zBits.size() + ", Sizeof negThreshold: " + negThreshold.size());
    //     Comparison cmp0 = new Comparison(zBits, negThreshold, binTriples, asymmetricBit, pidMapper, commonSender,
    //             new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME, pid, partyCount, threadID);
    //     int isGeqNegThreshold = cmp0.call();
    //     pid++;

    //     // z >= 0.5
    //     System.out.println("\tSizeof zBits: "+ zBits.size() + ", Sizeof posThreshold: " + posThreshold.size());
    //     Comparison cmp1 = new Comparison(zBits, posThreshold, binTriples, asymmetricBit, pidMapper, commonSender,
    //             new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME, pid, partyCount, threadID);
    //     int isGeqPosThreshold = cmp1.call();
    //     pid++;

    //     int isLtPosThreshold = Math.floorMod(isGeqPosThreshold + asymmetricBit, 2);

    //     // if z > neg threshold & z < pos threshold, z is in the linear range
    //     MultiplicationByte mult0 = new MultiplicationByte(
    //      isLtPosThreshold, isGeqNegThreshold, binTriples.get(binTiIndex), pidMapper,
    //      commonSender, new LinkedList<>(protocolIdQueue), clientId,
    //      Constants.BINARY_PRIME, pid, asymmetricBit, 0, partyCount, threadID);
    //     pid++;
    //     int inLinearRange = mult0.call();

    //     List<Byte> compResultsBin = new ArrayList<>();
    //     compResultsBin.add((byte)inLinearRange);
    //     compResultsBin.add((byte)isGeqPosThreshold);

    //     BigInteger[] compResults = CompareAndConvertField.changeBinaryToBigIntegerField(
    //      compResultsBin, bigIntTiShares, pid, pidMapper, commonSender,
    //      protocolIdQueue, asymmetricBit, clientId, prime, partyCount, threadID);
    //     pid++;

    //     // -0.5 >= z       :: 0
    //     // -0.5 < z =< 0.5 :: z + 0.5
    //     //  0.5 < z        :: 1
    //     BigInteger activationOutput = ((compResults[0].multiply(z.add(ONE_HALF)))
    //                                      .add(compResults[1].multiply(ONE)))
    //                                      .mod(prime);

    //     //activationOutput = LocalMath.truncate(z, prime, decimal_acc, asymmetricBit);
    //     return activationOutput;
    // }

}