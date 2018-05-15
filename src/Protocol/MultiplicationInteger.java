/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The protocol computes the multiplication of shares of x and y and returns the
 * share of the product
 *
 * @author anisha
 */
public class MultiplicationInteger extends Protocol implements Callable {

    int x;
    int y;
    TripleInteger tiShares;
    int parentID;
    int prime;

    /**
     * Constructor
     *
     * @param x share of x
     * @param y share of y
     * @param tiShares
     * @param senderQueue
     * @param receiverQueue
     * @param protocolIdQueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param oneShare
     * @param parentID
     */
    public MultiplicationInteger(int x, int y, TripleInteger tiShares,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientId, int prime,
            int protocolID, int oneShare, int parentID) {

        super(protocolID, senderQueue, receiverQueue, protocolIdQueue,
                clientId, oneShare);
        this.x = x;
        this.y = y;
        this.tiShares = tiShares;
        this.parentID = parentID;
        this.prime = prime;
    }

    /**
     * Waits for the shares of (x-u) and (y-v), computes the product and returns
     * the value
     *
     * @return shares of product
     */
    @Override
    public Object call() {
        initProtocol();
        //System.out.println("Waiting for receiver. parentID=" + parentID + " mult ID=" + protocolID);
        Message receivedMessage = null;
        List<Integer> diffList = null;
        try {
            receivedMessage = receiverQueue.take();
            diffList = (List<Integer>) receivedMessage.getValue();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        int d = Math.floorMod((x - tiShares.u) + diffList.get(0), prime);
        int e = Math.floorMod((y - tiShares.v) + diffList.get(1), prime);
        int product = tiShares.w + (d * tiShares.v) + (tiShares.u * e) + (d * e * oneShare);
        product = Math.floorMod(product, prime);
        //System.out.println("ti("+tiShares.u+","+tiShares.v+","+tiShares.w+"), "+"x*y("+x+","+y+"):"+product);
        //System.out.println("parent ID=" + parentID + " mult ID=" + protocolID + " successful, product returned");
        return product;

    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    private void initProtocol() {
        List<Integer> diffList = new ArrayList<>();
        diffList.add(Math.floorMod(x - tiShares.u, prime));
        diffList.add(Math.floorMod(y - tiShares.v, prime));

        Message senderMessage = new Message(Constants.localShares, diffList,
                clientID, protocolIdQueue);
        try {
            senderQueue.put(senderMessage);
            //System.out.println("sending message for protocol id:"+ protocolID);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Logger.getLogger(MultiplicationInteger.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}