/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import TrustedInitializer.Triple;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author keerthanaa
 */
public class ID3 extends Model {
    
    int classLabelCount, attrCount, datasetSize, rowStartIndex, partyDataSize, classAttrIndex;
    int[][] dataset;
    int classLabelTransactionSplit[][];
    int transactionsVector[], attrVector[];
    
    public ID3(int oneShares, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, List<Triple> binaryTriples, List<Triple> decimalTriple, 
            List<Integer> equalityShares, int classesCount, int attrCount, int dbSize, int rowStartIndex, 
            int[][] dataset, int classAttrIndex) {
        
        super(senderQueue, receiverQueue, clientId, oneShares, binaryTriples, decimalTriple, equalityShares);
        this.classLabelCount = classesCount;
        this.attrCount = attrCount;
        this.datasetSize = dbSize;
        this.rowStartIndex = rowStartIndex;
        this.dataset = dataset;
        this.partyDataSize = dataset.length;
        this.classAttrIndex = classAttrIndex;
    }
    
    void init() {
        transactionsVector = new int[datasetSize];
        attrVector = new int[attrCount];
        classLabelTransactionSplit = new int[classLabelCount][datasetSize];
        for(int i=0;i<partyDataSize;i++) {
            transactionsVector[i+rowStartIndex] = 1;
        }
        for(int i=0;i<attrCount;i++) {
            attrVector[i] = 1;
        }
    }
    
    /*int findCommonClassIndex() {
        for(int i=0;i<classLabelCount;i++) {
            
        }
        
    }*/
    
    void ID3_Model() {
        
    }
    
    
}
