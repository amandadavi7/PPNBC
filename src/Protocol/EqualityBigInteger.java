/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.TripleBigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigInteger;
/**
 * This programs takes two integers and checks if they are equal or not
 * return a nonzero number if equal and 0 if not
	
 * @author keerthanaa
 */
public class EqualityBigInteger extends CompositeProtocol implements Callable<BigInteger> {
    
    BigInteger xShare, yShare, rShare, prime;
    TripleBigInteger bigIntTiShare;
    
    /**
     * 
     * @param xShare
     * @param yShare
     * @param rShare
     * @param tiShare
     * @param asymmetricBit
     * @param pidMapper
     * @param senderqueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param protocolIdQueue
     * @param partyCount 
     */
    public EqualityBigInteger(BigInteger xShare, BigInteger yShare, BigInteger rShare,
            TripleBigInteger tiShare, int asymmetricBit, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderqueue, int clientId, BigInteger prime,
            int protocolID, Queue<Integer> protocolIdQueue, int partyCount,int threadID) {
        
        super(protocolID, pidMapper, senderqueue, protocolIdQueue, clientId,
                asymmetricBit, partyCount,threadID);
        
        this.xShare = xShare;
        this.yShare = yShare;
        this.rShare = rShare;
        this.bigIntTiShare = tiShare;
        this.prime = prime;
        
    }
    
    /**
     * 
     * @return 
     * @throws java.lang.InterruptedException 
     */
    @Override
    public BigInteger call() throws InterruptedException {
        
        BigInteger diff = (xShare.subtract(yShare)).mod(prime);        
        List<BigInteger> diffList = new ArrayList<>();
        diffList.add((diff.subtract(bigIntTiShare.u)).mod(prime));
        diffList.add((rShare.subtract(bigIntTiShare.v)).mod(prime));

        Message senderMessage = new Message(diffList, clientID, protocolIdQueue);
        senderMessage.setThreadID(threadID);
        try {
            senderQueue.put(senderMessage);
        } catch (InterruptedException ex) {
            Logger.getLogger(Equality.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Message receivedMessage = null;
        diffList = null;
        receivedMessage = pidMapper.get(protocolIdQueue).take();
        diffList = (List<BigInteger>) receivedMessage.getValue();

        BigInteger d = (diff.subtract(bigIntTiShare.u).add(diffList.get(0))).mod(prime);
        BigInteger e = (rShare.subtract(bigIntTiShare.v).add(diffList.get(1))).mod(prime);
        BigInteger product = (bigIntTiShare.w
                                .add(d.multiply(bigIntTiShare.v))
                                .add(bigIntTiShare.u.multiply(e))
                                .add(d.multiply(e).multiply(asymmetricBit == 1 ? 
                                    BigInteger.ONE : BigInteger.ZERO)))
                                .mod(prime);
   
        return product;
    }
}
