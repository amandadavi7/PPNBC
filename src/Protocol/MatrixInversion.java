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
import Utility.FileIO;
import Utility.Logging;
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
 * Matrix inversion using Newton Raphson
 *
 * @author anisha
 */
public class MatrixInversion extends CompositeProtocol implements
        Callable<BigInteger[][]> {

    private static BigInteger[][] Ashares, I2;
    private static int n;
    private static int globalPid;
    private static int nrRounds;
    List<TripleReal> tishares;
    List<TruncationPair> tiTruncationPair;
    private BigInteger prime;
    private int tiRealIndex;
    private int tiTruncationIndex;

    public MatrixInversion(BigInteger[][] Ashares, List<TripleReal> tishares,
            List<TruncationPair> tiTruncationPair,
            int protocolId,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientId, int asymmetricBit, int partyCount, BigInteger prime) {

        super(protocolId, senderQueue, receiverQueue, protocolIdQueue, clientId,
                asymmetricBit, partyCount);
        this.Ashares = Ashares;
        this.tishares = tishares;
        this.tiTruncationPair = tiTruncationPair;
        this.prime = prime;
        this.nrRounds = 20;
        //this.nrRounds = Constants.decimal_precision/2;
        n = Ashares.length;
        I2 = createIdentity(2);
        globalPid = 0;
        tiRealIndex = 0;
        tiTruncationIndex = 0;
        Logging.logMatrix("Our matrix:", Ashares);
    }

    @Override
    public BigInteger[][] call() throws Exception {
        startHandlers();
        BigInteger c = calculateTrace(Ashares);
        System.out.println("Trace:" + c);

        // compute cinv using Newton Raphson
        BigInteger cInv = computeCInv(c);

        System.out.println("cinv:" + cInv);

        BigInteger[][] X = createIdentity(cInv);

        // Number of rounds = k = f+e
        X = newtonRaphsonAlgorithm(Ashares, X, nrRounds);
        System.out.println("Inverse of the matrix:");
        //Logging.logMatrix("Inverse", X);
        tearDownHandlers();
        return X;

    }

    /**
     * Sum of diagonal elements of the matrix. The trace returned is over Zq.
     *
     * @param matrix
     * @return
     */
    private BigInteger calculateTrace(BigInteger[][] matrix) {
        BigInteger trace = FileIO.realToZq(0, Constants.decimal_precision, prime);
        for (int i = 0; i < n; i++) {
            trace = trace.add(matrix[i][i]).mod(prime);
        }
        return trace;
    }

    private BigInteger[][] createIdentity(BigInteger number) {
        BigInteger[][] I = new BigInteger[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    I[i][j] = number;
                } else {
                    I[i][j] = BigInteger.ZERO;
                }

            }
        }
        return I;
    }
    
    private BigInteger[][] createIdentity(int number) {
        BigInteger[][] I = new BigInteger[n][n];
        BigInteger diagonalElements = FileIO.realToZq(number*asymmetricBit, 
                Constants.decimal_precision, prime);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
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
            Xs = FileIO.realToZq(0.00000001, Constants.decimal_precision, prime);
        }
        return newtonRaphsonAlgorithmScalar(c, Xs);
    }

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

    private BigInteger[][] newtonRaphsonAlgorithm(BigInteger[][] A,
            BigInteger[][] X, int rounds) {
        ExecutorService es = Executors.newSingleThreadExecutor();

        int n = A.length;

        for (int i = 0; i < rounds; i++) {
            // AX = DM(A.X)
            System.out.println("Newton Raphson: round " + i + ", globalPid:" + globalPid);
            initQueueMap(recQueues, globalPid);

            //FileIO.writeToCSV(X, "/home/anisha/PPML Tests/output/", "X", clientID, prime);
//            Logging.logMatrix("X", X);

            MatrixMultiplication matrixMultiplication = new MatrixMultiplication(
                    A, X, tishares.subList(tiRealIndex, (int) (tiRealIndex + Math.pow(n, 3))),
                    tiTruncationPair.subList(tiTruncationIndex, tiTruncationIndex + n * n),
                    clientID, prime, globalPid, asymmetricBit, senderQueue,
                    recQueues.get(globalPid), new LinkedList<>(protocolIdQueue),
                    partyCount);

            // TODO uncomment to not reuse the shares
            //tiRealIndex += Math.pow(n, 3);
            //tiTruncationIndex += (n*n);
            globalPid++;
            Future<BigInteger[][]> multiplicationTask = es.submit(matrixMultiplication);
            BigInteger[][] AX = null;
            try {
                AX = multiplicationTask.get();
//                System.out.println("AX computed:"+ globalPid);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MatrixInversion.class.getName()).log(Level.SEVERE, null, ex);
            }

            //FileIO.writeToCSV(AX, "/home/anisha/PPML Tests/output/", "matrixMultiplication", clientID, prime);
//            Logging.logMatrix("AX", AX);

            BigInteger[][] subtractedAX = subtractFromTwo(AX);
//            System.out.println("AX subtracted:"+ globalPid);

            //FileIO.writeToCSV(subtractedAX, "/home/anisha/PPML Tests/output/", "subtractedAX", clientID, prime);
//            Logging.logMatrix("subtractedAX", subtractedAX);

            // X = DM(X.temp2)
            initQueueMap(recQueues, globalPid);
            BigInteger[][] Xs1 = null;
            MatrixMultiplication matrixMultiplicationNext = new MatrixMultiplication(
                    X, subtractedAX, tishares.subList(tiRealIndex,
                            tiRealIndex + (int) (tiRealIndex + Math.pow(n, 3))),
                    tiTruncationPair.subList(tiTruncationIndex, tiTruncationIndex + n * n),
                    clientID, prime, globalPid, asymmetricBit, senderQueue,
                    recQueues.get(globalPid), new LinkedList<>(protocolIdQueue),
                    partyCount);

            // TODO uncomment to not reuse the shares
            //tiRealIndex += Math.pow(n, 3);
            //tiTruncationIndex += (n*n);
            globalPid++;
            Future<BigInteger[][]> multiplicationTaskNext = es.submit(matrixMultiplicationNext);
            try {
                Xs1 = multiplicationTaskNext.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MatrixInversion.class.getName()).log(Level.SEVERE, null, ex);
            }

//            System.out.println("Xs+1 computed:"+ globalPid);
            //FileIO.writeToCSV(Xs1, "/home/anisha/PPML Tests/output/", "XS+1", clientID, prime);
//            Logging.logMatrix("Xs+1", Xs1);
            X = Xs1;
//            Logging.logMatrix("New X:", X);
//            System.out.println("Xs: " + FileIO.ZqToReal(X[0][0], 
//                    Constants.decimal_precision, prime));


        }
        es.shutdown();

        FileIO.writeToCSV(X, "/home/anisha/PPML Tests/output/", "matrixInversion", clientID, prime);
        //System.out.println("returning matrix inversion");
        return X;
    }

    /**
     * Newton Raphson for scalar values
     *
     * @param A
     * @param X
     * @return
     */
    private BigInteger newtonRaphsonAlgorithmScalar(BigInteger A,
            BigInteger X) {
        ExecutorService es = Executors.newSingleThreadExecutor();

        //TODO revert nrRounds
        for (int i = 0; i < nrRounds; i++) {

            // AX = DM(A.X)
            initQueueMap(recQueues, globalPid);
            MultiplicationReal multiplicationModule = new MultiplicationReal(A,
                    X, tishares.get(tiRealIndex), senderQueue,
                    recQueues.get(globalPid), new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);

            // TODO uncomment to not reuse the shares
            //tiRealIndex++;
            globalPid++;

            Future<BigInteger> multiplicationTask = es.submit(multiplicationModule);
            BigInteger AX = null;
            try {
                AX = multiplicationTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MatrixInversion.class.getName()).log(Level.SEVERE, null, ex);
            }

            initQueueMap(recQueues, globalPid);
            Truncation truncationModule = new Truncation(AX,
                    tiTruncationPair.get(tiTruncationIndex), senderQueue,
                    recQueues.get(globalPid), new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);

            // TODO uncomment to not reuse the shares
            //tiTruncationIndex++;
            globalPid++;
            Future<BigInteger> truncationTask = es.submit(truncationModule);

            BigInteger truncatedAX = null;
            try {
                truncatedAX = truncationTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MultiplicationReal.class.getName())
                        .log(Level.SEVERE, null, ex);
            }

            // subtracted AX = (2-AX). this computation is local
            BigInteger subtractedAX = FileIO.realToZq(2 * asymmetricBit,
                    Constants.decimal_precision, prime)
                    .subtract(truncatedAX).mod(prime);

            // X = DM(X.temp2)
            initQueueMap(recQueues, globalPid);
            MultiplicationReal multiplicationModuleNext = new MultiplicationReal(
                    X, subtractedAX, tishares.get(tiRealIndex),
                    senderQueue, recQueues.get(globalPid),
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);

            // TODO uncomment to not reuse the shares
            //tiRealIndex += Math.pow(n, 3);
            //tiTruncationIndex += (n*n);
            globalPid++;
            multiplicationTask = es.submit(multiplicationModuleNext);

            BigInteger XsNext = null;
            try {
                XsNext = multiplicationTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MatrixInversion.class.getName()).log(Level.SEVERE, null, ex);
            }

            initQueueMap(recQueues, globalPid);

            Truncation truncationModuleNext = new Truncation(XsNext,
                    tiTruncationPair.get(tiTruncationIndex),
                    senderQueue, recQueues.get(globalPid),
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);
            Future<BigInteger> truncationTaskNext = es.submit(truncationModuleNext);

            BigInteger truncatedX = null;
            try {
                truncatedX = truncationTaskNext.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MultiplicationReal.class.getName())
                        .log(Level.SEVERE, null, ex);
            }

            globalPid++;
            X = truncatedX;

        }
        es.shutdown();

        //System.out.println("returning matrix inversion");
        return X;
    }

}
