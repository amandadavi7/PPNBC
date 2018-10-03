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

/**
 * Broadcast a message to all other client sender queue
 *
 * @author anisha
 */
public class BaQueueHandler implements Runnable {

    BlockingQueue<Message> receiverQueue;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> senderQueues;

    int partyCount;
    
    int unicastReceiver;

    /**
     * Constructor
     *
     * @param n
     * @param receiverQueue
     * @param senderQueues
     */
    BaQueueHandler(int n, BlockingQueue<Message> receiverQueue,
            ConcurrentHashMap<Integer, BlockingQueue<Message>> senderQueues, 
            int unicastReceiver) {
        this.partyCount = n;
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
                    if (msg.getClientId() != unicastReceiver) {
                            senderQueues.putIfAbsent(unicastReceiver, new LinkedBlockingQueue<>());
                            senderQueues.get(unicastReceiver).put(msg);
                        }
                } else {
                    // broadcast the message to all parties
                    // TODO change this to party indexes
                    for (int i = 0; i < partyCount; i++) {
                        if (i != msgIndex) {
                            senderQueues.putIfAbsent(i, new LinkedBlockingQueue<>());
                            senderQueues.get(i).put(msg);
                        }
                    }
                }

            } catch (InterruptedException ex) {
                break;
            }
        }
    }

}
