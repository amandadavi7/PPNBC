/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import Communication.Connection;
import Utility.Constants;
import Communication.Message;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anisha
 */
public class PeerClient implements Runnable {

    ServerSocket serverSocket;
    Socket socketPeer;
    ObjectOutputStream oStream = null;
    ObjectInputStream iStream = null;

    BlockingQueue<Message> receiverQueue;
    int peerPort;

    public PeerClient(ServerSocket serverSocket, int port) {
        this.serverSocket = serverSocket;
        this.socketPeer = null;
        this.peerPort = port;
        
        // TODO innitialize this
        receiverQueue = new LinkedBlockingQueue<>();

    }

    @Override
    public void run() {
        //Client socket connection
        while (true) {
            socketPeer = Connection.initializeClientConnection(Constants.IP, peerPort);
            if (socketPeer != null && socketPeer.isConnected()) {
                break;
            }
        }

        // iniliatize socket components
        try {
            this.oStream = new ObjectOutputStream(socketPeer.getOutputStream());
            this.iStream = new ObjectInputStream(socketPeer.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(PeerClient.class.getName()).log(Level.SEVERE, null, ex);
        }

        while (true) {
            try {
                if (socketPeer != null && socketPeer.isConnected()) {
                    // TODO handle iStream close error
                    Message receivedMessage = (Message) iStream.readObject();
                    //System.out.println("Msg received:");
                    //receivedMessage.log();
                    receiverQueue.put(receivedMessage);
                } else {
                    break;
                }
            } catch (IOException ex) {
                Logger.getLogger(PeerClient.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException | InterruptedException ex) {
                Logger.getLogger(PeerClient.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

}
