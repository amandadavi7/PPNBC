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
    SenderQueueHandler senderThread;
    ReceiverQueueHandler receiverThread;
    
    private BlockingQueue<Message> commonSender;
    private BlockingQueue<Message> commonReceiver;

    int clientId;
    List<List<Integer>> x;
    List<List<Integer>> y;
    List<List<List<Integer>>> v;
    List<Triple> tiShares;
    int oneShares;

    public TestModel(List<List<Integer>> x, List<List<Integer>> y,
            List<List<List<Integer>>> v, List<Triple> tiShares,
            int oneShares, BlockingQueue<Message> senderQueue,
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
        senderThread = new SenderQueueHandler(1, commonSender, sendQueues);
        receiverThread = new ReceiverQueueHandler(commonReceiver, recQueues,1);
        queueHandlers.submit(senderThread);
        queueHandlers.submit(receiverThread);
    }

    public void compute() {
        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {

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
                    y.get(i), tiShares, sendQueues.get(i), recQueues.get(i), 
                    clientId, Constants.prime, i, oneShares);
            
            System.out.println("Submitted "+ i+" dotproduct");
            
            /*Multiplication multiplicationModule = new Multiplication(x.get(i).get(0),y.get(i).get(0),tiShares.get(i)
                    ,sendQueues.get(i),recQueues.get(i),clientId,Constants.prime,i,oneShares);*/
            
            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);
            
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            try {
                int result = dWorkerResponse.get();
                System.out.println("result:"+result+", #:"+i);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        receiverThread.setProtocolStatus();
        senderThread.setProtocolStatus();
        queueHandlers.shutdown();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }
}
