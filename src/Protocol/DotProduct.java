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
 *
 * @author keerthanaa
 */
public abstract class DotProduct extends CompositeProtocol {

    /**
     * Constructor for Dot Product abstract
     * Class Initializes queue and protocol
     * ID details
     *
     * @param senderqueue
     * @param receiverqueue
     * @param protocolIdQueue
     * @param clientID
     * @param protocolID
     * @param asymmetricBit
     * @param partyCount
     */
    public DotProduct(BlockingQueue<Message> senderqueue, BlockingQueue<Message> receiverqueue,
            Queue<Integer> protocolIdQueue,
            int clientID, int protocolID, int asymmetricBit, int partyCount) {

        super(protocolID, senderqueue, receiverqueue, protocolIdQueue, clientID,
                asymmetricBit, partyCount);

    }
    
    public DotProduct(BlockingQueue<Message> senderqueue, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            Queue<Integer> protocolIdQueue,
            int clientID, int protocolID, int asymmetricBit, int partyCount) {

        super(protocolID, senderqueue, pidMapper, protocolIdQueue, clientID,
                asymmetricBit, partyCount);

    }

}
