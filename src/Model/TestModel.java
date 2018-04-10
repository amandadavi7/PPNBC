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
import Protocol.OIS;
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

        recQueues = new ConcurrentHashMap<>(50, 0.9f, 1);
        sendQueues = new ConcurrentHashMap<>(50, 0.9f, 1);

        queueHandlers = Executors.newFixedThreadPool(2);
        senderThread = new SenderQueueHandler(1, commonSender, sendQueues);
        receiverThread = new ReceiverQueueHandler(1, commonReceiver, recQueues);
        queueHandlers.submit(senderThread);
        queueHandlers.submit(receiverThread);
    }
    
    public void callArgMax() {
        
        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<Integer[]>> taskList = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        int totalCases = v.size();
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {

            recQueues.putIfAbsent(i, new LinkedBlockingQueue<>());
            sendQueues.putIfAbsent(i, new LinkedBlockingQueue<>());

            ArgMax argmaxModule = new ArgMax(v.get(i), tiShares, oneShares, sendQueues.get(i), 
                recQueues.get(i), clientId, Constants.binaryPrime, i);
            
            System.out.println("submitted " + i + " argmax");
            
            Future<Integer[]> argmaxTask = es.submit(argmaxModule);
            taskList.add(argmaxTask);

        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer[]> dWorkerResponse = taskList.get(i);
            try {
                Integer[] result = dWorkerResponse.get();
                System.out.println("result:" + Arrays.toString(result) + ", #:" + i);
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
    
    public void callOIS(){
        
        System.out.println("calling OIS with v"+v);
        
        ExecutorService es = Executors.newSingleThreadExecutor();
        
        long startTime = System.currentTimeMillis();
        
        recQueues.putIfAbsent(0, new LinkedBlockingQueue<>());
        sendQueues.putIfAbsent(0, new LinkedBlockingQueue<>());

        OIS ois;
        
        if(v.isEmpty()){
            System.out.println("v is null");
            ois = new OIS(null,tiShares, oneShares, sendQueues.get(0), recQueues.get(0), clientId,
            Constants.binaryPrime, 0, 4, 1, 3);
        } else {
            System.out.println("v is not null");
            ois = new OIS(v.get(0),tiShares, oneShares, sendQueues.get(0), recQueues.get(0), clientId,
            Constants.binaryPrime, 0, 4, -1, 3);
        }
           
            
        Future<Integer[]> task = es.submit(ois);
        
        es.shutdown();

        try {
            Integer[] result = task.get();
            System.out.println("result:" + Arrays.toString(result));
        } catch (InterruptedException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    
        
        receiverThread.setProtocolStatus();
        senderThread.setProtocolStatus();
        queueHandlers.shutdown();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }
    
    public void callProtocol(int protocolType) {
        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        // totalcases number of protocols are submitted to the executorservice
        switch(protocolType) {
            case 1:
                for (int i = 0; i < totalCases; i++) {

                    recQueues.putIfAbsent(i, new LinkedBlockingQueue<>());
                    sendQueues.putIfAbsent(i, new LinkedBlockingQueue<>());
                    
                    Multiplication multiplicationModule = new Multiplication(x.get(i).get(0),y.get(i).get(0),tiShares.get(i)
                    ,sendQueues.get(i),recQueues.get(i),clientId,Constants.prime,i,oneShares,0);
                
                    System.out.println("Submitted " + i + " multiplication");
                
                    Future<Integer> multiplicationTask = es.submit(multiplicationModule);
                    taskList.add(multiplicationTask);
                }
                break;
            case 2:
                for (int i = 0; i < totalCases; i++) {

                    recQueues.putIfAbsent(i, new LinkedBlockingQueue<>());
                    sendQueues.putIfAbsent(i, new LinkedBlockingQueue<>());
                    
                    DotProduct DPModule = new DotProduct(x.get(i),
                    y.get(i), tiShares, sendQueues.get(i), recQueues.get(i),
                    clientId, Constants.prime, i, oneShares);  
                
                    System.out.println("Submitted " + i + " dotproduct");
                    
                    Future<Integer> DPTask = es.submit(DPModule);
                    taskList.add(DPTask);
                }
                break;
            case 3:
                for (int i = 0; i < totalCases; i++) {

                    recQueues.putIfAbsent(i, new LinkedBlockingQueue<>());
                    sendQueues.putIfAbsent(i, new LinkedBlockingQueue<>());
                    
                    Comparison comparisonModule = new Comparison(x.get(i), y.get(i), 
                    tiShares, oneShares, sendQueues.get(i),
                    recQueues.get(i), clientId, Constants.binaryPrime, i); 
                
                    System.out.println("submitted " + i + " comparison");
                    
                    Future<Integer> comparisonTask = es.submit(comparisonModule);
                    taskList.add(comparisonTask);
                }
                break;
        }
        
        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            try {
                Integer result = dWorkerResponse.get();
                System.out.println("result:" + result + ", #:" + i);
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
    

    public void compute() {
        
        //callArgMax();
        callOIS();
        
        // pass 1 - multiplication, 2 - dot product and 3 - comparison
        //callProtocol(3);
        
    }
}
