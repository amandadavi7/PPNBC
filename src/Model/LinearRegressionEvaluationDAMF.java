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
import Utility.Logging;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Run LinearRegressionEvaluation for 2 parties, n times. Add a random vector
 * [[r_i]] to each party and send it back to the client. Also send [[R]] to the
 * client to subtract.
 *
 * Each party receives the shares of x and the co-efficients(beta) and computes
 * the shares of y, such that y = beta.x
 *
 * @author anisha
 */
public class LinearRegressionEvaluationDAMF extends Model {

    private static final Logger LOGGER = Logger.getLogger(LinearRegressionEvaluationDAMF.class.getName());

    List<BigInteger> y;
    static BigInteger prime;
    String outputPath;
    int testCases;
    
    // The random ri 
    List<BigInteger> r;
    
    /**
     * Constructor
     *
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param partyCount
     * @param args
     *
     */
    public LinearRegressionEvaluationDAMF(int asymmetricBit,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            int clientId,
            int partyCount, String[] args) {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount);
        
        prime = BigInteger.valueOf(2).pow(Constants.INTEGER_PRECISION
                + 2 * Constants.DECIMAL_PRECISION + 1).nextProbablePrime();  //Zq must be a prime field

        initalizeModelVariables(args);

    }

    /**
     * Compute shares of the prediction for each entry of the dataset:x
     */
    public void predictValues() {

        long startTime = System.currentTimeMillis();
        
        BigInteger fac = BigInteger.valueOf(2).pow(Constants.INTEGER_PRECISION);
        // generate ri vector
        java.util.Random rand = new java.util.Random();
        r = new ArrayList<>(testCases);
        for (int i = 0; i < testCases; i++) {
            r.add(new BigInteger(Constants.INTEGER_PRECISION, rand).multiply(fac).mod(prime));
        }

        for (int i = 0; i < testCases; i++) {
            // mask y by adding random r
            y.set(i, y.get(i).add(r.get(i)).mod(prime));
        }
        
        // Broadcast random ri
        Message senderMessage = new Message(r,
                clientId, protocolIdQueue, true);
        try {
            commonSender.put(senderMessage);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, ErrorMessages.INTERRUPTED_EXCEPTION_PUT, ex);
        }

        if (asymmetricBit == 1) {
            for (int i = 0; i < partyCount - 1; i++) {
                try {
                    Message receivedMessage = pidMapper.get(protocolIdQueue).take();
                    List<BigInteger> otherPartyR = (List<BigInteger>) receivedMessage.getValue();
                    for (int j = 0; j < testCases; j++) {
                        r.set(j, r.get(j).add(otherPartyR.get(j)).mod(prime));
                    }
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, ErrorMessages.INTERRUPTED_EXCEPTION_TAKE, ex);
                }
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        //TODO: push time to a csv file
        LOGGER.log(Level.INFO, "Avg time duration:{0} for partyId:{1}", new Object[]{elapsedTime, clientId});
        FileIO.writeToCSV(y, outputPath, "maskedY", clientId);
        if(asymmetricBit == 1) {
            FileIO.writeToCSV(r, outputPath, "R", clientId);
        }
    }
    
    private void initalizeModelVariables(String[] args) {
        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.partyUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];

            switch (command) {
                case "yCsv":
                    y = FileIO.loadListFromFile(value);
                    break;
                case "output":
                    outputPath = value;
                    break;

            }
        }
        testCases = y.size();
    }
}
