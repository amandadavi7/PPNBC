/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Utility.Constants;
import com.sun.corba.se.impl.orbutil.closure.Constant;
import java.math.BigInteger;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

/**
 *
 * @author anisha
 */
public class MatricInversion extends CompositeProtocol implements Callable<Integer> {

    private static BigInteger[][] Ashares, I;
    private static int n;

    public MatricInversion(BigInteger[][] Ashares, int protocolId,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientId, int oneShare, int partyCount) {

        super(protocolId, senderQueue, receiverQueue, protocolIdQueue, clientId,
                oneShare, partyCount);
        this.Ashares = Ashares;
        n = Ashares.length;
        I = createIdentity();
    }

    @Override
    public Integer call() throws Exception {
        BigInteger c = calculateTrace(Ashares);
        BigInteger[][] X = new BigInteger[n][n];
        // compute cinv using Newton Raphson
        BigInteger cInv = computeCInv();
        
        //TODO do this for the matrix
        //X = cInv.multiply(I);
        // Number of rounds = k = f+e
        int rounds = Constants.decimal_precision + Constants.integer_precision;
        for(int i=0;i<rounds;i++) {
            // temp = DM(A.X)
            // temp2 = 2-temp local? oneshare?
            // X = DM(X.temp2)
        }
        
        return -1;

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

    private BigInteger computeCInv() {
        // create a queue and all matrix inversion again
        return null;
    }

}
