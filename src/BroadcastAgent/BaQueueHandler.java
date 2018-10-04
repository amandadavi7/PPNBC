/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BroadcastAgent;

import Communication.Message;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Broadcast a message to all other client sender queue
 *
 * @author anisha
 */
public class BaQueueHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(BaQueueHandler.class.getName());
    
    BlockingQueue<Message> receiverQueue;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> senderQueues;

    int partyCount;
    int unicastReceiver;

    /**
     * Constructor
     *
     * @param partyCount
     * @param receiverQueue
     * @param senderQueues
     * @param unicastReceiver
     */
    BaQueueHandler(int partyCount, BlockingQueue<Message> receiverQueue,
            ConcurrentHashMap<Integer, BlockingQueue<Message>> senderQueues, 
            int unicastReceiver) {
        this.partyCount = partyCount;
        this.receiverQueue = receiverQueue;
        this.senderQueues = senderQueues;
        this.unicastReceiver = unicastReceiver;
    }

    /**
     * Take message from the receiver queue and send it to all other client
     * sender queues
     */
    @Override
    public void run() {
        Message msg;
        while (!(Thread.currentThread().isInterrupted())) {
            try {
                msg = receiverQueue.take();
                int msgIndex = msg.getClientId();
                // check for unicast here
                if (msg.isUnicast()) {
                    // send the message to the party holding asymmetric bit
                    // TODO putIfAbsent might no longer be required, since we first wait for all connections first
                    if (msg.getClientId() != unicastReceiver) {
                            senderQueues.putIfAbsent(unicastReceiver, new LinkedBlockingQueue<>());
                            senderQueues.get(unicastReceiver).put(msg);
                        }
                } else {
                    // broadcast the message to all parties
                    // IMP: The party ids start from 0. 
                    // TODO putIfAbsent might no longer be required, since we first wait for all connections first
                    for (int i = 0; i < partyCount; i++) {
                        if (i != msgIndex) {
                            senderQueues.putIfAbsent(i, new LinkedBlockingQueue<>());
                            senderQueues.get(i).put(msg);
                        }
                    }
                }

            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, "Thread Interrupted", ex);
                break;
            }
        }
    }

}
