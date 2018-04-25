/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author anisha
 */
public class Protocol {
    //ExecutorService queueHandlers;
    
    BlockingQueue<Message> senderQueue;
    BlockingQueue<Message> receiverQueue;
    int protocolId, clientID, prime;

    public Protocol(int protocolId, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientID, int prime) {
        this.protocolId = protocolId;
        this.senderQueue = senderQueue;
        this.receiverQueue = receiverQueue;
        this.clientID = clientID;
        this.prime = prime;
    }
}
