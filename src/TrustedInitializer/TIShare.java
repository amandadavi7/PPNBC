/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrustedInitializer;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.math.BigInteger;
/**
 *
 * @author keerthanaa
 */
public class TIShare implements Serializable {

    public List<TripleInteger> decimalShares;
    public List<TripleByte> binaryShares;
    public List<TripleBigInteger> realShares;
    public List<TruncationPair> truncationPair;
    public List<Integer> equalityShares;
    public List<BigInteger> bigIntEqualityShares;
    public List<TripleBigInteger> bigIntShares;
    /**
     * Constructor
     */    
    public TIShare(){
        decimalShares = new LinkedList<>();
        binaryShares = new LinkedList<>();
        realShares = new LinkedList<>();
        truncationPair = new LinkedList<>();
        equalityShares = new LinkedList<>();
        bigIntShares = new LinkedList<>();
        bigIntEqualityShares = new LinkedList<>();
    }

    /**
     * add TripleInteger object to decimalShares
     *
     * @param t
     */
    public void addDecimal(TripleInteger t) {
        decimalShares.add(t);
    }

    /**
     * add TripleByte object to binaryShares
     *
     * @param t
     */
    public void addBinary(TripleByte t) {
        binaryShares.add(t);
    }

    /**
     * Add TripleBigInteger object to realShares
     *
     * @param t
     */
    public void addReal(TripleBigInteger t) {
        realShares.add(t);
    }
    
    /**
     * add Truncation pair object to real shares
     * @param t 
     */
    public void addTruncationPair(TruncationPair t){
        truncationPair.add(t);
    }

    public void addBigInt(TripleBigInteger t) {
        bigIntShares.add(t);
    }
    
    public void addEqualityShare(int i) {
        equalityShares.add(i);
    }

    public void addBigIntegerEqualityShare(BigInteger i) {
            bigIntEqualityShares.add(i);
    }
}
