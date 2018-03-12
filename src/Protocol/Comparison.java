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
import Utility.Logging;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anisha
 */
public class Comparison implements Callable<Integer> {

    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;

    ExecutorService sendqueueHandler;
    ExecutorService recvqueueHandler;

    private BlockingQueue<Message> commonSender;
    private BlockingQueue<Message> commonReceiver;
    List<Integer> x;
    List<Integer> y;
    int oneShare;
    List<Triple> tiShares;

    HashMap<Integer, Integer> dShares;
    HashMap<Integer, Integer> eShares;
    HashMap<Integer, Integer> multiplicationE;
    HashMap<Integer, Integer> cShares;

    int parentProtocolId;
    int clientID;
    int prime;
    int bitLength;

    /**
     * Constructor
     *
     * A comparison of two numbers with bit length L requires 3L-3 tiShares
     * L for computing dShares
     * L-2 for computing eShares
     * L-1 for computing cShares
     * 
     * @param x
     * @param y
     * @param tiShares
     * @param oneShare
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param prime
     * @param protocolID
     */
    public Comparison(List<Integer> x, List<Integer> y, List<Triple> tiShares,
            int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID) {

        this.x = x;
        this.y = y;
        this.oneShare = oneShare;
        this.tiShares = tiShares;
        this.commonSender = senderQueue;
        this.commonReceiver = receiverQueue;
        this.clientID = clientId;
        this.prime = prime;
        this.parentProtocolId = protocolID;

        bitLength = Math.max(x.size(), y.size());
        eShares = new HashMap<>();
        dShares = new HashMap<>();
        cShares = new HashMap<>();
        multiplicationE = new HashMap<>();

        //System.out.println("bitLength:" + bitLength);
        // Communication between the parent and the sub protocols
        recQueues = new ConcurrentHashMap<>();
        sendQueues = new ConcurrentHashMap<>();

        sendqueueHandler = Executors.newSingleThreadExecutor();
        recvqueueHandler = Executors.newSingleThreadExecutor();

        sendqueueHandler.execute(new SenderQueueHandler(protocolID, commonSender, sendQueues));
        recvqueueHandler.execute(new ReceiverQueueHandler(commonReceiver, recQueues));

    }

    /**
     * Waits for the shares of (x-u) and (y-v), computes the product and returns
     * the value. Returns 1 if x>=y, 0 otherwise
     *
     * @return
     * @throws Exception
     */
    @Override
    public Integer call() throws Exception {
        int w = -1;
        computeEShares();

        ExecutorService threadService = Executors.newCachedThreadPool();
        Runnable dThread = new Runnable() {
            @Override
            public void run() {
                try {
                    computeDSHares();
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        threadService.submit(dThread);
        
        Runnable eThread = new Runnable() {
            @Override
            public void run() {
                computeMultiplicationE();
            }
        };

        threadService.submit(eThread);
        threadService.shutdown();

        // Compute c and w sequentially when both threads end
        boolean threadsCompleted = threadService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        
        if (threadsCompleted) {
            computeCShares();
            w = computeW();
        }
        System.out.println("w:"+w+" protocol id:"+ parentProtocolId);

        tearDownHandlers();
        return w;
    }

    /**
     * Compute shares of [xi]+[yi]+1 locally
     */
    private void computeEShares() {
        for (int i = 0; i < bitLength; i++) {
            int eShare = x.get(i) + y.get(i) + oneShare;
            eShares.put(i, Math.floorMod(eShare, prime));
        }
        //Logging.logShares("eShares", eShares);
    }

    /**
     * compute and store multiplication of ei using distributed multiplication
     */
    private void computeMultiplicationE() {
        //System.out.println("Started multiplicationE");
        multiplicationE.put(bitLength - 1, eShares.get(bitLength - 1));
        // now multiply each eshare with the previous computed multiplication one at a time

        int subProtocolID = bitLength;
        int tiCounter = 0;
        for (int i = bitLength - 1; i > 1; i--) {
            // You don't need i = 0. 
            // Multiplication format: multiplicationE[i] * eShares[i-1]
            //compute local shares of d and e and add to the message queue

            //TODO use the correct sender and receiver queue
            ExecutorService es = Executors.newSingleThreadExecutor();

            if (!recQueues.containsKey(subProtocolID)) {
                BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                recQueues.put(subProtocolID, temp);
            }

            if (!sendQueues.containsKey(subProtocolID)) {
                BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                sendQueues.put(subProtocolID, temp2);
            }
            
            Multiplication multiplicationModule = new Multiplication(multiplicationE.get(i),
                    eShares.get(i - 1), tiShares.get(bitLength + (tiCounter++)),
                    sendQueues.get(subProtocolID), recQueues.get(subProtocolID),
                    clientID, prime, subProtocolID, oneShare);

            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            es.shutdown();

            try {
                multiplicationE.put(i - 1, multiplicationTask.get());
                //System.out.println("result of Multiplication:" + multiplicationE.get(i - 1));
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        multiplicationE.put(0, 0);
        //Logging.logShares("MultiplicationE", multiplicationE);
    }

    /**
     * Compute [di] * integration(ej); j=i+1->l using distributed multiplication
     */
    private void computeCShares() {

        ExecutorService es = Executors.newFixedThreadPool(bitLength);
        List<Future<Integer>> taskList = new ArrayList<>();
        int subProtocolID = bitLength + 1;

        for (int i = 0; i < bitLength - 1; i++) {
            //compute local shares of d and e and add to the message queue
            if (!recQueues.containsKey(subProtocolID + i)) {
                BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                recQueues.put(subProtocolID + i, temp);
            }

            if (!sendQueues.containsKey(subProtocolID + i)) {
                BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                sendQueues.put(subProtocolID + i, temp2);
            }

            Multiplication multiplicationModule = new Multiplication(multiplicationE.get(i + 1),
                    dShares.get(i), tiShares.get(i), sendQueues.get(subProtocolID + i),
                    recQueues.get(subProtocolID + i), clientID, prime, subProtocolID + i, oneShare);

            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);

        }

        es.shutdown();

        // Now when I got the result for all, compute y+ x*y and add it to d[i]
        for (int i = 0; i < bitLength - 1; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            try {
                cShares.put(i, dWorkerResponse.get());
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        cShares.put(bitLength - 1, dShares.get(bitLength - 1));
        //Logging.logShares("cShares", cShares);

    }

    /**
     * Compute 1+summation([ci]) locally
     * @return 
     */
    private int computeW() {
        int w = oneShare;
        for (int i = 0; i < bitLength; i++) {
            w += cShares.get(i);
            w = Math.floorMod(w, prime);
        }

        //Logging.logValue("w", w);
        return w;
    }

    /**
     * Compute [yi] - [yi*xi] using distributed multiplication
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void computeDSHares() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(bitLength);
        List<Future<Integer>> taskList = new ArrayList<>();

        // The protocols for computation of d are assigned id 0-bitLength-1
        for (int i = 0; i < bitLength; i++) {
            //compute local shares of d and e and add to the message queue
            
            if (!recQueues.containsKey(i)) {
                BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                recQueues.put(i, temp);
            }

            if (!sendQueues.containsKey(i)) {
                BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                sendQueues.put(i, temp2);
            }
            
            Multiplication multiplicationModule = new Multiplication(x.get(i),
                    y.get(i), tiShares.get(i),
                    sendQueues.get(i), recQueues.get(i), clientID,
                    prime, i, oneShare);
            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);

        }

        es.shutdown();

        // Now when I got the result for all, compute y+ x*y and add it to d[i]
        for (int i = 0; i < bitLength; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            int localDiff = y.get(i) - dWorkerResponse.get();
            localDiff = Math.floorMod(localDiff, prime);
            dShares.put(i, localDiff);
        }
        
        //Logging.logShares("dShares", dShares);
    }

    /**
     * Teardown all local threads 
     */
    private void tearDownHandlers() {
        recvqueueHandler.shutdownNow();
        sendqueueHandler.shutdownNow();
    }
}
