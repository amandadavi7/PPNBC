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
import Protocol.Multiplication;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.List;
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
    
    int numerator1, denominator1, numerator2, denominator2, pid;
    List<Triple> decimalTiShares, binaryTiShares;
    
    // TODO - take 2 primes / the prime here is currently dummy
    public CrossMultiplyCompare(int numerator1, int denominator1, int numerator2, 
            int denominator2, int oneShare, List<Triple> decimaltiShares, 
            List<Triple> binaryTiShares, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID){
        
        super(protocolID, senderQueue, receiverQueue, clientId, prime, oneShare);
        
        this.numerator1 = numerator1;
        this.denominator1 = denominator1;
        this.numerator2 = numerator2;
        this.denominator2 = denominator2;
        this.decimalTiShares = decimaltiShares;
        this.binaryTiShares = binaryTiShares;
        this.pid = 0;
    }
    
    @Override
    public Integer call() throws Exception {
        
        int decimalTiIndex = 0, binaryTiIndex = 0;
        startHandlers();
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        
        //Crossmultiplications
        System.out.println("calling mult1");
        initQueueMap(recQueues, sendQueues, pid);
        
        Multiplication multiplicationModule = new Multiplication(numerator1,
                    denominator2, decimalTiShares.get(decimalTiIndex),
                    sendQueues.get(pid), recQueues.get(pid), clientID,
                    Constants.prime, pid, oneShare,0);
        
        Future<Integer> firstCrossMultiplication = es.submit(multiplicationModule);
        pid++;
        decimalTiIndex++;
        
        System.out.println("calling mult2");
        initQueueMap(recQueues, sendQueues, pid);
        Multiplication multiplicationModule2 = new Multiplication(numerator2,
                    numerator1, decimalTiShares.get(decimalTiIndex),
                    sendQueues.get(pid), recQueues.get(pid), clientID,
                    Constants.prime, pid, oneShare,0);
        
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
        initQueueMap(recQueues, sendQueues, pid);
        BitDecomposition firstTask = new BitDecomposition(first, binaryTiShares,
                                        oneShare, Constants.bitLength, sendQueues.get(pid), 
                                        recQueues.get(pid), clientID, Constants.binaryPrime, pid);
        pid++;
        Future<List<Integer>> future1 = es.submit(firstTask);
        
        System.out.println("calling bitD2");
        initQueueMap(recQueues, sendQueues, pid);
        BitDecomposition secondTask = new BitDecomposition(second, binaryTiShares,
                                        oneShare, Constants.bitLength, sendQueues.get(pid), 
                                        recQueues.get(pid), clientID, Constants.binaryPrime, pid);
        pid++;
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
        initQueueMap(recQueues, sendQueues, pid);
        System.out.println("calling comparison");
        Comparison comparisonModule = new Comparison(firstNumber,
                                     secondNumber, binaryTiShares, oneShare,
                                    sendQueues.get(pid), recQueues.get(pid), clientID, 
                                    Constants.binaryPrime, pid);
        
        Future<Integer> comparisonTask = es.submit(comparisonModule);
        
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
