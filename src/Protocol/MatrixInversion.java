/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.MatrixMultiplication;
import TrustedInitializer.TripleBigInteger;
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

/**
 * Matrix inversion using Newton Raphson
 *
 * @author anisha
 */
public class MatrixInversion extends CompositeProtocol implements
        Callable<BigInteger[][]> {

    private static BigInteger[][] Ashares, I2;
    
    List<TripleBigInteger> tishares;
    List<TruncationPair> tiTruncationPair;
    
    private final int matrixSize;
    private int globalPid;
    private final int nrRounds;
    private int tiRealIndex;
    private int tiTruncationIndex;
    
    private final BigInteger prime;

    public MatrixInversion(BigInteger[][] Ashares, List<TripleBigInteger> tishares,
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
    public BigInteger[][] call() throws InterruptedException, ExecutionException {
        BigInteger c = calculateTrace(Ashares);
        BigInteger cInv = computeCInv(c);
        BigInteger[][] X = createIdentity(cInv);

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
    private BigInteger computeCInv(BigInteger c) throws InterruptedException {
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
            BigInteger[][] X, int rounds) throws InterruptedException, ExecutionException {
        
        int n = A.length;

        for (int i = 0; i < rounds; i++) {
            // AX = DM(A.X)
            System.out.println("Party: "+clientID+" NR round: "+i);
            //LOGGER.log(Level.INFO, "Party: {0} NR round: {1}", new Object[]{clientID, i});
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
            
            BigInteger[][] AX = matrixMultiplication.call();
            
            BigInteger[][] subtractedAX = subtractFromTwo(AX);
            
            // X = DM(X.temp2)
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
            X = matrixMultiplicationNext.call();

        }
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
            BigInteger X) throws InterruptedException {
        for (int i = 0; i < nrRounds; i++) {

            // AX = DM(A.X)
            MultiplicationBigInteger multiplicationModule = new MultiplicationBigInteger(A,
                    X, tishares.get(tiRealIndex), pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);

            // TODO uncomment to not reuse the shares
            //tiRealIndex++;
            globalPid++;

            BigInteger AX = multiplicationModule.call();

            Truncation truncationModule = new Truncation(AX,
                    tiTruncationPair.get(tiTruncationIndex), pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);

            // TODO uncomment to not reuse the shares
            //tiTruncationIndex++;
            globalPid++;
            
            BigInteger truncatedAX = truncationModule.call();

            // subtracted AX = (2-AX). this computation is local
            BigInteger subtractedAX = LocalMath.realToZq(2 * asymmetricBit,
                    Constants.DECIMAL_PRECISION, prime)
                    .subtract(truncatedAX).mod(prime);

            // X = DM(X.subtractedAX)
            MultiplicationBigInteger multiplicationModuleNext = new MultiplicationBigInteger(
                    X, subtractedAX, tishares.get(tiRealIndex),
                    pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);

            // TODO uncomment to not reuse the shares
            //tiRealIndex++;
            //tiTruncationIndex++;
            globalPid++;
            
            BigInteger XsNext = multiplicationModuleNext.call();

            Truncation truncationModuleNext = new Truncation(XsNext,
                    tiTruncationPair.get(tiTruncationIndex),
                    pidMapper, senderQueue, 
                    new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, partyCount);
            
            BigInteger truncatedX = null;
            truncatedX = truncationModuleNext.call();

            // TODO uncomment to not reuse the shares
            //tiTruncationIndex++;
            globalPid++;
            X = truncatedX;
        }   
        return X;
    }
}
