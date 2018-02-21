/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.Multiplication;
import TrustedInitializer.Triple;
import Utility.Constants;
import Utility.Logging;
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

    public TestModel(List<Integer> x, List<Integer> y, List<Triple> tiShares,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId) {
        this.x = x;
        this.y = y;
        this.tiShares = tiShares;
        this.senderQueue = senderQueue;
        this.receiverQueue = receiverQueue;
        this.clientId = clientId;
    }

//    public TestModel(List<Integer> x, List<Integer> y, List<Triple> tiShares,
//            ServerSocket server, BlockingQueue<Message> senderQueue,
//            int clientId) {
//        this(x, y, tiShares, server, senderQueue,
//                new LinkedBlockingQueue<Message>(), clientId);
//    }

    public void compute() {
        ExecutorService es = Executors.newSingleThreadExecutor();
        
        Multiplication multiplicationModule = new Multiplication(x.get(0), 
                y.get(0), tiShares.get(0), 
                senderQueue, receiverQueue, clientId, Constants.prime);
        
        Future<Integer> multiplicationTask = es.submit(multiplicationModule);
        
        try {
            int result = multiplicationTask.get();
            System.out.println("result of Multiplication:"+ result);
        } catch (InterruptedException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
