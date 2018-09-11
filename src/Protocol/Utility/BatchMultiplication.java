/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.Protocol;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Batch multiplication of list of xshares with yshares
 *
 * @author keerthanaa
 */
public abstract class BatchMultiplication extends Protocol {

    int parentID;

    /**
     * Constructor
     *
     * TODO - parent ID is only for testing - to be removed in the future
     *
     * @param senderQueue
     * @param receiverQueue
     * @param protocolIdQueue
     * @param clientId
     * @param protocolID
     * @param asymmetricBit
     * @param partyCount
     * @param parentID
     */
    public BatchMultiplication(BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientId,
            int protocolID, int asymmetricBit, int parentID, int partyCount) {

        super(protocolID, senderQueue, receiverQueue, protocolIdQueue, clientId,
                asymmetricBit, partyCount);
        this.parentID = parentID;
    }
    
    /**
     * Constructor
     *
     * TODO - parent ID is only for testing - to be removed in the future
     *
     * @param senderQueue
     * @param receiverQueue
     * @param protocolIdQueue
     * @param clientId
     * @param protocolID
     * @param asymmetricBit
     * @param partyCount
     * @param parentID
     */
    public BatchMultiplication(BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientId,
            int protocolID, int asymmetricBit, int parentID, int partyCount,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper) {

        super(protocolID, senderQueue, receiverQueue, protocolIdQueue, clientId,
                asymmetricBit, partyCount, pidMapper);
        this.parentID = parentID;
    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    abstract void initProtocol();

}
