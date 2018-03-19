/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Communication.ReceiverQueueHandler;
import Communication.SenderQueueHandler;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author keerthanaa
 */
public class DotProduct implements Callable<Integer> {
    
    List<Integer> xShares, yShares;
    BlockingQueue<Message> commonReceiver;
    BlockingQueue<Message> commonSender;
    int prime,clientID,protocolID,oneShare;
    List<Triple> tiShares;
    
    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;
    
    ExecutorService sendqueueHandler;
    ExecutorService recvqueueHandler;
    
    /**
     * Constructor
     * 
     * @param xShares
     * @param yShares
     * @param tiShares
     * @param senderqueue
     * @param receiverqueue
     * @param clientID
     * @param prime
     * @param protocolID 
     * @param oneShare 
     */
    public DotProduct(List<Integer> xShares, List<Integer> yShares, List<Triple> tiShares, 
            BlockingQueue<Message> senderqueue, BlockingQueue<Message> receiverqueue, 
            int clientID, int prime, int protocolID, int oneShare){
        this.prime = prime;
        this.clientID = clientID;
        this.xShares = xShares;
        this.yShares = yShares;
        this.tiShares = tiShares;
        this.commonSender = senderqueue;
        this.commonReceiver = receiverqueue;  
        this.protocolID = protocolID;
        this.oneShare = oneShare;
        
        recQueues = new ConcurrentHashMap<>();
        sendQueues = new ConcurrentHashMap<>();
        
        sendqueueHandler = Executors.newSingleThreadExecutor();
        recvqueueHandler = Executors.newSingleThreadExecutor();
        
        sendqueueHandler.execute(new SenderQueueHandler(protocolID,commonSender,sendQueues));
        recvqueueHandler.execute(new ReceiverQueueHandler(commonReceiver, recQueues));
        
    }
    
    /**
     * 
     * @return 
     */
    @Override
    public Integer call() {
        
        int dotProduct = 0;
        int vectorLength = xShares.size();
        //System.out.println("input len = "+vectorLength);
              
        ExecutorService mults = Executors.newFixedThreadPool(vectorLength);
        ExecutorCompletionService<Integer> multCompletionService = new ExecutorCompletionService<>(mults);
        
        for(int i=0;i<vectorLength;i++){
            
            if (!recQueues.containsKey(i)) {
                BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                recQueues.put(i, temp);
            }

            if (!sendQueues.containsKey(i)) {
                BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                sendQueues.put(i, temp2);
            }
            //System.out.println("my protocol: "+protocolID+", calling mult: "+i);
            multCompletionService.submit(new Multiplication(xShares.get(i), yShares.get(i), 
                    tiShares.get(i), sendQueues.get(i), recQueues.get(i), clientID, prime, i, oneShare));
        }
        
        for(int i=0;i<vectorLength;i++){
            try {
                Future<Integer> prod = multCompletionService.take();
                int product = prod.get();
                dotProduct += product;
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(DotProduct.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
        
        sendqueueHandler.shutdownNow();
        recvqueueHandler.shutdownNow();
        mults.shutdownNow();
        
        dotProduct = Math.floorMod(dotProduct,prime);
        System.out.println("dot product:"+dotProduct+", protocol id:"+ protocolID);
        return dotProduct;
        
    }
    
}
