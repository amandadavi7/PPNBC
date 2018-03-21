/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author keerthanaa
 */
public class ReceiverQueueHandler implements Runnable {

    BlockingQueue<Message> commonQueue;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> subQueues;
    boolean isProtocolCompleted;
    int protocolId;

    /**
     * Constructor
     *
     * @param commonQueue
     * @param subQueues
     */
    public ReceiverQueueHandler(int protocolId,
            BlockingQueue<Message> commonQueue, 
            ConcurrentHashMap<Integer, BlockingQueue<Message>> subQueues) {
        this.commonQueue = commonQueue;
        this.subQueues = subQueues;
        this.isProtocolCompleted = false;
        this.protocolId = protocolId;
    }

    /**
     * change protocol status to true (indicates the protocol has finished)
     */
    public void setProtocolStatus() {
        this.isProtocolCompleted = true;
    }

    /**
     * Take element from parent queue and add it to the sub queue
     */
    @Override
    public void run() {
        while (true) {
            if (!commonQueue.isEmpty()) {
                try {
                    Message queueObj = commonQueue.take();
                    int parentID = queueObj.getProtocolID();
                    Message strippedObj = (Message) queueObj.getValue();
                    int ID = strippedObj.getProtocolID();
                    System.out.println("adding to receiverqueue from " + 
                            parentID + " to subqueue " + ID + " message " + 
                            strippedObj.getValue());
                    subQueues.putIfAbsent(ID, new LinkedBlockingQueue<>());
                    subQueues.get(ID).put(strippedObj);
                } catch (InterruptedException ex) {                   
                    ex.printStackTrace();
                }
            } else if (isProtocolCompleted) {
                System.out.println("Shutting down receiver handler:"+protocolId);
                break;
            }
        }
    }

}
