/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.DotProduct;
import Protocol.DotProductReal;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
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

    List<List<BigInteger>> x;
    List<BigInteger> beta;
    List<BigInteger> y;
    int testCases;
    BigInteger prime;

    public LinearRegressionEvaluation(List<List<BigInteger>> x,
            List<BigInteger> beta,
            List<Triple> decimalTriples,
            int oneShares, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, BigInteger prime) {

        super(senderQueue, receiverQueue, clientId, oneShares, null, decimalTriples);

        this.x = x;
        this.beta = beta;
        this.prime = prime;
        testCases = x.size();
        y = new ArrayList<>();

    }

    public void predictValues() {

        startModelHandlers();

        computeDotProduct();
        //Dot product for each row
        teardownModelHandlers();
        writeToCSV();
        
    }

    public void computeDotProduct() {
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<BigInteger>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < testCases; i++) {

            initQueueMap(recQueues,sendQueues,i);
            
            DotProductReal DPModule = new DotProductReal(x.get(i),
                    beta, decimalTiShares, sendQueues.get(i), recQueues.get(i),
                    clientId, prime, i, oneShares);

            Future<BigInteger> DPTask = es.submit(DPModule);
            taskList.add(DPTask);
        }

        es.shutdown();

        for (int i = 0; i < testCases; i++) {
            Future<BigInteger> dWorkerResponse = taskList.get(i);
            try {
                BigInteger result = dWorkerResponse.get();
                y.add(result);
                System.out.println(" #:" + i);
                //System.out.println("result:" + result + ", #:" + i);
            } catch (InterruptedException ex) {
                System.out.println("EXCEPTION: id:"+i);
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                System.out.println("EXCEPTION: id:"+i);
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }

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
