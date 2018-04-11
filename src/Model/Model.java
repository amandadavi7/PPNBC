/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Communication.ReceiverQueueHandler;
import Communication.SenderQueueHandler;
import TrustedInitializer.Triple;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author keerthanaa
 */
public class Model {
    
    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;

    ExecutorService queueHandlers;
    SenderQueueHandler senderThread;
    ReceiverQueueHandler receiverThread;

    private BlockingQueue<Message> commonSender;
    private BlockingQueue<Message> commonReceiver;

    int clientId;
    List<Triple> binaryTiShares,decimalTiShares;
    int oneShares;
    
    public Model(BlockingQueue<Message> senderQueue, BlockingQueue<Message> receiverQueue, 
            int clientId, int oneShares, List<Triple> binaryTiShares, List<Triple> decimalTiShares) {
        
        this.binaryTiShares = binaryTiShares;
        this.decimalTiShares = decimalTiShares;
        this.oneShares = oneShares;
        this.commonSender = senderQueue;
        this.commonReceiver = receiverQueue;
        this.clientId = clientId;

        recQueues = new ConcurrentHashMap<>(50, 0.9f, 1);
        sendQueues = new ConcurrentHashMap<>(50, 0.9f, 1);

        queueHandlers = Executors.newFixedThreadPool(2);
        senderThread = new SenderQueueHandler(1, commonSender, sendQueues);
        receiverThread = new ReceiverQueueHandler(1, commonReceiver, recQueues);
        queueHandlers.submit(senderThread);
        queueHandlers.submit(receiverThread);
    }
    
    public void startModelHandlers(){
        queueHandlers.submit(senderThread);
        queueHandlers.submit(receiverThread);
    }
    
    public void teardownModelHandlers(){
        senderThread.setProtocolStatus();
        receiverThread.setProtocolStatus();
        queueHandlers.shutdown();
    }
    
    public void initQueueMap(
            ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues,
            ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues,
            int key) {

        recQueues.putIfAbsent(key, new LinkedBlockingQueue<>());
        sendQueues.putIfAbsent(key, new LinkedBlockingQueue<>());
    }
    
}