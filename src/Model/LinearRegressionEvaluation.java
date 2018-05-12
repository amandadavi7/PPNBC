/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.DotProductReal;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
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

    List<List<BigInteger>> x;
    List<BigInteger> beta;
    List<BigInteger> y;
    int testCases;
    BigInteger prime;

    /**
     * Constructor
     * @param x     data matrix 
     * @param beta  co-efficient array
     * @param decimalTriples
     * @param oneShares
     * @param senderQueue
     * @param receiverQueue 
     * @param clientId
     * @param prime 
     */
    public LinearRegressionEvaluation(List<List<BigInteger>> x,
            List<BigInteger> beta, List<Triple> decimalTriples,
            int oneShares, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, 
            BigInteger prime) {

        super(senderQueue, receiverQueue, clientId, oneShares, null, 
                decimalTriples);

        this.x = x;
        this.beta = beta;
        this.prime = prime;
        testCases = x.size();
        y = new ArrayList<>();

    }

    /**
     * Compute shares of the prediction for each entry of the dataset:x
     */
    public void predictValues() {

        startModelHandlers();
        computeDotProduct();
        teardownModelHandlers();
        writeToCSV();
        
    }

    /**
     * Compute the shares of the prediction using secure dot product such that
     * y[i] = x[i].beta
     */
    public void computeDotProduct() {
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<BigInteger>> taskList = new ArrayList<>();

        int tiStartIndex = 0;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < testCases; i++) {

            initQueueMap(recQueues,i);
            
            DotProductReal DPModule = new DotProductReal(x.get(i),
                    beta, decimalTiShares.subList(
                            tiStartIndex, tiStartIndex+x.get(i).size()), 
                    commonSender, recQueues.get(i), 
                    new LinkedList<>(protocolIdQueue),
                    clientId, prime, i, oneShare);

            Future<BigInteger> DPTask = es.submit(DPModule);
            taskList.add(DPTask);
            tiStartIndex += tiStartIndex+x.get(i).size();
        }

        es.shutdown();

        for (int i = 0; i < testCases; i++) {
            Future<BigInteger> dWorkerResponse = taskList.get(i);
            try {
                BigInteger result = dWorkerResponse.get();
                y.add(result);
                System.out.println(" #:" + i);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        //TODO: push time to a csv file
        System.out.println("Avg time duration:" + elapsedTime);
    }

    /**
     * Push results of the prediction (shares) to a csv to send it to the client
     * 
     * TODO: Move this to FileIO Utility
     */
    private void writeToCSV() {
        try {
            FileWriter writer = new FileWriter("y_"+clientId+".csv");
            for(int i=0;i<testCases;i++) {
                writer.write(y.get(i).toString());
                writer.write("\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(LinearRegressionEvaluation.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

}
