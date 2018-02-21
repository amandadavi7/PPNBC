/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author keerthanaa
 */
public class ReceiverQueueHandler implements Runnable{
    
    BlockingQueue<Message> commonQueue;
    ConcurrentHashMap<Integer, BlockingQueue<Message> > subQueues;
    
    public ReceiverQueueHandler(BlockingQueue<Message> commonQueue, ConcurrentHashMap<Integer, BlockingQueue<Message> > subQueues){
        this.commonQueue = commonQueue;        
        this.subQueues = subQueues;
    }
    
    @Override
    public void run(){
        while(true){
            try {
                Message obj = commonQueue.take();
                int ID = obj.getProtocolID();
                System.out.println("adding to subqueue");
                subQueues.get(ID).add(obj);
            } catch (InterruptedException ex) {
                Logger.getLogger(ReceiverQueueHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
}
