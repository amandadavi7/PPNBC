/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.CompositeProtocol;
import Protocol.OR_XOR;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author bhagatsanchya
 */
public class JaccardDistance extends CompositeProtocol implements Callable<List<List<Integer>>>{
    
    List<List<Integer>> trainingShares;
    List<Integer> testShare;
    List<Triple> decimalTiShares;

    /**
     * 
     * @param trainingShares
     * @param testShare
     * @param oneShare
     * @param tiShares
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param prime
     * @param protocolID 
     */
    public JaccardDistance(List<List<Integer>> trainingShares,
            List<Integer> testShare, int oneShare, List<Triple> tiShares, 
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID){
        
        super(protocolID, senderQueue, receiverQueue, clientId, prime, oneShare);
        
        this.trainingShares = trainingShares;
        this.testShare = testShare;
        this.decimalTiShares = tiShares;
        //bitLength = firstTrainShare.size(); 
    }
    
    @Override
    public List<List<Integer>> call() throws Exception {
        
       startHandlers();
       List<List<Integer>> result = new ArrayList<>();
       int startpid = 0;
       int attrLength = testShare.size(), tiStartIndex = 0;
       ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
       
       // TODO: Make OR_XOR module return both OR and XOR together
       List<Future<Integer[]>> taskList = new ArrayList<>();
       
       for(int i=0;i<trainingShares.size();i++) {
            initQueueMap(recQueues, sendQueues, startpid);
            OR_XOR orModule = new OR_XOR(trainingShares.get(i), testShare, 
                    decimalTiShares.subList(tiStartIndex, tiStartIndex+attrLength), 
                    oneShare, 1, sendQueues.get(startpid), recQueues.get(startpid), 
                    clientID, prime, startpid);
            
            Future<Integer[]> orTask = es.submit(orModule);
            taskList.add(orTask);
            startpid++;
            tiStartIndex += attrLength;
            
            initQueueMap(recQueues, sendQueues, startpid);
            OR_XOR xorModule = new OR_XOR(trainingShares.get(i), testShare, 
                    decimalTiShares.subList(tiStartIndex, tiStartIndex+attrLength),
                    oneShare, 2, sendQueues.get(startpid), 
                    recQueues.get(startpid), clientID, prime, startpid);
            
            Future<Integer[]> xorTask = es.submit(xorModule);
            taskList.add(xorTask);
            startpid++;
            tiStartIndex += attrLength;
       }

       es.shutdown();
       
            try {
                    // These scores are additive shares over prime (Constants.prime)
                    // We eventually need sum of individual list over prime
                    for(int i=0;i<trainingShares.size();i++) {
                        Future<Integer[]> orTask = taskList.get(2*i);
                        Future<Integer[]> xorTask = taskList.get(2*i + 1);
                        
                        Integer[] orOutput = orTask.get();
                        Integer[] xorOutput = xorTask.get();
                        

                        //row stores or, xor outputs for a training example
                        List<Integer> row = new ArrayList<>();
                        row.add(getScoreFromList(orOutput));
                        row.add(getScoreFromList(xorOutput));
                        result.add(row);
                    }
                    
            } catch (InterruptedException ex) {
                Logger.getLogger(JaccardDistance.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(JaccardDistance.class.getName()).log(Level.SEVERE, null, ex);
            }
       tearDownHandlers();
       return result;
    }
    
    public static int getScoreFromList(Integer[] scoreList){
        
        int sum = 0;
        for (int element: scoreList){
            
            sum  = sum + element;
        }
        
        return Math.floorMod(sum, Constants.prime);
    }
    
    public static void printScoreList(Integer[] scoreList){
        
        for (int element: scoreList){
            
            System.out.println("["+element+"]");
        }
    }
}
