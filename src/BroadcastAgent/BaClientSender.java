/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BroadcastAgent;

import Communication.Message;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This thread takes out messages pending to be sent to the client and sends it 
 * over the socket
 * @author anisha
 */
public class BaClientSender implements Runnable {

    Socket clientSocket;
    BlockingQueue<Message> senderQueue;
    ObjectOutputStream oStream = null;
    
    /**
     * Constructor
     *
     * @param socketserver
     * @param queue
     */
    BaClientSender(Socket socket, ObjectOutputStream oStream, 
            BlockingQueue<Message> senderQueue) {
        this.clientSocket = socket;
        this.senderQueue = senderQueue;
        this.oStream = oStream;
        
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
                msg = senderQueue.take();
                oStream.writeObject(msg);                
            } catch (InterruptedException | IOException ex) {
                break;
            } 
        }

        try {
            oStream.close();
            clientSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(BaClientSender.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
