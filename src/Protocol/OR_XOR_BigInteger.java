/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.BatchMultiplicationInteger;
import Protocol.Utility.BatchMultiplicationBigInteger;
import TrustedInitializer.TripleInteger;
import TrustedInitializer.TripleBigInteger;

import Utility.Constants;
import java.util.ArrayList;
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
import java.math.BigInteger;
/**
 *
 * @author keerthanaa
 */
public class OR_XOR_BigInteger extends CompositeProtocol implements Callable<BigInteger[]> {

    List<BigInteger> xShares, yShares;
    BigInteger constantMultiplier;
    List<TripleBigInteger> bigIntTiShares;
    int bitLength;
    BigInteger prime;

    /**
     * constantMultiplier = 1 for OR constantMultiplier = 2 for XOR Does OR or
     * XOR between given list of integers (size k) Takes k TI Shares
     *
     * @param x
     * @param y
     * @param tiShares
     * @param asymmetricBit
     * @param constantMultiplier
     * @param pidMapper
     * @param senderQueue
     * @param protocolIdQueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param partyCount
     */
    public OR_XOR_BigInteger(List<BigInteger> x, List<BigInteger> y, List<TripleBigInteger> tiShares,
            int asymmetricBit, int constantMultiplier,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, BigInteger prime, int protocolID, int partyCount,int threadID) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId, asymmetricBit, partyCount,threadID);

        this.xShares = x;
        this.yShares = y;
        this.prime = prime;
        bitLength = xShares.size();
        this.constantMultiplier = BigInteger.valueOf(constantMultiplier);
        this.bigIntTiShares = tiShares;
    }

    /**
     *
     * @return
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Override
    public BigInteger[] call() throws InterruptedException, ExecutionException {
        BigInteger[] output = new BigInteger[bitLength];
        //System.out.println("x=" + xShares + " y=" + yShares);
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);

        List<Future<BigInteger[]>> taskList = new ArrayList<>();

        int i = 0;
        int startpid = 0;

        do {

            int toIndex = Math.min(i + Constants.BATCH_SIZE, bitLength);

            BatchMultiplicationBigInteger batchMultiplication = new BatchMultiplicationBigInteger(
                    xShares.subList(i, toIndex),
                    yShares.subList(i, toIndex),
                    bigIntTiShares.subList(i, toIndex), pidMapper,
                    senderQueue, new LinkedList<>(protocolIdQueue),
                    clientID, prime, startpid, asymmetricBit, protocolId, partyCount,threadID);

            Future<BigInteger[]> multiplicationTask = es.submit(batchMultiplication);
            taskList.add(multiplicationTask);

            startpid++;
            i += Constants.BATCH_SIZE;
        } while (i < bitLength);

        es.shutdown();
        int globalIndex = 0;
        int taskLen = taskList.size();

        for (i = 0; i < taskLen; i++) {
            Future<BigInteger[]> prod = taskList.get(i);
            BigInteger[] products = prod.get();
            int prodLen = products.length;
            for (int j = 0; j < prodLen; j++) {
                output[globalIndex] = ((xShares.get(globalIndex)
                        .add(yShares.get(globalIndex)))
                        .subtract(constantMultiplier.multiply(products[j])))
                        .mod(prime);
           
                globalIndex++;
            }
        }


                // output[globalIndex] = Math.floorMod(xShares.get(globalIndex) + yShares.get(globalIndex)
                //         - (constantMultiplier * products[j]), prime);
        return output;
    }

}
