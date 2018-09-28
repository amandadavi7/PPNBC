/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import Communication.Message;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author keerthanaa
 */
public class PartyServer implements Runnable {

    Socket socket;
    BlockingQueue<Message> senderQueue;
    ObjectOutputStream oStream;
    int clientId;

    /**
     * Constructor
     * takes common sender queue, socket and output stream object
     * 
     * @param socket
     * @param queue
     * @param oStream
     */
    public PartyServer(Socket socket, BlockingQueue<Message> queue,
            ObjectOutputStream oStream, int clientId) {
        this.socket = socket;
        this.senderQueue = queue;
        this.oStream = oStream;
        this.clientId = clientId;
    }

    /**
     * Continuously running thread that takes entries from sender queue and send
     * them to BA
     */
    @Override
    public void run() {

        try {
            // first send the id for the BA to store
            oStream.writeInt(clientId);
        } catch (IOException ex) {
            Logger.getLogger(PartyServer.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        while (!(Thread.currentThread().isInterrupted())) {
            try {
                Message msg = senderQueue.take();
                oStream.writeObject(msg);
                oStream.reset();
                oStream.flush();
            } catch (InterruptedException | IOException ex) {
                break;
            }
        }

        try {
            oStream.close();
        } catch (IOException ex) {
            
        }

        System.out.println("Server closed");

    }
}
