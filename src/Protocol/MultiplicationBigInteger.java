/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.TripleBigInteger;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The protocol computes the multiplication of shares of x and y and returns the
 * share of the product
 *
 * @author anisha
 */
public class MultiplicationBigInteger extends Protocol implements Callable<BigInteger> {

    BigInteger x;
    BigInteger y;
    TripleBigInteger tiRealShares;
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
    public MultiplicationBigInteger(BigInteger x, BigInteger y, TripleBigInteger tiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, BigInteger prime,
            int protocolID, int asymmetricBit, int partyCount,int threadID) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue,
                clientId, asymmetricBit, partyCount,threadID);
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
     * @throws java.lang.InterruptedException
     */
    @Override
    public BigInteger call() throws InterruptedException {
        initProtocol();
        BigInteger d = BigInteger.ZERO;
        BigInteger e = BigInteger.ZERO;
        for (int i = 0; i < partyCount - 1; i++) {
            Message receivedMessage = pidMapper.get(protocolIdQueue).take();
            List<BigInteger> diffList = (List<BigInteger>) receivedMessage.getValue();
            d = d.add(diffList.get(0)).mod(prime);
            e = e.add(diffList.get(1)).mod(prime);
        }

        d = x.subtract(tiRealShares.u).mod(prime).add(d).mod(prime);
        e = y.subtract(tiRealShares.v).mod(prime).add(e).mod(prime);

        BigInteger product = tiRealShares.w
                .add(d.multiply(tiRealShares.v).mod(prime))
                .mod(prime)
                .add(e.multiply(tiRealShares.u).mod(prime))
                .mod(prime)
                .add(d.multiply(e).mod(prime).multiply(BigInteger.valueOf(asymmetricBit)).mod(prime))
                .mod(prime);

        return product;

    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    private void initProtocol() throws InterruptedException {
        List<BigInteger> diffList = new ArrayList<>();
        diffList.add(x.subtract(tiRealShares.u).mod(prime));
        diffList.add(y.subtract(tiRealShares.v).mod(prime));

        Message senderMessage = new Message(diffList,
                clientID, protocolIdQueue);
        senderMessage.setThreadID(threadID);
        senderQueue.put(senderMessage);
    }
}
