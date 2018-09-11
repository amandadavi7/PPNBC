/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Communication.ReceiverQueueHandler;
import java.util.LinkedList;
import java.util.Queue;
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

    ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    protected Queue<Integer> protocolIdQueue;

    ExecutorService queueHandlers;
    ReceiverQueueHandler receiverThread;

    BlockingQueue<Message> commonSender;
    BlockingQueue<Message> commonReceiver;

    int clientId;
    int partyCount;
    int asymmetricBit;

    /**
     * Constructor
     *
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param asymmetricBit
     * @param partyCount
     */
    public Model(BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId,
            int asymmetricBit, int partyCount) {

        this.asymmetricBit = asymmetricBit;
        this.partyCount = partyCount;
        this.commonSender = senderQueue;
        this.commonReceiver = receiverQueue;
        this.clientId = clientId;

        recQueues = new ConcurrentHashMap<>(50, 0.9f, 1);
        this.protocolIdQueue = new LinkedList<>();
        this.protocolIdQueue.add(1);

        queueHandlers = Executors.newSingleThreadExecutor();
        receiverThread = new ReceiverQueueHandler(1, commonReceiver, recQueues);
    }
    
    /**
     * 
     * @param senderQueue
     * @param pidMapper
     * @param clientId
     * @param asymmetricBit
     * @param partyCount 
     */
    public Model(BlockingQueue<Message> senderQueue,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, int clientId,
            int asymmetricBit, int partyCount) {

        this.asymmetricBit = asymmetricBit;
        this.partyCount = partyCount;
        this.commonSender = senderQueue;
        this.clientId = clientId;
        this.pidMapper = pidMapper;

        recQueues = new ConcurrentHashMap<>(50, 0.9f, 1);
        this.protocolIdQueue = new LinkedList<>();
        this.protocolIdQueue.add(1);
        pidMapper.putIfAbsent(protocolIdQueue, new LinkedBlockingQueue<>());

        queueHandlers = Executors.newSingleThreadExecutor();
        receiverThread = new ReceiverQueueHandler(1, commonReceiver, recQueues);
    }

    /**
     * Start Model Handlers
     */
    public void startModelHandlers() {
        //queueHandlers.submit(receiverThread);
    }

    /**
     * Shut down model handlers
     */
    public void teardownModelHandlers() {
        //receiverThread.setProtocolStatus();
        //queueHandlers.shutdown();
    }

    /**
     * Initialize Receiver Queue HashMap
     *
     * @param recQueues
     * @param key
     */
    public void initQueueMap(
            ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues,
            int key) {
        recQueues.putIfAbsent(key, new LinkedBlockingQueue<>());
        
    }

}
