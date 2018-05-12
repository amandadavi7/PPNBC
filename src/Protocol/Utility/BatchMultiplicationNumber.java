/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.Multiplication;
import Protocol.Protocol;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Batch multiplication of list of xshares with yshares
 * 
 * @author keerthanaa
 */
public class BatchMultiplicationNumber extends BatchMultiplication 
        implements Callable<Integer[]> {
    
    List<Integer> x;
    List<Integer> y;
    int prime;
    
    /**
     * Constructor
     *
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
    public BatchMultiplicationNumber(List<Integer> x, List<Integer> y, 
            List<Triple> tiShares, 
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientId, int prime,
            int protocolID, int oneShare, int parentID) {

        super(tiShares, senderQueue, receiverQueue, protocolIdQueue,clientId, protocolID, 
                oneShare, parentID);
        this.x = x;
        this.y = y;
        this.prime = prime;
    }

    /**
     * Waits for the shares of (x-u) and (y-v), computes the product and returns
     * the value
     *
     * @return shares of product
     * @throws Exception
     */
    @Override
    public Integer[] call() throws Exception{
        
        int batchSize = x.size();
        Integer[] products = new Integer[batchSize];
        
        initProtocol();
        //System.out.println("Waiting for receiver. parentID=" + parentID + " mult ID=" + protocolID);
        Message receivedMessage = null;
        List<List<Integer>> diffList = null;
        try {
            receivedMessage = receiverQueue.take();
            diffList = (List<List<Integer>>) receivedMessage.getValue();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        
        for(int i=0;i<batchSize;i++){
            int d = Math.floorMod((x.get(i) - tiShares.get(i).u) + diffList.get(i).get(0), prime);
            int e = Math.floorMod((y.get(i) - tiShares.get(i).v) + diffList.get(i).get(1), prime);
            int product = tiShares.get(i).w + (d * tiShares.get(i).v) + (tiShares.get(i).u * e) + (d * e * oneShare);
            product = Math.floorMod(product, prime);
            products[i] = product;
        }

        //System.out.println("parent ID=" + parentID + " mult ID=" + protocolID + " successful, product returned");
        return products;

    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    @Override
    void initProtocol() {
        List<List<Integer>> diffList = new ArrayList<>();
        int batchSize = x.size();
        
        for(int i=0;i<batchSize;i++){
            List<Integer> newRow = new ArrayList<>();
            newRow.add(Math.floorMod(x.get(i) - tiShares.get(i).u, prime));
            newRow.add(Math.floorMod(y.get(i) - tiShares.get(i).v, prime));
            diffList.add(newRow);
        }
        
        Message senderMessage = new Message(Constants.localShares, diffList,
                clientID, protocolIdQueue);
        
        try {
            senderQueue.put(senderMessage);
            //System.out.println("sending message for protocol id:"+ protocolID);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Logger.getLogger(Multiplication.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
}
