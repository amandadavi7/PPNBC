/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.DataMessage;
import Communication.Message;
import Utility.Constants;
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

    // There is one queue each since there are only 2 parties
    private static BlockingQueue<Message> senderQueue;
    private static BlockingQueue<Message> receiverQueue;
    int x;
    int y;
    int u;
    int v;
    int w;
    int clientID;
    int prime;

    public Multiplication(int x, int y, int u, int v, int w,
            BlockingQueue<Message> senderQueue, int clientId, int prime) {
        this(x, y, u, v, w, senderQueue, new LinkedBlockingQueue<Message>(),
                clientId, prime);
    }

    public Multiplication(int x, int y, int u, int v, int w,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime) {

        this.x = x;
        this.y = y;
        this.u = u;
        this.v = v;
        this.w = w;
        this.senderQueue = senderQueue;
        this.receiverQueue = receiverQueue;
        this.clientID = clientId;
        initProtocol();

    }

    @Override
    public Object call() throws Exception {
        Message receivedMessage = receiverQueue.take();
        List<Integer> diffList = (List<Integer>) receivedMessage.getValue();

        int d = Math.floorMod((x - u) + diffList.get(0), prime);
        int e = Math.floorMod((y - v) + diffList.get(1), prime);
        int product = w + (d * v) + (u * e) + (d * e);
        product = Math.floorMod(product, Constants.prime);
        return product;

    }

    private void initProtocol() {
        List<Integer> diffList = new ArrayList<>();
        diffList.add(Math.floorMod(x - u, prime));
        diffList.add(Math.floorMod(y - v, prime));

        Message senderMessage = new DataMessage(Constants.localShares, diffList,
                clientID);

    }

}
