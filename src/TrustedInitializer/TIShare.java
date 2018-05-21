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
    public List<TripleInteger> decimalShares;
    public List<TripleByte> binaryShares;
    public List<TripleReal> bigIntShares;
    
    public TIShare(){
        decimalShares = new LinkedList<>();
        binaryShares = new LinkedList<>();
        bigIntShares = new LinkedList<>();
    }
    
    /**
     * add Triple object to decimalshares
     * @param t 
     */
    public void addDecimal(TripleInteger t){
        decimalShares.add(t);
    }
    
    /**
     * add Triple object to binary shares
     * @param t 
     */
    public void addBinary(TripleByte t){
        binaryShares.add(t);
    }
    
    public void addBigInt(TripleReal t){
        bigIntShares.add(t);
    }
}
