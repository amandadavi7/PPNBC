/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author keerthanaa
 */
public class SenderQueueHandler implements Runnable{
    
    BlockingQueue<Message> commonQueue;
    ConcurrentHashMap<Integer, BlockingQueue<Message> > subQueues;
    int protocolID;
    boolean isProtocolCompleted;
    
    /**
     * Constructor
     * 
     * @param protocolID
     * @param commonQueue
     * @param subQueues 
     */
    public SenderQueueHandler(int protocolID, 
            BlockingQueue<Message> commonQueue, 
            ConcurrentHashMap<Integer, BlockingQueue<Message> > subQueues){
        this.commonQueue = commonQueue;        
        this.subQueues = subQueues;
        this.protocolID = protocolID;
        this.isProtocolCompleted = false;
    }
    
    /**
     * change protocol status to true (indicates the protocol has finished)
     */
    public void setProtocolStatus(){
        this.isProtocolCompleted = true;
    }
    
    /**
     * Take element from sub queue and encapsulate and add to parent queue
     */
    @Override
    public void run(){
        while(true){
            if(isProtocolCompleted && subQueues.isEmpty()) {
                //System.out.println("Shutting down sender queue handler for "+protocolID);
                break;
            }
            
            Iterator<Map.Entry<Integer, BlockingQueue<Message>>> it = subQueues.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<Integer, BlockingQueue<Message>> pair = it.next();
                if(!pair.getValue().isEmpty()){
                    try {
                        Message temp = pair.getValue().take();
                        temp.addProtocolID(protocolID);
                        //System.out.println("Adding to parent queue " + protocolID + " " + temp.getProtocolID());
                        commonQueue.put(temp);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    } catch (RuntimeException ex) {
                        ex.printStackTrace();
                    }                    
                } else if(isProtocolCompleted) {
                    it.remove();
                }
            }
        }
        
    }

}
