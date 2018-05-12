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
 * @author anisha
 */
public class Protocol {
    
    protected BlockingQueue<Message> senderQueue;
    protected BlockingQueue<Message> receiverQueue;
    protected int protocolId, clientID, oneShare;
    protected Queue<Integer> protocolIdQueue;

    public Protocol(int protocolId, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientID, int oneShare) {
        this.protocolId = protocolId;
        this.senderQueue = senderQueue;
        this.receiverQueue = receiverQueue;
        this.clientID = clientID;
        this.oneShare = oneShare;
        this.protocolIdQueue = protocolIdQueue;
        this.protocolIdQueue.add(protocolId);
    }
}
