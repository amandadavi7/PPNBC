/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.DataMessage;
import Communication.Message;
import Communication.ReceiverQueueHandler;
import Communication.SenderQueueHandler;
import TrustedInitializer.Triple;
import Utility.Constants;
import Utility.Logging;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
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
    List<Triple> tiShares;
    List<Integer> dShares;
    List<Integer> eShares;
    List<Integer> multiplicationE;
    List<Integer> cShares;

    int parentProtocolId;
    int clientID;
    int prime;
    int bitLength;

    public Comparison(List<Integer> x, List<Integer> y, List<Triple> tiShares,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID) {

        this.x = x;
        this.y = y;
        this.tiShares = tiShares;
        this.commonSender = senderQueue;
        this.commonReceiver = receiverQueue;
        this.clientID = clientId;
        this.prime = prime;
        this.parentProtocolId = protocolID;

        bitLength = Math.max(x.size(), y.size());

        // Communication between the parent and the sub protocols
        recQueues = new ConcurrentHashMap<>();
        sendQueues = new ConcurrentHashMap<>();

        ExecutorService sendqueueHandler = Executors.newSingleThreadExecutor();
        ExecutorService recvqueueHandler = Executors.newSingleThreadExecutor();
        
        sendqueueHandler.execute(new SenderQueueHandler(protocolID,commonSender,sendQueues));
        recvqueueHandler.execute(new ReceiverQueueHandler(commonReceiver, recQueues));

    }

    /**
     * Waits for the shares of (x-u) and (y-v), computes the product and returns
     * the value
     *
     * @return
     * @throws Exception
     */
    @Override
    public Integer call() throws Exception {
        computeEShares();
        //Start a thread for computing all the d shares
        computeDSHares();
        
        // start a thread to compute all the multiplication of e
        computeMultiplicationE();
                
        // Compute c when both threads end
        //compute w when c thread ends
        int w = computeW();
//        Message receivedMessage = receiverQueue.take();
//        List<Integer> diffList = (List<Integer>) receivedMessage.getValue();
//
//        int d = Math.floorMod((x - tiShares.u) + diffList.get(0), prime);
//        int e = Math.floorMod((y - tiShares.v) + diffList.get(1), prime);
//        int product = tiShares.w + (d * tiShares.v) + (tiShares.u * e) + (d * e);
//        product = Math.floorMod(product, Constants.prime);
//        return product;
        return w;
    }

    private void computeEShares() {
        for (int i = 0; i < bitLength; i++) {
            int eShare = x.get(i) + y.get(i) + 1;
            eShares.add(i, Math.floorMod(eShare, prime));
        }
    }

    // convert this to a thread
    private void computeMultiplicationE() {
        multiplicationE.add(bitLength - 1, eShares.get(bitLength - 1));
        // now multiply each eshare with the previous computed multiplication one at a time

        int subProtocolID = bitLength;
        for (int i = bitLength - 1; i > 1; i--) {
            // You don't need i = 0. 
            // Multiplication format: multiplicationE[i] * eShares[i-1]
            //compute local shares of d and e and add to the message queue

            //TODO use the correct sender and receiver queue
            ExecutorService es = Executors.newSingleThreadExecutor();
            
            BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
            recQueues.put(subProtocolID, temp);
            BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
            sendQueues.put(subProtocolID,temp2);
            
            
            Multiplication multiplicationModule = new Multiplication(multiplicationE.get(i),
                    eShares.get(i - 1), tiShares.get(bitLength + 1),
                    sendQueues.get(subProtocolID), recQueues.get(subProtocolID), 
                    clientID, Constants.prime, subProtocolID);

            Future<Integer> multiplicationTask = es.submit(multiplicationModule);

            try {
                multiplicationE.add(i - 1, multiplicationTask.get());
                System.out.println("result of Multiplication:" + multiplicationE.get(i - 1));
            } catch (InterruptedException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        multiplicationE.add(0, 0);
        computeCShares();

    }

    private void computeCShares() {

        ExecutorService es = Executors.newFixedThreadPool(bitLength);
        List<Future<Integer>> taskList = new ArrayList<Future<Integer>>();

        for (int i = 0; i < bitLength - 1; i++) {
            //compute local shares of d and e and add to the message queue
            BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
            recQueues.put(i, temp);
            BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
            sendQueues.put(i,temp2);
            
            Multiplication multiplicationModule = new Multiplication(multiplicationE.get(i + 1),
                    dShares.get(i), tiShares.get(i),
                    sendQueues.get(i), recQueues.get(i), clientID, Constants.prime, i);

            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);

        }

        // Now when I got the result for all, compute y+ x*y and add it to d[i]
        for (int i = 0; i < bitLength; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            try {
                cShares.add(i, dWorkerResponse.get());
            } catch (InterruptedException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        cShares.add(bitLength - 1, dShares.get(bitLength - 1));

        computeW();
    }

    private int computeW() {
        int w = 1;
        for (int i = 0; i < bitLength; i++) {
            w += cShares.get(i);
            w = Math.floorMod(w, Constants.prime);
        }

        System.out.println("w calculated:" + w);
        return w;
    }

    private void computeDSHares() throws InterruptedException, ExecutionException {
        
        ExecutorService es = Executors.newFixedThreadPool(bitLength);
        List<Future<Integer>> taskList = new ArrayList<Future<Integer>>();

        // The protocols for computation of d are assigned id 0-bitLength-1
        for (int i = 0; i < bitLength; i++) {
            //compute local shares of d and e and add to the message queue
            BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
            recQueues.put(i, temp);
            BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
            sendQueues.put(i,temp2);
            
            Multiplication multiplicationModule = new Multiplication(x.get(i),
                    y.get(i), tiShares.get(i),
                    sendQueues.get(i), recQueues.get(i), clientID, 
                    Constants.prime, i);
            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);
        
        }

        // Now when I got the result for all, compute y+ x*y and add it to d[i]
        for (int i = 0; i < bitLength; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            dShares.add(i, y.get(i) + dWorkerResponse.get());
        }
    }
}
