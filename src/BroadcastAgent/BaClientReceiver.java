/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BroadcastAgent;

import Communication.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Every client receives the message and puts it in the receiver queue for 
 * broadcasting. There are (partyCount) receiver threads
 * @author anisha
 */
public class BaClientReceiver implements Runnable {

    Socket clientSocket;
    BlockingQueue<Message> receiverQueue;
    ObjectInputStream iStream = null;
    int clientId;

    /**
     * Constructor
     *
     * @param socketserver
     * @param queue
     */
    BaClientReceiver(Socket socket, ObjectInputStream iStream, 
            BlockingQueue<Message> receiverQueue, int clientId) {
        this.clientSocket = socket;
        this.receiverQueue = receiverQueue;
        this.iStream = iStream;
        this.clientId = clientId;
        
    }

    /**
     * Continuously running thread that takes entries from senderqueue and send
     * them to other parties
     */
    @Override
    public void run() {

        Message msg;
        while (!(Thread.currentThread().isInterrupted())) {
            try {
                //TODO packet the message with the client id
                msg = (Message) iStream.readObject();
                receiverQueue.add(msg);
            } catch (IOException ex) {
                Logger.getLogger(BaClientReceiver.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(BaClientReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        try {
            iStream.close();
            clientSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(BaClientReceiver.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
