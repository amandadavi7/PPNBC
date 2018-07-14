/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.BatchMultiplicationInteger;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
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
public class OR_XOR extends CompositeProtocol implements Callable<Integer[]> {

    List<Integer> xShares, yShares;
    int constantMultiplier;
    List<TripleInteger> decimalTiShares;
    int bitLength;
    int prime;

    /**
     * constantMultiplier = 1 for OR constantMultiplier = 2 for XOR 
     * Does OR or XOR between given list of integers (size k) 
     * Takes k TI Shares
     *
     * @param x
     * @param y
     * @param tiShares
     * @param asymmetricBit
     * @param constantMultiplier
     * @param senderQueue
     * @param receiverQueue
     * @param protocolIdQueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param partyCount
     */
    public OR_XOR(List<Integer> x, List<Integer> y, List<TripleInteger> tiShares,
            int asymmetricBit, int constantMultiplier, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientId, int prime, int protocolID, int partyCount) {

        super(protocolID, senderQueue, receiverQueue, protocolIdQueue, clientId, asymmetricBit, partyCount);

        this.xShares = x;
        this.yShares = y;
        this.prime = prime;
        bitLength = xShares.size();
        this.constantMultiplier = constantMultiplier;
        this.decimalTiShares = tiShares;
    }

    /**
     * 
     * @return 
     */
    @Override
    public Integer[] call() {
        startHandlers();
        Integer[] output = new Integer[bitLength];
        System.out.println("x=" + xShares + " y=" + yShares);
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        int i = 0;
        int startpid = 0;

        do {
            System.out.println("Protocol " + protocolId + " batch " + startpid);
            initQueueMap(recQueues, startpid);

            int toIndex = Math.min(i + Constants.batchSize, bitLength);

            BatchMultiplicationInteger batchMultiplication = new BatchMultiplicationInteger(
                    xShares.subList(i, toIndex),
                    yShares.subList(i, toIndex),
                    decimalTiShares.subList(i, toIndex),
                    senderQueue, recQueues.get(startpid), new LinkedList<>(protocolIdQueue),
                    clientID, prime, startpid, asymmetricBit, protocolId, partyCount);

            Future<Integer[]> multiplicationTask = es.submit(batchMultiplication);
            taskList.add(multiplicationTask);

            startpid++;
            i += Constants.batchSize;
        } while (i < bitLength);

        es.shutdown();
        int globalIndex = 0;
        int taskLen = taskList.size();

        for (i = 0; i < taskLen; i++) {
            try {
                Future<Integer[]> prod = taskList.get(i);
                Integer[] products = prod.get();
                int prodLen = products.length;
                for (int j = 0; j < prodLen; j++) {
                    output[globalIndex] = Math.floorMod(xShares.get(globalIndex) + yShares.get(globalIndex)
                            - (constantMultiplier * products[j]), prime);
                    globalIndex++;
                }
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(OR_XOR.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        tearDownHandlers();
        return output;
    }

}
