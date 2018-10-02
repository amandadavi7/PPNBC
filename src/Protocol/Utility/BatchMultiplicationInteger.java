/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import TrustedInitializer.TripleInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Batch multiplication of list of xshares with yshares
 *
 * @author keerthanaa
 */
public class BatchMultiplicationInteger extends BatchMultiplication
        implements Callable<Integer[]> {

    List<Integer> x;
    List<Integer> y;
    List<TripleInteger> tiShares;

    int prime;
    
    public final static int PRIORITY = 0;

    /**
     * Constructor
     *
     * @param x share of x
     * @param y share of y
     * @param tiShares
     * @param pidMapper
     * @param senderQueue
     * @param protocolIdQueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param asymmetricBit
     * @param parentID
     * @param partyCount
     */
    public BatchMultiplicationInteger(List<Integer> x, List<Integer> y,
            List<TripleInteger> tiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, int prime, int protocolID, int asymmetricBit,
            int parentID, int partyCount) {

        super(pidMapper, senderQueue, protocolIdQueue, clientId, protocolID,
                asymmetricBit, parentID, partyCount);
        this.x = x;
        this.y = y;
        this.prime = prime;
        this.tiShares = tiShares;
    }

    /**
     * Waits for the shares of (x-u) and (y-v), computes the product and returns
     * the value
     *
     * @return shares of product
     * @throws Exception
     */
    @Override
    public Integer[] call() throws Exception {

        int batchSize = x.size();
        Integer[] products = new Integer[batchSize];

        initProtocol();
        Message receivedMessage = null;
        List<Integer> d = new ArrayList<>(Collections.nCopies(batchSize, 0));
        List<Integer> e = new ArrayList<>(Collections.nCopies(batchSize, 0));
        List<List<Integer>> diffList = null;
        for (int i = 0; i < partyCount - 1; i++) {
            try {
                receivedMessage = pidMapper.get(protocolIdQueue).take();
                diffList = (List<List<Integer>>) receivedMessage.getValue();
                for (int j = 0; j < batchSize; j++) {
                    d.set(j, Math.floorMod(d.get(j) + diffList.get(j).get(0), prime));
                    e.set(j, Math.floorMod(e.get(j) + diffList.get(j).get(1), prime));
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(BatchMultiplicationInteger.class.getName())
                    .log(Level.SEVERE, null, ex);
            }
        }

        for (int i = 0; i < batchSize; i++) {
            int D = Math.floorMod(x.get(i) - tiShares.get(i).u + d.get(i), prime);
            int E = Math.floorMod(y.get(i) - tiShares.get(i).v + e.get(i), prime);
            int product = tiShares.get(i).w + (D * tiShares.get(i).v)
                    + (tiShares.get(i).u * E) + (D * E * asymmetricBit);
            product = Math.floorMod(product, prime);
            products[i] = product;
        }

        return products;

    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    @Override
    void initProtocol() {
        List<List<Integer>> diffList = new ArrayList<>();
        int batchSize = x.size();

        for (int i = 0; i < batchSize; i++) {
            List<Integer> newRow = new ArrayList<>();
            newRow.add(Math.floorMod(x.get(i) - tiShares.get(i).u, prime));
            newRow.add(Math.floorMod(y.get(i) - tiShares.get(i).v, prime));
            diffList.add(newRow);
        }

        Message senderMessage = new Message(diffList,
                clientID, protocolIdQueue);

        try {
            senderQueue.put(senderMessage);
        } catch (InterruptedException ex) {
            Logger.getLogger(BatchMultiplicationInteger.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

    }

}
