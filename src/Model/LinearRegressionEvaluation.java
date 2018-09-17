/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.DotProductReal;
import Protocol.Utility.BatchTruncation;
import TrustedInitializer.TripleReal;
import TrustedInitializer.TruncationPair;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
    BigInteger[] y;
    BigInteger prime;
    String outputPath;
    int testCases;
    List<TripleReal> realTiShares;
    List<TruncationPair> truncationTiShares;

    /**
     * Constructor
     *
     * @param realTriples
     * @param truncationShares
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param partyCount
     * @param args
     *
     */
    public LinearRegressionEvaluation(List<TripleReal> realTriples,
            List<TruncationPair> truncationShares,
            int asymmetricBit, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue,
            int clientId,
            int partyCount, String[] args) {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount);
        this.realTiShares = realTriples;
        this.truncationTiShares = truncationShares;

        prime = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1).nextProbablePrime();  //Zq must be a prime field

        initalizeModelVariables(args);
        
    }

    /**
     * Compute shares of the prediction for each entry of the dataset:x
     */
    public void predictValues() {

        long startTime = System.currentTimeMillis();
        computeDotProduct();
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        //TODO: push time to a csv file
        System.out.println("Avg time duration:" + elapsedTime + " for partyId:"
                + clientId + ", for size:" + y.length);
        FileIO.writeToCSV(y, outputPath, "y", clientId);

    }

    /**
     * Compute the shares of the prediction using secure dot product such that
     * y[i] = x[i].beta
     */
    public void computeDotProduct() {
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<BigInteger>> taskList = new ArrayList<>();

        int tiStartIndex = 0;
        for (int i = 0; i < testCases; i++) {

            DotProductReal DPModule = new DotProductReal(x.get(i),
                    beta, realTiShares.subList(
                            tiStartIndex, tiStartIndex + x.get(i).size()),
                    pidMapper, commonSender, 
                    new LinkedList<>(protocolIdQueue),
                    clientId, prime, i, asymmetricBit, partyCount);

            Future<BigInteger> DPTask = es.submit(DPModule);
            taskList.add(DPTask);
            tiStartIndex += x.get(i).size();
        }

        

        BigInteger[] dotProductResult = new BigInteger[testCases];
        for (int i = 0; i < testCases; i++) {
            Future<BigInteger> dWorkerResponse = taskList.get(i);
            try {
                BigInteger result = dWorkerResponse.get();
                dotProductResult[i] = result;
                //System.out.println(" #:" + i+ ", result:"+ result);
            } catch (InterruptedException ex) {
                Logger.getLogger(LinearRegressionEvaluation.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(LinearRegressionEvaluation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        BatchTruncation truncationModule = new BatchTruncation(dotProductResult,
                truncationTiShares, pidMapper, 
                commonSender,
                new LinkedList<>(protocolIdQueue),
                clientId, prime, testCases, asymmetricBit, partyCount);
        Future<BigInteger[]> truncationTask = es.submit(truncationModule);

        try {
            y = truncationTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(LinearRegressionTraining.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        es.shutdown();
    }

    private void initalizeModelVariables(String[] args) {
        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.partyUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];

            switch (command) {
                case "xCsv":
                    x = FileIO.loadMatrixFromFile(value);
                    break;
                case "beta":
                    beta = FileIO.loadListFromFile(value);
                    break;
                case "output":
                    outputPath = value;
                    break;

            }

        }
        testCases = x.size();
        y = new BigInteger[testCases];
    }

}
