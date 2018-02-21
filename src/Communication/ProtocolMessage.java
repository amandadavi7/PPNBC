/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.io.Serializable;

/**
 *
 * @author keerthanaa
 */
public class ProtocolMessage implements Serializable, Message {
    int protocolID;
    Message value;
    
    /**
     * Constructor
     * 
     * @param protocolID
     * @param value 
     */
    public ProtocolMessage (int protocolID, Message value){
        this.protocolID = protocolID;
        this.value = value;        
    }
    
    /**
     * get protocol ID
     * 
     * @return 
     */
    @Override
    public int getProtocolID() {
        return protocolID;
    }

    public Message getDataMessage(){
        return value;        
    }

   
    /**
     * Log values for the message
     */
    public void log() {
        System.out.println("Message: Protocol ID-" + protocolID + ", Value-" + value);
    }

    /**
     * return value object
     * 
     * @return 
     */
    @Override
    public Object getValue() {
        return getDataMessage();
    }

    
}
