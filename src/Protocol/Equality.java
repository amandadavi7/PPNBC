/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This programs takes two integers and checks if they are equal or not
 * return a nonzero number if equal and 0 if not
 * @author keerthanaa
 */
public class Equality extends Protocol implements Callable<Integer> {
    int xShare, yShare, rShare, oneShare;
    Triple tiShare;
    
    public Equality(int xShare, int yShare, int rShare,
            Triple tiShare, int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID) {
        
        super(protocolID, senderQueue, receiverQueue, clientId, prime);
        
        this.xShare = xShare;
        this.yShare = yShare;
        this.rShare = rShare;
        this.tiShare = tiShare;
        this.oneShare = oneShare;
        
    }
    
    
    @Override
    public Integer call() throws Exception {
        int diff = Math.floorMod(this.xShare - this.yShare, prime);
        
        //To be verified.......
        //Repetition of multiplication.......It does not make sense to create an 
        //unnecessary thread here by calling a multiplication protocol (will create concurrent hashmap etc etc)
        
        List<Integer> diffList = new ArrayList<>();
        diffList.add(Math.floorMod(diff - tiShare.u, prime));
        diffList.add(Math.floorMod(rShare - tiShare.v, prime));

        Message senderMessage = new Message(Constants.localShares, diffList,
                clientID, protocolId);
        try {
            senderQueue.put(senderMessage);
            //System.out.println("sending message for protocol id:"+ protocolID);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Logger.getLogger(Multiplication.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Message receivedMessage = null;
        diffList = null;
        try {
            receivedMessage = receiverQueue.take();
            diffList = (List<Integer>) receivedMessage.getValue();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        int d = Math.floorMod((diff - tiShare.u) + diffList.get(0), prime);
        int e = Math.floorMod((rShare - tiShare.v) + diffList.get(1), prime);
        int product = tiShare.w + (d * tiShare.v) + (tiShare.u * e) + (d * e * oneShare);
        product = Math.floorMod(product, prime);
        
        return product;
    }
    
}
