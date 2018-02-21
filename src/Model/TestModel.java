/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.Comparison;
import Protocol.DotProduct;
import Protocol.Multiplication;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 *
 * @author anisha
 */
public class TestModel {

    private BlockingQueue<Message> senderQueue;
    private BlockingQueue<Message> receiverQueue;
    int clientId;
    List<Integer> x;
    List<Integer> y;
    List<Triple> tiShares;
    int oneShares;

    public TestModel(List<Integer> x, List<Integer> y, List<Triple> tiShares,
            int oneShares,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId) {
        this.x = x;
        this.y = y;
        this.tiShares = tiShares;
        this.oneShares = oneShares;
        this.senderQueue = senderQueue;
        this.receiverQueue = receiverQueue;
        this.clientId = clientId;
    }

    public void compute() {
        ExecutorService es = Executors.newSingleThreadExecutor();

        //Multiplication multiplicationModule = new Multiplication(x.get(0), 
        //        y.get(0), tiShares.get(0), 
        //        senderQueue, receiverQueue, clientId, Constants.prime,0);
        //Future<Integer> multiplicationTask = es.submit(multiplicationModule);
//        DotProduct dotproductModule = new DotProduct(x, y, tiShares, senderQueue, 
//                receiverQueue, clientId, Constants.prime, 1);
//        
//        Future<Integer> dotProduct = es.submit(dotproductModule);
        Comparison comparisonModule = new Comparison(x, y, tiShares, oneShares, senderQueue,
                receiverQueue, clientId, Constants.binaryPrime, 1);

        Future<Integer> comparisonTask = es.submit(comparisonModule);
        
        try {
            int result = comparisonTask.get();
            System.out.println("result of dot product:" + result);
        } catch (InterruptedException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
