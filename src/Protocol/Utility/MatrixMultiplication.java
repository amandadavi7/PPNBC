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
import Protocol.Truncation;
import TrustedInitializer.TripleReal;
import TrustedInitializer.TruncationPair;
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
 * uses n*l*m TripleReal shares
 * uses n*l TruncationPair shares
 *
 * @author anisha
 */
public class MatrixMultiplication extends CompositeProtocol implements
        Callable<BigInteger[][]> {

    BigInteger[][] a, b;
    List<TripleReal> tiRealShares;
    List<TruncationPair> tiTruncationPair;
    BigInteger prime;
    int globalProtocolId;

    /**
     * Uses n-1 tiShares
     *
     * @param a
     * @param b
     * @param tiRealshares
     * @param clientID
     * @param prime
     * @param protocolID
     * @param asymmetricBit
     * @param senderQueue
     * @param receiverQueue
     * @param protocolIdQueue
     * @param partyCount
     */
    public MatrixMultiplication(BigInteger[][] a, BigInteger[][] b,
            List<TripleReal> tiRealshares, List<TruncationPair> tiTruncationPair,
            int clientID, BigInteger prime, int protocolID,
            int asymmetricBit, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue,
            Queue<Integer> protocolIdQueue, int partyCount) {

        super(protocolID, senderQueue, receiverQueue, protocolIdQueue, clientID,
                asymmetricBit, partyCount);
        this.a = a;
        this.b = b;
        this.tiRealShares = tiRealshares;
        this.tiTruncationPair = tiTruncationPair;
        this.prime = prime;
        globalProtocolId = 0;
    }

    /**
     * a: n*m, b = m*l Local matrix multiplication of a(n*t) and b(t*m) to give
     * result c(n*m)
     *
     * @return
     * @throws Exception
     */
    @Override
    public BigInteger[][] call() throws Exception {

        startHandlers();
        int n = a.length;
        int m = a[0].length;
        int l = b[0].length;
        BigInteger[][] c2f = new BigInteger[n][l];
        BigInteger[][] c = new BigInteger[n][l];

        int tiRealStartIndex = 0;
        
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
                        col, tiRealShares.subList(
                                tiRealStartIndex, tiRealStartIndex + m),
                        senderQueue, recQueues.get(i),
                        new LinkedList<>(protocolIdQueue),
                        clientID, prime, globalProtocolId++, asymmetricBit, partyCount);

                Future<BigInteger> DPTask = es.submit(DPModule);
                taskList.add(DPTask);
                tiRealStartIndex += m;

            }
        }

        int testCases = taskList.size();
        int tiTruncationStartIndex = 0;

        List<Future<BigInteger[]>> taskListTruncation = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < l; j++) {
                Future<BigInteger> dWorkerResponse = taskList.get(i);
                try {
                    //TODO do truncation here
                    c2f[i][j] = dWorkerResponse.get();
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(LinearRegressionTraining.class.getName())
                            .log(Level.SEVERE, null, ex);
                }
            }

            //TODO do not reuse tiTruncationPair
            Truncation truncationPair = new Truncation(c2f[i], 
                    tiTruncationPair.subList(tiTruncationStartIndex, 
                            tiTruncationStartIndex+c2f[i].length),
                    senderQueue, receiverQueue, new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalProtocolId++, asymmetricBit, partyCount);
            Future<BigInteger[]> truncationTask = es.submit(truncationPair);
            taskListTruncation.add(truncationTask);
            
            tiTruncationStartIndex += c2f[i].length;

        }

        for (int i = 0; i < n; i++) {
            Future<BigInteger[]> dWorkerResponse = taskListTruncation.get(i);
            try {
                //TODO do truncation here
                c[i] = dWorkerResponse.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(LinearRegressionTraining.class.getName())
                        .log(Level.SEVERE, null, ex);
            }

        }

        es.shutdown();
        tearDownHandlers();
        
        System.out.println("returning matrix multiplication:"+ c.length+", "+ c[0].length);
        return c;

    }

}
