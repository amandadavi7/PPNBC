/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author keerthanaa
 */
public class Model {

    ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper;
    protected Queue<Integer> protocolIdQueue;

    BlockingQueue<Message> commonSender;
    BlockingQueue<Message> commonReceiver;

    int clientId;
    int partyCount;
    int asymmetricBit;
    int modelProtocolId;
    
    /**
     * 
     * @param senderQueue
     * @param pidMapper
     * @param clientId
     * @param asymmetricBit
     * @param partyCount
     * @param protocolIdQueue
     * @param protocolID
     */
    public Model(ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue, int clientId, int asymmetricBit,
            int partyCount, Queue<Integer> protocolIdQueue, int protocolID) {

        this.asymmetricBit = asymmetricBit;
        this.partyCount = partyCount;
        this.commonSender = senderQueue;
        this.clientId = clientId;
        this.pidMapper = pidMapper;
        this.modelProtocolId = protocolID;
        this.protocolIdQueue = protocolIdQueue;
        protocolIdQueue.add(protocolID);
        pidMapper.putIfAbsent(protocolIdQueue, new LinkedBlockingQueue<>());

    }
}
