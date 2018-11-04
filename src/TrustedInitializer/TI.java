/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrustedInitializer;

import Utility.Connection;
import Utility.Constants;
import Utility.Logging;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Trusted Initializer that generates the randomness
 *
 * @author keerthanaa
 */
public class TI {

    static int tiPort, clientCount, decTriples, binTriples, bigIntTriples, 
            truncationPairs;
    static TIShare[] tiShare;

    /**
     * Constructor
     *
     * @param args
     */
    public static void initializeVariables(String[] args) {

        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.tiUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];
            switch (command) {
                case "port":
                    tiPort = Integer.parseInt(value);
                    break;
                case "partyCount":
                    clientCount = Integer.valueOf(value);
                    break;
                case "decimal":
                    decTriples = Integer.parseInt(value);
                    break;
                case "binary":
                    binTriples = Integer.valueOf(value);
                    break;
                case "real":
                    bigIntTriples = Integer.parseInt(value);
                    break;
                case "truncation":
                    truncationPairs = Integer.valueOf(value);
                    break;
                    
            }
        }

        tiShare = new TIShare[clientCount];
        for (int i = 0; i < clientCount; i++) {
            tiShare[i] = new TIShare();
        }
    }

    

    /**
     * Send shares to parties
     */
    public static void sendShares() throws IOException {
        System.out.println("Sending shares to parties");
        ServerSocket tiserver = Connection.createServerSocket(tiPort);

        ExecutorService send = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        for (int i = 0; i < clientCount; i++) {

            Runnable sendtask = new TItoPeerCommunication(tiserver, tiShare[i]);
            send.execute(sendtask);

        }
        send.shutdown();
    }

    /**
     * Main method
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 4) {
            Logging.tiUsage();
            System.exit(0);
        }

        initializeVariables(args);

        generateRandomShares();
        System.out.println("Generated shares");

        try {
            sendShares();
        } catch (IOException ex) {
            Logger.getLogger(TI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void generateRandomShares() {
        RandomGenerator.generateDecimalTriples(decTriples, clientCount, tiShare);
        RandomGenerator.generateBinaryTriples(binTriples, clientCount, tiShare);
        RandomGenerator.generateBigIntTriples(bigIntTriples, clientCount, tiShare);
        RandomGenerator.generateTruncationPairs(truncationPairs, clientCount, tiShare);
    }

    

}
