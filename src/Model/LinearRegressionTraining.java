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
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final Logger LOGGER = Logger.getLogger(LinearRegressionTraining.class.getName());
    static BigInteger[][] x;
    static BigInteger[][] xT;
    BigInteger[][] y;

    static List<TruncationPair> tiTruncationPair;
    static List<TripleReal> realTriples;

    static BigInteger prime;
    String outputPath;
    int globalProtocolId;
    
    /**
     * 
     * @param realTriples
     * @param tiTruncationPair
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param asymmetricBit
     * @param partyCount
     * @param args 
     */
    public LinearRegressionTraining(List<TripleReal> realTriples,
            List<TruncationPair> tiTruncationPair,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue,
            int clientId, int asymmetricBit, int partyCount, String[] args) {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount);
        LinearRegressionTraining.tiTruncationPair = tiTruncationPair;
        LinearRegressionTraining.realTriples = realTriples;
        globalProtocolId = 0;
        prime = BigInteger.valueOf(2).pow(Constants.INTEGER_PRECISION
                + 2 * Constants.DECIMAL_PRECISION + 1).nextProbablePrime();  //Zq must be a prime field
        initalizeModelVariables(args);
    }

    /**
     * Compute shares of the coefficients for the training dataset
     */
    public void trainModel() {

        long startTime = System.currentTimeMillis();
        
        xT = LocalMath.transposeMatrix(x);
        
        //TODO can do both local matrix multiplication in parallel
        BigInteger[][] gamma1 = LocalMath.localMatrixMultiplication(xT, x, prime);
        BigInteger[][] gamma2 = LocalMath.localMatrixMultiplication(xT, y, prime);
        
        MatrixInversion matrixInversion = new MatrixInversion(gamma1,
                realTriples, tiTruncationPair, globalProtocolId, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId, asymmetricBit,
                partyCount, prime);

        globalProtocolId++;

        BigInteger[][] gamma1Inv = null;
        try {
            gamma1Inv = matrixInversion.call();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        System.out.println("Printed gammaInv:" + gamma1Inv[0][0] + ", partyId:" + clientId);
        
        MatrixMultiplication matrixMultiplication = new MatrixMultiplication(gamma1Inv,
                gamma2, realTriples, tiTruncationPair, clientId, prime, globalProtocolId,
                asymmetricBit, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), partyCount);

        BigInteger[][] beta = null;
        try {
            beta = matrixMultiplication.call();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        //TODO: push time to a csv file
        LOGGER.log(Level.INFO, "Avg time duration:{0} for partyId:{1}", 
                new Object[]{elapsedTime, clientId});
        System.out.println("Printed beta:" + beta[0][0] + ", partyId:" + clientId);
        
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
