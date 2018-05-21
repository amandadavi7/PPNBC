/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrustedInitializer;

import java.io.Serializable;

/**
 *
 * @author keerthanaa
 */
public class TripleInteger implements Triple, Serializable{
    public int u,v,w;
    
    /**
     * Logs the Triplet
     */
    @Override
    public void log() {
        System.out.println("ti share: u:" + u +", v:"+ v +", w:" + w);
    }
    
}
