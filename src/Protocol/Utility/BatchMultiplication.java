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
     * @param pidMapper
     * @param senderQueue
     * @param protocolIdQueue
     * @param clientId
     * @param protocolID
     * @param asymmetricBit
     * @param partyCount
     * @param parentID
     */
    public BatchMultiplication(ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId,
            int protocolID, int asymmetricBit, int parentID, int partyCount) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId,
                asymmetricBit, partyCount);
        this.parentID = parentID;
    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    abstract void initProtocol() throws InterruptedException;

}
