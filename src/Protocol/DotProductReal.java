/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.BatchMultiplicationReal;
import TrustedInitializer.TripleReal;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dot product of two matrices x and y, shared element wise
 *
 * uses xShares.size() tiShares
 *
 * @author anisha
 */
public class DotProductReal extends DotProduct implements Callable<BigInteger> {

    private static final Logger LOGGER = Logger.getLogger(DotProductReal.class.getName());
    List<BigInteger> xShares, yShares;
    BigInteger prime;
    List<TripleReal> tiShares;

    /**
     * Constructor for DotProduct on Real Numbers
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
    public DotProductReal(List<BigInteger> xShares, List<BigInteger> yShares,
            List<TripleReal> tiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderqueue,
            Queue<Integer> protocolIdQueue,
            int clientID, BigInteger prime,
            int protocolID, int asymmetricBit, int partyCount) {

        super(pidMapper, senderqueue, protocolIdQueue, clientID, protocolID,
                asymmetricBit, partyCount);

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
    public BigInteger call() {

        BigInteger dotProduct = BigInteger.ZERO;
        int vectorLength = xShares.size();

        ExecutorService mults = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        ExecutorCompletionService<BigInteger[]> multCompletionService = new ExecutorCompletionService<>(mults);

        int i = 0;
        int startpid = 0;

        do {
            int toIndex = Math.min(i + Constants.BATCH_SIZE, vectorLength);

            multCompletionService.submit(new BatchMultiplicationReal(xShares.subList(i, toIndex),
                    yShares.subList(i, toIndex), tiShares.subList(i, toIndex), pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, startpid, asymmetricBit, protocolId, partyCount));

            startpid++;
            i = toIndex;

        } while (i < vectorLength);
        
        mults.shutdown();

        for (i = 0; i < startpid; i++) {
            try {
                Future<BigInteger[]> prod = multCompletionService.take();
                BigInteger[] products = prod.get();
                for (BigInteger j : products) {
                    dotProduct = dotProduct.add(j).mod(prime);
                }
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        return dotProduct;

    }

}
