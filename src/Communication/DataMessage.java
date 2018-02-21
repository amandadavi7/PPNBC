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
public class DataMessage implements Serializable, Message {

    String variableName;
    Object value;
    int clientId;
    int protocolId;

    /**
     * Constructor
     *
     * @param name
     * @param value
     * @param clientId
     */
    public DataMessage(String name, Object value, int clientId, int protocolId) {
        variableName = name;
        this.value = value;
        this.clientId = clientId;
        this.protocolId = protocolId;
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
    public int getProtocolID(){
      return protocolId;  
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
