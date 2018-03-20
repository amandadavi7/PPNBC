/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

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
                        Message temp = q.take();
                        Message msg = new ProtocolMessage(protocolID, temp);
                        System.out.println("Adding to parent queue " + protocolID + " " + temp.getProtocolID());
                        commonQueue.put(msg);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    } catch (RuntimeException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        
    }
    
}
