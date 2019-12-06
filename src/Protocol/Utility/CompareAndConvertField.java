/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.BitDecomposition;
import Protocol.BitDecompositionBigInteger;
import Protocol.Comparison;
import Protocol.OR_XOR;
import Protocol.OR_XOR_BigInteger;
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
import java.math.BigInteger;
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
            boolean changeFieldToDecimal,  List<TripleInteger> decimalTiShares,int threadID) throws InterruptedException, ExecutionException {
        
        ExecutorService es = Executors.newFixedThreadPool(2);
        List<Integer> xShares = null, yShares = null;
        int comparisonTICount = (2*bitLength) + ((bitLength*(bitLength-1))/2);
        int bitDTICount = bitLength*3 - 2;
        int binaryTiIndex = 0;
        
        // Do 2 bit decompositions in parallel for both the inputs and get bit shares
        
        BitDecomposition xBitTask = new BitDecomposition(x, 
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + bitDTICount),
                asymmetricBit, bitLength, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                clientId, Constants.BINARY_PRIME, pid, partyCount,threadID);
        pid++;
        binaryTiIndex += bitDTICount;
        Future<List<Integer>> future1 = es.submit(xBitTask);
        
        BitDecomposition yBitTask = new BitDecomposition(y,
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + bitDTICount),
                asymmetricBit, bitLength, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                clientId, Constants.BINARY_PRIME, pid, partyCount,threadID);
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
                clientId, Constants.BINARY_PRIME, pid, partyCount,threadID);
        
        int comparisonResult = comparisonModule.call();
        
        if(changeFieldToDecimal) {
            Integer[] result = changeBinaryToDecimalField(Arrays.asList(comparisonResult),
                    decimalTiShares, pid, pidMapper, senderQueue, protocolIdQueue,
                    asymmetricBit, clientId, prime, partyCount,threadID);
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
            int asymmetricBit, int clientId, int prime, int partyCount,int threadID) throws InterruptedException, ExecutionException {
        
        List<Integer> dummy = new ArrayList<>(Collections.nCopies(binaryNumbers.size(), 0));
        OR_XOR xorModule;
        
        if(asymmetricBit == 1) {
            xorModule = new OR_XOR(binaryNumbers, dummy, decimalTiShares, asymmetricBit,
                    2, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, partyCount,threadID);
        } else {
            xorModule = new OR_XOR(dummy, binaryNumbers, decimalTiShares, asymmetricBit,
                    2, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, partyCount,threadID);
        }
        
        Integer[] results = xorModule.call();
        return results;
    }   
    
    public static BigInteger[] changeBinaryToBigIntegerField(List<Byte> binaryNumbers,
            List<TripleBigInteger> bigIntTiShares, int pid,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, Queue<Integer> protocolIdQueue,
            int asymmetricBit, int clientId, BigInteger prime, int partyCount,int threadID) throws InterruptedException, ExecutionException {
        
        List<BigInteger> binaryNumbersBigInt = new ArrayList<>();
        for(int i=0; i< binaryNumbers.size(); i++) {
            binaryNumbersBigInt.add( binaryNumbers.get(i)==1 ? BigInteger.ONE : BigInteger.ZERO );
        }

        List<BigInteger> dummy = new ArrayList<>(Collections.nCopies(binaryNumbers.size(), BigInteger.ZERO));
       

        OR_XOR_BigInteger xorModule;
        
        if(asymmetricBit == 1) {
            xorModule = new OR_XOR_BigInteger(binaryNumbersBigInt, dummy, bigIntTiShares, asymmetricBit,
                    2, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, partyCount,threadID);
        } else {
            xorModule = new OR_XOR_BigInteger(dummy, binaryNumbersBigInt, bigIntTiShares, asymmetricBit,
                    2, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                    clientId, prime, pid, partyCount,threadID);
        }
        
        BigInteger[] results = xorModule.call();
        return results;
    } 

    /* 
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
     * @param changeFieldToBigInt
     * @param decimalTiShares
     * @return 
     * @throws java.lang.InterruptedException 
     * @throws java.util.concurrent.ExecutionException 
     */
    public static BigInteger compareBigIntegers(BigInteger x, BigInteger y, List<TripleByte> binaryTiShares,
            int asymmetricBit, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, Queue<Integer> protocolIdQueue,
            int clientId, BigInteger prime, int bitLength, int partyCount, int pid,
            boolean changeFieldToBigInt,  List<TripleBigInteger> bigIntTiShares,int threadID) throws InterruptedException, ExecutionException {
        
        ExecutorService es = Executors.newFixedThreadPool(2);
        List<Integer> xShares = null, yShares = null;
        int comparisonTICount = (2*bitLength) + ((bitLength*(bitLength-1))/2);
        int bitDTICount = bitLength*3 - 2;
        int binaryTiIndex = 0;
        
        // Do 2 bit decompositions in parallel for both the inputs and get bit shares
        
        BitDecompositionBigInteger xBitTask = new BitDecompositionBigInteger(x, 
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + bitDTICount),
                asymmetricBit, bitLength, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                clientId, Constants.BINARY_PRIME, pid, partyCount,threadID);
        pid++;
        binaryTiIndex += bitDTICount;
        Future<List<Integer>> future1 = es.submit(xBitTask);
        
        BitDecompositionBigInteger yBitTask = new BitDecompositionBigInteger(y,
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + bitDTICount),
                asymmetricBit, bitLength, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                clientId, Constants.BINARY_PRIME, pid, partyCount,threadID);
        pid++;
        binaryTiIndex += bitDTICount;
        Future<List<Integer>> future2 = es.submit(yBitTask);
        es.shutdown();
        
        xShares = future1.get();
        yShares = future2.get();
        
  //       System.out.println("x       : " + x);
  //       System.out.println("y       : " + y);
  //       System.out.print("x-decomp: ");
  //       for(int i=0; i<bitLength; i++)
  //       	System.out.print(xShares.get(i));
  //       System.out.println();
		// System.out.print("y-decomp: ");
  //       for(int i=0; i<bitLength; i++)
  //       	System.out.print(yShares.get(i));
  //       System.out.println();


        // call comparison between bitshares
        Comparison comparisonModule = new Comparison(xShares, yShares,
                binaryTiShares.subList(binaryTiIndex, binaryTiIndex + comparisonTICount), 
                asymmetricBit, pidMapper, senderQueue, new LinkedList<>(protocolIdQueue), 
                clientId, Constants.BINARY_PRIME, pid, partyCount,threadID);
        
        Integer comparisonResult = comparisonModule.call();
        
        if(changeFieldToBigInt) {
            BigInteger[] result = changeBinaryToBigIntegerField(Arrays.asList(comparisonResult.byteValue()),
                    bigIntTiShares, pid, pidMapper, senderQueue, protocolIdQueue,
                    asymmetricBit, clientId, prime, partyCount,threadID);
            return result[0];
        } else {
            return BigInteger.valueOf(comparisonResult);
        }
    }

}
