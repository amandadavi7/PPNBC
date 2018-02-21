/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

import java.io.Serializable;

/**
 * The interface encapsulates the DataMessage and the ProtocolMessage 
 * from the communication layers
 * @author anisha
 */
public interface Message {
    public Object getValue();
}
