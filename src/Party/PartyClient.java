/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import Communication.Message;
import Communication.ProtocolMessage;
import Utility.Connection;
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
public class PartyClient implements Runnable{
    
    BlockingQueue<Message> receiverQueue;
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
    public PartyClient(BlockingQueue<Message> queue, String ip, int port){
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
                            Message msg = (Message) iStream.readObject();
                            System.out.println("recieved value from receiver queue:"+msg.getValue());
                            receiverQueue.put(msg);
                        } else {
                            break;
                        }
                    }
            } catch (IOException ex)  {
                Logger.getLogger(PartyClient.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(PartyClient.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(PartyClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
}
