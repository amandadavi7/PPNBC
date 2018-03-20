/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Communication.ReceiverQueueHandler;
import Communication.SenderQueueHandler;
import Protocol.ArgMax;
import Protocol.Comparison;
import Protocol.DotProduct;
import Protocol.Multiplication;
import TrustedInitializer.Triple;
import Utility.Constants;
import Utility.Logging;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 *
 * @author anisha
 */
public class TestModel {

    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;

    ExecutorService queueHandlers;
    
    private BlockingQueue<Message> commonSender;
    private BlockingQueue<Message> commonReceiver;

    int clientId;
    List<List<Integer>> x;
    List<List<Integer>> y;
    //List<List<List<Integer>>> v;
    List<List<List<Integer>>> v;
    List<Triple> tiShares;
    int oneShares;

    public TestModel(List<List<Integer>> x, List<List<Integer>> y,
            List<List<List<Integer>>> v, List<Triple> tiShares,
            int oneShares,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId) {
        this.x = x;
        this.y = y;
        this.v = v;
        this.tiShares = tiShares;
        this.oneShares = oneShares;
        this.commonSender = senderQueue;
        this.commonReceiver = receiverQueue;
        this.clientId = clientId;

        recQueues = new ConcurrentHashMap<>();
        sendQueues = new ConcurrentHashMap<>();

        queueHandlers = Executors.newFixedThreadPool(2);
        queueHandlers.submit(new SenderQueueHandler(1, commonSender, sendQueues));
        queueHandlers.submit(new ReceiverQueueHandler(commonReceiver, recQueues));
    }

    public void compute() {
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        // The protocols for computation of d are assigned id 0-bitLength-1
        for (int i = 0; i < totalCases; i++) {
            //compute local shares of d and e and add to the message queue

            if (!recQueues.containsKey(i)) {
                recQueues.put(i, new LinkedBlockingQueue<>());
            }

            if (!sendQueues.containsKey(i)) {
                sendQueues.put(i, new LinkedBlockingQueue<>());
            }
            
            
            /*ArgMax multiplicationModule = new ArgMax(v, tiShares, oneShares, sendQueues.get(i), 
                recQueues.get(i), clientId, Constants.binaryPrime, i);*/
            
            
            /*Comparison multiplicationModule = new Comparison(x.get(i), y.get(i), 
                    tiShares, oneShares, sendQueues.get(i),
                recQueues.get(i), clientId, Constants.binaryPrime, i);*/
            
            DotProduct multiplicationModule = new DotProduct(x.get(i),
                    y.get(i), tiShares,
                    sendQueues.get(i), recQueues.get(i), clientId,
                    Constants.prime, i, oneShares);
            
            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);
            
        }

        es.shutdown();

        // Now when I got the result for all, compute y+ x*y and add it to d[i]
        for (int i = 0; i < totalCases; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            try {
                int result = dWorkerResponse.get();
                recQueues.remove(i);
                sendQueues.remove(i);
                System.out.println("result:"+result+", #:"+i);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
        
        queueHandlers.shutdownNow();
        
        /*Multiplication multiplicationModule = new Multiplication(x.get(0), 
                y.get(0), tiShares.get(0), 
                senderQueue, receiverQueue, clientId, Constants.prime,0, oneShares);
        Future<Integer> multiplicationTask = es.submit(multiplicationModule);*/
 /*DotProduct dotproductModule = new DotProduct(x, y, tiShares, senderQueue, 
                receiverQueue, clientId, Constants.prime, 1, oneShares);        
        Future<Integer> dotProduct = es.submit(dotproductModule);*/
 /*Comparison comparisonModule = new Comparison(x, y, tiShares, oneShares, senderQueue,
                receiverQueue, clientId, Constants.binaryPrime, 1);
        Future<Integer> comparisonTask = es.submit(comparisonModule);*/
//        ArgMax argmaxModule = new ArgMax(v, tiShares, oneShares, senderQueue, 
//                receiverQueue, clientId, Constants.binaryPrime, 1);
//        Future<Integer[]> argmaxTask = es.submit(argmaxModule);
//        try {
//            Integer[] result = argmaxTask.get();
//            System.out.println("result of argmax " + Arrays.toString(result));
//        } catch (InterruptedException | ExecutionException ex) {
//            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
}
