/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.TripleReal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The protocol computes the multiplication of shares of x and y and returns the
 * share of the product
 *
 * @author anisha
 */
public class MultiplicationReal extends Protocol implements Callable<BigInteger> {

    private static final Logger LOGGER = Logger.getLogger(MultiplicationReal.class.getName());
    
    BigInteger x;
    BigInteger y;
    TripleReal tiRealShares;
    BigInteger prime;
    
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
     * @param partyCount
     *
     */
    public MultiplicationReal(BigInteger x, BigInteger y, TripleReal tiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, BigInteger prime,
            int protocolID, int asymmetricBit, int partyCount) {

        
        super(protocolID, pidMapper, senderQueue, protocolIdQueue,
                clientId, asymmetricBit, partyCount);
        this.x = x;
        this.y = y;
        this.tiRealShares = tiShares;
        this.prime = prime;
    }

    /**
     * Waits for the shares of (x-u) and (y-v), computes the product and returns
     * the value
     *
     * @return shares of product
     */
    @Override
    public BigInteger call() {
        initProtocol();
        BigInteger d = BigInteger.ZERO;
        BigInteger e = BigInteger.ZERO;
        for (int i = 0; i < partyCount - 1; i++) {
            try {
                Message receivedMessage = pidMapper.get(protocolIdQueue).take();
                List<BigInteger> diffList = (List<BigInteger>) receivedMessage.getValue();
                d = d.add(diffList.get(0));
                e = e.add(diffList.get(1));
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        
        d = x.subtract(tiRealShares.u).add(d).mod(prime);
        e = y.subtract(tiRealShares.v).add(e).mod(prime);
        BigInteger product = tiRealShares.w
                .add(d.multiply(tiRealShares.v))
                .add(e.multiply(tiRealShares.u))
                .add(d.multiply(e).multiply(BigInteger.valueOf(asymmetricBit)))
                .mod(prime);

        return product;

    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    private void initProtocol() {
        List<BigInteger> diffList = new ArrayList<>();
        diffList.add(x.subtract(tiRealShares.u).mod(prime));
        diffList.add(y.subtract(tiRealShares.v).mod(prime));

        Message senderMessage = new Message(diffList,
                clientID, protocolIdQueue);
        try {
            senderQueue.put(senderMessage);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }

}
