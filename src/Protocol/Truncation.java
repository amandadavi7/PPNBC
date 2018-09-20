/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.TruncationPair;
import Utility.Constants;
import java.math.BigInteger;
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
public class Truncation extends Protocol implements Callable<BigInteger> {

    BigInteger wShares;
    BigInteger zShares;
    BigInteger T;
    TruncationPair truncationShares;

    BigInteger prime;

    public Truncation(BigInteger wShares,
            TruncationPair tiShares, 
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

    }

    @Override
    public BigInteger call() throws Exception {
        computeAndShareZShare();
        computeSecretShare();

        return T;
    }

    private void computeAndShareZShare() {

        zShares = wShares.add(truncationShares.r).mod(prime);
        // broadcast it to n parties
        Message senderMessage = new Message(zShares,
                clientID, protocolIdQueue);

        try {
            senderQueue.put(senderMessage);
        } catch (InterruptedException ex) {
            Logger.getLogger(Truncation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void computeSecretShare() {
        Message receivedMessage = null;
        BigInteger diffList = null;
        // Receive all zShares
        for (int i = 0; i < partyCount - 1; i++) {
            try {
                receivedMessage = pidMapper.get(protocolIdQueue).take();
                diffList = (BigInteger) receivedMessage.getValue();
                zShares = zShares.add(diffList).mod(prime);
            } catch (InterruptedException ex) {
            }
        }

        // Constants
        BigInteger roundOffBit = BigInteger.valueOf(2).pow(Constants.INTEGER_PRECISION
                + 2 * Constants.DECIMAL_PRECISION - 1);
        BigInteger fInv = prime.add(BigInteger.ONE).divide(BigInteger.valueOf(2)).
                pow(Constants.DECIMAL_PRECISION).mod(prime);
        BigInteger fpow2 = BigInteger.valueOf(2).pow(Constants.DECIMAL_PRECISION);

        
        BigInteger c = zShares.add(roundOffBit);
        BigInteger cp = c.mod(fpow2);
        BigInteger S = wShares.add(truncationShares.rp).mod(prime).
                subtract(cp.multiply(BigInteger.valueOf(asymmetricBit)));
        T = S.multiply(fInv).mod(prime);

    }

}
