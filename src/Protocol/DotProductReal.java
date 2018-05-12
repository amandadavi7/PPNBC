/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.BatchMultiplicationReal;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.math.BigInteger;
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
 * @author anisha
 */
public class DotProductReal extends DotProduct implements Callable<BigInteger> {

    List<BigInteger> xShares, yShares;
    BigInteger prime;

    /**
     * Constructor
     *
     * @param xShares
     * @param yShares
     * @param tiShares
     * @param senderqueue
     * @param receiverqueue
     * @param clientID
     * @param prime
     * @param protocolID
     * @param oneShare
     */
    public DotProductReal(List<BigInteger> xShares, List<BigInteger> yShares, 
            List<Triple> tiShares, BlockingQueue<Message> senderqueue, 
            BlockingQueue<Message> receiverqueue, Queue<Integer> protocolIdQueue,
            int clientID, BigInteger prime, 
            int protocolID, int oneShare) {
        
        super(tiShares, senderqueue, receiverqueue, protocolIdQueue,clientID, protocolID, oneShare);
        
        this.xShares = xShares;
        this.yShares = yShares;
        this.prime = prime;
        
    }

    /**
     * Do a batchmultiplication on chunks of vector, collate the results and return (10 mults per batch)
     * 
     * @return
     */
    @Override
    public BigInteger call() {

        BigInteger dotProduct = BigInteger.ZERO;
        int vectorLength = xShares.size();
        startHandlers();
        
        ExecutorService mults = Executors.newFixedThreadPool(Constants.threadCount);
        ExecutorCompletionService<BigInteger[]> multCompletionService = new ExecutorCompletionService<>(mults);
        
        int i=0;
        int startpid = 0;
        
        do {
            int toIndex = Math.min(i+Constants.batchSize,vectorLength);
            
            initQueueMap(recQueues, startpid);
            
            multCompletionService.submit(new BatchMultiplicationReal(xShares.subList(i, toIndex), 
                    yShares.subList(i, toIndex), tiShares.subList(i, toIndex), senderQueue, 
                    recQueues.get(startpid), new LinkedList<>(protocolIdQueue),
                    clientID, prime, startpid, oneShare, protocolId));
            
            startpid++;
            i = toIndex;
            
        } while(i < vectorLength);

        mults.shutdown();

        for (i = 0; i < startpid; i++) {
            try {
                Future<BigInteger[]> prod = multCompletionService.take();
                BigInteger[] products = prod.get();
                for(BigInteger j: products){
                    dotProduct = dotProduct.add(j);
                }
            } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
            }
        }

        tearDownHandlers();
        
        dotProduct = dotProduct.mod(prime);
        return dotProduct;

    }

}
