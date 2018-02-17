/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.io.Serializable;

/**
 * Message object would contain the variable, value for the client i for a func
 * f
 *
 * @author anisha
 */
public class Message implements Serializable {

    String variableName;
    Object value;
    int funId;
    int clientId;

    /**
     * Constructor
     *
     * @param name
     * @param value
     * @param funcId
     * @param clientId
     */
    public Message(String name, Object value, int funcId, int clientId) {
        variableName = name;
        this.value = value;
        this.funId = funcId;
        this.clientId = clientId;
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
     * Get variable value
     *
     * @return value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Get function id
     *
     * @return function id
     */
    public int getFunId() {
        return funId;
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
                + ", funId-" + funId + ", clientId-" + clientId);
    }
}
