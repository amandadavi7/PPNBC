/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrustedInitializer;

import Utility.Connection;
import Utility.Constants;
import Utility.Logging;
import Utility.Random;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    public static void sendShares() {
        System.out.println("Sending shares to parties");
        ServerSocket tiserver = Connection.createServerSocket(tiPort);

        ExecutorService send = Executors.newFixedThreadPool(Constants.threadCount);
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

        sendShares();
    }

    private static void generateRandomShares() {
        Random.generateDecimalTriples(decTriples, clientCount, tiShare);
        Random.generateBinaryTriples(binTriples, clientCount, tiShare);
        Random.generateBigIntTriples(bigIntTriples, clientCount, tiShare);
        Random.generateTruncationPairs(truncationPairs, clientCount, tiShare);
    }

    

}
