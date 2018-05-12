/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Communication.ReceiverQueueHandler;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author anisha
 */
public class CompositeProtocol extends Protocol {

    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    
    ExecutorService queueHandlers;
    ReceiverQueueHandler receiverThread;

    /**
     * 
     * @param protocolId
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param prime 
     * @param oneShare 
     */
    public CompositeProtocol(int protocolId, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolQueue,
            int clientId, int oneShare) {
        
        super(protocolId, senderQueue, receiverQueue, protocolQueue, clientId, 
                oneShare);
        
        recQueues = new ConcurrentHashMap<>();
        queueHandlers = Executors.newSingleThreadExecutor();
        receiverThread = new ReceiverQueueHandler(protocolId, super.receiverQueue, recQueues);

    }
    
    public void initQueueMap(
            ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues,
            int key) {

        recQueues.putIfAbsent(key, new LinkedBlockingQueue<>());
    }

    /**
     * Start the local threads for queue handlers
     */
    public void startHandlers() {
        queueHandlers.submit(receiverThread);
    }
    
    /**
     * Teardown all local threads
     */
    public void tearDownHandlers() {
        receiverThread.setProtocolStatus();
        queueHandlers.shutdown();
    }

}
