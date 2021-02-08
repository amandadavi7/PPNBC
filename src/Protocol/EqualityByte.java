/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.ParallelMultiplication;
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
    List<TripleByte> tiShares;
    
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
    public EqualityByte(List<Integer> x, List<Integer> y, List<TripleByte> tiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, int prime, int protocolID, 
            int asymmetricBit, int partyCount) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue,
                clientId, asymmetricBit, partyCount);

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

 //       System.out.println("x=" + x + " y=" + y);
 //       System.out.println("bitLength: " + x.size());

        int bitLength = x.size();
        // x é um vetor com o secret sharing de cada bit
        // y é é um vetor com o secret sharing de cada bit
        // r é o vetor resultante da comparação se x é igual a y onde cada posição de r é 0 quando os bits são diferentes e 1 quando os bits são iguais
        // todas as operações são em inteiros porque esta em secret sharing
        ArrayList<Integer> r = new ArrayList<>();
        for (int i = 0; i < bitLength; i++) {
            int bitResult = Math.floorMod(x.get(i) + y.get(i) + asymmetricBit, Constants.BINARY_PRIME);
            r.add(bitResult);
        }
        
        return new ParallelMultiplication(r, tiShares, clientID, Constants.BINARY_PRIME, 
        			0, asymmetricBit, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),partyCount).call();

    }
}
