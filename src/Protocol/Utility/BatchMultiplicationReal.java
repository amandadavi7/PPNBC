/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.Multiplication;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.math.BigInteger;
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
public class BatchMultiplicationReal extends BatchMultiplication 
        implements Callable<BigInteger[]> {
    
    List<BigInteger> x;
    List<BigInteger> y;
    BigInteger prime;
    
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
    public BatchMultiplicationReal(List<BigInteger> x, List<BigInteger> y, 
            List<Triple> tiShares, 
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolQueue,
            int clientId, BigInteger prime,
            int protocolID, int oneShare, int parentID) {

        super(tiShares, senderQueue, receiverQueue, protocolQueue, clientId, protocolID, 
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
    public BigInteger[] call() throws Exception{
        
        int batchSize = x.size();
        BigInteger[] products = new BigInteger[batchSize];
        
        initProtocol();
        //System.out.println("Waiting for receiver. parentID=" + parentID + " mult ID=" + protocolID);
        Message receivedMessage = null;
        List<List<BigInteger>> diffList = null;
        try {
            receivedMessage = receiverQueue.take();
            diffList = (List<List<BigInteger>>) receivedMessage.getValue();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        
        for(int i=0;i<batchSize;i++){
            // TODO convert TI share to BigInteger
            BigInteger d = x.get(i)
                    .subtract(BigInteger.valueOf(tiShares.get(i).u))
                    .add(diffList.get(i).get(0)).mod(prime);
            BigInteger e = y.get(i)
                    .subtract(BigInteger.valueOf(tiShares.get(i).v))
                    .add(diffList.get(i).get(1)).mod(prime);
            
            BigInteger product = BigInteger.valueOf(tiShares.get(i).w)
                    .add(d.multiply(BigInteger.valueOf(tiShares.get(i).v)))
                    .add(e.multiply(BigInteger.valueOf(tiShares.get(i).u)))
                    .add(d.multiply(e).multiply(BigInteger.valueOf(oneShare)))
                    .mod(prime);
            
            products[i] = product;
        }

        return products;

    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    @Override
    void initProtocol() {
        List<List<BigInteger>> diffList = new ArrayList<>();
        int batchSize = x.size();
        
        for(int i=0;i<batchSize;i++){
            List<BigInteger> newRow = new ArrayList<>();
            newRow.add(x.get(i).subtract(BigInteger.valueOf(tiShares.get(i).u))
                    .mod(prime));
            newRow.add(y.get(i).subtract(BigInteger.valueOf(tiShares.get(i).v))
                    .mod(prime));
            diffList.add(newRow);
        }
        
        Message senderMessage = new Message(Constants.localShares, diffList,
                clientID, protocolQueue);
        
        try {
            senderQueue.put(senderMessage);
            //System.out.println("sending message for protocol id:"+ protocolId);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Logger.getLogger(Multiplication.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
}
