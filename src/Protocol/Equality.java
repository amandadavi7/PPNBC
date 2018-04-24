/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.Triple;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

/**
 * This programs takes two integers and checks if they are equal or not
 * return a nonzero number if equal and 0 if not
 * @author keerthanaa
 */
public class Equality extends CompositeProtocol implements Callable<Integer> {
    
    public Equality(int x, int y, List<Triple> tiShares,
            int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID) {
        
        super(protocolID, senderQueue, receiverQueue, clientId, prime);
        
    }
    
    
    @Override
    public Integer call() throws Exception {
        return 0;
    }
    
}
