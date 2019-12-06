/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.BatchMultiplicationBigInteger;
import TrustedInitializer.TripleBigInteger;
import Utility.Constants;
import java.math.BigInteger;
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

/**
 * Dot product of two matrices x and y, shared element wise
 *
 * uses xShares.size() tiShares
 *
 * @author anisha
 */
public class DotProductBigInteger extends DotProduct implements Callable<BigInteger> {

    List<BigInteger> xShares, yShares;
    BigInteger prime;
    List<TripleBigInteger> tiShares;

    /**
     * Constructor for DotProduct on BigInteger Numbers
     *
     * @param xShares
     * @param yShares
     * @param tiShares
     * @param senderqueue
     * @param pidMapper
     * @param protocolIdQueue
     * @param clientID
     * @param prime
     * @param protocolID
     * @param asymmetricBit
     * @param partyCount
     */
    public DotProductBigInteger(List<BigInteger> xShares, List<BigInteger> yShares,
            List<TripleBigInteger> tiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderqueue,
            Queue<Integer> protocolIdQueue,
            int clientID, BigInteger prime,
            int protocolID, int asymmetricBit, int partyCount, int threadID) {

        super(pidMapper, senderqueue, protocolIdQueue, clientID, protocolID,
                asymmetricBit, partyCount, threadID);

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
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Override
    public BigInteger call() throws InterruptedException, ExecutionException {

        BigInteger dotProduct = BigInteger.ZERO;
        int vectorLength = xShares.size();

        ExecutorService mults = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        ExecutorCompletionService<BigInteger[]> multCompletionService = new ExecutorCompletionService<>(mults);

        int i = 0;
        int startpid = 0;

        do {
           // System.out.println("\t[i="+i+"]: creating batch mult task");
            int toIndex = Math.min(i + Constants.BATCH_SIZE, vectorLength);

            multCompletionService.submit(new BatchMultiplicationBigInteger(xShares.subList(i, toIndex),
                    yShares.subList(i, toIndex), tiShares.subList(i, toIndex), pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, startpid, asymmetricBit, protocolId, partyCount, threadID));

            startpid++;
            i = toIndex;

        } while (i < vectorLength);

        mults.shutdown();
       
        for (i = 0; i < startpid; i++) {
            //System.out.println("\t[i="+i+"]: taking instance of dp");
            Future<BigInteger[]> prod = multCompletionService.take();
            //System.out.println("\t[i="+i+"]: getting product");
            BigInteger[] products = prod.get();
            for (BigInteger j : products) {
                dotProduct = dotProduct.add(j).mod(prime);
            }
        }

        //System.out.println("\tReturning result");
        
        return dotProduct;
    }

}
