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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import java.util.stream.Collectors;

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

    List<Integer> x;
    List<Integer> y;
    int oneShare;
    List<Triple> tiShares;

    int[] dShares;
    int[] eShares;
    int[] multiplicationE;
    int[] cShares;

    int clientID;
    int prime;
    int bitLength;
    int cProcessId;

    /**
     * Constructor
     *
     * A comparison of two numbers with bit length L requires 3L-3 tiShares
     * L for computing dShares
     * L-2 for computing eShares
     * L-1 for computing cShares
     * 
     * @param x List of bits of shares of x
     * @param y List of bits of shares of y
     * @param tiShares
     * @param oneShare  [[1]] with the Party
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

        super(protocolID, senderQueue, receiverQueue);
        this.x = x;
        this.y = y;
        this.oneShare = oneShare;
        this.tiShares = tiShares;
        this.clientID = clientId;
        this.prime = prime;

        bitLength = Math.max(x.size(), y.size());
        eShares = new int[bitLength];
        dShares = new int[bitLength];
        cShares = new int[bitLength];
        multiplicationE = new int[bitLength];

        //System.out.println("bitLength:" + bitLength);
        // Communication between the parent and the sub protocols
        recQueues = new ConcurrentHashMap<>();
        sendQueues = new ConcurrentHashMap<>();

        queueHandlers = Executors.newFixedThreadPool(2);
        senderThread = new SenderQueueHandler(protocolID, super.senderQueue, sendQueues);
        receiverThread = new ReceiverQueueHandler(protocolID, super.receiverQueue, recQueues);
        queueHandlers.submit(senderThread);
        queueHandlers.submit(receiverThread);

    }

    /**
     * Waits for the shares of (x-u) and (y-v), computes the product and returns
     * the value. Returns 1 if x>=y, 0 otherwise
     *
     * @return shares of [1] if x>=y and [0] if x<y
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
                computeMultiplicationEParallel();
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

        System.out.println("w:" + w + " protocol id:" + protocolId);

        tearDownHandlers();
        return w;
    }

    /**
     * Compute shares of [xi]+[yi]+1 locally
     */
    private void computeEShares() {
        for (int i = 0; i < bitLength; i++) {
            int eShare = x.get(i) + y.get(i) + oneShare;
            eShares[i] = Math.floorMod(eShare, prime);

        }
        //Logging.logShares("eShares", eShares);
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
        int startpid = 0;

        // The protocols for computation of d are assigned id 0-bitLength-1
        do {
            System.out.println("Protocol " + protocolId + " batch " + startpid);
            initQueueMap(recQueues, sendQueues, startpid);

            int toIndex = (i + Constants.batchSize < bitLength)
                    ? (i + Constants.batchSize) : bitLength;

            BatchMultiplication batchMultiplication = new BatchMultiplication(
                    x.subList(i, toIndex),
                    y.subList(i, toIndex),
                    tiShares.subList(i, toIndex),
                    sendQueues.get(startpid), recQueues.get(startpid),
                    clientID, prime, startpid, oneShare, protocolId);

            Future<Integer[]> multiplicationTask = es.submit(batchMultiplication);
            taskList.add(multiplicationTask);

            startpid++;
            i += Constants.batchSize;
        } while (i < bitLength);

        es.shutdown();

        int taskLen = taskList.size();
        // Now when I got the result for all, compute y - x*y and add it to d[i]
        for (i = 0; i < taskLen; i++) {
            try {
                Future<Integer[]> prod = taskList.get(i);
                Integer[] products = prod.get();
                int prodLen = products.length;
                for (int j = 0; j < prodLen; j++) {
                    int globalIndex = i * 10 + j;
                    int localDiff = y.get(globalIndex) - products[j];
                    localDiff = Math.floorMod(localDiff, prime);
                    dShares[globalIndex] = localDiff;
                    //dShares.put(globalIndex, localDiff);
                }
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }

        }

        //Logging.logShares("dShares", dShares);
    }

    /**
     * compute and store multiplication of ei using distributed multiplication
     */
    /*private void computeMultiplicationE() {
        //System.out.println("Started multiplicationE");
        multiplicationE[bitLength-1] = eShares[bitLength-1];
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

            Multiplication multiplicationModule = new Multiplication(
                    multiplicationE[i],
                    eShares[i - 1], tiShares.get(bitLength + (tiCounter++)),
                    sendQueues.get(subProtocolID), recQueues.get(subProtocolID),
                    clientID, prime, subProtocolID, oneShare, protocolId);

            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            es.shutdown();

            try {
                multiplicationE[i-1] = multiplicationTask.get();
                //System.out.println("result of Multiplication:" + multiplicationE.get(i - 1));
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        multiplicationE[0] = 0;
        //Logging.logShares("MultiplicationE", multiplicationE);
    }*/

    private void computeMultiplicationEParallel() {
        List<Integer> tempMultE = Arrays.stream(eShares).boxed().collect(Collectors.toList());
        
        int mainIndex = bitLength - 1;
        multiplicationE[mainIndex--] = eShares[bitLength - 1];

        List<Integer> dShareList = Arrays.stream(dShares).boxed().collect(Collectors.toList());
        
        int startpid = bitLength;
        
        // Runs log n times
        while (tempMultE.size() > 1) {
            ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
            List<Future<Integer[]>> taskList = new ArrayList<>();

            int i = 0;
            // batch multiply each pair of tempMultE[i], tempMult[i+1]
            do {
                System.out.println("Protocol " + protocolId + " batch " + startpid);
                initQueueMap(recQueues, sendQueues, startpid);

                int toIndex = (i + Constants.batchSize < bitLength)
                        ? (i + Constants.batchSize) : (bitLength);

                BatchMultiplication batchMultiplication = new BatchMultiplication(
                        tempMultE.subList(i, toIndex - 1),
                        tempMultE.subList(i + 1, toIndex),
                        tiShares.subList(i, toIndex), sendQueues.get(startpid),
                        recQueues.get(startpid), clientID, prime, startpid,
                        oneShare, protocolId);

                Future<Integer[]> multiplicationTask = es.submit(batchMultiplication);
                taskList.add(multiplicationTask);

                startpid++;
                i += Constants.batchSize;
            } while (i < bitLength - 1);

            es.shutdown();

            int taskLen = taskList.size();
            // Now when I got the result for all, compute y+ x*y and add it to d[i]
            for (i = 0; i < taskLen; i++) {
                try {
                    Future<Integer[]> prod = taskList.get(i);
                    Integer[] products = prod.get();
                    // update all values
                    tempMultE.clear();
                    tempMultE = Arrays.stream(products).collect(Collectors.toList());
                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                }

            }

            // store the main value in the end
            multiplicationE[mainIndex--] = tempMultE.get(tempMultE.size() - 1);
        }
        
        cProcessId = startpid;

        multiplicationE[0] = 0;

    }

    /**
     * Compute [di] * integration(ej); j=i+1->l using distributed multiplication
     */
    private void computeCShares() {

        List<Integer> multiplicationEList = Arrays.stream(multiplicationE).boxed().collect(Collectors.toList());
        List<Integer> dShareList = Arrays.stream(dShares).boxed().collect(Collectors.toList());

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        int startpid = cProcessId;
        int i = 0;

        do {
            System.out.println("Protocol " + protocolId + " batch " + startpid);
            initQueueMap(recQueues, sendQueues, startpid);

            int toIndex = (i + Constants.batchSize < bitLength - 1)
                    ? (i + Constants.batchSize) : (bitLength - 1);

            BatchMultiplication batchMultiplication = new BatchMultiplication(
                    multiplicationEList.subList(i + 1, toIndex + 1),
                    dShareList.subList(i, toIndex),
                    tiShares.subList(i, toIndex), sendQueues.get(startpid),
                    recQueues.get(startpid), clientID, prime, startpid,
                    oneShare, protocolId);

            Future<Integer[]> multiplicationTask = es.submit(batchMultiplication);
            taskList.add(multiplicationTask);

            startpid++;
            i += Constants.batchSize;
        } while (i < bitLength - 1);

        es.shutdown();

        int taskLen = taskList.size();
        // Now when I got the result for all, compute y+ x*y and add it to d[i]
        for (i = 0; i < taskLen; i++) {
            try {
                Future<Integer[]> prod = taskList.get(i);
                Integer[] products = prod.get();
                int prodLen = products.length;
                for (int j = 0; j < prodLen; j++) {
                    int globalIndex = i * 10 + j;
                    cShares[globalIndex] = products[j];
                    //cShares.put(globalIndex, products[j]);
                }
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }

        }

        cShares[bitLength - 1] = dShares[bitLength - 1];
        //cShares.put(bitLength - 1, dShares[bitLength - 1]);
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
            w += cShares[i];
            w = Math.floorMod(w, prime);
        }

        //Logging.logValue("w", w);
        return w;
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
