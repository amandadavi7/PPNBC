/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.DotProduct;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Each party receives the shares of x and the co-efficients(beta) and computes 
 * the shares of y, such that y = beta.x
 * 
 * @author anisha
 */
public class LinearRegressionEvaluation extends Model {

    List<List<Integer>> x;
    List<Integer> beta;
    List<Integer> y;
    int testCases;

    public LinearRegressionEvaluation(List<List<Integer>> thetaPower,
            List<Integer> beta,
            List<Triple> decimalTriples,
            int oneShares, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId) {

        super(senderQueue, receiverQueue, clientId, oneShares, null, decimalTriples);

        this.x = thetaPower;
        this.beta = beta;
        testCases = thetaPower.size();

    }

    public List<Integer> predictValues() {

        startModelHandlers();

        computeDotProduct();
        //Dot product for each row
        teardownModelHandlers();
        return y;
    }

    public void computeDotProduct() {
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < testCases; i++) {

            initQueueMap(recQueues,sendQueues,i);
            
            DotProduct DPModule = new DotProduct(x.get(i),
                    beta, decimalTiShares, sendQueues.get(i), recQueues.get(i),
                    clientId, Constants.prime, i, oneShares);

            Future<Integer> DPTask = es.submit(DPModule);
            taskList.add(DPTask);
        }

        es.shutdown();

        for (int i = 0; i < testCases; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            try {
                Integer result = dWorkerResponse.get();
                y.add(result);
                System.out.println("result:" + result + ", #:" + i);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }

}
