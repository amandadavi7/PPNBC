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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Batch Truncation of n elements 
 * TODO better name
 * @author anisha
 */
public class Truncation extends CompositeProtocol implements Callable<BigInteger[]> {

    BigInteger[] wShares;
    List<BigInteger> zShares;
    BigInteger[] T;
    List<TruncationPair> truncationShares;

    BigInteger prime;
    int batchSize;

    public Truncation(BigInteger[] wShares,
            List<TruncationPair> tiShares, BlockingQueue<Message> senderqueue,
            BlockingQueue<Message> receiverqueue, Queue<Integer> protocolIdQueue,
            int clientID, BigInteger prime,
            int protocolID, int asymmetricBit, int partyCount) {

        super(protocolID, senderqueue, receiverqueue, protocolIdQueue, clientID,
                asymmetricBit, partyCount);

        this.wShares = wShares;
        this.prime = prime;
        this.truncationShares = tiShares;
        
        zShares = new ArrayList<>();
        batchSize = wShares.length;
        T = new BigInteger[batchSize];
    }

    @Override
    public BigInteger[] call() throws Exception {
        computeAndShareZShare();
        computeSecretShare();

        return T;
    }

    private void computeAndShareZShare() {

        for (int i = 0; i < batchSize; i++) {
            zShares.add(i, wShares[i].add(truncationShares.get(0).r).mod(prime));
        }

        // broadcast it to n parties
        List<List<BigInteger>> diffList = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            List<BigInteger> newRow = new ArrayList<>();
            newRow.add(zShares.get(i));
            diffList.add(newRow);
        }

        Message senderMessage = new Message(Constants.localShares, diffList,
                clientID, protocolIdQueue);

        try {
            senderQueue.put(senderMessage);
            //System.out.println("sending message for protocol id:"+ protocolId);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Logger.getLogger(MultiplicationInteger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void computeSecretShare() {
        Message receivedMessage = null;
        List<BigInteger> z = new ArrayList<>(Collections.nCopies(batchSize, BigInteger.ZERO));
        List<List<BigInteger>> diffList = null;
        // Receive all zShares
        for (int i = 0; i < partyCount - 1; i++) {
            try {
                receivedMessage = receiverQueue.take();
                diffList = (List<List<BigInteger>>) receivedMessage.getValue();
                for (int j = 0; j < batchSize; j++) {
                    z.set(j, z.get(j).add(diffList.get(j).get(0)));
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        BigInteger roundOffBit = BigInteger.valueOf(2).pow(Constants.integer_precision
                + Constants.decimal_precision - 1);
        BigInteger fInv = prime.add(BigInteger.ONE).divide(BigInteger.valueOf(2)).
                pow(Constants.decimal_precision).mod(prime);
        BigInteger fpow2 = BigInteger.valueOf(2).pow(Constants.decimal_precision);

        for (int i = 0; i < batchSize; i++) {
            // Add local z to the sum
            BigInteger Z = zShares.get(i)
                    .add(z.get(i)).mod(prime);
            BigInteger c = Z.add(roundOffBit);
            BigInteger cp = c.mod(fpow2);
            BigInteger S = wShares[i].add(truncationShares.get(i).rp).
                    subtract(cp.multiply(BigInteger.valueOf(asymmetricBit)));
            T[i] = S.multiply(fInv).mod(prime);

        }

    }

}
