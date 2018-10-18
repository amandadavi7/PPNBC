/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.BatchMultiplicationReal;
import Protocol.Utility.BatchTruncation;
import TrustedInitializer.TripleReal;
import TrustedInitializer.TruncationPair;
import Utility.Constants;
import java.math.BigInteger;
import java.util.ArrayList;
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

    List<BigInteger> xShares, yShares;
    BigInteger prime;
    List<TripleReal> tiShares;
    int globalProtocolId;
    List<TruncationPair> tiTruncationPair;

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
     * @param tiTruncationPair
     */
    public DotProductReal(List<BigInteger> xShares, List<BigInteger> yShares,
            List<TripleReal> tiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderqueue,
            Queue<Integer> protocolIdQueue,
            int clientID, BigInteger prime,
            int protocolID, int asymmetricBit, int partyCount,
            List<TruncationPair> tiTruncationPair) {

        super(pidMapper, senderqueue, protocolIdQueue, clientID, protocolID,
                asymmetricBit, partyCount);

        this.xShares = xShares;
        this.yShares = yShares;
        this.prime = prime;
        this.tiShares = tiShares;
        this.tiTruncationPair = tiTruncationPair;

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

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<BigInteger[]>> taskList = new ArrayList<>(vectorLength / Constants.BATCH_SIZE);

        int i = 0;

        do {
            int toIndex = Math.min(i + Constants.BATCH_SIZE, vectorLength);

            BatchMultiplicationReal batchMultiplicationReal = 
                    new BatchMultiplicationReal(xShares.subList(i, toIndex),
                    yShares.subList(i, toIndex), tiShares.subList(i, toIndex), 
                            pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalProtocolId, asymmetricBit, protocolId, partyCount);

            taskList.add(es.submit(batchMultiplicationReal));

            globalProtocolId++;
            i = toIndex;

        } while (i < vectorLength);
        
        int testCases = globalProtocolId;

        //mults.shutdown();
        List<Future<BigInteger[]>> taskListTruncation = new ArrayList<>(testCases);
        int tiTruncationStartIndex = 0;

        for (i = 0; i < testCases; i++) {
            try {
                Future<BigInteger[]> prod = taskList.get(i);
                BigInteger[] products = prod.get();
                
                // Call batch truncation on each product
                BatchTruncation truncationModule = new BatchTruncation(products,
                        tiTruncationPair, pidMapper, senderQueue,
                        new LinkedList<>(protocolIdQueue),
                        clientID, prime, globalProtocolId++, asymmetricBit, partyCount);
                
                
                Future<BigInteger[]> truncationTask = es.submit(truncationModule);
                taskListTruncation.add(truncationTask);

            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(DotProductReal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        for (i = 0; i < taskListTruncation.size(); i++) {
            Future<BigInteger[]> dWorkerResponse = taskListTruncation.get(i);
            BigInteger[] products;
            try {
                products = dWorkerResponse.get();
                for (BigInteger j : products) {
//                    if(protocolId == 0) {
//                        System.out.print(" " + j);
//                    }
                    dotProduct = dotProduct.add(j).mod(prime);
                }
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(DotProductReal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

//        if(protocolId == 0) {
//            System.out.println("");
//            System.out.println("Dot product done for protocol id:" + protocolId);
//        }
//            
        //System.out.println("dot product result:" + dotProduct);
        es.shutdown();

        return dotProduct;

    }

}
