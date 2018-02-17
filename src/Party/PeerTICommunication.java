/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import Utility.Constants;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anisha
 */
public class PeerTICommunication implements Callable<Integer[][]> {

    Socket socket = null;
    ObjectOutputStream oStream = null;
    ObjectInputStream iStream = null;
    Integer[][] tiShares;
    int noOfFuncToCompute;

    public PeerTICommunication(Socket socket, Integer[][] tiShares, int noOfFuncToCompute) {
        this.socket = socket;
        this.tiShares = tiShares;
        this.noOfFuncToCompute = noOfFuncToCompute;
        try {

            oStream = new ObjectOutputStream(socket.getOutputStream());
            iStream = new ObjectInputStream(socket.getInputStream());

        } catch (IOException ex) {
            Logger.getLogger(PeerTICommunication.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public Integer[][] call() throws Exception {

        try {
            tiShares = (Integer[][]) iStream.readObject();
        } catch (IOException ex) {
            System.out.println("Check socket connection:" + ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(PeerTICommunication.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("tiSharesReceived:");
        for (int i = 0; i < noOfFuncToCompute; i++) {
            for (int j = 0; j < 3; j++) {
                System.out.print(tiShares[i][j] + " ");
            }
            System.out.println("");
        }
        return tiShares;
    }
}
