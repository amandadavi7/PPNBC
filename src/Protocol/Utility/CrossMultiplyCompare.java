/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.CompositeProtocol;
import Protocol.MultiplicationInteger;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
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

/**
 * Class to cross multiply and compare 2 fractions
 *
 * @author keerthanaa
 */
public class CrossMultiplyCompare extends CompositeProtocol implements Callable<Integer> {

    int numerator1, denominator1, numerator2, denominator2, pid, decimalPrime, binaryPrime;
    int bitDTICount, comparisonTICount, bitLength;
    List<TripleInteger> decimalTiShares;
    List<TripleByte> binaryTiShares;

    /**
     * Takes 2 decimal TI Shares and
     *
     * @param numerator1
     * @param denominator1
     * @param numerator2
     * @param denominator2
     * @param asymmetricBit
     * @param decimaltiShares
     * @param binaryTiShares
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param decimalPrime
     * @param binaryPrime
     * @param protocolID
     * @param protocolIdQueue
     * @param partyCount
     * @param bitLength
     */
    public CrossMultiplyCompare(int numerator1, int denominator1, int numerator2,
            int denominator2, int asymmetricBit, List<TripleInteger> decimaltiShares,
            List<TripleByte> binaryTiShares, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, int clientId, int decimalPrime,
            int binaryPrime, int protocolID, Queue<Integer> protocolIdQueue,
            int partyCount, int bitLength,int threadID) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId, asymmetricBit, partyCount,threadID);

        this.numerator1 = numerator1;
        this.denominator1 = denominator1;
        this.numerator2 = numerator2;
        this.denominator2 = denominator2;
        this.decimalTiShares = decimaltiShares;
        this.binaryTiShares = binaryTiShares;
        this.decimalPrime = decimalPrime;
        this.binaryPrime = binaryPrime;
        this.pid = 0;
        this.bitLength = bitLength;
        this.comparisonTICount = (2 * bitLength) + ((bitLength * (bitLength - 1)) / 2);
        this.bitDTICount = bitLength * 3 - 2;

    }

    /**
     *
     * @return
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Override
    public Integer call() throws InterruptedException, ExecutionException {

        int decimalTiIndex = 0, binaryTiIndex = 0;
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);

        //Crossmultiplications
        MultiplicationInteger multiplicationModule = new MultiplicationInteger(numerator1,
                denominator2, decimalTiShares.get(decimalTiIndex), pidMapper,
                senderQueue, new LinkedList<>(protocolIdQueue), clientID,
                decimalPrime, pid, asymmetricBit, 0, partyCount,threadID);

        Future<Integer> firstCrossMultiplication = es.submit(multiplicationModule);
        pid++;
        decimalTiIndex++;

        MultiplicationInteger multiplicationModule2 = new MultiplicationInteger(numerator2,
                denominator1, decimalTiShares.get(decimalTiIndex),
                pidMapper, senderQueue, new LinkedList<>(protocolIdQueue), clientID,
                decimalPrime, pid, asymmetricBit, 0, partyCount,threadID);

        Future<Integer> secondCrossMultiplication = es.submit(multiplicationModule2);
        pid++;
        decimalTiIndex++;

        int first = firstCrossMultiplication.get();
        int second = secondCrossMultiplication.get();

        es.shutdown();

        int result = CompareAndConvertField.compareIntegers(first, second, binaryTiShares,
                asymmetricBit, pidMapper, senderQueue, protocolIdQueue, clientID,
                pid, bitLength, partyCount, pid, false, null,threadID);

        //binaryTiIndex += 2*bitDTICount + comparisonTICount;
        return result;
    }

}
