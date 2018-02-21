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
    int prime,clientID,protocolID;
    List<Triple> tiShares;
    
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
     */
    public DotProduct(List<Integer> xShares, List<Integer> yShares, List<Triple> tiShares, 
            BlockingQueue<Message> senderqueue, BlockingQueue<Message> receiverqueue, 
            int clientID, int prime, int protocolID){
        this.prime = prime;
        this.clientID = clientID;
        this.xShares = xShares;
        this.yShares = yShares;
        this.tiShares = tiShares;
        this.commonSender = senderqueue;
        this.commonReceiver = receiverqueue;  
        this.protocolID = protocolID;
    }
    
    /**
     * 
     * @return 
     */
    @Override
    public Integer call() {
        int dotProduct = 0;
        int vectorLength = xShares.size();
        System.out.println("input len = "+vectorLength);
        
        ConcurrentHashMap<Integer,BlockingQueue<Message> > recQueues = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer,BlockingQueue<Message> > sendQueues = new ConcurrentHashMap<>();
        
        ExecutorService sendqueueHandler = Executors.newSingleThreadExecutor();
        ExecutorService recvqueueHandler = Executors.newSingleThreadExecutor();
        
        sendqueueHandler.execute(new SenderQueueHandler(protocolID,commonSender,sendQueues));
        recvqueueHandler.execute(new ReceiverQueueHandler(commonReceiver, recQueues));
      
        ExecutorService mults = Executors.newFixedThreadPool(vectorLength);
        ExecutorCompletionService<Integer> multCompletionService = new ExecutorCompletionService<>(mults);
        
        for(int i=0;i<vectorLength;i++){
            BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
            recQueues.put(i, temp);
            BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
            sendQueues.put(i,temp2);
            multCompletionService.submit(new Multiplication(xShares.get(i), yShares.get(i), 
                    tiShares.get(i), sendQueues.get(i), recQueues.get(i), clientID, prime, i));
        }
        
        for(int i=0;i<vectorLength;i++){
            try {
                Future<Integer> prod = multCompletionService.take();
                int product = prod.get();
                dotProduct += product;
                System.out.println("answer so far is "+dotProduct);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(DotProduct.class.getName()).log(Level.SEVERE, null, ex);
            }
        }        
        
        sendqueueHandler.shutdownNow();
        recvqueueHandler.shutdownNow();
        
        return dotProduct;
        
    }
    
}
