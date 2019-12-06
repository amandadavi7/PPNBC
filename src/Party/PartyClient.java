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
import java.util.List;
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

    ObjectInputStream iStream = null;
    Socket receiveSocket = null;
    ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper;
    List<ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>>> pidMapperList=null;
    /**
     * Constructor
     * takes common receiver queue, socket and input stream object
     * @param pidMapper
     * @param socket
     * @param iStream
     */
    public PartyClient(ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            Socket socket, ObjectInputStream iStream) {
        this.pidMapper = pidMapper;
        this.receiveSocket = socket;
        this.iStream = iStream;
    }

    public PartyClient(List<ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>>> pidMapperList,
                       Socket socket, ObjectInputStream iStream) {
        this.pidMapperList = pidMapperList;
        this.receiveSocket = socket;
        this.iStream = iStream;
    }

    /**
     * receive message from other party and add to receiver queue
     */
    @Override
    public void run() {

        while (!(Thread.currentThread().isInterrupted())) {
            Message msgs;
            try {
                msgs = (Message) iStream.readObject();
                if(pidMapperList!=null){
                    ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> tempMapper = pidMapperList.get(msgs.getThreadID());
                    if (tempMapper.contains(msgs.getProtocolIDs())) {
                        System.out.print("Queue already contains protocol IDs:");
                        for (int x : msgs.getProtocolIDs()) {
                            System.out.print(" " + x);
                        }
                        System.out.println("");
                    }
                    tempMapper.putIfAbsent(msgs.getProtocolIDs(), new LinkedBlockingQueue<>());
                    tempMapper.get(msgs.getProtocolIDs()).add(msgs);
                }else{
                    pidMapper.putIfAbsent(msgs.getProtocolIDs(), new LinkedBlockingQueue<>());
                    pidMapper.get(msgs.getProtocolIDs()).add(msgs);
                }

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
