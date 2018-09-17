/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class to layout contract for a higher level protocol to be created
 *
 * @author anisha
 */
public class CompositeProtocol extends Protocol {

    /**
     * Constructor
     *
     * @param protocolId
     * @param pidMapper
     * @param senderQueue
     * @param protocolIdQueue
     * @param clientId
     * @param asymmetricBit
     * @param partyCount
     */
    public CompositeProtocol(int protocolId, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, int asymmetricBit, int partyCount) {

        super(protocolId, pidMapper, senderQueue, protocolIdQueue, clientId, 
                asymmetricBit, partyCount);
        
    }
}
