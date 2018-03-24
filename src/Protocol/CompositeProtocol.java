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

    int clientID;
    int prime;

    /**
     * 
     * @param protocolId
     * @param senderQueue
     * @param receiverQueue 
     */
    public CompositeProtocol(int protocolId, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue) {
        super(protocolId, senderQueue, receiverQueue);
    }

    /**
     * 
     * @param protocolId
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param prime 
     */
    public CompositeProtocol(int protocolId, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime) {
        super(protocolId, senderQueue, receiverQueue);
        this.clientID = clientId;
        this.prime = prime;

        recQueues = new ConcurrentHashMap<>();
        sendQueues = new ConcurrentHashMap<>();

        queueHandlers = Executors.newFixedThreadPool(2);
        senderThread = new SenderQueueHandler(protocolId, super.senderQueue, sendQueues);
        receiverThread = new ReceiverQueueHandler(protocolId, super.receiverQueue, recQueues);

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
