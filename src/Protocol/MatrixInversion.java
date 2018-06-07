/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.BatchTruncation;
import Protocol.Utility.MatrixMultiplication;
import TrustedInitializer.TripleReal;
import TrustedInitializer.TruncationPair;
import Utility.Constants;
import Utility.FileIO;
import java.math.BigDecimal;
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

    private static BigInteger[][] Ashares, I;
    private static int n;
    private static int globalPid;
    private static int nrRounds;
    List<TripleReal> tishares;
    List<TruncationPair> tiTruncationPair;
    private BigInteger prime;

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
        this.nrRounds = 10;
        //this.nrRounds = Constants.decimal_precision/2;
        n = Ashares.length;
        I = createIdentity();
        globalPid = 0;
    }

    @Override
    public BigInteger[][] call() throws Exception {
        startHandlers();
        BigInteger c = calculateTrace(Ashares);

        // compute cinv using Newton Raphson
        BigInteger cInv = computeCInv(c);

        System.out.println("cinv:" + cInv);

        BigInteger[][] X = computeX0(cInv);

        // Number of rounds = k = f+e
        X = newtonRaphsonAlgorithm(Ashares, X, nrRounds);
        tearDownHandlers();
        return X;

    }

    private BigInteger calculateTrace(BigInteger[][] matrix) {
        BigInteger trace = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            trace = trace.add(matrix[i][i]).mod(prime);
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

    private BigInteger computeCInv(BigInteger c) {
        // compute cnv using newton raphson method
        // start with a very small value
        c = FileIO.realToZq(5, Constants.decimal_precision, prime);
        System.out.println("C:" + c);
        // one party takes 0.01 rest all take 0
        BigInteger Xs = BigInteger.ZERO;
        if (asymmetricBit == 1) {
            Xs = FileIO.realToZq(0.01, Constants.decimal_precision, prime);
        }
        return newtonRaphsonAlgorithmScalar(c, Xs, nrRounds);
    }

    private BigInteger[][] computeX0(BigInteger cInv) {
        BigInteger[][] X = new BigInteger[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                X[i][j] = cInv.multiply(I[i][j]);
            }
        }
        return X;
    }

    private BigInteger[][] subtractFromTwo(BigInteger[][] AX) {
        int marixSize = AX.length;
        BigInteger[][] subtractedAX = new BigInteger[marixSize][marixSize];
        for (int i = 0; i < marixSize; i++) {
            for (int j = 0; j < marixSize; j++) {
                subtractedAX[i][j] = BigInteger.valueOf(2 * asymmetricBit).subtract(AX[i][j]).mod(prime);
            }
        }
        return subtractedAX;
    }

    private BigInteger subtractFromTwoScalar(BigInteger AX) {
        BigInteger subtractedAX = BigInteger.valueOf(2 * asymmetricBit).
                subtract(AX).mod(prime);
        return subtractedAX;
    }

    private BigInteger[][] newtonRaphsonAlgorithm(BigInteger[][] A,
            BigInteger[][] X, int rounds) {
        ExecutorService es = Executors.newSingleThreadExecutor();

        int n = A.length;

        for (int i = 0; i < rounds; i++) {
            // AX = DM(A.X)
            System.out.println("Newton Raphson: round " + i + " on A:" + A.length
                    + "," + A[0].length + " and X:" + X.length + ", " + X[0].length + ", globalPid:" + globalPid);
            int tiRealIndex = 0;
            int tiTruncationIndex = 0;
            initQueueMap(recQueues, globalPid);

            FileIO.writeToCSV(X, "/home/anisha/PPML Tests/output/", "X", clientID, prime);

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

            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MatrixInversion.class.getName()).log(Level.SEVERE, null, ex);
            }

            FileIO.writeToCSV(AX, "/home/anisha/PPML Tests/output/", "matrixMultiplication", clientID, prime);

            BigInteger[][] subtractedAX = subtractFromTwo(AX);

            FileIO.writeToCSV(subtractedAX, "/home/anisha/PPML Tests/output/", "subtractedAX", clientID, prime);

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

            FileIO.writeToCSV(Xs1, "/home/anisha/PPML Tests/output/", "matrixMultiplication2", clientID, prime);
            X = Xs1;

        }
        es.shutdown();

        //System.out.println("returning matrix inversion");
        return X;
    }

    private BigInteger newtonRaphsonAlgorithmScalar(BigInteger A,
            BigInteger X, int rounds) {
        ExecutorService es = Executors.newSingleThreadExecutor();

        for (int i = 0; i < rounds; i++) {
            // AX = DM(A.X)
            int tiRealIndex = 0;
            int tiTruncationIndex = 0;
            initQueueMap(recQueues, globalPid);

            MultiplicationReal multiplicationModule = new MultiplicationReal(A,
                    X,
                    tishares.get(tiRealIndex),
                    senderQueue,
                    recQueues.get(globalPid), new LinkedList<>(protocolIdQueue),
                    clientID,
                    prime, globalPid, asymmetricBit, partyCount);

            // TODO uncomment to not reuse the shares
            //tiRealIndex += Math.pow(n, 3);
            //tiTruncationIndex += (n*n);
            globalPid++;
            Future<BigInteger> multiplicationTask = es.submit(multiplicationModule);
            BigInteger AX = null;
            try {
                AX = multiplicationTask.get();
                System.out.println("AX:" + AX + ", for A:" + A + ",X:" + X);

            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MatrixInversion.class.getName()).log(Level.SEVERE, null, ex);
            }

            initQueueMap(recQueues, globalPid);
            
            BigInteger[] axMatrix = new BigInteger[1];
            axMatrix[0] = AX;

//            BatchTruncation truncationModule = new BatchTruncation(axMatrix,
//                    tiTruncationPair.subList(0,
//                            1),
//                    senderQueue, recQueues.get(globalPid),
//                    new LinkedList<>(protocolIdQueue),
//                    clientID, prime, globalPid++, asymmetricBit, partyCount);
//            Future<BigInteger[]> truncationTask = es.submit(truncationModule);
//            
//            BigInteger[] c = null;
//            try {
//                c = truncationTask.get();
//            } catch (InterruptedException | ExecutionException ex) {
//                Logger.getLogger(MatrixInversion.class.getName())
//                        .log(Level.SEVERE, null, ex);
//            }
//            
//            BigInteger truncatedAX = c[0];
//            System.out.println("truncatedAX:"+ truncatedAX);
            

            Truncation truncationModule = new Truncation(AX,
                    tiTruncationPair.get(tiTruncationIndex),
                    senderQueue, recQueues.get(globalPid),
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);
            Future<BigInteger> truncationTask = es.submit(truncationModule);

            BigInteger truncatedAX = null;
            try {
                truncatedAX = truncationTask.get();
                System.out.println("truncatedAX:"+ truncatedAX);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MultiplicationReal.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
            globalPid++;

            BigInteger subtractedAX = subtractFromTwoScalar(truncatedAX);
            System.out.println("subAX:" + subtractedAX);

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
            try {
                X = multiplicationTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MatrixInversion.class.getName()).log(Level.SEVERE, null, ex);
            }

            initQueueMap(recQueues, globalPid);

            Truncation truncationModuleNext = new Truncation(X,
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

            System.out.println("result:" + truncatedX);
        }
        es.shutdown();

        //System.out.println("returning matrix inversion");
        return X;
    }

}
