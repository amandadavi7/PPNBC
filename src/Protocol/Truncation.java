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
 * Truncation of n elements batch wise
 *
 * @author anisha
 */
public class Truncation extends CompositeProtocol implements Callable<List<BigInteger>> {

    List<BigInteger> wShares;
    List<BigInteger> zShares;
    List<TruncationPair> truncationShares;
    List<BigInteger> C, Cp;
    List<BigInteger> T;

    BigInteger prime, fpow2;

    int batchSize;

    public Truncation(List<BigInteger> wShares,
            List<TruncationPair> tiShares, BlockingQueue<Message> senderqueue,
            BlockingQueue<Message> receiverqueue, Queue<Integer> protocolIdQueue,
            int clientID, BigInteger prime,
            int protocolID, int oneShare, int partyCount) {

        super(protocolID, senderqueue, receiverqueue, protocolIdQueue, clientID,
                oneShare, partyCount);

        this.wShares = wShares;
        this.prime = prime;
        this.truncationShares = tiShares;
        batchSize = wShares.size();
        zShares = new ArrayList<>();
        C = new ArrayList<>();
        Cp = new ArrayList<>();
        T = new ArrayList<>();

        fpow2 = BigInteger.valueOf(2).pow(Constants.decimal_precision);
    }

    @Override
    public List<BigInteger> call() throws Exception {
        computeAndShareZShare();
        computeC();
        computeSecretShare();

        return T;
    }

    private void computeAndShareZShare() {

        for (int i = 0; i < batchSize; i++) {
            zShares.add(i, wShares.get(i).add(truncationShares.get(0).r).mod(prime));
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

    private void computeC() {
        Message receivedMessage = null;
        List<BigInteger> z = new ArrayList<>(Collections.nCopies(batchSize, BigInteger.ZERO));
        List<List<BigInteger>> diffList = null;
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
        for (int i = 0; i < batchSize; i++) {
            // Add local z to the sum
            BigInteger Z = zShares.get(i)
                    .add(z.get(i)).mod(prime);
            C.add(Z.add(roundOffBit));
            Cp.add(C.get(i).mod(fpow2));

        }

    }

    private void computeSecretShare() {
        BigInteger fInv = prime.add(BigInteger.ONE).divide(BigInteger.valueOf(2)).
                pow(Constants.decimal_precision).mod(prime);

        BigInteger S;

        for (int i = 0; i < batchSize; i++) {
            S = wShares.get(i).add(truncationShares.get(i).rp).
                    subtract(Cp.get(i).multiply(BigInteger.valueOf(oneShare)));
            T.add(S.multiply(fInv).mod(prime));

        }
    }
}
