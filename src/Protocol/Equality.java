/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.Triple;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This programs takes two integers and checks if they are equal or not
 * return a nonzero number if equal and 0 if not
 * @author keerthanaa
 */
public class Equality extends CompositeProtocol implements Callable<Integer> {
    int xShare, yShare, rShare, prime;
    TripleInteger decimalTiShare;
    
    public Equality(int xShare, int yShare, int rShare,
            TripleInteger tiShare, int asymmetricBit, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderqueue, int clientId, int prime,
            int protocolID, Queue<Integer> protocolIdQueue, int partyCount) {
        
        super(protocolID, pidMapper, senderqueue, protocolIdQueue, clientId,
                asymmetricBit, partyCount);
        
        this.xShare = xShare;
        this.yShare = yShare;
        this.rShare = rShare;
        this.decimalTiShare = tiShare;
        this.prime = prime;
        
    }
    
    
    @Override
    public Integer call() throws Exception {
        int diff = Math.floorMod(this.xShare - this.yShare, prime);
        
        List<Integer> diffList = new ArrayList<>();
        diffList.add(Math.floorMod(diff - decimalTiShare.u, prime));
        diffList.add(Math.floorMod(rShare - decimalTiShare.v, prime));

        Message senderMessage = new Message(diffList,
                clientID, protocolIdQueue);
        try {
            senderQueue.put(senderMessage);
            //System.out.println("sending message for protocol id:"+ protocolID);
        } catch (InterruptedException ex) {
            Logger.getLogger(Equality.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Message receivedMessage = null;
        diffList = null;
        try {
            receivedMessage = pidMapper.get(protocolIdQueue).take();
            diffList = (List<Integer>) receivedMessage.getValue();
        } catch (InterruptedException ex) {
            Logger.getLogger(Equality.class.getName()).log(Level.SEVERE, null, ex);
        }

        int d = Math.floorMod((diff - decimalTiShare.u) + diffList.get(0), prime);
        int e = Math.floorMod((rShare - decimalTiShare.v) + diffList.get(1), prime);
        int product = decimalTiShare.w + (d * decimalTiShare.v) + (decimalTiShare.u * e) + (d * e * asymmetricBit);
        product = Math.floorMod(product, prime);
        
        return product;
    }
    
}
