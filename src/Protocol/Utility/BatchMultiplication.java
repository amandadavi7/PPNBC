/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.CompositeProtocol;
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
public abstract class BatchMultiplication extends Protocol {

    List<Triple> tiShares;
    int parentID;

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
    public BatchMultiplication(List<Triple> tiShares,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolQueue,
            int clientId, 
            int protocolID, int oneShare, int parentID) {

        super(protocolID, senderQueue, receiverQueue, protocolQueue, clientId, oneShare);
        this.tiShares = tiShares;
        this.parentID = parentID;
    }

    /**
     * Bundle the d and e values and add to the sender queue
     */
    abstract void initProtocol();

}
