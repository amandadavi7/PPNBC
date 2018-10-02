/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import TrustedInitializer.TripleReal;
import java.math.BigInteger;
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
public class BatchMultiplicationReal extends BatchMultiplication
        implements Callable<BigInteger[]> {

    List<BigInteger> x;
    List<BigInteger> y;
    List<TripleReal> tiShares;

    BigInteger prime;
    
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
    public BatchMultiplicationReal(List<BigInteger> x, List<BigInteger> y,
            List<TripleReal> tiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, BigInteger prime, int protocolID, int asymmetricBit,
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
    public BigInteger[] call() throws Exception {

        int batchSize = x.size();
        BigInteger[] products = new BigInteger[batchSize];

        initProtocol();
        Message receivedMessage = null;
        List<BigInteger> d = new ArrayList<>(Collections.nCopies(batchSize, 
                BigInteger.ZERO));
        List<BigInteger> e = new ArrayList<>(Collections.nCopies(batchSize, 
                BigInteger.ZERO));
        List<List<BigInteger>> diffList = null;
        for (int i = 0; i < partyCount - 1; i++) {
            try {
                receivedMessage = pidMapper.get(protocolIdQueue).take();
                diffList = (List<List<BigInteger>>) receivedMessage.getValue();
                for(int j=0;j<batchSize;j++) {
                    d.set(j, d.get(j).add(diffList.get(j).get(0)).mod(prime));
                    e.set(j, e.get(j).add(diffList.get(j).get(1)).mod(prime));
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(BatchMultiplicationReal.class.getName())
                    .log(Level.SEVERE, null, ex);
            }
        }

        for (int i = 0; i < batchSize; i++) {
            // TODO convert TI share to BigInteger
            BigInteger D = x.get(i)
                    .subtract(tiShares.get(i).u)
                    .add(d.get(i)).mod(prime);
            BigInteger E = y.get(i)
                    .subtract(tiShares.get(i).v)
                    .add(e.get(i)).mod(prime);

            BigInteger product = tiShares.get(i).w
                    .add(D.multiply(tiShares.get(i).v))
                    .add(E.multiply(tiShares.get(i).u))
                    .add(D.multiply(E).multiply(BigInteger
                            .valueOf(asymmetricBit)))
                    .mod(prime);

            products[i] = product;
        }

        return products;

    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    @Override
    void initProtocol() {
        List<List<BigInteger>> diffList = new ArrayList<>();
        int batchSize = x.size();

        for (int i = 0; i < batchSize; i++) {
            List<BigInteger> newRow = new ArrayList<>();
            newRow.add(x.get(i).subtract(tiShares.get(i).u).mod(prime));
            newRow.add(y.get(i).subtract(tiShares.get(i).v).mod(prime));
            diffList.add(newRow);
        }

        Message senderMessage = new Message(diffList,
                clientID, protocolIdQueue);

        try {
            senderQueue.put(senderMessage);
        } catch (InterruptedException ex) {
            Logger.getLogger(BatchMultiplicationReal.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

    }

}
