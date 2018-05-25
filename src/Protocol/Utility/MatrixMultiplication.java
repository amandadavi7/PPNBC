/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Model.LinearRegressionTraining;
import Protocol.CompositeProtocol;
import Protocol.DotProductReal;
import TrustedInitializer.TripleReal;
import Utility.Constants;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to take care of matrix multiplication
 *
 * @author anisha
 */
public class MatrixMultiplication extends CompositeProtocol implements 
        Callable<BigInteger[][]> {

    BigInteger[][] a, b;
    List<TripleReal> tishares;
    BigInteger prime;

    /**
     * Uses n-1 tiShares
     *
     * @param a
     * @param b
     * @param tishares
     * @param clientID
     * @param prime
     * @param protocolID
     * @param oneShare
     * @param senderQueue
     * @param receiverQueue
     * @param protocolIdQueue
     * @param partyCount
     */
    public MatrixMultiplication(BigInteger[][] a, BigInteger[][] b,
            List<TripleReal> tishares,
            int clientID, BigInteger prime, int protocolID,
            int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue,
            Queue<Integer> protocolIdQueue, int partyCount) {

        super(protocolID, senderQueue, receiverQueue, protocolIdQueue, clientID, 
                oneShare, partyCount);
        this.a = a;
        this.b = b;
        this.tishares = tishares;
        this.prime = prime;
    }

    /**
     * a: n*m, b = m*l
     * Local matrix multiplication of a(n*t) and b(t*m) to give result c(n*m)
     * @return
     * @throws Exception
     */
    @Override
    public BigInteger[][] call() throws Exception {
        
        startHandlers();
        int n = a.length;
        int m = a[0].length;
        int l = b[0].length;
        BigInteger[][] c = new BigInteger[n][l];

        int tiStartIndex = 0;

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<BigInteger>> taskList = new ArrayList<>();

        int protocolIndex = 0;
        for (int i = 0; i < n; i++) {
            List<BigInteger> row = new ArrayList<>();
            for (int k = 0; k < m; k++) {
                row.add(a[i][k]);
            }
            for (int j = 0; j < l; j++) {
                // get jth col from b
                List<BigInteger> col = new ArrayList<>();
                for (int k = 0; k < m; k++) {
                    col.add(b[k][j]);
                }

                initQueueMap(recQueues, protocolIndex);
                DotProductReal DPModule = new DotProductReal(row,
                        col, tishares.subList(
                                tiStartIndex, tiStartIndex + l),
                        senderQueue, recQueues.get(i),
                        new LinkedList<>(protocolIdQueue),
                        clientID, prime, i, oneShare, partyCount);
            
                Future<BigInteger> DPTask = es.submit(DPModule);
                taskList.add(DPTask);
                tiStartIndex += l;

            }
        }

        es.shutdown();
        int testCases = taskList.size();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < l; j++) {
                Future<BigInteger> dWorkerResponse = taskList.get(i);
                try {
                    c[i][j] = dWorkerResponse.get();
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(LinearRegressionTraining.class.getName())
                            .log(Level.SEVERE, null, ex);
                } 
            }
        }
        
        tearDownHandlers();
        return c;

    }

}
