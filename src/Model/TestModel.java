/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.ArgMax;
import Protocol.BitDecomposition;
import Protocol.Comparison;
import Protocol.DotProduct;
import Protocol.Multiplication;
import Protocol.OIS;
import Protocol.OR_XOR;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 *
 * @author anisha
 */
public class TestModel extends Model{

    List<List<Integer>> x;
    List<List<Integer>> y;
    List<List<List<Integer>>> v;
    
    public TestModel(List<List<Integer>> x, List<List<Integer>> y,
            List<List<List<Integer>>> v, List<Triple> binaryTriples, List<Triple> decimalTriples,
            int oneShares, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId) {
        
        super(senderQueue, receiverQueue, clientId, oneShares, binaryTriples, decimalTriples, null);
        
        this.x = x;
        this.y = y;
        this.v = v;
        
    }
    
     public void callBitDecomposition(){
        
        ExecutorService es = Executors.newFixedThreadPool(1);
        initQueueMap(recQueues, sendQueues, 1);
        //TODO: change this to just take integers instead of wasting memory on List<Integer> 
        BitDecomposition bitTest = new BitDecomposition(x.get(0).get(0), y.get(0).get(0),
                binaryTiShares, oneShares, Constants.bitLength, sendQueues.get(1),
                recQueues.get(1), clientId, Constants.binaryPrime, 1);
        
        Future<List<Integer>> bitdecompositionTask = es.submit(bitTest);
        
        try {
            List<Integer> result = bitdecompositionTask.get();
            System.out.println("result of bitDecomposition: " + result);
        } catch (InterruptedException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {

            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }
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

            ArgMax argmaxModule = new ArgMax(v.get(i), binaryTiShares, oneShares, sendQueues.get(i), 
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
    
    public void callOR_XOR(){
        
        System.out.println("calling or_xor with x="+x+" y="+y);
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        
        for(int i=0;i<totalCases;i++) {
            
            initQueueMap(recQueues, sendQueues, i);
            OR_XOR or_xor = new OR_XOR(x.get(i), y.get(i), decimalTiShares, oneShares, 1, sendQueues.get(i), 
                    recQueues.get(i), clientId, Constants.prime, i);

            Future<Integer[]> task = es.submit(or_xor);
            taskList.add(task);
        }
        
        es.shutdown();

        for(int i=0;i<totalCases;i++) {
            try {
                Future<Integer[]> task = taskList.get(i);
                Integer[] result = task.get();
                System.out.println("result: "+i+": " + Arrays.toString(result));
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
            ois = new OIS(null,binaryTiShares, oneShares, sendQueues.get(0), recQueues.get(0), clientId,
            Constants.binaryPrime, 0, 4, 1, 3);
        } else {
            System.out.println("v is not null");
            ois = new OIS(v.get(0),binaryTiShares, oneShares, sendQueues.get(0), recQueues.get(0), clientId,
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
                    
                    Multiplication multiplicationModule = new Multiplication(x.get(i).get(0),y.get(i).get(0),decimalTiShares.get(i)
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
                    y.get(i), decimalTiShares, sendQueues.get(i), recQueues.get(i),
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
                    binaryTiShares, oneShares, sendQueues.get(i),
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
        
        startModelHandlers();
        
        //callArgMax();
        //callOIS();
        //callOR_XOR();
        callBitDecomposition();
        
        // pass 1 - multiplication, 2 - dot product and 3 - comparison
        //callProtocol(3);
        
        
        teardownModelHandlers();
        
    }
         
}
