/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Model.KNN;
import Protocol.BitDecomposition;
import Protocol.Comparison;
import Protocol.CompositeProtocol;
import Protocol.MultiplicationInteger;
import TrustedInitializer.Triple;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
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
public class CrossMultiplyCompare extends CompositeProtocol implements Callable<Integer> {
    
    int numerator1, denominator1, numerator2, denominator2, pid, decimalPrime, binaryPrime;
    List<TripleInteger> decimalTiShares;
    List<TripleByte> binaryTiShares;
    
    /**
     * Takes 2 decimal TI Shares and 
     * @param numerator1
     * @param denominator1
     * @param numerator2
     * @param denominator2
     * @param oneShare
     * @param decimaltiShares
     * @param binaryTiShares
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param decimalPrime
     * @param binaryPrime
     * @param protocolID
     * @param protocolIdQueue 
     */
    public CrossMultiplyCompare(int numerator1, int denominator1, int numerator2, 
            int denominator2, int oneShare, List<TripleInteger> decimaltiShares, 
            List<TripleByte> binaryTiShares, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int decimalPrime,
            int binaryPrime, int protocolID, Queue<Integer> protocolIdQueue, int partyCount){
        
        super(protocolID, senderQueue, receiverQueue, protocolIdQueue, clientId, oneShare,partyCount);
        
        this.numerator1 = numerator1;
        this.denominator1 = denominator1;
        this.numerator2 = numerator2;
        this.denominator2 = denominator2;
        this.decimalTiShares = decimaltiShares;
        this.binaryTiShares = binaryTiShares;
        this.decimalPrime = decimalPrime;
        this.binaryPrime = binaryPrime;
        this.pid = 0;
    }
    
    @Override
    public Integer call() throws Exception {
        
        int decimalTiIndex = 0, binaryTiIndex = 0;
        startHandlers();
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        
        //Crossmultiplications
        System.out.println("calling mult1");
        initQueueMap(recQueues, pid);
        
        MultiplicationInteger multiplicationModule = new MultiplicationInteger(numerator1,
                    denominator2, decimalTiShares.get(decimalTiIndex),
                    senderQueue, recQueues.get(pid), new LinkedList<>(protocolIdQueue), clientID,
                    decimalPrime, pid, oneShare,0,partyCount);
        
        Future<Integer> firstCrossMultiplication = es.submit(multiplicationModule);
        pid++;
        decimalTiIndex++;
        
        System.out.println("calling mult2");
        initQueueMap(recQueues, pid);
        MultiplicationInteger multiplicationModule2 = new MultiplicationInteger(numerator2,
                    denominator1, decimalTiShares.get(decimalTiIndex),
                    senderQueue, recQueues.get(pid), new LinkedList<>(protocolIdQueue), clientID,
                    decimalPrime, pid, oneShare,0,partyCount);
        
        Future<Integer> secondCrossMultiplication = es.submit(multiplicationModule2);
        pid++;
        decimalTiIndex++;
        
        int first = 0, second = 0;
        try {
            first = firstCrossMultiplication.get();
            second = secondCrossMultiplication.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("cross multiplication results: "+first+" and "+second);
        
        // TODO - binaryTiShares sublist in bit decompositions
        System.out.println("calling bitD1");
        initQueueMap(recQueues, pid);
        int binaryTiCount = 3*Constants.bitLength - 2;
        BitDecomposition firstTask = new BitDecomposition(first, binaryTiShares.subList(binaryTiIndex, binaryTiIndex + binaryTiCount),
                                        oneShare, Constants.bitLength, senderQueue, 
                                        recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                                        clientID, binaryPrime, pid,partyCount);
        pid++;
        binaryTiIndex += binaryTiCount;
        Future<List<Integer>> future1 = es.submit(firstTask);
        
        System.out.println("calling bitD2");
        initQueueMap(recQueues, pid);
        BitDecomposition secondTask = new BitDecomposition(second, binaryTiShares.subList(binaryTiIndex, binaryTiIndex + binaryTiCount),
                                        oneShare, Constants.bitLength, senderQueue, 
                                        recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                                        clientID, binaryPrime, pid,partyCount);
        pid++;
        binaryTiIndex += binaryTiCount;
        Future<List<Integer>> future2 = es.submit(secondTask);
        
        List<Integer> firstNumber = null, secondNumber = null;
        try {
            firstNumber = future1.get();
            secondNumber = future2.get();            
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("bitD results: " + firstNumber + " and " + secondNumber);
        
        // TODO - binaryti index management in Comparison
        initQueueMap(recQueues, pid);
        System.out.println("calling comparison");
        binaryTiCount = (2*Constants.bitLength) + (Constants.bitLength*(Constants.bitLength-1)/2);
        Comparison comparisonModule = new Comparison(firstNumber,
                                     secondNumber, binaryTiShares.subList(binaryTiIndex, binaryTiIndex + binaryTiCount), oneShare,
                                    senderQueue, recQueues.get(pid), new LinkedList<>(protocolIdQueue), 
                                    clientID, binaryPrime, pid, partyCount);
        
        Future<Integer> comparisonTask = es.submit(comparisonModule);
        pid++; 
        binaryTiIndex += binaryTiCount;
        es.shutdown();
        int result = 0;
        try {
            result = comparisonTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        tearDownHandlers();
        return result;
    }
    
}
