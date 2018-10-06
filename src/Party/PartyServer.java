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

    private static final Logger LOGGER = Logger.getLogger(PartyServer.class.getName());
    
    Socket socket;
    BlockingQueue<Message> senderQueue;
    ObjectOutputStream oStream;
    int clientId, asymmetricBit;

    /**
     * Constructor
     * takes common sender queue, socket and output stream object
     * 
     * @param socket
     * @param queue
     * @param oStream
     */
    public PartyServer(Socket socket, BlockingQueue<Message> queue,
            ObjectOutputStream oStream, int clientId, int asymmetricBit) {
        this.socket = socket;
        this.senderQueue = queue;
        this.oStream = oStream;
        this.clientId = clientId;
        this.asymmetricBit = asymmetricBit;
    }

    /**
     * Continuously running thread that takes entries from sender queue and send
     * them to BA
     */
    @Override
    public void run() {

        try {
            // first send the id for the BA to store
            LOGGER.log(Level.INFO, "clientID:{0},asymmetricBit:{1}", new Object[]{clientId, asymmetricBit});
            oStream.writeInt(clientId);
            oStream.writeInt(asymmetricBit);
            oStream.flush();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error sending clientId", ex);
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
            LOGGER.log(Level.SEVERE, "Error closing stream", ex);
        }

        LOGGER.log(Level.INFO, "Party Server Closed for client:{0}", clientId);

    }
}
