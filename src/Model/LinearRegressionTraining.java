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
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anisha
 */
public class LinearRegressionTraining extends Model {

    static BigInteger[][] x;
    static BigInteger[][] xT;
    BigInteger[][] y;

    List<TruncationPair> tiTruncationPair;
    
    BigInteger prime;
    String outputPath;
    int globalProtocolId;
    

    /**
     * Constructor
     *
     * @param realTriples
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
            BlockingQueue<Message> receiverQueue, int clientId, int asymmetricBit,
            int partyCount, String[] args) {

        super(senderQueue, receiverQueue, clientId, asymmetricBit, null,
                null, realTriples, partyCount);
        this.tiTruncationPair = tiTruncationPair;
        globalProtocolId = 0;
        initalizeModelVariables(args);
    }

    private void printMatrix(BigInteger[][] x) {
        int rows = x.length;
        int cols = x[0].length;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.err.print(x[i][j] + " ");
            }
            System.out.println();
        }
    }

    /**
     * Compute shares of the prediction for each entry of the dataset:x
     */
    public void trainModel() {

        startModelHandlers();
        transposeMatrix();

        //TODO can do both local matrix multiplication in parallel
        BigInteger[][] gamma1 = localMatrixMultiplication(xT, x);
        printMatrix(gamma1);
        BigInteger[][] gamma2 = localMatrixMultiplication(xT, y);

        MatrixInversion matrixInversion = new MatrixInversion(gamma1,
                realTiShares, tiTruncationPair, globalProtocolId++, commonSender, commonReceiver,
                new LinkedList<>(protocolIdQueue), clientId, asymmetricBit, 
                partyCount, prime);
        
        BigInteger[][] gamma1Inv = null;
        try {
            gamma1Inv = matrixInversion.call();
        } catch (Exception ex) {
            Logger.getLogger(LinearRegressionTraining.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        MatrixMultiplication matrixMultiplication = new MatrixMultiplication(gamma1Inv, 
                gamma2, realTiShares, tiTruncationPair, clientId, prime, globalProtocolId++, 
                asymmetricBit, commonSender, 
                commonReceiver, new LinkedList<>(protocolIdQueue), partyCount);
        
        BigInteger[][] beta = null;
        
        try {
            beta = matrixInversion.call();
        } catch (Exception ex) {
            Logger.getLogger(LinearRegressionTraining.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        
        teardownModelHandlers();
        FileIO.writeToCSV(beta, outputPath, clientId);

    }

    void transposeMatrix() {
        int rows = x.length;
        int cols = x[0].length;
        xT = new BigInteger[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                xT[j][i] = x[i][j];
            }
        }

        printMatrix(xT);

    }

    /**
     * Local matrix Multiplication
     *
     * @param a
     * @param b
     * @param c
     */
    BigInteger[][] localMatrixMultiplication(BigInteger[][] a,
            BigInteger[][] b) {

        int crows = a.length;
        int ccol = b[0].length;
        BigInteger[][] c = new BigInteger[crows][ccol];
        int m = a[0].length;

        for (int i = 0; i < crows; i++) {
            for (int j = 0; j < ccol; j++) {
                // dot product of ith row of a and jth row of b
                BigInteger sum = BigInteger.ZERO;
                for (int k = 0; k < m; k++) {
                    sum = sum.add(a[i][k].multiply(b[k][j])).mod(prime);
                }
                //System.out.println("sum:"+sum);
                c[i][j] = localScale(sum);
                
            }
        }
        return c;
    }

    //TODO convert this to matrix scaling
    BigInteger localScale(BigInteger value) {

        BigInteger scaleFactor = BigInteger.valueOf(2).pow(Constants.decimal_precision);
        value = value.divide(scaleFactor).mod(prime);
        return value;
    }

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
                    //TODO generalize it
                    List<BigInteger> yList = FileIO.loadListFromFile(value, prime);
                    int len = yList.size();
                    y = new BigInteger[len][1];
                    for (int i = 0; i < len; i++) {
                        y[i][1] = yList.get(i);
                    }
                    break;
                case "output":
                    outputPath = value;
                    break;

            }

        }
        
        if(x == null || y == null || outputPath == null) {
            Logging.lrTrainingUsage();
            System.exit(1);
        }
        prime = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1).nextProbablePrime();  //Zq must be a prime field

    }

}
