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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author keerthanaa
 */
public class OIS extends CompositeProtocol implements Callable<Integer[]>{
    List<List<Integer>> featureVectorTransposed;
    List<Integer> yShares;
    List<Triple> tiShares;
    int numberCount,bitLength, oneShare;
    
    public OIS(List<List<Integer>> features, List<Triple> tiShares,
            int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID, int bitLength, int k, int numberCount){
        
        super(protocolID, senderQueue, receiverQueue, clientId, prime);
        this.numberCount = numberCount;
        this.bitLength = bitLength;
        this.oneShare = oneShare;
        this.tiShares = tiShares;
        
        
        featureVectorTransposed = new ArrayList<>();
        if(features==null) {
            System.out.println("features is null");
            for(int i=0;i<bitLength;i++){
                List<Integer> temp = new ArrayList<>();
                for(int j=0;j<numberCount;j++){
                    temp.add(0);
                }
                featureVectorTransposed.add(temp);
            }
        } else {
            System.out.println("features is not null");
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
            System.out.println("setting 1 for "+k);
            yShares.set(k, 1);
        }
    }
    
    /**
     * 
     * @return
     * @throws Exception 
     */
    @Override
    public Integer[] call() throws Exception {
        System.out.println("numcount "+numberCount+" bitlen "+bitLength);
        Integer[] output = new Integer[bitLength];
        System.out.println("yshares is "+yShares);
        
        startHandlers();
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        int tiStartIndex = 0;
        
        for(int i=0;i<bitLength;i++){
            
            initQueueMap(recQueues, sendQueues, i);
            
            System.out.println("calling dp between with pid "+ i + " - " +featureVectorTransposed.get(i)+" and "+yShares);
            
            DotProduct dp = new DotProduct(featureVectorTransposed.get(i), yShares, 
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
        System.out.println("returning " + Arrays.toString(output));
        return output;
    }
    
}
