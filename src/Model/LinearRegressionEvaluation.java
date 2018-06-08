/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.DotProductReal;
import TrustedInitializer.TripleReal;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;
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
    BigInteger prime;
    String outputPath;
    int testCases;
    List<TripleReal> realTiShares;

    /**
     * Constructor
     *
     * @param realTriples
     * @param asymmetricBit
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param partyCount
     * @param args
     * @param protocolIdQueue
     *
     */
    public LinearRegressionEvaluation(List<TripleReal> realTriples,
            int asymmetricBit, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId,
            int partyCount, String[] args, LinkedList<Integer> protocolIdQueue) {

        super(senderQueue, receiverQueue, clientId, asymmetricBit, partyCount, protocolIdQueue);
        this.realTiShares = realTriples;

        y = new ArrayList<>();

        prime = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1).nextProbablePrime();  //Zq must be a prime field

        initalizeModelVariables(args);

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

            initQueueMap(recQueues, i);

            DotProductReal DPModule = new DotProductReal(x.get(i),
                    beta, realTiShares.subList(
                            tiStartIndex, tiStartIndex + x.get(i).size()),
                    commonSender, recQueues.get(i),
                    new LinkedList<>(protocolIdQueue),
                    clientId, prime, i, asymmetricBit, partyCount);

            Future<BigInteger> DPTask = es.submit(DPModule);
            taskList.add(DPTask);
            tiStartIndex += x.get(i).size();
        }

        es.shutdown();

        for (int i = 0; i < testCases; i++) {
            Future<BigInteger> dWorkerResponse = taskList.get(i);
            try {
                BigInteger result = dWorkerResponse.get();
                y.add(result);
                //System.out.println(" #:" + i);
            } catch (InterruptedException ex) {
                Logger.getLogger(LinearRegressionEvaluation.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(LinearRegressionEvaluation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        //TODO: push time to a csv file
        System.out.println("Avg time duration:" + elapsedTime + " for partyId:"
                + clientId + ", for size:" + y.size());
    }

    /**
     * Push results of the prediction (shares) to a csv to send it to the client
     *
     * TODO: Move this to FileIO Utility
     */
    private void writeToCSV() {
        try {
            try (FileWriter writer = new FileWriter(outputPath + "y_" + clientId
                    + ".csv")) {
                for (int i = 0; i < testCases; i++) {
                    writer.write(y.get(i).toString());
                    writer.write("\n");
                }
            }
            System.out.println("Written all lines");
        } catch (IOException ex) {
            Logger.getLogger(LinearRegressionEvaluation.class.getName()).log(Level.SEVERE, null, ex);
        }

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
                case "yCsv":
                    //TODO generalize it
                    beta = FileIO.loadListFromFile(value, prime);
                    break;
                case "output":
                    outputPath = value;
                    break;

            }

        }
        testCases = x.size();
    }

}
