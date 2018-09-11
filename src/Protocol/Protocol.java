/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Base class to layout contract for a protocol to be created
 *
 * @author anisha
 */
public class Protocol {

    protected BlockingQueue<Message> senderQueue;
    protected BlockingQueue<Message> receiverQueue;
    protected int protocolId, clientID, asymmetricBit, partyCount;
    protected Queue<Integer> protocolIdQueue;
    protected ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper;

    public Protocol(int protocolId, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientID, int asymmetricBit, int partyCount) {
        this.protocolId = protocolId;
        this.senderQueue = senderQueue;
        this.receiverQueue = receiverQueue;
        this.clientID = clientID;
        this.asymmetricBit = asymmetricBit;
        this.partyCount = partyCount;
        this.protocolIdQueue = protocolIdQueue;
        this.protocolIdQueue.add(protocolId);
    }
    
    public Protocol(int protocolId, BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientID, int asymmetricBit, int partyCount, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper) {
        this.protocolId = protocolId;
        this.senderQueue = senderQueue;
        this.clientID = clientID;
        this.asymmetricBit = asymmetricBit;
        this.partyCount = partyCount;
        this.protocolIdQueue = protocolIdQueue;
        this.protocolIdQueue.add(protocolId);
        this.pidMapper = pidMapper;
        this.pidMapper.putIfAbsent(protocolIdQueue, new LinkedBlockingQueue<>());
    }
}
