/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.Protocol;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class to take care of multiplying all numbers in list in parallel
 * @author keerthanaa
 */
public class ParallelMultiplication extends Protocol implements Callable<Integer> {
    
    List<Integer> wRow;
    List<Triple> tishares;
    int startProtocolID;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    
    public ParallelMultiplication(List<Integer> row, List<Triple> tishares, 
            int clientID, int prime, int protocolID, int startProtocolID, 
            int oneShare, ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues, 
            ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues,
            BlockingQueue<Message> senderQueue, BlockingQueue<Message> receiverQueue) {
        
        super(protocolID,senderQueue,receiverQueue,clientID,prime, oneShare);
        this.wRow = row;
        this.tishares = tishares;
        this.startProtocolID = startProtocolID;
        this.sendQueues = sendQueues;
        this.recQueues = recQueues;
    }
    
    /**
     * 
     * @return
     * @throws Exception 
     */
    @Override
    public Integer call() throws Exception {
        List<Integer> products = new ArrayList<>(wRow);
        int tiStartIndex = 0;
        
        while(products.size()>1){
            int size = products.size();
            int push = -1;
            int toIndex1 = size/2;
            int toIndex2 = size;
            if(size%2==1){
                toIndex2--;
                push = products.get(size-1);
            }
            
            //System.out.println("products size:"+size+",toIndex1 "+toIndex1+",toIndex2 "+toIndex2);
            
            ExecutorService batchmults = Executors.newFixedThreadPool(Constants.threadCount);
            ExecutorCompletionService<Integer[]> multCompletionService = new ExecutorCompletionService<>(batchmults);
            
            int i1=0;
            int i2=toIndex1;
            int startpid = startProtocolID;
            
            do {
                
                int tempIndex1 = Math.min(i1+Constants.batchSize, toIndex1);
                int tempIndex2 = Math.min(i2+Constants.batchSize, toIndex2);
              
                recQueues.putIfAbsent(startpid, new LinkedBlockingQueue<>());
                sendQueues.putIfAbsent(startpid, new LinkedBlockingQueue<>());
                //System.out.println("calling batchmult with pid:"+startpid+",indices:"+tempIndex1+","+tempIndex2);
                
                multCompletionService.submit(new BatchMultiplication(products.subList(i1, tempIndex1), 
                    products.subList(i2, tempIndex2), tishares.subList(tiStartIndex, tiStartIndex+tempIndex1), 
                    sendQueues.get(startpid), recQueues.get(startpid), clientID, prime, startpid, oneShare, protocolId));
                
                tiStartIndex += tempIndex1;
                startpid++;
                i1 = tempIndex1;
                i2 = tempIndex2;                
                
            } while(i1<toIndex1 && i2<toIndex2);
            
            batchmults.shutdown();
            List<Integer> newProducts = new ArrayList<>();
            for(int i=0;i<startpid-startProtocolID;i++) {
                    Future<Integer[]> prodFuture = multCompletionService.take();
                    Integer[] newProds = prodFuture.get();
                    for(int j: newProds){
                        newProducts.add(j);
                    }
            }
            
            products.clear();
            products = new ArrayList<>(newProducts);
            
            if(push!=-1) {
                products.add(push);
            }
            
        }
        System.out.println("returning "+products.get(0));
        return products.get(0);
        
        
        /*
        int product = wRow.get(0);

        for (int i = 0; i < wRow.size() - 1; i++) {
            Multiplication multiplicationTask = new Multiplication(product, wRow.get(i + 1),
                    tishares.get(i), senderQueue, receiverQueue,
                    clientID, prime, startProtocolID, oneShare,protocolId);
            product = (int) multiplicationTask.call();

        }
        //System.out.println("returning "+product);
        return product;   */     
    }
    
}
