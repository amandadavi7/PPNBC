/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author keerthanaa
 */
public abstract class DotProduct extends CompositeProtocol {


    /**
     * Constructor
     *
     * @param tiShares
     * @param senderqueue
     * @param receiverqueue
     * @param protocolIdQueue
     * @param clientID
     * @param protocolID
     * @param asymmetricBit
     */
    public DotProduct(BlockingQueue<Message> senderqueue, BlockingQueue<Message> receiverqueue,
            Queue<Integer> protocolIdQueue,
            int clientID, int protocolID, int asymmetricBit, int partyCount) {

        super(protocolID, senderqueue, receiverqueue, protocolIdQueue,clientID, 
                asymmetricBit, partyCount);

    }

}
