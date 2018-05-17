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
    BlockingQueue<BaMessagePacket> receiverQueue;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> senderQueues;

    BaQueueHandler(int n, BlockingQueue<BaMessagePacket> receiverQueue,
            ConcurrentHashMap<Integer, BlockingQueue<Message>> senderQueues) {
        this.partyCount = n;
        this.receiverQueue = receiverQueue;
        this.senderQueues = senderQueues;
    }

    @Override
    public void run() {
        while (!(Thread.currentThread().isInterrupted())) {
            try {
                BaMessagePacket msg = receiverQueue.take();
                int msgIndex = msg.clientId;
                for (int i = 0; i < partyCount; i++) {
                    if (i != msgIndex) {
                        senderQueues.putIfAbsent(i, new LinkedBlockingQueue<>());
                        //System.out.println("adding message to senderQueue "+i);
                        senderQueues.get(i).put(msg.message);
                    }
                }
            } catch (InterruptedException ex) {
                System.out.println("Breaking out of BAQueueHandler");
                break;
                //Logger.getLogger(BaQueueHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
