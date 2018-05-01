/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import TrustedInitializer.Triple;
import Utility.Constants;
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
public class JaccardDistance extends CompositeProtocol implements Callable<Integer>{
    
    List<Integer> firstTrainShare;
    List<Integer> secondTrainShare;
    List<Integer> testShare;
    int oneShare;
    List<Triple> tiShares;
    int bitLength;
   

    public JaccardDistance(List<Integer> firstTrainShare, List<Integer> secondTrainShare,
            List<Integer> testShare, int oneShare, List<Triple> tiShares, 
            int bitLength,
            BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID){
        
        super(protocolID, senderQueue, receiverQueue, clientId, prime);
        
        this.firstTrainShare = firstTrainShare;
        this.secondTrainShare = secondTrainShare;
        this.oneShare = oneShare;
        this.tiShares = tiShares;
        bitLength = firstTrainShare.size(); 
        
        System.out.println("First train share: " + firstTrainShare);
        System.out.println("Second train share: " + secondTrainShare);
        System.out.println("Test share: " + testShare);
    }
    
    @Override
    public Integer call() throws Exception {
        
       startHandlers();
       int result = -1;
       int startpid = 1;
       ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
       // TODO: Make OR_XOR module return both OR and XOR together
       
       // or module for train 1 - test distance 
       initQueueMap(recQueues, sendQueues, startpid);
       OR_XOR orModule = new OR_XOR(firstTrainShare, testShare
                                    ,tiShares, oneShare, 1,
                        sendQueues.get(startpid), recQueues.get(startpid), clientID, prime, startpid);
       startpid++;
       //xor module for train 1 - test distance
       initQueueMap(recQueues, sendQueues, startpid);
       OR_XOR xorModule = new OR_XOR(firstTrainShare, testShare, 
                                     tiShares, oneShare, 2,
                        sendQueues.get(startpid), recQueues.get(startpid), clientID, prime, startpid);
       startpid++;
       // or module for train 2 - test distance
       initQueueMap(recQueues, sendQueues, startpid);
       OR_XOR orModule2 = new OR_XOR(secondTrainShare, testShare
                                    ,tiShares, oneShare, 1,
                        sendQueues.get(startpid), recQueues.get(startpid), clientID, prime, startpid);
       startpid++;
       //xor module for train 2 - test distance
       initQueueMap(recQueues, sendQueues, startpid);
       OR_XOR xorModule2 = new OR_XOR(secondTrainShare, testShare, 
                                     tiShares, oneShare, 2,
                        sendQueues.get(startpid), recQueues.get(startpid), clientID, prime, startpid);
//         initQueueMap(recQueues, sendQueues, i);
//            OR_XOR or_xor = new OR_XOR(x.get(i), y.get(i), decimalTiShares, oneShares, 1, sendQueues.get(i), 
//                    recQueues.get(i), clientId, Constants.prime, i);
       
       Future<Integer[]> orTask = es.submit(orModule);
       Future<Integer[]> xorTask = es.submit(xorModule);
       Future<Integer[]> orTask2 = es.submit(orModule2);
       Future<Integer[]> xorTask2 = es.submit(xorModule2);

       es.shutdown();
       
            try {
                    Integer[] orResult = orTask.get();
                    Integer[] xorResult = xorTask.get();
                    
                    Integer[] orResult2 = orTask2.get();
                    Integer[] xorResult2 = xorTask2.get();
                    
                    System.out.println("or Result:" + orResult);
                    System.out.println("xor Result:" + xorResult);
                    System.out.println("or2 Result:" + orResult2);
                    System.out.println("xor2 Result:" + xorResult2);
//                    int orScore = getScoreFromList(orResult);
//                    int xorScore = getScoreFromList(xorResult);
//                    
//                    int orScore2 = getScoreFromList(orResult2);
//                    int xorScore2 = getScoreFromList(xorResult2);
                    
//                    result = crossMultiplyAndCompare(orScore, xorScore, orScore2, 
//                             xorScore2, startpid);
                    
            } catch (InterruptedException ex) {
                Logger.getLogger(JaccardDistance.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(JaccardDistance.class.getName()).log(Level.SEVERE, null, ex);
            }
       tearDownHandlers();
       return result;
    }
    
    public int crossMultiplyAndCompare(int orScore, int xorScore, int orScore2,
                                       int xorScore2, int startpid){
        int result = -1;
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        
        // Multiplication 1 -> XOR1a, OR2a AND XOR1b, OR2b
        //TODO check prime here for multiplication, Check startpid/protocolID here
        Multiplication multiplicationModule = new Multiplication(xorScore,
                    orScore2, tiShares.get(startpid),
                    sendQueues.get(startpid), recQueues.get(startpid), clientID,
                    Constants.prime, startpid, oneShare, protocolId);
        Future<Integer> firstCrossMultiplication = es.submit(multiplicationModule);
        startpid++;
        
        Multiplication multiplicationModule2 = new Multiplication(xorScore2,
                    orScore, tiShares.get(startpid),
                    sendQueues.get(startpid), recQueues.get(startpid), clientID,
                    Constants.prime, startpid, oneShare, protocolId);
        
        Future<Integer> secondCrossMultiplication = es.submit(multiplicationModule2);
         
        es.shutdown();
        
        try {
                int firstDistance = firstCrossMultiplication.get();
                int secondDistance = secondCrossMultiplication.get();
                
                System.out.println("First distance share: " + firstDistance);
                System.out.println("First distance share: " + secondDistance);
                //TODO Test existing code and Add comparison here
                
                
                
                    
            } catch (InterruptedException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            }
        return result;
    }
    
    public static int getScoreFromList(Integer[] scoreList){
        
        int sum = 0;
        for (int element: scoreList){
            
            sum  = sum + element;
        }
        
        return sum;
    }
//    public static void main(String[] args) {
//        
//        Integer[] scoreList = new Integer[3];
//        scoreList[0] = 0;
//        scoreList[1] = 1;
//        scoreList[2] = 1;
//        
//        int score = getScoreFromList(scoreList);
//        System.out.println("Score: " + score);
//    }
}
