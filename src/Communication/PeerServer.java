/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import Communication.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anisha
 */
public class PeerServer implements Runnable {

    ServerSocket socketServer;

    BlockingQueue<Message> senderQueue;

    public PeerServer(ServerSocket socketServer) {
        this.socketServer = socketServer;
        //TODO initialize sender queue
        senderQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {

        try {
            Socket clientSocket = socketServer.accept();
            System.out.println("Connecting to:" + clientSocket);
            ObjectInputStream iStream;
            ObjectOutputStream oStream;

            oStream = new ObjectOutputStream(
                    clientSocket.getOutputStream());
            iStream = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                Message msg;

                //consuming messages and sending to peer
                msg = senderQueue.take();

                //System.out.println("Msg to send:");
                //msg.log();
                oStream.writeObject(msg);
                oStream.flush();
            }

        } catch (IOException ex) {
            Logger.getLogger(PeerServer.class.getName()).log(Level.SEVERE, null, ex);

        } catch (InterruptedException ex) {
            Logger.getLogger(PeerServer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
