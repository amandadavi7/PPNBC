/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Communication.ReceiverQueueHandler;
import Communication.SenderQueueHandler;
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
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;

    ExecutorService queueHandlers;
    SenderQueueHandler senderThread;
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
            BlockingQueue<Message> receiverQueue, int clientId, int prime, int oneShare) {
        
        super(protocolId, senderQueue, receiverQueue, clientId, prime, oneShare);
        
        recQueues = new ConcurrentHashMap<>();
        sendQueues = new ConcurrentHashMap<>();

        queueHandlers = Executors.newFixedThreadPool(2);
        senderThread = new SenderQueueHandler(protocolId, super.senderQueue, sendQueues);
        receiverThread = new ReceiverQueueHandler(protocolId, super.receiverQueue, recQueues);

    }
    
    public void initQueueMap(
            ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues,
            ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues,
            int key) {

        recQueues.putIfAbsent(key, new LinkedBlockingQueue<>());
        sendQueues.putIfAbsent(key, new LinkedBlockingQueue<>());
    }

    /**
     * Start the local threads for queue handlers
     */
    public void startHandlers() {
        queueHandlers.submit(senderThread);
        queueHandlers.submit(receiverThread);
    }
    
    /**
     * Teardown all local threads
     */
    public void tearDownHandlers() {
        senderThread.setProtocolStatus();
        receiverThread.setProtocolStatus();
        queueHandlers.shutdown();
    }

}
