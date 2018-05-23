/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrustedInitializer;

import java.math.BigInteger;

/**
 *
 * @author anisha
 */
public class TruncationPair {
    public BigInteger r,rp;
    
    /**
     * Logs the Pair
     */
    public void log() {
        System.out.println("ti share: r:" + r + ", r':" + rp);
    }
}
