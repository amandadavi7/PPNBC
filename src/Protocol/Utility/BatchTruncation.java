/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.CompositeProtocol;
import TrustedInitializer.TruncationPair;
import Utility.Constants;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Batch Truncation of n elements: converts shares of value from 2^2f to 2^f
 *
 * uses wShares.length TruncationPair shares
 *
 * @author anisha
 */
public class BatchTruncation extends CompositeProtocol implements Callable<BigInteger[]> {

    BigInteger[] wShares;
    BigInteger[] T;
    
    List<BigInteger> zShares;
    List<TruncationPair> truncationShares;

    BigInteger prime;
    int batchSize;
    
    static BigInteger roundOffBit;
    static BigInteger fInv;
    static BigInteger fpow2;

    /**
     * Constructor
     * @param wShares
     * @param tiShares
     * @param pidMapper
     * @param senderqueue
     * @param protocolIdQueue
     * @param clientID
     * @param prime
     * @param protocolID
     * @param asymmetricBit
     * @param partyCount 
     */
    public BatchTruncation(BigInteger[] wShares,
            List<TruncationPair> tiShares, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderqueue,
            Queue<Integer> protocolIdQueue,
            int clientID, BigInteger prime,
            int protocolID, int asymmetricBit, int partyCount) {

        super(protocolID, pidMapper, senderqueue, protocolIdQueue, clientID,
                asymmetricBit, partyCount);

        this.wShares = wShares;
        this.prime = prime;
        this.truncationShares = tiShares;

        batchSize = wShares.length;
        zShares = new ArrayList<>(batchSize);
        T = new BigInteger[batchSize];
        
        roundOffBit = BigInteger.valueOf(2).pow(Constants.INTEGER_PRECISION
                + 2 * Constants.DECIMAL_PRECISION - 1);
        
        fInv = prime.add(BigInteger.ONE).divide(BigInteger.valueOf(2)).
                pow(Constants.DECIMAL_PRECISION).mod(prime);
        
        fpow2 = BigInteger.valueOf(2).pow(Constants.DECIMAL_PRECISION);
    }

    @Override
    public BigInteger[] call() throws Exception {
        computeAndShareZShare();
        computeSecretShare();
        
        return T;
    }

    /**
     * Compute Z = [[w]] + [[r]] mod prime 
     */
    private void computeAndShareZShare() {

        // broadcast it to n parties
        List<BigInteger> diffList = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            zShares.add(i, wShares[i].add(truncationShares.get(i).r).mod(prime));
            diffList.add(zShares.get(i));
        }

        Message senderMessage = new Message(diffList,
                clientID, protocolIdQueue);

        try {
            senderQueue.put(senderMessage);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Logger.getLogger(BatchTruncation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Compute the shares of the truncated value
     */
    private void computeSecretShare() {
        Message receivedMessage = null;
        List<BigInteger> diffList = null;
        // Receive all zShares
        for (int i = 0; i < partyCount - 1; i++) {
            try {
                receivedMessage = pidMapper.get(protocolIdQueue).take();
                diffList = (List<BigInteger>) receivedMessage.getValue();
                for (int j = 0; j < batchSize; j++) {
                    zShares.set(j, zShares.get(j).add(diffList.get(j)).mod(prime));
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        for (int i = 0; i < batchSize; i++) {
            BigInteger c = zShares.get(i).add(roundOffBit);
            BigInteger cp = c.mod(fpow2);
            BigInteger S = wShares[i].add(truncationShares.get(i).rp).mod(prime).
                    subtract(cp.multiply(BigInteger.valueOf(asymmetricBit)));
            T[i] = S.multiply(fInv).mod(prime);
        }

    }

}
