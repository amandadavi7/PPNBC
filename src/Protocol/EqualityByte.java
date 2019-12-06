/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.TripleByte;
import Utility.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.LinkedList;

/**
 * The protocol compares two numbers x and y in bits and returns shares of 1 if 
 * x = y and shares of 0 otherwise
 * 
 * The input to the protocol is shares of x and y in bits
 * 
 * @author ariel
 */
public class EqualityByte extends CompositeProtocol implements Callable<Integer> {

    List<Integer> x;
    List<Integer> y;
    TripleByte tiShares;
    
/**
     * Constructor
     *
     * Equality test between two numbers
     *
     * @param x List of bits of shares of x
     * @param y List of bits of shares of y
     * @param tiShares
     * @param pidMapper
     * @param senderQueue
     * @param protocolIdQueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param asymmetricBit [[1]] with the Party
     * @param partyCount
     */
    public EqualityByte(List<Integer> x, List<Integer> y, TripleByte tiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, int prime, int protocolID, 
            int asymmetricBit, int partyCount, int threadID) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue,
                clientId, asymmetricBit, partyCount, threadID);

        this.x = x;
        this.y = y;
        this.tiShares = tiShares;

    }

    
    /**
     * Locally computes ri = xi + yi and uses MultiplicationByte protocol
     * to multiply across all ri's. Returns shares of 1 if x=y, 0 otherwise
     *
     * @return shares of [1] if x=y and [0] otherwise
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Override
    public Integer call() throws InterruptedException, ExecutionException {

        //System.out.println("x=" + x + " y=" + y);

        int bitLength = x.size();
        ArrayList<Integer> r = new ArrayList<>();
        for (int i = 0; i < bitLength; i++) {
            int bitResult = Math.floorMod(x.get(i) + y.get(i) + asymmetricBit, Constants.BINARY_PRIME);
            r.add(bitResult);
        }

        // call MultiplicationByte protocol: multiplies sequentially across r vector
        int result = r.get(0);
        int pid = 0;
        for (int i = 1; i < bitLength; i++) {
            MultiplicationByte MBModule = new MultiplicationByte(result, r.get(i), tiShares, pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue), clientID, Constants.BINARY_PRIME, pid, asymmetricBit, 0, partyCount, threadID);
            result = MBModule.call();
            pid++;
        }

        return result;

    }
}