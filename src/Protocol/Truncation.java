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

    private static BigInteger fInv;

    public Truncation(BigInteger wShares,
            TruncationPair tiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderqueue,
            Queue<Integer> protocolIdQueue,
            int clientID, BigInteger prime,
            int protocolID, int asymmetricBit, int partyCount,int threadID) {

        super(protocolID, pidMapper, senderqueue, protocolIdQueue, clientID,
                asymmetricBit, partyCount,threadID);
        this.wShares = wShares;
        this.prime = prime;
        this.truncationShares = tiShares;

        fInv = prime.add(BigInteger.ONE).divide(BigInteger.valueOf(2)).
                pow(Constants.DECIMAL_PRECISION).mod(prime);

    }

    @Override
    public BigInteger call() throws InterruptedException {
        computeAndShareZShare();
        computeSecretShare();

        return T;
    }

    private void computeAndShareZShare() throws InterruptedException {

        zShares = wShares.add(truncationShares.r).mod(prime);
        // broadcast it to n parties
        Message senderMessage = new Message(zShares,
                clientID, protocolIdQueue);
        senderMessage.setThreadID(threadID);
        senderQueue.put(senderMessage);
    }

    private void computeSecretShare() throws InterruptedException {
        Message receivedMessage = null;
        BigInteger diffList = null;
        // Receive all zShares
        for (int i = 0; i < partyCount - 1; i++) {
            receivedMessage = pidMapper.get(protocolIdQueue).take();
            diffList = (BigInteger) receivedMessage.getValue();
            zShares = zShares.add(diffList).mod(prime);
        }

        BigInteger c = zShares.add(Constants.ROUND_OFF_BIT);
        BigInteger cp = c.mod(Constants.F_POW_2);
        BigInteger S = wShares.add(truncationShares.rp).mod(prime).
                subtract(cp.multiply(BigInteger.valueOf(asymmetricBit)));
        T = S.multiply(fInv).mod(prime);

    }

}
