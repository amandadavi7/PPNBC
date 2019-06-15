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
    public List<TruncationPair> truncationPair;
    public List<Integer> equalityShares;
    public List<List<List<Integer>>> rowShares;
    public List<List<Integer>> colShares;
    public List<Integer> wholeNumShares;

    /**
     * Constructor
     */
    public TIShare() {
        decimalShares = new LinkedList<>();
        binaryShares = new LinkedList<>();
        bigIntShares = new LinkedList<>();
        truncationPair = new LinkedList<>();
        equalityShares = new LinkedList<>();
        rowShares = new LinkedList<>();
        colShares = new LinkedList<>();
        wholeNumShares = new LinkedList<>();
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
     * Add TripleReal object to bigIntShares
     *
     * @param t
     */
    public void addBigInt(TripleReal t) {
        bigIntShares.add(t);
    }

    /**
     * add Truncation pair object to real shares
     *
     * @param t
     */
    public void addTruncationPair(TruncationPair t) {
        truncationPair.add(t);
    }

    public void addEqualityShare(int i) {
        equalityShares.add(i);
    }

    public void addRowShare(int i, int num, int treeNum) {
        if(rowShares.size() == treeNum) {
            List<List<Integer>> currRowShare = new LinkedList<>();
            rowShares.add(currRowShare);
        }
        if (rowShares.get(treeNum).size() == i) {
            List<Integer> currShare = new LinkedList<>();
            rowShares.get(treeNum).add(currShare);
        }
        rowShares.get(treeNum).get(i).add(num);
    }

    public void addColShare(int i, int currValue) {
        if (colShares.size() == i) {
            List<Integer> currShare = new LinkedList<>();
            colShares.add(currShare);
        }
        colShares.get(i).add(currValue);
    }

    void addWholeNumShare(int currValue) {
        wholeNumShares.add(currValue);
    }
}
