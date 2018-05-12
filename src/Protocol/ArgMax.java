/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.ParallelMultiplication;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author keerthanaa
 */
public class ArgMax extends CompositeProtocol implements Callable<Integer[]> {

    List<List<Integer>> vShares;
    List<Triple> tiShares;    
    int bitLength, numberCount, prime;

    HashMap<Integer, ArrayList<Integer>> wIntermediate;
    Integer[] wOutput;

    /**
     * vShares - shares of all the numbers to be compared (k numbers) each share
     * vShare(j) contains l bits
     *
     * @param vShares
     * @param tiShares
     * @param oneShare
     * @param senderQueue
     * @param receiverQueue
     * @param protocolIdQueue
     * @param clientId
     * @param prime
     * @param protocolID
     */
    public ArgMax(List<List<Integer>> vShares, List<Triple> tiShares,
            int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientId, int prime,
            int protocolID) {
        
        super(protocolID, senderQueue, receiverQueue, protocolIdQueue, clientId, oneShare);
        
        this.vShares = vShares;
        this.tiShares = tiShares;
        this.prime = prime;

        numberCount = vShares.size();
        bitLength = 0;
        for (List<Integer> row : vShares) {
            bitLength = Math.max(bitLength, row.size());
        }

        wIntermediate = new HashMap<>();
        for (int i = 0; i < numberCount; i++) {
            wIntermediate.put(i, new ArrayList<>());
        }

        wOutput = new Integer[numberCount];

    }

    /**
     * Computes ArgMax by invoking comparison and multiplication protocols
     *
     * @return
     * @throws Exception
     */
    @Override
    public Integer[] call() throws Exception {
        
        if (numberCount == 1) {
            wOutput[0] = 1;
            return wOutput;
        }
        
        startHandlers();

        int tiIndex = computeComparisons();
        computeW(tiIndex);

        tearDownHandlers();
        return wOutput;
    }

    /**
     * each number in (1,..,k) to be compared with (k-1) other numbers does
     * multithreaded k*(k-1) comparisons and stores the results in wIntermediate
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private int computeComparisons() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        int tiIndex = 0;
        //int tiCount = 3 * bitLength - 3;
        int tiCount = 2*bitLength + bitLength*(bitLength-1)/2;
        for (int i = 0; i < numberCount; i++) {
            for (int j = 0; j < numberCount; j++) {
                if (i != j) {
                    int key = (i * numberCount) + j;
                    
                    initQueueMap(recQueues, key);
                    
                    //Extract the required number of tiShares and pass it to comparison protocol
                    //each comparison needs tiCount shares
                    List<Triple> tiComparsion = tiShares.subList(tiIndex, tiIndex + tiCount);
                    tiIndex += tiCount;
                    Comparison comparisonModule = new Comparison(vShares.get(i), vShares.get(j), tiComparsion,
                            oneShare, senderQueue, recQueues.get(key), new LinkedList<>(protocolIdQueue),clientID, prime, key);
                    Future<Integer> comparisonTask = es.submit(comparisonModule);
                    taskList.add(comparisonTask);
                }
            }
        }

        es.shutdown();

        for (int i = 0; i < numberCount * (numberCount - 1); i++) {
            Future<Integer> w_temp = taskList.get(i);
            int key = i / (numberCount - 1);
            wIntermediate.get(key).add(w_temp.get());
        }
        
        for (int i = 0; i < numberCount; i++) {
            System.out.println("w[" + Integer.toString(i) + "]:" + wIntermediate.get(i));
        }

        return tiIndex;
    }

    /**
     * Create numberCount threads to multiply all w values for each entry in
     * HashMap store the result in wOutput
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void computeW(int tiIndex) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();

        //Each row has n-2 multiplications to do for n-1 numbers
        int tiCount = numberCount - 2;
        
        for (int i = 0; i < numberCount; i++) {
            
            List<Triple> tishares = tiShares.subList(tiIndex, tiIndex + tiCount);
            tiIndex += tiCount;
            
            initQueueMap(recQueues, i);
            
            ParallelMultiplication rowMultiplication = new ParallelMultiplication(
                    wIntermediate.get(i), tishares, clientID, prime, i, 
                    oneShare, senderQueue, recQueues.get(i), 
                    new LinkedList<>(protocolIdQueue));
            
            Future<Integer> wRowProduct = es.submit(rowMultiplication);
            taskList.add(wRowProduct);
        }

        for (int i = 0; i < numberCount; i++) {
            Future<Integer> w_temp = taskList.get(i);
            wOutput[i] = w_temp.get();
        }

        es.shutdown();
    }

}
