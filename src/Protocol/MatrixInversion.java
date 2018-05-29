/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.MatrixMultiplication;
import TrustedInitializer.TripleReal;
import Utility.Constants;
import java.math.BigInteger;
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
 *
 * @author anisha
 */
public class MatrixInversion extends CompositeProtocol implements Callable<BigInteger[][]> {

    private static BigInteger[][] Ashares, I;
    private static int n;
    private static int globalPid;
    List<TripleReal> tishares;
    private BigInteger prime;

    public MatrixInversion(BigInteger[][] Ashares, List<TripleReal> tishares,
            int protocolId,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientId, int oneShare, int partyCount, BigInteger prime) {

        super(protocolId, senderQueue, receiverQueue, protocolIdQueue, clientId,
                oneShare, partyCount);
        this.Ashares = Ashares;
        this.tishares = tishares;
        this.prime = prime;
        n = Ashares.length;
        I = createIdentity();
        globalPid = 0;
    }

    @Override
    public BigInteger[][] call() throws Exception {
        BigInteger c = calculateTrace(Ashares);

        // compute cinv using Newton Raphson
        BigInteger[][] cInv = computeCInv(c);

        BigInteger[][] X = computeX0(cInv);
        
        // Number of rounds = k = f+e
        int rounds = Constants.decimal_precision + Constants.integer_precision;
        X=newtonRaphsonAlgorithm(X, rounds);
        return X;

    }

    private BigInteger calculateTrace(BigInteger[][] matrix) {
        BigInteger trace = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            trace = trace.add(matrix[i][i]);
        }

        return trace;
    }

    private BigInteger[][] createIdentity() {
        BigInteger[][] I = new BigInteger[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    I[i][j] = BigInteger.ONE;
                } else {
                    I[i][j] = BigInteger.ZERO;
                }

            }
        }
        return I;
    }

    private BigInteger[][] computeCInv(BigInteger c) {
        // compute cnv using newton raphson method
        BigInteger[][] cInvMatrix = new BigInteger[1][1];
        cInvMatrix[0][0] = c;
        return newtonRaphsonAlgorithm(cInvMatrix, 1);
    }

    private BigInteger[][] computeX0(BigInteger[][] cInv) {
        BigInteger[][] X = new BigInteger[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                X[i][j] = cInv[0][0].multiply(I[i][j]);
            }
        }
        return X;
    }

    private BigInteger[][] subtractFromTwo(BigInteger[][] AX) {
        //TODO? Element waise substraction?
        BigInteger[][] subtractedAX = new BigInteger[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                subtractedAX[i][j] = BigInteger.valueOf(2 * oneShare).subtract(AX[i][j]).mod(prime);
            }
        }
        return subtractedAX;
    }

    private BigInteger[][] newtonRaphsonAlgorithm(BigInteger[][] X, int rounds) {
        ExecutorService es = Executors.newFixedThreadPool(2);
        if(X.length == 1 && X[0].length == 1) {
            // scalar matrix. Assign a small value and return.
            
        }
        for (int i = 0; i < rounds; i++) {
            // temp = DM(A.X)
            int tiIndex = 0;
            initQueueMap(recQueues, globalPid);

            MatrixMultiplication matrixMultiplication = new MatrixMultiplication(
                    Ashares, X, tishares.subList(tiIndex, tiIndex + n),
                    clientID, prime, globalPid, oneShare, senderQueue,
                    recQueues.get(globalPid), new LinkedList<>(protocolIdQueue),
                    partyCount);

            tiIndex += n;
            globalPid++;
            Future<BigInteger[][]> multiplicationTask = es.submit(matrixMultiplication);
            BigInteger[][] AX = null;
            try {
                AX = multiplicationTask.get();

            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            }

            // TODO: temp2 = 2-temp local? oneshare? 2 over zq?
            BigInteger[][] subtractedAX = subtractFromTwo(AX);

            // X = DM(X.temp2)
            matrixMultiplication = new MatrixMultiplication(
                    X, subtractedAX, tishares.subList(tiIndex, tiIndex + n),
                    clientID, prime, globalPid, oneShare, senderQueue,
                    recQueues.get(globalPid), new LinkedList<>(protocolIdQueue),
                    partyCount);

            tiIndex += n;
            globalPid++;
            multiplicationTask = es.submit(matrixMultiplication);
            try {
                X = multiplicationTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        es.shutdown();

        return X;
    }

}
