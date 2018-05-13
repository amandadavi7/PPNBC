/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrustedInitializer;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author keerthanaa
 */
public class TItoPeerCommunication implements Runnable {
    ServerSocket tiSocket;
    TIShare tishare;
    Socket socket;
    
    /**
     * Constructor
     * @param tiSocket
     * @param tishare 
     */
    public TItoPeerCommunication(ServerSocket tiSocket, TIShare tishare){
        this.tiSocket = tiSocket;
        this.tishare = tishare;
    }
    
    
    /**
     * Connects to a party and send tishares
     */
    @Override
    public void run() {
        try{
            socket = tiSocket.accept();
            System.out.println("Connected to:" + socket + ": sending,");
            ObjectOutputStream oStream = new ObjectOutputStream(socket.getOutputStream());
            oStream.writeObject(tishare);
            for(TripleInteger t: tishare.decimalShares){
                System.out.println("u : " + t.u + ",v : " + t.v + ",w : " + t.w);
            }
            for(TripleByte t: tishare.binaryShares){
                System.out.println("u : " + t.u + ",v : " + t.v + ",w : " + t.w);
            }
            for(TripleReal t: tishare.bigIntShares){
                System.out.println("u : " + t.u + ",v : " + t.v + ",w : " + t.w);
            }
        } catch (IOException ex){
            Logger.getLogger(TItoPeerCommunication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
