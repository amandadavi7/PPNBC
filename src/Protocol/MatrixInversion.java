/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.MatrixMultiplication;
import TrustedInitializer.TripleReal;
import TrustedInitializer.TruncationPair;
import Utility.Constants;
import Utility.LocalMath;
import java.math.BigInteger;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Matrix inversion using Newton Raphson
 *
 * @author anisha
 */
public class MatrixInversion extends CompositeProtocol implements
        Callable<BigInteger[][]> {

    private static final Logger LOGGER = Logger.getLogger(MatrixInversion.class.getName()); 
    private static BigInteger[][] Ashares, I2;
    
    List<TripleReal> tishares;
    List<TruncationPair> tiTruncationPair;
    
    private final int matrixSize;
    private int globalPid;
    private final int nrRounds;
    private int tiRealIndex;
    private int tiTruncationIndex;
    
    private final BigInteger prime;

    public MatrixInversion(BigInteger[][] Ashares, List<TripleReal> tishares,
            List<TruncationPair> tiTruncationPair,
            int protocolId, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, int asymmetricBit, int partyCount, BigInteger prime) {

        super(protocolId, pidMapper, senderQueue, protocolIdQueue, clientId,
                asymmetricBit, partyCount);
        this.Ashares = Ashares;
        this.tishares = tishares;
        this.tiTruncationPair = tiTruncationPair;
        this.prime = prime;
        this.nrRounds = Constants.NEWTON_RAPHSON_ROUNDS;    // Number of rounds = k = f+e
        
        matrixSize = Ashares.length;
        I2 = createIdentity(2);
        globalPid = 0;
        tiRealIndex = 0;
        tiTruncationIndex = 0;
    }

    @Override
    public BigInteger[][] call() throws Exception {
        BigInteger c = calculateTrace(Ashares);
        BigInteger cInv = computeCInv(c);
        BigInteger[][] X = createIdentity(cInv);

        //System.out.println("Xs:" + X[0][0]);
        X = newtonRaphsonAlgorithm(Ashares, X, nrRounds);
        return X;
    }

    /**
     * Sum of diagonal elements of the matrix. The trace returned is over Zq.
     *
     * @param matrix
     * @return
     */
    private BigInteger calculateTrace(BigInteger[][] matrix) {
        BigInteger trace = LocalMath.realToZq(0, Constants.DECIMAL_PRECISION, prime);
        for (int i = 0; i < matrixSize; i++) {
            trace = trace.add(matrix[i][i]).mod(prime);
        }
        return trace;
    }

    /**
     * Create a matrix I.number
     * TODO: Can be moved to Utility
     * @param number
     * @return 
     */
    private BigInteger[][] createIdentity(BigInteger number) {
        BigInteger[][] I = new BigInteger[matrixSize][matrixSize];
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                if (i == j) {
                    I[i][j] = number;
                } else {
                    I[i][j] = BigInteger.ZERO;
                }

            }
        }
        return I;
    }
    
    /**
     * Create a matrix I*number over Zq
     * TODO: Can be moved to Utility
     * @param number
     * @return 
     */
    private BigInteger[][] createIdentity(int number) {
        BigInteger[][] I = new BigInteger[matrixSize][matrixSize];
        BigInteger diagonalElements = LocalMath.realToZq(number*asymmetricBit, 
                Constants.DECIMAL_PRECISION, prime);
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                if (i == j) {
                    I[i][j] = diagonalElements;
                } else {
                    I[i][j] = BigInteger.ZERO;
                }

            }
        }
        return I;
    }

    /**
     * Compute C inverse. this is done using distributed newton Raphson
     * algorithm start with a very small value (Xs). one party takes 0.01 rest
     * all take 0
     *
     * @param c
     * @return
     */
    private BigInteger computeCInv(BigInteger c) {
        BigInteger Xs = BigInteger.ZERO;
        if (asymmetricBit == 1) {
            Xs = LocalMath.realToZq(0.00000001, Constants.DECIMAL_PRECISION, prime);
        }
        return newtonRaphsonAlgorithm(c, Xs);
    }

    /**
     * Compute (2.I - A.X) each of which is over Zq
     * @param AX
     * @return 
     */
    private BigInteger[][] subtractFromTwo(BigInteger[][] AX) {
        int marixSize = AX.length;
        BigInteger[][] subtractedAX = new BigInteger[marixSize][marixSize];
        for (int i = 0; i < marixSize; i++) {
            for (int j = 0; j < marixSize; j++) {
                subtractedAX[i][j] = I2[i][j].subtract(AX[i][j]).mod(prime);
            }
        }
        return subtractedAX;
    }

    /**
     * Distributed Newton Raphson algorithm
     * @param A
     * @param X
     * @param rounds
     * @return 
     */
    private BigInteger[][] newtonRaphsonAlgorithm(BigInteger[][] A,
            BigInteger[][] X, int rounds) {
        ExecutorService es = Executors.newSingleThreadExecutor();

        int n = A.length;

        for (int i = 0; i < rounds; i++) {
            // AX = DM(A.X)
            System.out.println("Party: "+clientID+" NR round: "+i);
            MatrixMultiplication matrixMultiplication = new MatrixMultiplication(
                    A, X, tishares.subList(tiRealIndex, (int) (tiRealIndex + Math.pow(n, 3))),
                    tiTruncationPair.subList(tiTruncationIndex, tiTruncationIndex + n * n),
                    clientID, prime, globalPid, asymmetricBit, pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue),
                    partyCount);
            
            // TODO uncomment to not reuse the shares
            //tiRealIndex += Math.pow(n, 3);
            //tiTruncationIndex += (n*n);
            globalPid++;
            Future<BigInteger[][]> multiplicationTask = es.submit(matrixMultiplication);
            BigInteger[][] AX = null;
            try {
                AX = multiplicationTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            
            System.out.println("computed AX:" + AX[0][0]);

            BigInteger[][] subtractedAX = subtractFromTwo(AX);
            
            // X = DM(X.temp2)
            BigInteger[][] Xs1 = null;
            MatrixMultiplication matrixMultiplicationNext = new MatrixMultiplication(
                    X, subtractedAX, tishares.subList(tiRealIndex,
                            tiRealIndex + (int) (tiRealIndex + Math.pow(n, 3))),
                    tiTruncationPair.subList(tiTruncationIndex, tiTruncationIndex + n * n),
                    clientID, prime, globalPid, asymmetricBit, pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue),
                    partyCount);

            // TODO uncomment to not reuse the shares
            //tiRealIndex += Math.pow(n, 3);
            //tiTruncationIndex += (n*n);
            globalPid++;
            Future<BigInteger[][]> multiplicationTaskNext = es.submit(matrixMultiplicationNext);
            try {
                Xs1 = multiplicationTaskNext.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }

            X = Xs1;
            System.out.println("new X:" + X[0][0]);

        }
        es.shutdown();
        return X;
    }

    /**
     * Newton Raphson for scalar values
     * This does not use matrix multiplication. thus lesser overhead
     * @param A
     * @param X
     * @return
     */
    private BigInteger newtonRaphsonAlgorithm(BigInteger A,
            BigInteger X) {
        ExecutorService es = Executors.newSingleThreadExecutor();

        for (int i = 0; i < 20; i++) {

            // AX = DM(A.X)
            MultiplicationReal multiplicationModule = new MultiplicationReal(A,
                    X, tishares.get(tiRealIndex), pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);

            // TODO uncomment to not reuse the shares
            //tiRealIndex++;
            globalPid++;

            Future<BigInteger> multiplicationTask = es.submit(multiplicationModule);
            BigInteger AX = null;
            try {
                AX = multiplicationTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }

            Truncation truncationModule = new Truncation(AX,
                    tiTruncationPair.get(tiTruncationIndex), pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);

            // TODO uncomment to not reuse the shares
            //tiTruncationIndex++;
            globalPid++;
            Future<BigInteger> truncationTask = es.submit(truncationModule);

            BigInteger truncatedAX = null;
            try {
                truncatedAX = truncationTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }

            // subtracted AX = (2-AX). this computation is local
            BigInteger subtractedAX = LocalMath.realToZq(2 * asymmetricBit,
                    Constants.DECIMAL_PRECISION, prime)
                    .subtract(truncatedAX).mod(prime);

            // X = DM(X.subtractedAX)
            MultiplicationReal multiplicationModuleNext = new MultiplicationReal(
                    X, subtractedAX, tishares.get(tiRealIndex),
                    pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);

            // TODO uncomment to not reuse the shares
            //tiRealIndex++;
            //tiTruncationIndex++;
            globalPid++;
            multiplicationTask = es.submit(multiplicationModuleNext);

            BigInteger XsNext = null;
            try {
                XsNext = multiplicationTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }

            Truncation truncationModuleNext = new Truncation(XsNext,
                    tiTruncationPair.get(tiTruncationIndex),
                    pidMapper, senderQueue, 
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);
            Future<BigInteger> truncationTaskNext = es.submit(truncationModuleNext);

            BigInteger truncatedX = null;
            try {
                truncatedX = truncationTaskNext.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }

            // TODO uncomment to not reuse the shares
            //tiTruncationIndex++;
            globalPid++;
            X = truncatedX;

        }
        es.shutdown();

        return X;
    }

}
