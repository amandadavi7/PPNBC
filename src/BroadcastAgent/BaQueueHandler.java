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

    BlockingQueue<BaMessagePacket> receiverQueue;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> senderQueues;
    
    int partyCount;

    /**
     * Constructor
     *
     * @param n
     * @param receiverQueue
     * @param senderQueues
     */
    BaQueueHandler(int n, BlockingQueue<BaMessagePacket> receiverQueue,
            ConcurrentHashMap<Integer, BlockingQueue<Message>> senderQueues) {
        this.partyCount = n;
        this.receiverQueue = receiverQueue;
        this.senderQueues = senderQueues;
    }

    /**
     * Take message from the receiver queue and send it to all other client
     * sender queues
     */
    @Override
    public void run() {
        while (!(Thread.currentThread().isInterrupted())) {
            try {
                BaMessagePacket msg = receiverQueue.take();
                int msgIndex = msg.clientId;
                for (int i = 0; i < partyCount; i++) {
                    if (i != msgIndex) {
                        senderQueues.putIfAbsent(i, new LinkedBlockingQueue<>());
                        senderQueues.get(i).put(msg.message);
                    }
                }
            } catch (InterruptedException ex) {
                break;
            }
        }
    }

}
