/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.TripleInteger;
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
public class MultiplicationInteger extends Protocol implements Callable {

    int x;
    int y;
    TripleInteger tiShares;
    int parentID;
    int prime;

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
    public MultiplicationInteger(int x, int y, TripleInteger tiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, int prime,
            int protocolID, int asymmetricBit, int parentID, int partyCount) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue,
                clientId, asymmetricBit, partyCount);
        this.x = x;
        this.y = y;
        this.tiShares = tiShares;
        this.parentID = parentID;
        this.prime = prime;
    }

    /**
     * Waits for the shares of (x-u) and (y-v), computes the product and returns
     * the value
     *
     * @return shares of product
     */
    @Override
    public Object call() {
        initProtocol();
        Message receivedMessage = null;
        int d = 0, e = 0;
        List<Integer> diffList = null;
        for (int i = 0; i < partyCount - 1; i++) {
            try {
                receivedMessage = pidMapper.get(protocolIdQueue).take();
                diffList = (List<Integer>) receivedMessage.getValue();
                d += diffList.get(0);
                e += diffList.get(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(MultiplicationInteger.class.getName())
                    .log(Level.SEVERE, null, ex);
            }
        }

        d = Math.floorMod(x - tiShares.u + d, prime);
        e = Math.floorMod(y - tiShares.v + e, prime);
        int product = tiShares.w + (d * tiShares.v) + (tiShares.u * e) + 
                (d * e * asymmetricBit);
        product = Math.floorMod(product, prime);
        return product;

    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    private void initProtocol() {
        List<Integer> diffList = new ArrayList<>();
        diffList.add(Math.floorMod(x - tiShares.u, prime));
        diffList.add(Math.floorMod(y - tiShares.v, prime));

        Message senderMessage = new Message(diffList,
                clientID, protocolIdQueue);
        try {
            senderQueue.put(senderMessage);
        } catch (InterruptedException ex) {
            Logger.getLogger(MultiplicationInteger.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

    }

}
