/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.DotProductReal;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import TrustedInitializer.TripleReal;
import Utility.Constants;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anisha
 */
public class LinearRegressionTraining extends Model {

    static BigInteger[][] x;
    static BigInteger[][] xT;
    BigInteger[] y;

    BigInteger prime;
    String outputPath;

    /**
     * Constructor
     *
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param oneShares
     * @param binaryTiShares
     * @param decimalTiShares
     * @param realTiShares
     * @param partyCount
     */
    public LinearRegressionTraining(BigInteger[][] x,
            BigInteger[] y, List<TripleReal> realTriples,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int oneShares,
            List<TripleByte> binaryTiShares, List<TripleInteger> decimalTiShares,
            List<TripleReal> realTiShares, int partyCount, BigInteger prime) {

        super(senderQueue, receiverQueue, clientId, oneShares, binaryTiShares,
                decimalTiShares, realTiShares, partyCount);
        this.x = x;
        this.y = y;
        this.prime = prime;
        this.outputPath = outputPath;
    }

    /**
     * Compute shares of the prediction for each entry of the dataset:x
     */
    public void trainModel() {

        startModelHandlers();
        init();
        //computeTranspose();
        // parallely:
        // 1. gamma1 = matrixMultiplication(Xt.X), matrix inverse
        // 2. gamma2 = matrixMultiplication(Xy.Y)
        // 3. beta = matrixMultiplication(gamma1.gamma2)
        teardownModelHandlers();
        //writeToCSV();

    }

    private void init() {

    }

    static void transpose() {
        int rows = x.length;
        int cols = x[0].length;
        xT = new BigInteger[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                xT[j][i] = x[i][j];
            }
        }
    }

    /**
     * Local matrix Multiplication
     *
     * @param a
     * @param b
     * @param c
     */
    void localMatrixMultiplication(BigInteger[][] a,
            BigInteger[][] b, BigInteger[][] c) {

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<BigInteger>> taskList = new ArrayList<>();

        int crows = a.length;
        int ccol = b[0].length;
        int m = a[0].length;

        for (int i = 0; i < crows; i++) {
            for (int j = 0; j < ccol; j++) {
                // dot product of ith row of a and jth row of b
                BigInteger sum = BigInteger.ZERO;
                for (int k = 0; k < m; k++) {
                    sum.add(a[i][k].multiply(b[k][j])).mod(prime);
                }

                c[i][j] = sum;
            }
        }
    }

    //TODO convert this to matrix scaling
    BigInteger localScale(BigInteger value, int currentScale, int newScale) {
        
        BigInteger scaleFactor = BigInteger.valueOf(2).pow(Constants.decimal_precision);
        value = value.divide(scaleFactor).mod(prime);
        return value;
    }
    
}
