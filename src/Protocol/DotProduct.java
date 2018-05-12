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

/**
 *
 * @author keerthanaa
 */
public abstract class DotProduct extends CompositeProtocol {

    List<Triple> tiShares;

    /**
     * Constructor
     *
     * @param tiShares
     * @param senderqueue
     * @param receiverqueue
     * @param protocolIdQueue
     * @param clientID
     * @param protocolID
     * @param oneShare
     */
    public DotProduct(List<Triple> tiShares,
            BlockingQueue<Message> senderqueue, BlockingQueue<Message> receiverqueue,
            Queue<Integer> protocolIdQueue,
            int clientID, int protocolID, int oneShare) {

        super(protocolID, senderqueue, receiverqueue, protocolIdQueue,clientID, oneShare);
        this.tiShares = tiShares;

    }

}
