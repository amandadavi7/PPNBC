/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author anisha
 */
public class Protocol {
    //ExecutorService queueHandlers;
    int protocolId;

    public Protocol(int protocolId) {
        this.protocolId = protocolId;
    }

    public void initQueueMap(
            ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues,
            ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues,
            int key) {

        recQueues.putIfAbsent(key, new LinkedBlockingQueue<>());
        sendQueues.putIfAbsent(key, new LinkedBlockingQueue<>());
    }

    public void clearQueueMap(
            ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues,
            ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues,
            int key) {
        recQueues.remove(key);
        sendQueues.remove(key);
    }
    
    public void teardown() {
    
    }
}
