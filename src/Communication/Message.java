/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.io.Serializable;
import java.util.Stack;

/**
 * The interface encapsulates the DataMessage and the ProtocolMessage 
 * from the communication layers
 * @author anisha
 */
public class Message implements Serializable {
    String variableName;
    Object value;
    int clientId;
    Stack<Integer> protocolIds;

    /**
     * Constructor
     *
     * @param name
     * @param value
     * @param clientId
     * @param protocolId
     */
    public Message(String name, Object value, int clientId, int protocolId) {
        variableName = name;
        this.value = value;
        this.clientId = clientId;
        protocolIds = new Stack<>();
        protocolIds.push(protocolId);
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
        return protocolIds.peek();  
    }
    
    /**
     * remove the top protocol ID and return
     * 
     * @return 
     */
    public int popProtocolID(){
        return protocolIds.pop();
    }
    
    /**
     * Add protocol ID to the stack
     * 
     * @param pid 
     */
    public void addProtocolID(int pid){
        protocolIds.push(pid);
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
