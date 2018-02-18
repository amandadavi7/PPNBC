/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author keerthanaa
 */
public class PeerClient implements Runnable{
    
    BlockingQueue<ProtocolMessage> receiverQueue;
    String peerServerIP;
    int peerServerPort;
    ObjectOutputStream oStream = null;
    ObjectInputStream iStream = null;
    Socket receiveSocket = null;
    
    /**
     * Constructor
     * 
     * @param queue
     * @param ip
     * @param port 
     */
    public PeerClient(BlockingQueue<ProtocolMessage> queue, String ip, int port){
        this.receiverQueue = queue;
        this.peerServerIP = ip;
        this.peerServerPort = port;        
    }
    
    /**
     * receive message and add to receiver queue
     */
    @Override
    public void run(){
        while(true) {
            try {
                    while(receiveSocket==null || !receiveSocket.isConnected()){
                        receiveSocket = Connection.initializeClientConnection(peerServerIP, peerServerPort);
                    }
                    iStream = new ObjectInputStream(receiveSocket.getInputStream());
                    oStream = new ObjectOutputStream(receiveSocket.getOutputStream());
                    
                    while(true) {
                        if(receiveSocket != null && receiveSocket.isConnected()) {
                            ProtocolMessage msg = (ProtocolMessage) iStream.readObject();
                            receiverQueue.put(msg);
                        } else {
                            break;
                        }
                    }
            } catch (IOException ex)  {
                Logger.getLogger(PeerClient.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(PeerClient.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(PeerClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
}
