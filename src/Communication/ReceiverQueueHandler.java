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

    /**
     * Constructor
     *
     * @param commonQueue
     * @param subQueues
     */
    public ReceiverQueueHandler(BlockingQueue<Message> commonQueue, ConcurrentHashMap<Integer, BlockingQueue<Message>> subQueues) {
        this.commonQueue = commonQueue;
        this.subQueues = subQueues;
    }

    /**
     * Take element from parent queue and add it to the sub queue
     */
    @Override
    public void run() {
        while (true) {
            try {
                Message queueObj = commonQueue.take();
                int parentID = queueObj.getProtocolID();
                Message strippedObj = (Message) queueObj.getValue();
                int ID = strippedObj.getProtocolID();
                System.out.println("adding to from " + parentID + " to subqueue " + ID + " message " + strippedObj);
                if (!subQueues.containsKey(ID)) {
                    subQueues.put(ID, new LinkedBlockingQueue<>());
                }
                subQueues.get(ID).put(strippedObj);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }

    }

}
