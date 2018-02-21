/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author keerthanaa
 */
public class SenderQueueHandler implements Runnable{
    
    BlockingQueue<Message> commonQueue;
    ConcurrentHashMap<Integer, BlockingQueue<Message> > subQueues;
    int protocolID;
    
    /**
     * Constructor
     * 
     * @param protocolID
     * @param commonQueue
     * @param subQueues 
     */
    public SenderQueueHandler(int protocolID, BlockingQueue<Message> commonQueue, ConcurrentHashMap<Integer, BlockingQueue<Message> > subQueues){
        this.commonQueue = commonQueue;        
        this.subQueues = subQueues;
        this.protocolID = protocolID;
    }
    
    /**
     * Take element from sub queue and encapsulate and add to parent queue
     */
    @Override
    public void run(){
        while(true){
            for (BlockingQueue<Message> q: subQueues.values()){
                if(q.size()>0){
                    try {
                        Message msg = new ProtocolMessage(protocolID, q.take());
                        System.out.println("Adding to parent queue " + protocolID + " " + msg);
                        commonQueue.add(msg);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SenderQueueHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        
    }
    
}
