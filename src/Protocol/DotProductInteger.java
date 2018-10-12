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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author keerthanaa
 */
public class DotProductInteger extends DotProduct implements Callable<Integer> {

    List<Integer> xShares, yShares;
    List<TripleInteger> tiShares;
    int prime;

    /**
     * Constructor for DotProduct on Integers
     *
     * @param xShares
     * @param yShares
     * @param tiShares
     * @param pidMapper
     * @param senderqueue
     * @param protocolIdQueue
     * @param clientID
     * @param prime
     * @param protocolID
     * @param asymmetricBit
     * @param partyCount
     */
    public DotProductInteger(List<Integer> xShares, List<Integer> yShares, 
            List<TripleInteger> tiShares, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderqueue, 
            Queue<Integer> protocolIdQueue,
            int clientID, int prime, int protocolID, int asymmetricBit, int partyCount) {

        super(pidMapper, senderqueue, protocolIdQueue, clientID, protocolID, asymmetricBit, partyCount);

        this.xShares = xShares;
        this.yShares = yShares;
        this.prime = prime;
        this.tiShares = tiShares;

    }

    /**
     * Do a BatchMultiplication on chunks of vector, collate the results and
     * return
     *
     * @return
     */
    @Override
    public Integer call() {

        int dotProduct = 0;
        int vectorLength = xShares.size();
        
        ExecutorService mults = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        ExecutorCompletionService<Integer[]> multCompletionService = new ExecutorCompletionService<>(mults);

        int i = 0;
        int startpid = 0;

        do {
            int toIndex = Math.min(i + Constants.BATCH_SIZE, vectorLength);

            System.out.println("Protocol " + protocolId + " batch " + startpid);
            multCompletionService.submit(new BatchMultiplicationInteger(xShares.subList(i, toIndex),
                    yShares.subList(i, toIndex), tiShares.subList(i, toIndex), 
                    pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                    clientID, prime, startpid, asymmetricBit, protocolId, partyCount));

            startpid++;
            i = toIndex;

        } while (i < vectorLength);

        mults.shutdown();

        for (i = 0; i < startpid; i++) {
            try {
                Future<Integer[]> prod = multCompletionService.take();
                Integer[] products = prod.get();
                for (int j : products) {
                    dotProduct = Math.floorMod(dotProduct + j, prime);
                }
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(DotProductInteger.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        dotProduct = Math.floorMod(dotProduct, prime);
        System.out.println("dot product:" + dotProduct + ", protocol id:" + protocolId);
        return dotProduct;

    }

}
