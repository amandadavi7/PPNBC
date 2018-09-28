/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import TrustedInitializer.TripleReal;
import TrustedInitializer.TruncationPair;
import Utility.Constants;
import Utility.ErrorMessages;
import Utility.FileIO;
import Utility.LocalMath;
import java.math.BigInteger;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Run LinearRegressionEvaluation for 2 parties, n times. Add a random vector
 * <ri> to each party and send it back to the client. Also send <R> to the
 * client to subtract.
 *
 * Each party receives the shares of x and the co-efficients(beta) and computes
 * the shares of y, such that y = beta.x
 *
 * @author anisha
 */
public class LinearRegressionEvaluationDAMF extends LinearRegressionEvaluation {

    private static final Logger LOGGER = Logger.getLogger(LinearRegressionEvaluationDAMF.class.getName());

    // The random ri 
    BigInteger[] r;
    BigInteger[] maskedY;
    
    /**
     * Constructor
     *
     * @param realTriples
     * @param truncationShares
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param partyCount
     * @param args
     *
     */
    public LinearRegressionEvaluationDAMF(List<TripleReal> realTriples,
            List<TruncationPair> truncationShares,
            int asymmetricBit,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            int clientId,
            int partyCount, String[] args) {

        super(realTriples,
                truncationShares, asymmetricBit, pidMapper, senderQueue,
                clientId, partyCount, args);

    }

    /**
     * Compute shares of the prediction for each entry of the dataset:x
     */
    @Override
    public void predictValues() {

        long startTime = System.currentTimeMillis();
        super.predictValues();

        // generate ri vector
        java.util.Random rand = new java.util.Random();
        r = new BigInteger[testCases];
        for (int i = 0; i < testCases; i++) {
            r[i] = new BigInteger(Constants.INTEGER_PRECISION, rand);
        }

        // local multiplication
        maskedY = LocalMath.hadamardMultiplication(y, r, prime);
        // Broadcast random ri
        Message senderMessage = new Message(r,
                clientId, protocolIdQueue);
        try {
            commonSender.put(senderMessage);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, ErrorMessages.INTERRUPTED_EXCEPTION_PUT, ex);
        }

        for (int i = 0; i < partyCount - 1; i++) {
            try {
                Message receivedMessage = pidMapper.get(protocolIdQueue).take();
                BigInteger[] otherPartyR = (BigInteger[]) receivedMessage.getValue();
                for (int j = 0; j < testCases; j++) {
                    r[j] = r[j].add(otherPartyR[j]).mod(prime);
                }
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, ErrorMessages.INTERRUPTED_EXCEPTION_TAKE, ex);
            }
        }
        
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        //TODO: push time to a csv file
        LOGGER.log(Level.INFO, "Avg time duration:{0} for partyId:{1}", new Object[]{elapsedTime, clientId});
        FileIO.writeToCSV(maskedY, outputPath, "maskedY", clientId);
        FileIO.writeToCSV(r, outputPath, "r", clientId);
    }

}
