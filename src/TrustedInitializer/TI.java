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

    static int tiPort, clientCount, decTriples, binTriples, realTriples,
            truncationPairs, equalityCount, equalityBigIntCount, bigIntTriples, rowCount, columnCount, ensembleRounds, rowRange, columnRange,
            eTRowCount, eTColCount, featureCount, treeCount, classValueCount;
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
                    realTriples = Integer.parseInt(value);
                    break;
                case "truncation":
                    truncationPairs = Integer.valueOf(value);
                    break;
                case "equality":
                    equalityCount = Integer.parseInt(value);
                    break;
                case "equalityBigInt":
                    equalityBigIntCount = Integer.parseInt(value);
                    break;
                case "bigInt":
                    bigIntTriples = Integer.parseInt(value);
                case "rowCount":
                    rowCount = Integer.parseInt(value);
                    break;
                case "columnCount":
                    columnCount = Integer.parseInt(value);
                    break;
                case "rowRange":
                    rowRange = Integer.parseInt(value);
                    break;
                case "columnRange":
                    columnRange = Integer.parseInt(value);
                    break;
                case "ensembleRounds":
                    ensembleRounds = Integer.parseInt(value);
                    break;
                case "eTRowCount":
                    eTRowCount = Integer.parseInt(value);
                    break;
                case "eTColCount":
                    eTColCount = Integer.parseInt(value) - 1;
                    break;
                case "featureCount":
                    featureCount = Integer.parseInt(value);
                    break;
                case "treeCount":
                    treeCount = Integer.parseInt(value);
                    break;
                case "classValueCount":
                    classValueCount = Integer.parseInt(value);
                    break;
            }
        }

        tiShare = new TIShare[clientCount];
        for (int i = 0; i < clientCount; i++) {
            tiShare[i] = new TIShare();
            if (ensembleRounds != 0) {
                for (int j = 0; j < ensembleRounds; j++) {
                    tiShare[i].ensembleShares.add(new TIShare());
                }
            }
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
        if (ensembleRounds == 0) {
            RandomGenerator.generateDecimalTriples(decTriples, clientCount, tiShare, -1);
            RandomGenerator.generateBinaryTriples(binTriples, clientCount, tiShare, -1);
            RandomGenerator.generateRealTriples(realTriples, clientCount, tiShare, -1);
            RandomGenerator.generateTruncationPairs(truncationPairs, clientCount, tiShare, -1);
            RandomGenerator.generateEqualityShares(equalityCount, clientCount, tiShare, -1);
            RandomGenerator.generateBigIntegerEqualityShares(equalityBigIntCount, clientCount, tiShare, -1);
            RandomGenerator.generateBigIntTriples(bigIntTriples, clientCount, tiShare, -1);
            RandomGenerator.generateRowShares(eTRowCount, eTColCount, treeCount, clientCount, tiShare);
            RandomGenerator.generateColShares(featureCount, treeCount, eTColCount, clientCount, tiShare);
            RandomGenerator.generateWholeNumShares(classValueCount, clientCount, tiShare);
        } else {
            for (int i = 0; i < ensembleRounds; i++) {
                RandomGenerator.generateDecimalTriples(decTriples, clientCount, tiShare, i);
                RandomGenerator.generateBinaryTriples(binTriples, clientCount, tiShare, i);
                RandomGenerator.generateRealTriples(realTriples, clientCount, tiShare, i);
                RandomGenerator.generateTruncationPairs(truncationPairs, clientCount, tiShare, i);
                RandomGenerator.generateEqualityShares(equalityCount, clientCount, tiShare, i);
                RandomGenerator.generateBigIntegerEqualityShares(equalityBigIntCount, clientCount, tiShare, i);
                RandomGenerator.generateBigIntTriples(bigIntTriples, clientCount, tiShare, i);
                RandomGenerator.generateRowFeatureSampling(rowRange, rowCount, true, clientCount, tiShare, i);
                RandomGenerator.generateRowFeatureSampling(columnRange, columnCount, false, clientCount, tiShare, i);
            }
        }
    }
}
