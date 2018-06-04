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

/**
 *
 * @author keerthanaa
 */
public class PartyServer implements Runnable {

    Socket socket;
    BlockingQueue<Message> senderQueue;
    ObjectOutputStream oStream;

    /**
     * Constructor
     *
     * @param socket
     * @param queue
     * @param oStream
     */
    public PartyServer(Socket socket, BlockingQueue<Message> queue, 
            ObjectOutputStream oStream) {
        this.socket = socket;
        this.senderQueue = queue;
        this.oStream = oStream;
    }

    /**
     * Continuously running thread that takes entries from senderqueue and send
     * them to other parties
     */
    @Override
    public void run() {

        while (!(Thread.currentThread().isInterrupted())) {
            try {
                Message msg = senderQueue.take();
                oStream.writeObject(msg);
                oStream.reset();
                //oStream.flush();
            } catch (InterruptedException | IOException ex) {
                break;
            } 
        }
        
        try {
            oStream.close();
        } catch (IOException ex) {
            //Logger.getLogger(PartyServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("Server closed");

    }
}
