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
 *
 * @author anisha
 */
public class BaQueueHandler implements Runnable {

    int partyCount;
    BlockingQueue<Message> receiverQueue;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> senderQueues;

    BaQueueHandler(int n, BlockingQueue<Message> receiverQueue,
            ConcurrentHashMap<Integer, BlockingQueue<Message>> senderQueues) {
        this.partyCount = n;
        this.receiverQueue = receiverQueue;
        this.senderQueues = senderQueues;
    }

    @Override
    public void run() {
        while (!(Thread.currentThread().isInterrupted())) {
            try {
                Message msg = receiverQueue.take();
                // TODO message index = receiver client id
                int msgIndex = 1;
                for (int i = 0; i < partyCount; i++) {
                    if (i != msgIndex) {
                        senderQueues.putIfAbsent(i, new LinkedBlockingQueue<>());
                        senderQueues.get(i).put(msg);
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(BaQueueHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
