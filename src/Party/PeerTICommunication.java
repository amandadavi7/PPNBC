/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import TrustedInitializer.TIShare;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receive all Ti shares
 *
 * @author anisha
 */
public class PeerTICommunication implements Callable<TIShare> {

    Socket socket = null;
    ObjectInputStream iStream = null;
    TIShare tiShares;

    /**
     * 
     * @param socket
     * @param tiShares 
     */
    public PeerTICommunication(Socket socket, TIShare tiShares) {
        this.socket = socket;
        this.tiShares = tiShares;
        try {
            iStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(PeerTICommunication.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * 
     * @return 
     */
    @Override
    public TIShare call() {

        try {
            tiShares = (TIShare) iStream.readObject();
        } catch (IOException ex) {
            System.out.println("Check socket connection:" + ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(PeerTICommunication.class.getName()).log(Level.SEVERE, null, ex);
        }

        return tiShares;
    }
}
