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
import Utility.Logging;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author anisha
 */
public class Multiplication implements Callable {

    private static BlockingQueue<Message> senderQueue;
    private static BlockingQueue<Message> receiverQueue;
    int x;
    int y;
    Triple tiShares;
    int clientID;
    int prime;
    int protocolID;

//    public Multiplication(int x, int y, int u, int v, int w,
//            BlockingQueue<Message> senderQueue, int clientId, int prime) {
//        this(x, y, u, v, w, senderQueue, new LinkedBlockingQueue<Message>(),
//                clientId, prime);
//    }

    public Multiplication(int x, int y, Triple tiShares,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime, int protocolID) {

        this.x = x;
        this.y = y;
        this.prime = prime;
        this.tiShares = tiShares;
        this.senderQueue = senderQueue;
        this.receiverQueue = receiverQueue;
        this.clientID = clientId;
        this.protocolID = protocolID;
        
        Logging.logValue("x", x);
        Logging.logValue("y", y);
        tiShares.log();
        System.out.println("prime:"+prime);
        
        initProtocol();

    }

    @Override
    public Object call() throws Exception {
        System.out.println("Waiting for receiver." + protocolID);
        Message receivedMessage = receiverQueue.take();
        List<Integer> diffList = (List<Integer>) receivedMessage.getValue();

        int d = Math.floorMod((x - tiShares.u) + diffList.get(0), prime);
        int e = Math.floorMod((y - tiShares.v) + diffList.get(1), prime);
        int product = tiShares.w + (d * tiShares.v) + (tiShares.u * e) + (d * e);
        product = Math.floorMod(product, Constants.prime);
        System.out.println("protocol " + protocolID + " successful");
        return product;

    }

    private void initProtocol() {
        List<Integer> diffList = new ArrayList<>();
        diffList.add(Math.floorMod(x - tiShares.u, prime));
        diffList.add(Math.floorMod(y - tiShares.v, prime));

        Message senderMessage = new DataMessage(Constants.localShares, diffList,
                clientID,protocolID);
        senderQueue.add(senderMessage);
        
        System.out.println("sending msg:"+ senderMessage.getValue());

    }

}
