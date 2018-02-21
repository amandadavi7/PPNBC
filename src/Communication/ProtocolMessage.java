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
    DataMessage value;
    
    /**
     * Constructor
     * 
     * @param protocolID
     * @param value 
     */
    public ProtocolMessage (int protocolID, DataMessage value){
        this.protocolID = protocolID;
        this.value = value;        
    }
    
    /**
     * get protocol ID
     * 
     * @return 
     */
    public int getProtocolID() {
        return protocolID;
    }

    public DataMessage getDataMessage(){
        return value;        
    }

   
    /**
     * Log values for the message
     */
    public void log() {
        System.out.println("Message: Protocol ID-" + protocolID + ", Value-" + value);
    }

    /**
     * Return the value field of the protocol
     * @return 
     */
    @Override
    public Object getValue() {
        return getDataMessage();
    }

    
}
