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
public class KNN extends Model {
    
    List<List<Integer>> trainingShares;
    List<Integer> testShare;
    List<Integer> classLabels;
    
    
    public KNN(int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, List<Triple> binaryTriples, 
            List<Triple> decimalTriples, List<List<Integer>> trainingShares, List<Integer> testShare, 
            List<Integer> classLabels
            ) {
        
        super(senderQueue, receiverQueue, clientId, oneShare, binaryTriples, decimalTriples);
        this.trainingShares = trainingShares;
        this.testShare = testShare;
        this.classLabels = classLabels;
    }
    
    int KNN_Model(){
        //Jaccard Computation
        return 0;
    }
    
}
