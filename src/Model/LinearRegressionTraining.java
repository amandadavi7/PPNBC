/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.MatrixInversion;
import Protocol.Utility.MatrixMultiplication;
import TrustedInitializer.TripleReal;
import TrustedInitializer.TruncationPair;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;
import Utility.LocalMath;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Takes as input horizontally partitioned data and computed shares of the 
 * coefficients
 * 
 * Computes <beta> = (xT.x)^(-1).(xT.y)
 * @author anisha
 */
public class LinearRegressionTraining extends Model {

    static BigInteger[][] x;
    static BigInteger[][] xT;
    BigInteger[][] y;

    List<TruncationPair> tiTruncationPair;
    List<TripleReal> realTriples;

    BigInteger prime;
    String outputPath;
    int globalProtocolId;

    /**
     * Constructor
     *
     * @param realTriples
     * @param tiTruncationPair
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param asymmetricBit
     * @param partyCount
     * @param args
     */
    public LinearRegressionTraining(List<TripleReal> realTriples,
            List<TruncationPair> tiTruncationPair,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, 
            int clientId, int asymmetricBit, int partyCount, String[] args) {

        super(senderQueue, receiverQueue, clientId, asymmetricBit, partyCount);
        this.tiTruncationPair = tiTruncationPair;
        this.realTriples = realTriples;
        globalProtocolId = 0;
        prime = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1).nextProbablePrime();  //Zq must be a prime field
        initalizeModelVariables(args);
    }

    /**
     * Compute shares of the coefficients for the training dataset
     */
    public void trainModel() {

        startModelHandlers();
        long startTime = System.currentTimeMillis();
        
        xT = LocalMath.transposeMatrix(x);
        
        //TODO can do both local matrix multiplication in parallel
        BigInteger[][] gamma1 = LocalMath.localMatrixMultiplication(xT, x, prime);
        BigInteger[][] gamma2 = LocalMath.localMatrixMultiplication(xT, y, prime);
        
        initQueueMap(recQueues, globalProtocolId);
        MatrixInversion matrixInversion = new MatrixInversion(gamma1,
                realTriples, tiTruncationPair, globalProtocolId, commonSender,
                recQueues.get(globalProtocolId),
                new LinkedList<>(protocolIdQueue), clientId, asymmetricBit,
                partyCount, prime);

        globalProtocolId++;

        BigInteger[][] gamma1Inv = null;
        try {
            gamma1Inv = matrixInversion.call();
        } catch (Exception ex) {
            Logger.getLogger(LinearRegressionTraining.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        initQueueMap(recQueues, globalProtocolId);
        MatrixMultiplication matrixMultiplication = new MatrixMultiplication(gamma1Inv,
                gamma2, realTriples, tiTruncationPair, clientId, prime, globalProtocolId,
                asymmetricBit, commonSender,
                recQueues.get(globalProtocolId), new LinkedList<>(protocolIdQueue), partyCount);

        BigInteger[][] beta = null;
        try {
            beta = matrixMultiplication.call();
        } catch (Exception ex) {
            Logger.getLogger(LinearRegressionTraining.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        //TODO: push time to a csv file
        System.out.println("Avg time duration:" + elapsedTime + " for partyId:"
                + clientId);

        teardownModelHandlers();
        FileIO.writeToCSV(beta, outputPath, "beta", clientId);
        
    }

    /**
     * Initialize the input matrix, the label vector and the output file path
     * @param args 
     */
    private void initalizeModelVariables(String[] args) {

        for (String arg : args) {
            String[] currInput = arg.split("=");
            String command = currInput[0];
            String value = currInput[1];

            switch (command) {
                case "xCsv":
                    List<List<BigInteger>> xList = FileIO.loadMatrixFromFile(value, prime);
                    int row = xList.size();
                    int col = xList.get(0).size();
                    x = new BigInteger[row][col];
                    for (int i = 0; i < row; i++) {
                        for (int j = 0; j < col; j++) {
                            x[i][j] = xList.get(i).get(j);
                        }
                    }
                    break;
                case "yCsv":
                    List<BigInteger> yList = FileIO.loadListFromFile(value, prime);
                    int len = yList.size();
                    y = new BigInteger[len][1];
                    for (int i = 0; i < len; i++) {
                        y[i][0] = yList.get(i);
                    }
                    break;
                case "output":
                    outputPath = value;
                    break;

            }

        }

        if (x == null || y == null || outputPath == null) {
            Logging.lrTrainingUsage();
            System.exit(1);
        }

    }

}
