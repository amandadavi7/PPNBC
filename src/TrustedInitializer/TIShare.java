/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrustedInitializer;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author keerthanaa
 */
public class TIShare implements Serializable {
    public List<Triple> decimalShares,binaryShares;
    
    public TIShare(){
        decimalShares = new LinkedList<Triple>();
        binaryShares = new LinkedList<Triple>();
    }
    
    /**
     * add Triple object to decimalshares
     * @param t 
     */
    public void addDecimal(Triple t){
        decimalShares.add(t);
    }
    
    /**
     * add Triple object to binary shares
     * @param t 
     */
    public void addBinary(Triple t){
        binaryShares.add(t);
    }
}
