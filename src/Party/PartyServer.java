/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import Communication.Message;
import java.io.IOException;
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
public class PartyServer implements Runnable{
    
    ServerSocket socketServer;
    BlockingQueue<Message> senderQueue;

    /**
     * Constructor 
     * @param socketserver
     * @param queue 
     */
    public PartyServer(ServerSocket socketserver, BlockingQueue<Message> queue){
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
            ObjectOutputStream oStream = new ObjectOutputStream(clientSocket.getOutputStream());
            
            while(true){
                Message msg = senderQueue.take();
                oStream.writeObject(msg);
                //oStream.flush();
                
            }
            
        } catch (IOException | InterruptedException ex){
            Logger.getLogger(PartyServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
