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
 *
 * @author keerthanaa
 */
public class TI {
    
    static int tiPort, decTriples, binTriples, bigIntTriples;
    static TIShare[] uvw;
    
    /**
     * Constructor
     * 
     * @param port
     * @param count1
     * @param count2 
     * @param count3 
     */
    public static void initializeVariables(String port,String count1, String count2, String count3){
        tiPort = Integer.parseInt(port);
        decTriples = Integer.parseInt(count1);
        binTriples = Integer.parseInt(count2);
        bigIntTriples = Integer.parseInt(count3);
        uvw = new TIShare[Constants.clientCount];
        for(int i=0;i<Constants.clientCount;i++){
            uvw[i] = new TIShare();
        }
    }
    
    public static void generateDecimalTriples(){
        Random rand = new Random();
        for(int i=0;i<decTriples;i++){
            int U = rand.nextInt(Constants.prime);
            int V = rand.nextInt(Constants.prime);
            int W = Math.floorMod(U*V,Constants.prime);
            int usum = 0, vsum = 0, wsum = 0;
            for(int j=0;j<Constants.clientCount-1;j++){
                TripleInteger t = new TripleInteger();
                t.u = rand.nextInt(Constants.prime);
                t.v = rand.nextInt(Constants.prime);
                t.w = rand.nextInt(Constants.prime);
                usum+=t.u;
                vsum+=t.v;
                wsum+=t.w;
                uvw[j].addDecimal(t);
            }
            TripleInteger t = new TripleInteger();
            t.u = Math.floorMod(U-usum, Constants.prime);
            t.v = Math.floorMod(V-vsum, Constants.prime);
            t.w = Math.floorMod(W-wsum, Constants.prime);
            uvw[Constants.clientCount-1].addDecimal(t);
        }
    }
    
    public static void generateBinaryTriples(){
        Random rand = new Random();
        for(int i=0;i<binTriples;i++){
            int U = rand.nextInt(Constants.binaryPrime);
            int V = rand.nextInt(Constants.binaryPrime);
            int W = U*V;
            int usum = 0, vsum = 0, wsum = 0;
            for(int j=0;j<Constants.clientCount-1;j++){
                TripleByte t = new TripleByte();
                t.u = (byte) rand.nextInt(Constants.binaryPrime);
                t.v = (byte) rand.nextInt(Constants.binaryPrime);
                t.w = (byte) rand.nextInt(Constants.binaryPrime);
                usum+=t.u;
                vsum+=t.v;
                wsum+=t.w;
                uvw[j].addBinary(t);
            }
            TripleByte t = new TripleByte();
            t.u = (byte) Math.floorMod(U-usum, Constants.binaryPrime);
            t.v = (byte) Math.floorMod(V-vsum, Constants.binaryPrime);
            t.w = (byte) Math.floorMod(W-wsum, Constants.binaryPrime);
            uvw[Constants.clientCount-1].addBinary(t);
        }
    }
    
    public static void generateBigIntTriples(){
        Random rand = new Random();
        BigInteger Zq = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1).nextProbablePrime(); 
        BigInteger U = new BigInteger(Constants.integer_precision, rand);
        BigInteger V = new BigInteger(Constants.integer_precision, rand);
        BigInteger W = U.multiply(V);
        W = W.mod(Zq);
        
        BigInteger usum = BigInteger.ZERO, vsum = BigInteger.ZERO, wsum = BigInteger.ZERO;
            for(int j=0;j<Constants.clientCount-1;j++){
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
            uvw[Constants.clientCount-1].addBigInt(t);
    }
    
    /**
     * Send shares to parties
     */
    public static void sendShares(){
        System.out.println("Sending shares to parties");
        ServerSocket tiserver = Connection.createServerSocket(tiPort);
        
        for(int i=0;i<Constants.clientCount;i++){
            ExecutorService send = Executors.newSingleThreadExecutor();
            Runnable sendtask = new TItoPeerCommunication(tiserver, uvw[i]);
            send.execute(sendtask);
            send.shutdown();
        }
    }
    
    public static void main(String[] args) {
        if(args.length < 3){
            Logging.tiUsage();
            System.exit(0);
        }
        
        initializeVariables(args[0],args[1],args[2],args[3]);
        
        System.out.println("Generating " + decTriples+" decimal triples, " + 
                binTriples + " binary triples and " + bigIntTriples + "real triples");
        
        generateDecimalTriples();
        generateBinaryTriples();
        generateBigIntTriples();
        
        sendShares();        
    }
    
}
