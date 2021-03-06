/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BroadcastAgent;

import Communication.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Every client receives the message and puts it in the receiver queue for
 * broadcasting. There are (partyCount) receiver threads
 *
 * @author anisha
 */
public class BaClientReceiver implements Callable<Boolean> {

    private static final Logger LOGGER = Logger.getLogger(BaClientReceiver.class.getName());
    Socket clientSocket;
    
    BlockingQueue<Message> receiverQueue;
    ObjectInputStream iStream = null;
    
    int clientId, totalClients;
    static AtomicInteger counter = new AtomicInteger();

    /**
     * Constructor
     *
     * @param socketserver
     * @param queue
     */
    BaClientReceiver(Socket socket, ObjectInputStream iStream,
            BlockingQueue<Message> receiverQueue, int clientId,
            int totalCount) {
        this.clientSocket = socket;
        this.receiverQueue = receiverQueue;
        this.iStream = iStream;
        this.clientId = clientId;
        this.totalClients = totalCount;
    }

    /**
     * Continuously running thread that takes entries from socket and adds to
     * the receiver queue
     */
    @Override
    public Boolean call() {

        while (!(Thread.currentThread().isInterrupted())) {
            try {
                Message msg = (Message) iStream.readObject();
                receiverQueue.add(msg);
            } catch (IOException | ClassNotFoundException ex) {
                if (counter.incrementAndGet() >= totalClients) {
                    break;
                }
            } 
        }

        try {
            iStream.close();
            clientSocket.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error closing socket", ex);
        }
        return true;
    }
}
