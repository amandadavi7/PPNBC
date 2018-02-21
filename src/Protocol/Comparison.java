/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.DataMessage;
import Communication.Message;
import TrustedInitializer.Triple;
import Utility.Constants;
import Utility.Logging;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
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
public class Comparison implements Callable<Integer> {

    private BlockingQueue<Message> senderQueue;
    private BlockingQueue<Message> receiverQueue;
    List<Integer> x;
    List<Integer> y;
    List<Triple> tiShares;
    List<Integer> dShares;
    List<Integer> eShares;
    List<Integer> multiplicationE;
    List<Integer> cShares;

    int clientID;
    int prime;
    int bitLength;
    int w;

    public Comparison(List<Integer> x, List<Integer> y, List<Triple> tiShares,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime) {

        this.x = x;
        this.y = y;
        this.tiShares = tiShares;
        this.senderQueue = senderQueue;
        this.receiverQueue = receiverQueue;
        this.clientID = clientId;
        this.prime = prime;

        bitLength = Math.max(x.size(), y.size());

    }

    private void computeEShares() {
        for (int i = 0; i < bitLength; i++) {
            int eShare = x.get(i) + y.get(i) + 1;
            eShares.add(i, Math.floorMod(eShare, prime));
        }
        // TODO it should be here. Handle this properly
        //calculateMultiplicationE();
    }

    // convert this to a thread
    private void calculateMultiplicationE() {
        multiplicationE.add(bitLength - 1, eShares.get(bitLength - 1));
        // now multiply each eshare with the previous computed multiplication one at a time

        for (int i = bitLength - 1; i > 1; i--) {
            // You don't need i = 0. 
            // Multiplication format: multiplicationE[i] * eShares[i-1]
            //compute local shares of d and e and add to the message queue

            //TODO use the correct sender and receiver queue
            ExecutorService es = Executors.newSingleThreadExecutor();
            Multiplication multiplicationModule = new Multiplication(multiplicationE.get(i),
                    eShares.get(i-1), tiShares.get(bitLength+1),
                    senderQueue, receiverQueue, clientID, Constants.prime);

            Future<Integer> multiplicationTask = es.submit(multiplicationModule);

            try {
                multiplicationE.add(i-1,multiplicationTask.get());
                System.out.println("result of Multiplication:" + multiplicationE.get(i-1));
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

        // TODO use the correct queue
        for (int i = 0; i < bitLength-1; i++) {
            //compute local shares of d and e and add to the message queue
            Multiplication multiplicationModule = new Multiplication(multiplicationE.get(i+1),
                    dShares.get(i), tiShares.get(i),
                    senderQueue, receiverQueue, clientID, Constants.prime);

            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);
            
        }

        // Now when I got the result for all, compute y+ x*y and add it to d[i]
        for (int i = 0; i < bitLength; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            try {
                cShares.add(i,dWorkerResponse.get());
            } catch (InterruptedException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        cShares.add(bitLength-1,dShares.get(bitLength-1));
        
        computeW();
    }
    
    private void computeW() {
        w = 1;
        for (int i = 0; i < bitLength; i++) {
            w += cShares.get(i);
            w = Math.floorMod(w, Constants.prime);
        }
        
        System.out.println("w calculated:"+ w);
    }
    
    private void computeDSHares() throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(bitLength);
        List<Future<Integer>> taskList = new ArrayList<Future<Integer>>();

        // TODO use the correct queue
        for (int i = 0; i < bitLength; i++) {
            //compute local shares of d and e and add to the message queue
            Multiplication multiplicationModule = new Multiplication(x.get(i),
                    y.get(i), tiShares.get(i),
                    senderQueue, receiverQueue, clientID, Constants.prime);

            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);
            
        }

        // Now when I got the result for all, compute y+ x*y and add it to d[i]
        for (int i = 0; i < bitLength; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            dShares.add(i,y.get(i) + dWorkerResponse.get());
        }
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
//        Message receivedMessage = receiverQueue.take();
//        List<Integer> diffList = (List<Integer>) receivedMessage.getValue();
//
//        int d = Math.floorMod((x - tiShares.u) + diffList.get(0), prime);
//        int e = Math.floorMod((y - tiShares.v) + diffList.get(1), prime);
//        int product = tiShares.w + (d * tiShares.v) + (tiShares.u * e) + (d * e);
//        product = Math.floorMod(product, Constants.prime);
//        return product;
        return -1;
    }
}
