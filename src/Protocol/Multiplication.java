/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.DataMessage;
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
 * The protocol computes the multiplication of shares of x and y
 * and returns the share of the product
 * @author anisha
 */
public class Multiplication extends Protocol implements Callable {

    private BlockingQueue<Message> senderQueue;
    private BlockingQueue<Message> receiverQueue;
    int x;
    int y;
    Triple tiShares;
    int clientID;
    int prime;
    int protocolID;
    int oneShare;
    int parentID;

    /**
     * Constructor
     * @param x share of x
     * @param y share of y
     * @param tiShares
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param oneShare 
     */
    public Multiplication(int x, int y, Triple tiShares, 
            BlockingQueue<Message> senderQueue, 
            BlockingQueue<Message> receiverQueue, int clientId, int prime, 
            int protocolID, int oneShare, int parentID) {

        this.x = x;
        this.y = y;
        this.tiShares = tiShares;
        this.senderQueue = senderQueue;
        this.receiverQueue = receiverQueue;
        this.clientID = clientId;
        this.prime = prime;
        this.protocolID = protocolID;
        this.oneShare = oneShare;
        this.parentID = parentID;
        
    }

    /**
     * Waits for the shares of (x-u) and (y-v), computes the product and returns
     * the value
     * @return shares of product
     * @throws Exception 
     */
    @Override
    public Object call() throws Exception {
        initProtocol();
        System.out.println("Waiting for receiver. parentID="+parentID+" mult ID=" + protocolID);
        Message receivedMessage = receiverQueue.take();
        List<Integer> diffList = (List<Integer>) receivedMessage.getValue();
        
        int d = Math.floorMod((x - tiShares.u) + diffList.get(0), prime);
        int e = Math.floorMod((y - tiShares.v) + diffList.get(1), prime);
        int product = tiShares.w + (d * tiShares.v) + (tiShares.u * e) + (d * e * oneShare);
        product = Math.floorMod(product, prime);
        //System.out.println("ti("+tiShares.u+","+tiShares.v+","+tiShares.w+"), "+"x*y("+x+","+y+"):"+product);
        System.out.println("parent ID="+parentID+" mult ID="+protocolID+" successful, product returned");
        return product;

    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    private void initProtocol() {
        List<Integer> diffList = new ArrayList<>();
        diffList.add(Math.floorMod(x - tiShares.u, prime));
        diffList.add(Math.floorMod(y - tiShares.v, prime));

        Message senderMessage = new DataMessage(Constants.localShares, diffList,
                clientID, protocolID);
        try {
            senderQueue.put(senderMessage);
            //System.out.println("sending message for protocol id:"+ protocolID);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Logger.getLogger(Multiplication.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

}
