/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author keerthanaa
 */
public class PeerServer implements Runnable{
    
    ServerSocket socketServer;
    BlockingQueue<ProtocolMessage> senderQueue;

    /**
     * Constructor 
     * @param socketserver
     * @param queue 
     */
    public PeerServer(ServerSocket socketserver, BlockingQueue<ProtocolMessage> queue){
        this.socketServer = socketserver;
        this.senderQueue = queue;
    }
    
    /**
     * Continuously running thread that takes entries from senderqueue and 
     * send them to other parties
     */
    @Override
    public void run(){
        
        try {
            Socket clientSocket = socketServer.accept();
            System.out.println("Connected to:" + clientSocket);
            ObjectInputStream iStream = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream oStream = new ObjectOutputStream(clientSocket.getOutputStream());
            
            while(true){
                ProtocolMessage msg = senderQueue.take();
                oStream.writeObject(msg);
                oStream.flush();
                
            }
            
        } catch (IOException ex){
            Logger.getLogger(PeerServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex){
            Logger.getLogger(PeerServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
