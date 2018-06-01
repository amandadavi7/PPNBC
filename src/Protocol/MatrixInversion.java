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
 *
 * @author anisha
 */
public class MatrixInversion extends CompositeProtocol implements 
        Callable<BigInteger[][]> {

    private static BigInteger[][] Ashares, I;
    private static int n;
    private static int globalPid;
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
        //int rounds = Constants.decimal_precision + Constants.integer_precision;
        int rounds = 5;
        X=newtonRaphsonAlgorithm(Ashares, X, rounds);
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

    private BigInteger[][] computeCInv(BigInteger c) {
        // compute cnv using newton raphson method
        // start with a very small value
        BigInteger[][] cInvMatrix = new BigInteger[1][1];
        BigInteger[][] cMatrix = new BigInteger[1][1];
        cMatrix[0][0] = c;
        cInvMatrix[0][0] = BigDecimal.valueOf(0.01).toBigInteger();
        return newtonRaphsonAlgorithm(cMatrix, cInvMatrix, 5);
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
        int marixSize = AX.length;
        BigInteger[][] subtractedAX = new BigInteger[marixSize][marixSize];
        for (int i = 0; i < marixSize; i++) {
            for (int j = 0; j < marixSize; j++) {
                subtractedAX[i][j] = BigInteger.valueOf(2 * asymmetricBit).subtract(AX[i][j]).mod(prime);
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
            /*System.out.println("Newton Raphson: round "+ i +" on A:"+ A.length+
                    ","+A[0].length+" and X:"+ X.length+", "+X[0].length);*/
            int tiRealIndex = 0;
            int tiTruncationIndex = 0;
            initQueueMap(recQueues, globalPid);

            MatrixMultiplication matrixMultiplication = new MatrixMultiplication(
                    A, X, tishares.subList(tiRealIndex, (int) (tiRealIndex + Math.pow(n, 3))),
                    tiTruncationPair.subList(tiTruncationIndex, tiTruncationIndex + n*n),
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

            // TODO: temp2 = 2-temp local? asymmetricBit? 2 over zq?
            BigInteger[][] subtractedAX = subtractFromTwo(AX);

            // X = DM(X.temp2)
            matrixMultiplication = new MatrixMultiplication(
                    X, subtractedAX, tishares.subList(tiRealIndex, 
                            tiRealIndex + (int) (tiRealIndex + Math.pow(n, 3))),
                    tiTruncationPair.subList(tiTruncationIndex, tiTruncationIndex + n*n),
                    clientID, prime, globalPid, asymmetricBit, senderQueue,
                    recQueues.get(globalPid), new LinkedList<>(protocolIdQueue),
                    partyCount);

            // TODO uncomment to not reuse the shares
            //tiRealIndex += Math.pow(n, 3);
            //tiTruncationIndex += (n*n);
            globalPid++;
            multiplicationTask = es.submit(matrixMultiplication);
            try {
                X = multiplicationTask.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(MatrixInversion.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        es.shutdown();
        
        //System.out.println("returning matrix inversion");

        return X;
    }

}
