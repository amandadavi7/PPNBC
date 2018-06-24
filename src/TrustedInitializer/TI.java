/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrustedInitializer;

import Utility.Connection;
import Utility.Constants;
import Utility.Logging;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Trusted Initializer that generates the randomness
 *
 * @author keerthanaa
 */
public class TI {

    static int clientCount, tiPort;
    static int decTriples, binTriples, bigIntTriples;
    static TIShare[] uvw;

    /**
     * Constructor
     *
     * @param port
     * @param partyCount
     * @param decimalCount
     * @param binaryCount
     * @param bigIntegerCount
     */
    public static void initializeVariables(String port, String partyCount,
            String decimalCount, String binaryCount, String bigIntegerCount) {
        tiPort = Integer.parseInt(port);
        clientCount = Integer.parseInt(partyCount);
        decTriples = Integer.parseInt(decimalCount);
        binTriples = Integer.parseInt(binaryCount);
        bigIntTriples = Integer.parseInt(bigIntegerCount);
        uvw = new TIShare[clientCount];
        for (int i = 0; i < clientCount; i++) {
            uvw[i] = new TIShare();
        }
    }

    /**
     * Generate Decimal Triples
     */
    public static void generateDecimalTriples() {
        Random rand = new Random();
        for (int i = 0; i < decTriples; i++) {
            int U = rand.nextInt(Constants.prime);
            int V = rand.nextInt(Constants.prime);
            int W = Math.floorMod(U * V, Constants.prime);
            int usum = 0, vsum = 0, wsum = 0;
            for (int j = 0; j < clientCount - 1; j++) {
                TripleInteger t = new TripleInteger();
                t.u = rand.nextInt(Constants.prime);
                t.v = rand.nextInt(Constants.prime);
                t.w = rand.nextInt(Constants.prime);
                usum += t.u;
                vsum += t.v;
                wsum += t.w;
                uvw[j].addDecimal(t);
            }
            TripleInteger t = new TripleInteger();
            t.u = Math.floorMod(U - usum, Constants.prime);
            t.v = Math.floorMod(V - vsum, Constants.prime);
            t.w = Math.floorMod(W - wsum, Constants.prime);
            uvw[clientCount - 1].addDecimal(t);
        }
    }

    /**
     * Generate Binary Triples
     */
    public static void generateBinaryTriples() {
        Random rand = new Random();
        for (int i = 0; i < binTriples; i++) {
            int U = rand.nextInt(Constants.binaryPrime);
            int V = rand.nextInt(Constants.binaryPrime);
            int W = U * V;
            int usum = 0, vsum = 0, wsum = 0;
            for (int j = 0; j < clientCount - 1; j++) {
                TripleByte t = new TripleByte();
                t.u = (byte) rand.nextInt(Constants.binaryPrime);
                t.v = (byte) rand.nextInt(Constants.binaryPrime);
                t.w = (byte) rand.nextInt(Constants.binaryPrime);
                usum += t.u;
                vsum += t.v;
                wsum += t.w;
                uvw[j].addBinary(t);
            }
            TripleByte t = new TripleByte();
            t.u = (byte) Math.floorMod(U - usum, Constants.binaryPrime);
            t.v = (byte) Math.floorMod(V - vsum, Constants.binaryPrime);
            t.w = (byte) Math.floorMod(W - wsum, Constants.binaryPrime);
            uvw[clientCount - 1].addBinary(t);
        }
    }

    /**
     * Generate Big Integer Triples
     */
    public static void generateBigIntTriples() {
        Random rand = new Random();
        BigInteger Zq = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1).nextProbablePrime();

        for (int i = 0; i < bigIntTriples; i++) {
            BigInteger U = new BigInteger(Constants.integer_precision, rand);
            BigInteger V = new BigInteger(Constants.integer_precision, rand);
            BigInteger W = U.multiply(V);
            W = W.mod(Zq);

            BigInteger usum = BigInteger.ZERO, vsum = BigInteger.ZERO, wsum = BigInteger.ZERO;
            for (int j = 0; j < clientCount - 1; j++) {
                TripleReal t = new TripleReal();
                t.u = new BigInteger(Constants.integer_precision, rand);
                t.v = new BigInteger(Constants.integer_precision, rand);
                t.w = new BigInteger(Constants.integer_precision, rand);
                usum = usum.add(t.u);
                vsum = vsum.add(t.v);
                wsum = wsum.add(t.w);
                uvw[j].addBigInt(t);
            }
            TripleReal t = new TripleReal();
            t.u = (U.subtract(usum)).mod(Zq);
            t.v = (V.subtract(vsum)).mod(Zq);
            t.w = (W.subtract(wsum)).mod(Zq);
            uvw[clientCount - 1].addBigInt(t);
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

            Runnable sendtask = new TItoPeerCommunication(tiserver, uvw[i]);
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

        initializeVariables(args[0], args[1], args[2], args[3], args[4]);

        System.out.println("Generating " + decTriples + " decimal triples, "
                + binTriples + " binary triples and " + bigIntTriples + " real triples");

        generateDecimalTriples();
        generateBinaryTriples();
        generateBigIntTriples();

        System.out.println("Generated " + uvw[0].bigIntShares.size() + " bigInt shares");

        sendShares();
    }

}
