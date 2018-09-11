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
 * Base class to layout contract for a higher level protocol to be created
 *
 * @author anisha
 */
public class CompositeProtocol extends Protocol {

    protected ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    //protected ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper;
    
    ExecutorService queueHandlers;
    ReceiverQueueHandler receiverThread;

    /**
     * Constructor
     *
     * @param protocolId
     * @param senderQueue
     * @param receiverQueue
     * @param protocolIdQueue
     * @param clientId
     * @param asymmetricBit
     * @param partyCount
     */
    public CompositeProtocol(int protocolId, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientId, int asymmetricBit, int partyCount) {

        super(protocolId, senderQueue, receiverQueue, protocolIdQueue, clientId,
                asymmetricBit, partyCount);

        recQueues = new ConcurrentHashMap<>();
        queueHandlers = Executors.newSingleThreadExecutor();
        receiverThread = new ReceiverQueueHandler(protocolId,
                super.receiverQueue, recQueues);

    }
    
    public CompositeProtocol(int protocolId, BlockingQueue<Message> senderQueue,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, Queue<Integer> protocolIdQueue,
            int clientId, int asymmetricBit, int partyCount) {

        super(protocolId, senderQueue, protocolIdQueue, clientId, asymmetricBit,
                partyCount, pidMapper);

        //recQueues = new ConcurrentHashMap<>();
        queueHandlers = Executors.newSingleThreadExecutor();
        
        //receiverThread = new ReceiverQueueHandler(protocolId,
        //        super.receiverQueue, recQueues);

    }

    /**
     * Initialize the receiver map for the protocol ID = key
     *
     * @param recQueues
     * @param key
     */
    public void initQueueMap(
            ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues,
            int key) {

        recQueues.putIfAbsent(key, new LinkedBlockingQueue<>());
    }

    /**
     * Start the local threads for queue handlers
     */
    public void startHandlers() {
        //queueHandlers.submit(receiverThread);
    }

    /**
     * Teardown all local threads for the protocol
     */
    public void tearDownHandlers() {
        //receiverThread.setProtocolStatus();
        //queueHandlers.shutdown();
    }

}
