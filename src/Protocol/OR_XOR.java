/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Protocol.Utility.BatchMultiplication;
import Communication.Message;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author keerthanaa
 */
public class OR_XOR extends CompositeProtocol implements Callable<Integer[]> {
    
    List<Integer> xShares, yShares;
    int oneShare, constantMultiplier;
    List<Triple> tiShares;
    int bitLength;
    
    public OR_XOR(List<Integer> x, List<Integer> y, List<Triple> tiShares,
            int oneShare, int constantMultiplier, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID) {
        
        super(protocolID, senderQueue, receiverQueue, clientId, prime);
        
        this.xShares = x;
        this.yShares = y;
        this.oneShare = oneShare;
        bitLength = xShares.size(); 
        this.constantMultiplier = constantMultiplier;
        this.tiShares = tiShares;
        
    }
    
    @Override
    public Integer[] call() throws Exception {
        startHandlers();
        Integer[] output = new Integer[bitLength];
        System.out.println("x="+xShares+" y="+yShares);
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        int i = 0;
        int startpid = 0;

        do {
            System.out.println("Protocol " + protocolId + " batch " + startpid);
            initQueueMap(recQueues, sendQueues, startpid);

            int toIndex = Math.min(i + Constants.batchSize, bitLength);

            BatchMultiplication batchMultiplication = new BatchMultiplication(
                    xShares.subList(i, toIndex),
                    yShares.subList(i, toIndex),
                    tiShares.subList(i, toIndex),
                    sendQueues.get(startpid), recQueues.get(startpid),
                    clientID, prime, startpid, oneShare, protocolId);

            Future<Integer[]> multiplicationTask = es.submit(batchMultiplication);
            taskList.add(multiplicationTask);

            startpid++;
            i += Constants.batchSize;
        } while (i < bitLength);

        es.shutdown();
        int globalIndex = 0;
        int taskLen = taskList.size();
        
        for (i = 0; i < taskLen; i++) {
            try {
                Future<Integer[]> prod = taskList.get(i);
                Integer[] products = prod.get();
                int prodLen = products.length;
                for (int j = 0; j < prodLen; j++) {
                    output[globalIndex] = Math.floorMod(xShares.get(globalIndex) + yShares.get(globalIndex)
                            - (constantMultiplier*products[j]), prime);
                    globalIndex++;
                }
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }

        }
        
        tearDownHandlers();
        return output;
    }
    
}
