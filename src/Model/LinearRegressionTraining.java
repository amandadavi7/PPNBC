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

    static List<List<BigInteger>> x;
    static List<List<BigInteger>> xT;
    List<BigInteger> y;

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
    public LinearRegressionTraining(List<List<BigInteger>> x,
            List<BigInteger> y, List<TripleReal> realTriples,
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
        xT = new ArrayList<>();

    }

    static void transpose() {
        final int N = x.get(0).size();
        for (int i = 0; i < N; i++) {
            List<BigInteger> col = new ArrayList<>();
            for (List<BigInteger> row : x) {
                col.add(row.get(i));
            }
            xT.add(col);
        }
    }

    /**
     * Distributed matrix multiplication of a(n*t) and b(t*m) to give result
     * c(n*m)
     *
     * @param a
     * @param b
     */
    void matrixMultiplication(List<List<BigInteger>> a,
            List<List<BigInteger>> b, List<List<BigInteger>> c) {

        int n = a.size();
        int m = b.get(0).size();
        int tiStartIndex = 0;

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<BigInteger>> taskList = new ArrayList<>();

        int protocolIndex = 0;
        for (int i = 0; i < n; i++) {
            List<BigInteger> row = new ArrayList<>();
            for (int j = 0; j < m; j++) {
                // get col from b
                List<BigInteger> col = new ArrayList<>();
                for (List<BigInteger> rowB : b) {
                    col.add(rowB.get(j));
                }
                initQueueMap(recQueues, protocolIndex);
                DotProductReal DPModule = new DotProductReal(a.get(i),
                        col, realTiShares.subList(
                                tiStartIndex, tiStartIndex + m),
                        commonSender, recQueues.get(i),
                        new LinkedList<>(protocolIdQueue),
                        clientId, prime, i, oneShare, partyCount);

                Future<BigInteger> DPTask = es.submit(DPModule);
                taskList.add(DPTask);
                tiStartIndex += m;

            }
        }
        
        es.shutdown();
        int testCases = taskList.size();
        
        for (int i = 0; i < n; i++) {
            List<BigInteger> row = new ArrayList<>();
            for (int j = 0; j < m; j++) {
                Future<BigInteger> dWorkerResponse = taskList.get(i);
                BigInteger result = null;
                try {
                    result = dWorkerResponse.get();
                } catch (InterruptedException ex) {
                    Logger.getLogger(LinearRegressionTraining.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(LinearRegressionTraining.class.getName()).log(Level.SEVERE, null, ex);
                }
                row.add(result);
            }
            c.add(row);
        }

    }

}
