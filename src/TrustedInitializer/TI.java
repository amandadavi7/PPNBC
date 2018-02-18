/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrustedInitializer;

import Communication.Connection;
import Utility.Constants;
import Utility.Logging;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author keerthanaa
 */
public class TI {
    
    static int tiPort, decTriples, binTriples;
    static TIShare[] uvw;
    
    /**
     * Constructor
     * 
     * @param port
     * @param count1
     * @param count2 
     */
    public static void initializeVariables(String port,String count1, String count2){
        tiPort = Integer.parseInt(port);
        decTriples = Integer.parseInt(count1);
        binTriples = Integer.parseInt(count2);
        uvw = new TIShare[Constants.clientCount];
        for(int i=0;i<Constants.clientCount;i++){
            uvw[i] = new TIShare();
        }
    }
    
    public static void generateUVW(){
        Random rand = new Random();
        for(int i=0;i<decTriples;i++){
            int U = rand.nextInt(Constants.prime)+1;
            int V = rand.nextInt((Constants.prime-1)/U + 1);
            int W = U*V;
            int usum = 0, vsum = 0, wsum = 0;
            for(int j=0;j<Constants.clientCount-1;j++){
                Triple t = new Triple();
                t.u = rand.nextInt(Constants.prime);
                t.v = rand.nextInt(Constants.prime);
                t.w = rand.nextInt(Constants.prime);
                usum+=t.u;
                vsum+=t.v;
                wsum+=t.w;
                uvw[j].addDecimal(t);
            }
            Triple t = new Triple();
            t.u = Math.floorMod(U-usum, Constants.prime);
            t.v = Math.floorMod(V-vsum, Constants.prime);
            t.w = Math.floorMod(W-wsum, Constants.prime);
            uvw[Constants.clientCount-1].addDecimal(t);
        }
        
        for(int i=0;i<binTriples;i++){
            int U = rand.nextInt(Constants.binaryPrime);
            int V = rand.nextInt(Constants.binaryPrime);
            int W = U*V;
            int usum = 0, vsum = 0, wsum = 0;
            for(int j=0;j<Constants.clientCount-1;j++){
                Triple t = new Triple();
                t.u = rand.nextInt(Constants.binaryPrime);
                t.v = rand.nextInt(Constants.binaryPrime);
                t.w = rand.nextInt(Constants.binaryPrime);
                usum+=t.u;
                vsum+=t.v;
                wsum+=t.w;
                uvw[j].addBinary(t);
            }
            Triple t = new Triple();
            t.u = Math.floorMod(U-usum, Constants.binaryPrime);
            t.v = Math.floorMod(V-vsum, Constants.binaryPrime);
            t.w = Math.floorMod(W-wsum, Constants.binaryPrime);
            uvw[Constants.clientCount-1].addBinary(t);
        }
    }
    
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
        
        initializeVariables(args[0],args[1],args[2]);
        
        System.out.println("Generating " + decTriples+" decimal triples and " + 
                binTriples + " binary triples");
        generateUVW();
        
        sendShares();        
    }
    
}
