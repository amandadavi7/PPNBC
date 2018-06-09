/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Communication.ReceiverQueueHandler;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import TrustedInitializer.TripleReal;
import TrustedInitializer.TruncationPair;
import java.util.LinkedList;
import java.util.List;
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
     *
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param asymmetricBit
     */
    public Model(BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int asymmetricBit, int partyCount) {

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

    public void startModelHandlers() {
        queueHandlers.submit(receiverThread);
    }

    public void teardownModelHandlers() {
        receiverThread.setProtocolStatus();
        queueHandlers.shutdown();
    }

    public void initQueueMap(
            ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues,
            int key) {
        recQueues.putIfAbsent(key, new LinkedBlockingQueue<>());
    }

}
