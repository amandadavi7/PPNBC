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

    String variableName;
    Object value;
    int clientId;
    Queue<Integer> protocolIds;

    /**
     * Constructor
     *
     * @param name
     * @param value
     * @param clientId
     * @param protocolId
     */
    //TODO - variableName - obsolete, clientId??
    public Message(String name, Object value, int clientId,
            Queue<Integer> protocolQueue) {
        variableName = name;
        this.value = value;
        this.clientId = clientId;
        protocolIds = protocolQueue;
    }

    /**
     * Get variable name
     *
     * @return variable name
     */
    public String getName() {
        return variableName;
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
     * Log values for the message
     */
    public void log() {
        System.out.println("Message: Name-" + variableName + ", Value-" + value
                + ", clientId-" + clientId);
    }

}
