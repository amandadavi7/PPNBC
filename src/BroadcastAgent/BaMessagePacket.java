/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BroadcastAgent;

import Communication.Message;

/**
 * Bundle the Message and the client Id together for BA
 * @author anisha
 */
public class BaMessagePacket {
    Message message;
    int clientId;
    
    /**
     * Constructor 
     * @param message
     * @param clientId 
     */
    public BaMessagePacket(Message message, int clientId) {
        this.message = message;
        this.clientId = clientId;
    }
}
