/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.BitDecomposition;
import Protocol.Comparison;
import Protocol.OR_XOR;
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

/**
 *
 * @author keerthanaa
 */
public class CompareAndConvertField {
    
    /**
     * Take 2 integers and bit decompose and compare them
     * provide changeFieldToDecimal as true and decimal TI shares if field needs
     * to be changed
     * 
     * @param x
     * @param y
     * @param binaryTiShares
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param protocolIdQueue
     * @param clientId
     * @param prime
     * @param bitLength
     * @param partyCount
     * @param pid
     * @param changeFieldToDecimal
     * @param decimalTiShares
     * @return 
     * @throws java.lang.InterruptedException 
     * @throws java.util.concurrent.ExecutionException 
     */
    public static int compareIntegers(int x, int y, List<TripleByte> binaryTiShares,
            int asymmetricBit, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, Queue<Integer> protocolIdQueue,
            int clientId, int prime, int bitLength, int partyCount, int pid,
            boolean changeFieldToDecimal,  List<TripleInteger> decimalTiShares) throws InterruptedException, ExecutionException {
        
        ExecutorService es = Executors.newFixedThreadPool(2);
        List<Integer> xShares = null, yShares = null;
        int comparisonTICount = (2*bitLength) + ((bitLength*(bitLength-1))/2);
        int bitDTICount = bitLength*3 - 2;
        int binaryTiIndex = 0;
        
        // Do 2 bit decompositions in parallel for both the inputs and get bit shares
        
        BitDecomposition xBitTask = new BitDecomposition(x, 
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + bitDTICount),
                asymmetricBit, bitLength, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                clientId, Constants.BINARY_PRIME, pid, partyCount);
        pid++;
        binaryTiIndex += bitDTICount;
        Future<List<Integer>> future1 = es.submit(xBitTask);
        
        BitDecomposition yBitTask = new BitDecomposition(y,
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + bitDTICount),
                asymmetricBit, bitLength, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                clientId, Constants.BINARY_PRIME, pid, partyCount);
        pid++;
        binaryTiIndex += bitDTICount;
        Future<List<Integer>> future2 = es.submit(yBitTask);
        es.shutdown();
        
        xShares = future1.get();
        yShares = future2.get();
        
        // call comparison between bitshares
        Comparison comparisonModule = new Comparison(xShares, yShares, 
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + comparisonTICount), 
                asymmetricBit, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue), 
                clientId, Constants.BINARY_PRIME, pid, partyCount);
        
        int comparisonResult = comparisonModule.call();
        
        if(changeFieldToDecimal) {
            Integer[] result = changeBinaryToDecimalField(Arrays.asList(comparisonResult),
                    decimalTiShares, pid, pidMapper, senderQueue, protocolIdQueue,
                    asymmetricBit, clientId, prime, partyCount);
            return (int) result[0];
        } else {
            return comparisonResult;
        }
    }
    
    /**
     * Takes a list of binary numbers and converts them to decimal prime
     * 
     * @param binaryNumbers
     * @param decimalTiShares
     * @param pid
     * @param pidMapper
     * @param senderQueue
     * @param protocolIdQueue
     * @param asymmetricBit
     * @param clientId
     * @param prime
     * @param partyCount
     * @return 
     * @throws java.lang.InterruptedException 
     * @throws java.util.concurrent.ExecutionException 
     */
    public static Integer[] changeBinaryToDecimalField(List<Integer> binaryNumbers,
            List<TripleInteger> decimalTiShares, int pid,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, Queue<Integer> protocolIdQueue,
            int asymmetricBit, int clientId, int prime, int partyCount) throws InterruptedException, ExecutionException {
        
        List<Integer> dummy = new ArrayList<>(Collections.nCopies(binaryNumbers.size(), 0));
        OR_XOR xorModule;
        
        if(asymmetricBit == 1) {
            xorModule = new OR_XOR(binaryNumbers, dummy, decimalTiShares, asymmetricBit,
                    2, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, partyCount);
        } else {
            xorModule = new OR_XOR(dummy, binaryNumbers, decimalTiShares, asymmetricBit,
                    2, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, partyCount);
        }
        
        Integer[] results = xorModule.call();
        return results;
    }   
    
}
