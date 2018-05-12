/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.Protocol;
import TrustedInitializer.Triple;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

/**
 * Batch multiplication of list of xshares with yshares
 *
 * @author keerthanaa
 */
public abstract class BatchMultiplication extends Protocol {

    List<Triple> tiShares;
    int parentID;

    /**
     * Constructor
     *
     * @param tiShares
     * @param senderQueue
     * @param receiverQueue
     * @param protocolIdQueue
     * @param clientId
     * @param protocolID
     * @param oneShare
     */
    public BatchMultiplication(List<Triple> tiShares,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientId, 
            int protocolID, int oneShare, int parentID) {

        super(protocolID, senderQueue, receiverQueue, protocolIdQueue, clientId, oneShare);
        this.tiShares = tiShares;
        this.parentID = parentID;
    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    abstract void initProtocol();

}
