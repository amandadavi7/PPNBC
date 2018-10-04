/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.io.Serializable;
import java.util.Queue;

/**
 * The class wraps the data with headers for communication between parties
 *
 * @author anisha
 */
public class Message implements Serializable {

    Object value;
    int clientId;
    Queue<Integer> protocolIds;
    boolean isUnicast;

    /**
     * Constructor
     *
     * @param value
     * @param clientId
     * @param protocolIdQueue
     */
    public Message(Object value, int clientId,
            Queue<Integer> protocolIdQueue) {
        this.value = value;
        this.clientId = clientId;
        protocolIds = protocolIdQueue;
    }
    
    /**
     * Constructor
     *
     * @param value
     * @param clientId
     * @param protocolIdQueue
     */
    public Message(Object value, int clientId,
            Queue<Integer> protocolIdQueue, boolean isUnicast) {
        this.value = value;
        this.clientId = clientId;
        this.isUnicast = isUnicast;
        protocolIds = protocolIdQueue;
    }

    /**
     * Get protocol ID
     *
     * @return
     */
    public int getProtocolID() {
        return protocolIds.peek();
    }

    /**
     * Get protocol ID
     *
     * @return
     */
    public Queue<Integer> getProtocolIDs() {
        return protocolIds;
    }

    /**
     * remove the top protocol ID and return
     *
     * @return
     */
    public int pollProtocolID() {
        return protocolIds.poll();
    }

    /**
     * Add protocol ID to the stack
     *
     * @param pid
     */
    public void addProtocolID(int pid) {
        protocolIds.add(pid);
    }

    /**
     * Get variable value
     *
     * @return value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Get client id
     *
     * @return client id
     */
    public int getClientId() {
        return clientId;
    }
    
    /**
     * Get unicast bit
     *
     * @return unicast bit
     */
    public boolean isUnicast() {
        return isUnicast;
    }

    /**
     * Log values for the message
     */
    public void log() {
        System.out.println("Message: Value-" + value
                + ", clientId-" + clientId);
    }

}
