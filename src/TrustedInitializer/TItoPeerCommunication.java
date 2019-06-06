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
     *
     * @param tiSocket
     * @param tishare
     */
    public TItoPeerCommunication(ServerSocket tiSocket, TIShare tishare) {
        this.tiSocket = tiSocket;
        this.tishare = tishare;
    }

    /**
     * Connects to a party and send tiShares
     */
    @Override
    public void run() {
        try {
            socket = tiSocket.accept();
            System.out.println("Connected to:" + socket + ": sending,");
            ObjectOutputStream oStream = new ObjectOutputStream(socket.getOutputStream());
            oStream.writeObject(tishare);
            System.out.println("Sent " + tishare.bigIntShares.size() + " shares to "
                    + socket.getInetAddress());
            //TODO the socket life depends on the below print now 
            //(TI will be made offline, So it doesn't matter)
            tishare.decimalShares.forEach((t) -> {
                System.out.println("u : " + t.u + ",v : " + t.v + ",w : " + t.w);
            });
            tishare.binaryShares.forEach((t) -> {
                System.out.println("u : " + t.u + ",v : " + t.v + ",w : " + t.w);
            });
            tishare.realShares.forEach((t) -> {
                System.out.println("u : " + t.u + ",v : " + t.v + ",w : " + t.w);
            });
            tishare.bigIntShares.forEach((t) -> {
                System.out.println("u : " + t.u + ",v : " + t.v + ",w : " + t.w);
            });
            tishare.truncationPair.forEach((t) -> {
                System.out.println("r : " + t.r + ",rp : " + t.rp);
            });
        } catch (IOException ex){
            Logger.getLogger(TItoPeerCommunication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
