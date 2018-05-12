/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.Triple;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

/**
 *
 * @author keerthanaa
 */
public abstract class DotProduct extends CompositeProtocol {

    List<Triple> tiShares;

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
    public DotProduct(List<Triple> tiShares,
            BlockingQueue<Message> senderqueue, BlockingQueue<Message> receiverqueue,
            Queue<Integer> protocolQueue,
            int clientID, int protocolID, int oneShare) {

        super(protocolID, senderqueue, receiverqueue, protocolQueue,clientID, oneShare);
        this.tiShares = tiShares;

    }

}
