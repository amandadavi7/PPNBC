/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.BatchMultiplicationByte;
import Protocol.Utility.BatchMultiplicationInteger;
import TrustedInitializer.Triple;
import TrustedInitializer.TripleByte;
import Utility.Constants;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author keerthanaa
 */
public class DotProductByte extends DotProduct implements Callable<Integer> {

    List<Integer> xShares, yShares;
    List<TripleByte> tiShares;
    int prime;

    /**
     * Constructor
     *
     * @param xShares
     * @param yShares
     * @param tiShares
     * @param senderqueue
     * @param receiverqueue
     * @param protocolIdQueue
     * @param clientID
     * @param prime
     * @param protocolID
     * @param asymmetricBit
     */
    public DotProductByte(List<Integer> xShares, List<Integer> yShares, List<TripleByte> tiShares,
            BlockingQueue<Message> senderqueue, BlockingQueue<Message> receiverqueue,
            Queue<Integer> protocolIdQueue,
            int clientID, int prime, int protocolID, int asymmetricBit, int partyCount) {
        
        super(senderqueue, receiverqueue, protocolIdQueue,clientID, protocolID, asymmetricBit, partyCount);
        
        this.xShares = xShares;
        this.yShares = yShares;
        this.tiShares = tiShares;
        this.prime = prime;
        
    }

    /**
     * Do a batch multiplication on chunks of vector, collate the results and 
     * return
     * 
     * @return
     */
    @Override
    public Integer call() {

        int dotProduct = 0;
        int vectorLength = xShares.size();
        startHandlers();
        
        ExecutorService mults = Executors.newFixedThreadPool(Constants.threadCount);
        ExecutorCompletionService<Integer[]> multCompletionService = new ExecutorCompletionService<>(mults);
        
        int i=0;
        int startpid = 0;
        
        do {
            int toIndex = Math.min(i+Constants.batchSize,vectorLength);
            
            System.out.println("Protocol "+protocolId+" batch "+startpid);
            initQueueMap(recQueues, startpid);
            
            multCompletionService.submit(new BatchMultiplicationByte(xShares.subList(i, toIndex), 
                    yShares.subList(i, toIndex), tiShares.subList(i, toIndex), senderQueue, 
                    recQueues.get(startpid), new LinkedList<>(protocolIdQueue),
                    clientID, prime, startpid, asymmetricBit, protocolId, partyCount));
            
            startpid++;
            i = toIndex;
            
        } while(i < vectorLength);

        mults.shutdown();

        for (i = 0; i < startpid; i++) {
            try {
                Future<Integer[]> prod = multCompletionService.take();
                Integer[] products = prod.get();
                for(int j: products){
                    dotProduct+=j;
                }
            } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
            }
        }

        tearDownHandlers();
        
        dotProduct = Math.floorMod(dotProduct, prime);
        System.out.println("dot product:" + dotProduct + ", protocol id:" + protocolId);
        return dotProduct;

    }

}
