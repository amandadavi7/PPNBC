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
import java.util.stream.IntStream;

/**
 *
 * @author keerthanaa
 */
public class DotProduct extends Protocol implements Callable<Integer> {

    List<Integer> xShares, yShares;
    BlockingQueue<Message> commonReceiver;
    BlockingQueue<Message> commonSender;
    int prime, clientID, protocolID, oneShare;
    List<Triple> tiShares;

    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;

    ExecutorService queueHandlers;
    SenderQueueHandler senderThread;
    ReceiverQueueHandler receiverThread;

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
            int clientID, int prime, int protocolID, int oneShare) {
        super(protocolID);
        this.prime = prime;
        this.clientID = clientID;
        this.xShares = xShares;
        this.yShares = yShares;
        this.tiShares = tiShares;
        this.commonSender = senderqueue;
        this.commonReceiver = receiverqueue;
        this.protocolID = protocolID;
        this.oneShare = oneShare;

        recQueues = new ConcurrentHashMap<>(10, 0.9f, 1);
        sendQueues = new ConcurrentHashMap<>(10, 0.9f, 1);

        queueHandlers = Executors.newFixedThreadPool(2);
        senderThread = new SenderQueueHandler(protocolID, commonSender, sendQueues);
        receiverThread = new ReceiverQueueHandler(protocolID,commonReceiver, recQueues);
        queueHandlers.submit(senderThread);
        queueHandlers.submit(receiverThread);
        
    }

    /**
     *
     * @return
     */
    @Override
    public Integer call() {

        int dotProduct = 0;
        int vectorLength = xShares.size();
        
        ExecutorService mults = Executors.newFixedThreadPool(Constants.threadCount);
        ExecutorCompletionService<Integer[]> multCompletionService = new ExecutorCompletionService<>(mults);
        
        int i=0;
        int batchsize = 10;
        int startpid = 0;
        
        while(i+batchsize < vectorLength) {
            
            System.out.println("Protocol "+protocolID+" batch "+startpid);
            initQueueMap(recQueues, sendQueues, startpid);
            
            multCompletionService.submit(new BatchMultiplication(xShares.subList(i, i+batchsize), 
                    yShares.subList(i, i+batchsize), tiShares, sendQueues.get(startpid), recQueues.get(startpid), 
                    clientID, prime, startpid, oneShare, protocolID));
            
            startpid++;
            i+=batchsize;
        }
        
        if(i<vectorLength) {
            System.out.println("Protocol "+protocolID+" batch "+startpid);
            initQueueMap(recQueues, sendQueues, startpid);
            
            multCompletionService.submit(new BatchMultiplication(xShares.subList(i, vectorLength), 
                    yShares.subList(i, vectorLength), tiShares, sendQueues.get(startpid), recQueues.get(startpid), 
                    clientID, prime, startpid, oneShare, protocolID));
            
            startpid++;
            
        }

        mults.shutdown();

        for (i = 0; i < startpid; i++) {
            try {
                Future<Integer[]> prod = multCompletionService.take();
                Integer[] products = prod.get();
                for(int j: products){
                    dotProduct+=j;
                }
            } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
            }
        }

        senderThread.setProtocolStatus();
        receiverThread.setProtocolStatus();
        queueHandlers.shutdown();
        
        dotProduct = Math.floorMod(dotProduct, prime);
        System.out.println("dot product:" + dotProduct + ", protocol id:" + protocolID);
        return dotProduct;

    }

}
