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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anisha
 */
public class Comparison extends Protocol implements Callable<Integer> {

    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;

    ExecutorService queueHandlers;
    SenderQueueHandler senderThread;
    ReceiverQueueHandler receiverThread;

    private BlockingQueue<Message> commonSender;
    private BlockingQueue<Message> commonReceiver;
    List<Integer> x;
    List<Integer> y;
    int oneShare;
    List<Triple> tiShares;

    ConcurrentSkipListMap<Integer, Integer> dShares;
    HashMap<Integer, Integer> eShares;
    ConcurrentSkipListMap<Integer, Integer> multiplicationE;
    HashMap<Integer, Integer> cShares;

    int protocolID;
    int clientID;
    int prime;
    int bitLength;

    /**
     * Constructor
     *
     * A comparison of two numbers with bit length L requires 3L-3 tiShares L
     * for computing dShares L-2 for computing eShares L-1 for computing cShares
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

        super(protocolID);
        this.x = x;
        this.y = y;
        this.oneShare = oneShare;
        this.tiShares = tiShares;
        this.commonSender = senderQueue;
        this.commonReceiver = receiverQueue;
        this.clientID = clientId;
        this.prime = prime;
        this.protocolID = protocolID;

        bitLength = Math.max(x.size(), y.size());
        eShares = new HashMap<>();
        dShares = new ConcurrentSkipListMap<>();
        cShares = new HashMap<>();
        multiplicationE = new ConcurrentSkipListMap<>();

        //System.out.println("bitLength:" + bitLength);
        // Communication between the parent and the sub protocols
        recQueues = new ConcurrentHashMap<>();
        sendQueues = new ConcurrentHashMap<>();

        queueHandlers = Executors.newFixedThreadPool(2);
        senderThread = new SenderQueueHandler(protocolID, commonSender, sendQueues);
        receiverThread = new ReceiverQueueHandler(protocolID, commonReceiver, recQueues);
        queueHandlers.submit(senderThread);
        queueHandlers.submit(receiverThread);

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

        ExecutorService threadService = Executors.newFixedThreadPool(Constants.threadCount);
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

        System.out.println("w:" + w + " protocol id:" + protocolID);

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

        initQueueMap(recQueues, sendQueues, subProtocolID);

        for (int i = bitLength - 1; i > 1; i--) {
            // You don't need i = 0. 
            // Multiplication format: multiplicationE[i] * eShares[i-1]
            //compute local shares of d and e and add to the message queue

            //TODO use the correct sender and receiver queue
            ExecutorService es = Executors.newSingleThreadExecutor();

            Multiplication multiplicationModule = new Multiplication(multiplicationE.get(i),
                    eShares.get(i - 1), tiShares.get(bitLength + (tiCounter++)),
                    sendQueues.get(subProtocolID), recQueues.get(subProtocolID),
                    clientID, prime, subProtocolID, oneShare, protocolID);

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

        List<Integer> multiplicationEList = new ArrayList<>(multiplicationE.values());
        List<Integer> dShareList = new ArrayList<>(dShares.values());

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        int startpid = bitLength + 1;

        int i = 0;
        int batchsize = 10;

        // The protocols for computation of d are assigned id 0-bitLength-1
        while (i + batchsize < bitLength) {

            System.out.println("Protocol " + protocolID + " batch " + startpid);
            initQueueMap(recQueues, sendQueues, startpid);

            BatchMultiplication batchMultiplication = new BatchMultiplication(
                    multiplicationEList.subList(i + 1, i + 1 + batchsize),
                    dShareList.subList(i, i + batchsize),
                    tiShares.subList(i, i + batchsize), sendQueues.get(startpid),
                    recQueues.get(startpid), clientID, prime, startpid,
                    oneShare, protocolID);

            Future<Integer[]> multiplicationTask = es.submit(batchMultiplication);
            taskList.add(multiplicationTask);

            startpid++;
            i += batchsize;
        }

        if (i < bitLength) {
            System.out.println("Protocol " + protocolID + " batch " + startpid);
            initQueueMap(recQueues, sendQueues, startpid);

            BatchMultiplication batchMultiplication = new BatchMultiplication(
                    multiplicationEList.subList(i + 1, bitLength -1),
                    dShareList.subList(i, bitLength - 2),
                    tiShares.subList(i, bitLength - 2), sendQueues.get(startpid),
                    recQueues.get(startpid), clientID, prime, startpid,
                    oneShare, protocolID);
            
            Future<Integer[]> multiplicationTask = es.submit(batchMultiplication);
            taskList.add(multiplicationTask);

            startpid++;

        }

        es.shutdown();

        // Now when I got the result for all, compute y+ x*y and add it to d[i]
        for (i = 0; i < startpid; i++) {
            try {
                Future<Integer[]> prod = taskList.get(i);
                Integer[] products = prod.get();
                for (int j = 0; j < products.length; j++) {
                    cShares.put(i+j, products[j]);
                }
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
        }

        cShares.put(bitLength - 1, dShares.get(bitLength - 1));
        //Logging.logShares("cShares", cShares);

    }

    /**
     * Compute 1+summation([ci]) locally
     *
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
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void computeDSHares() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        int i = 0;
        int batchsize = 10;
        int startpid = 0;

        // The protocols for computation of d are assigned id 0-bitLength-1
        while (i + batchsize < bitLength) {

            System.out.println("Protocol " + protocolID + " batch " + startpid);
            initQueueMap(recQueues, sendQueues, startpid);

            BatchMultiplication batchMultiplication = new BatchMultiplication(
                    x.subList(i, i + batchsize), y.subList(i, i + batchsize),
                    tiShares.subList(i, i + batchsize), sendQueues.get(startpid),
                    recQueues.get(startpid), clientID, prime, startpid,
                    oneShare, protocolID);

            Future<Integer[]> multiplicationTask = es.submit(batchMultiplication);
            taskList.add(multiplicationTask);

            startpid++;
            i += batchsize;
        }

        if (i < bitLength) {
            System.out.println("Protocol " + protocolID + " batch " + startpid);
            initQueueMap(recQueues, sendQueues, startpid);

            BatchMultiplication batchMultiplication = new BatchMultiplication(
                    x.subList(i, bitLength), y.subList(i, bitLength),
                    tiShares.subList(i, bitLength), sendQueues.get(startpid),
                    recQueues.get(startpid), clientID, prime, startpid,
                    oneShare, protocolID);

            Future<Integer[]> multiplicationTask = es.submit(batchMultiplication);
            taskList.add(multiplicationTask);

            startpid++;

        }

        es.shutdown();

        // Now when I got the result for all, compute y+ x*y and add it to d[i]
        for (i = 0; i < startpid; i++) {
            try {
                Future<Integer[]> prod = taskList.get(i);
                Integer[] products = prod.get();
                for (int j = 0; j < products.length; j++) {
                    int localDiff = y.get(i + j) - products[j];
                    localDiff = Math.floorMod(localDiff, prime);
                    dShares.put(i + j, localDiff);
                }
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }

        }

        //Logging.logShares("dShares", dShares);
    }

    /**
     * Teardown all local threads
     */
    private void tearDownHandlers() {
        senderThread.setProtocolStatus();
        receiverThread.setProtocolStatus();
        queueHandlers.shutdown();
    }

    /*private List<Integer> convertMapToList(HashMap<Integer, Integer> multiplicationE1) {
        List<Integer> list = new ArrayList<>();
        Iterator i = multiplicationE.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            list.add((Integer) entry.getKey(), (Integer) entry.getValue());
        }
        return list;
    }*/
}
