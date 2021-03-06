/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.CompositeProtocol;
import Protocol.DotProductReal;
import TrustedInitializer.TripleReal;
import TrustedInitializer.TruncationPair;
import Utility.Constants;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Class to take care of matrix multiplication
 *
 * uses n*l*m TripleReal shares uses n*l TruncationPair shares
 *
 * @author anisha
 */
public class MatrixMultiplication extends CompositeProtocol implements
        Callable<BigInteger[][]> {

    BigInteger[][] a;
    List<List<BigInteger>> bT;
    List<TripleReal> tiRealShares;
    List<TruncationPair> tiTruncationPair;
    BigInteger prime;
    int globalProtocolId;
    int n, m, l;

    /**
     * Constructor
     * Uses n-1 tiShares
     *
     * @param a
     * @param b
     * @param tiRealshares
     * @param tiTruncationPair
     * @param clientID
     * @param prime
     * @param protocolID
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param protocolIdQueue
     * @param partyCount
     */
    public MatrixMultiplication(BigInteger[][] a, BigInteger[][] b,
            List<TripleReal> tiRealshares, List<TruncationPair> tiTruncationPair,
            int clientID, BigInteger prime, int protocolID,
            int asymmetricBit,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue, int partyCount) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientID,
                asymmetricBit, partyCount);
        this.a = a;
        this.tiRealShares = tiRealshares;
        this.tiTruncationPair = tiTruncationPair;
        this.prime = prime;

        globalProtocolId = 0;
        bT = new ArrayList<>();
        n = a.length;
        m = a[0].length;
        l = b[0].length;
        for (int i = 0; i < l; i++) {
            List<BigInteger> col = new ArrayList<>(m);
            for (int j = 0; j < m; j++) {
                col.add(b[j][i]);
            }
            bT.add(col);
        }

    }

    /**
     * a: n*m, b = m*l Local matrix multiplication of a(n*t) and b(t*m) to give
     * result c(n*m)
     *
     * @return
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Override
    public BigInteger[][] call() throws InterruptedException, ExecutionException {

        BigInteger[][] c2f = new BigInteger[n][l];
        BigInteger[][] c = new BigInteger[n][l];

        int tiRealStartIndex = 0;

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<BigInteger>> taskList = new ArrayList<>(n * l);

        for (int i = 0; i < n; i++) {
            List<BigInteger> row = Arrays.asList(a[i]);
            for (int j = 0; j < l; j++) {
                DotProductReal DPModule = new DotProductReal(row,
                        bT.get(j), tiRealShares.subList(
                        tiRealStartIndex, tiRealStartIndex + m),
                        pidMapper, senderQueue,
                        new LinkedList<>(protocolIdQueue),
                        clientID, prime, globalProtocolId++, asymmetricBit, partyCount);

                Future<BigInteger> DPTask = es.submit(DPModule);
                taskList.add(DPTask);
                //TODO: uncomment this to avoid reusing the shares 
                //tiRealStartIndex += m;
            }
        }

        int testCases = 0;
        int tiTruncationStartIndex = 0;

        List<Future<BigInteger[]>> taskListTruncation = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < l; j++) {
                Future<BigInteger> dWorkerResponse = taskList.get(testCases++);
                c2f[i][j] = dWorkerResponse.get();
            }
            
            BatchTruncation truncationModule = new BatchTruncation(c2f[i],
                    tiTruncationPair.subList(tiTruncationStartIndex,
                            tiTruncationStartIndex + c2f[i].length),
                    pidMapper, senderQueue, 
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalProtocolId++, asymmetricBit, partyCount);
            
            Future<BigInteger[]> truncationTask = es.submit(truncationModule);
            taskListTruncation.add(truncationTask);
                //TODO: uncomment this to avoid reusing the shares 
            //tiTruncationStartIndex += c2f[i].length;
        }
        // release the memory once done
        taskList.clear();
        
        for (int i = 0; i < n; i++) {
            Future<BigInteger[]> dWorkerResponse = taskListTruncation.get(i);
            c[i] = dWorkerResponse.get();
        }

        es.shutdown();
        return c;
    }
}
