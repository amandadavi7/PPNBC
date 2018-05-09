/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Takes a feature vector, k (index (0 index) of the feature that needs to be selected and returns shares of xk
 * asymmetric computation one party sends k=-1 and feature vector, another party sends k and featurevector = null
 * @author keerthanaa
 */
public class OIS extends CompositeProtocol implements Callable<Integer[]>{
    List<List<Integer>> featureVectorTransposed;
    List<Integer> yShares;
    List<Triple> tiShares;
    int numberCount,bitLength, prime;
    
    /**
     * Constructor
     * 
     * @param features
     * @param tiShares
     * @param oneShare
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param bitLength
     * @param k
     * @param numberCount 
     */
    public OIS(List<List<Integer>> features, List<Triple> tiShares,
            int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID, int bitLength, int k, int numberCount){
        
        super(protocolID, senderQueue, receiverQueue, clientId, oneShare);
        this.numberCount = numberCount;
        this.bitLength = bitLength;
        this.tiShares = tiShares;
        this.prime = prime;
        
        
        featureVectorTransposed = new ArrayList<>();
        if(features==null) {
            //System.out.println("features is null");
            for(int i=0;i<bitLength;i++){
                List<Integer> temp = new ArrayList<>();
                for(int j=0;j<numberCount;j++){
                    temp.add(0);
                }
                featureVectorTransposed.add(temp);
            }
        } else {
            //System.out.println("features is not null");
            for(int i=0;i<bitLength;i++){
                featureVectorTransposed.add(new ArrayList<>());
            }
            for(int j=0;j<numberCount;j++){
                for(int i=0;i<bitLength;i++){
                    featureVectorTransposed.get(i).add(features.get(j).get(i));
                }
            }
            
        }
        
        yShares = new ArrayList<>(Collections.nCopies(numberCount, 0));
        if(k!=-1){
            //System.out.println("setting 1 for "+k);
            yShares.set(k, 1);
        }
    }
    
    /**
     * for each bit, call dot product between all the nth bits of the features and yshares vector
     * @return
     * @throws Exception 
     */
    @Override
    public Integer[] call() throws Exception {
        //System.out.println("numcount "+numberCount+" bitlen "+bitLength);
        Integer[] output = new Integer[bitLength];
        //System.out.println("yshares is "+yShares+" featuretransposed " +featureVectorTransposed);
        
        startHandlers();
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        int tiStartIndex = 0;
        
        for(int i=0;i<bitLength;i++){
            
            initQueueMap(recQueues, sendQueues, i);
            
            //System.out.println("parentID-"+ protocolId +", dp with pid "+ i + " - " +featureVectorTransposed.get(i)+" and "+yShares);
            
            DotProductNumber dp = new DotProductNumber(featureVectorTransposed.get(i), yShares, 
                    tiShares.subList(tiStartIndex, tiStartIndex+numberCount), sendQueues.get(i), recQueues.get(i), 
                    clientID, prime, i, oneShare);
            
            Future<Integer> dpTask = es.submit(dp);
            taskList.add(dpTask);
            
            tiStartIndex += numberCount;
        }
        
        es.shutdown();
        
        for(int i=0;i<bitLength;i++){
            Future<Integer> dotprod = taskList.get(i);
            output[i] = dotprod.get();
        }
        
        tearDownHandlers();
        System.out.println("OIS PID: "+protocolId+"-returning " + Arrays.toString(output));
        return output;
    }
    
}
