/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrustedInitializer;

import Utility.Connection;
import Utility.Constants;
import Utility.Logging;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Trusted Initializer that generates the randomness
 *
 * @author keerthanaa
 */
public class TI {

    static int clientCount, decTriples, binTriples, bigIntTriples, 
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

    }

    private static void generateRandomShares() {
        RandomGenerator.generateDecimalTriples(decTriples, clientCount, tiShare);
        RandomGenerator.generateBinaryTriples(binTriples, clientCount, tiShare);
        RandomGenerator.generateBigIntTriples(bigIntTriples, clientCount, tiShare);
        RandomGenerator.generateTruncationPairs(truncationPairs, clientCount, tiShare);
    }

    

}
