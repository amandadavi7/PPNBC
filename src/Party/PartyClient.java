/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import Communication.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author keerthanaa
 */
public class PartyClient implements Runnable {

    BlockingQueue<Message> receiverQueue;
    ObjectInputStream iStream = null;
    Socket receiveSocket = null;
    ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper;

    /**
     * Constructor
     * takes common receiver queue, socket and input stream object
     * @param queue
     * @param socket
     * @param iStream
     */
    public PartyClient(BlockingQueue<Message> queue, Socket socket,
            ObjectInputStream iStream) {
        this.receiverQueue = queue;
        this.receiveSocket = socket;
        this.iStream = iStream;
    }
    
    
    public PartyClient(ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            Socket socket, ObjectInputStream iStream) {
        this.pidMapper = pidMapper;
        this.receiveSocket = socket;
        this.iStream = iStream;
    }

    /**
     * receive message from BA and add to receiver queue
     */
    @Override
    public void run() {

        while (!(Thread.currentThread().isInterrupted())) {
            Message msgs;
            try {
                msgs = (Message) iStream.readObject();
                // TODO cleanup
                //receiverQueue.put(msgs);
                pidMapper.putIfAbsent(msgs.getProtocolIDs(), new LinkedBlockingQueue<>());
                pidMapper.get(msgs.getProtocolIDs()).add(msgs);
            } catch (IOException ex) {
                try {
                    iStream.close();
                } catch (IOException ex1) {
                    Logger.getLogger(PartyClient.class.getName()).log(Level.SEVERE, null, ex1);
                }
                break;
            } catch (ClassNotFoundException ex) {
                break;
            }

        }

    }
}
