/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.BitDecomposition;
import Protocol.Comparison;
import Protocol.CompositeProtocol;
import Protocol.Multiplication;
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
    //List<Integer> firstTrainShare;
    //List<Integer> secondTrainShare;
    List<Integer> testShare;
    List<Triple> decimalTiShares;
    //List<Triple> binaryTiShares;
    //int bitLength;
   

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
        //this.binaryTiShares = binaryTiShares;
        //bitLength = firstTrainShare.size(); 
        
//        System.out.println("First train share: " + firstTrainShare);
//        System.out.println("Second train share: " + secondTrainShare);
//        System.out.println("Test share: " + testShare);
    }
    
    @Override
    public List<List<Integer>> call() throws Exception {
        
       startHandlers();
       List<List<Integer>> result = new ArrayList<>();
       //int result = -1;
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
       
       // or module for train 1 - test distance 
//       initQueueMap(recQueues, sendQueues, startpid);
//       OR_XOR orModule = new OR_XOR(firstTrainShare, testShare
//                                    ,tiShares, oneShare, 1,
//                        sendQueues.get(startpid), recQueues.get(startpid), clientID, prime, startpid);
//       startpid++;
//       
//       //xor module for train 1 - test distance
//       initQueueMap(recQueues, sendQueues, startpid);
//       OR_XOR xorModule = new OR_XOR(firstTrainShare, testShare, 
//                                     tiShares, oneShare, 2,
//                                    sendQueues.get(startpid), 
//                                    recQueues.get(startpid), 
//                                    clientID, prime, startpid);
//       startpid++;
//       
//       // or module for train 2 - test distance
//       initQueueMap(recQueues, sendQueues, startpid);
//       OR_XOR orModule2 = new OR_XOR(secondTrainShare, testShare
//                                    ,tiShares, oneShare, 1,
//                                     sendQueues.get(startpid), 
//                                     recQueues.get(startpid), clientID,
//                                     prime, startpid);
//       startpid++;
//       
//       //xor module for train 2 - test distance
//       initQueueMap(recQueues, sendQueues, startpid);
//       OR_XOR xorModule2 = new OR_XOR(secondTrainShare, testShare, 
//                                     tiShares, oneShare, 2,
//                                     sendQueues.get(startpid), recQueues.get(startpid),
//                                     clientID, prime, startpid);
//       startpid++;
//       
//       //Submit all tasks for execution
//       Future<Integer[]> orTask = es.submit(orModule);
//       Future<Integer[]> xorTask = es.submit(xorModule);
//       Future<Integer[]> orTask2 = es.submit(orModule2);
//       Future<Integer[]> xorTask2 = es.submit(xorModule2);

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
//                    Integer[] orResult = orTask.get();
//                    Integer[] xorResult = xorTask.get();    
//                    Integer[] orResult2 = orTask2.get();
//                    Integer[] xorResult2 = xorTask2.get();
//                  
//                    System.out.println("or 1");
//                    printScoreList(orResult);
//                    
//                    System.out.println("xor 1");
//                    printScoreList(xorResult);
//                    
//                    System.out.println("or 2");
//                    printScoreList(orResult2);
//                    
//                    System.out.println("xor 2");
//                    printScoreList(xorResult2);
//
//                    int orScore = getScoreFromList(orResult);
//                    int xorScore = getScoreFromList(xorResult);         
//                    int orScore2 = getScoreFromList(orResult2);
//                    int xorScore2 = getScoreFromList(xorResult2);
//                    
//                    System.out.println("or Score:" + orScore);
//                    System.out.println("xor Score:" + xorScore);
//                    System.out.println("or2 Score:" + orScore2);
//                    System.out.println("xor2 Score:" + xorScore2);
//                    
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
    
//    public int crossMultiplyAndCompare(int orScore, int xorScore, int orScore2,
//                                       int xorScore2, int startpid){
//        int result = -1;
//        
//        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
//        
//        // Multiplication 1 -> XOR1a, OR2a AND XOR1b, OR2b
//        //TODO check prime here for multiplication, Check startpid/protocolID here
//        System.out.println("Cross Multiplication!");
//        
//        initQueueMap(recQueues, sendQueues, startpid);
//        Multiplication multiplicationModule = new Multiplication(xorScore,
//                    orScore2, tiShares.get(startpid),
//                    sendQueues.get(startpid), recQueues.get(startpid), clientID,
//                    Constants.prime, startpid, oneShare,protocolId);
//        Future<Integer> firstCrossMultiplication = es.submit(multiplicationModule);
//        startpid++;
//        
//        initQueueMap(recQueues, sendQueues, startpid);
//        Multiplication multiplicationModule2 = new Multiplication(xorScore2,
//                    orScore, tiShares.get(startpid),
//                    sendQueues.get(startpid), recQueues.get(startpid), clientID,
//                    Constants.prime, startpid, oneShare, protocolId);
//        
//        Future<Integer> secondCrossMultiplication = es.submit(multiplicationModule2);
//        startpid++; 
//        es.shutdown();
//        
//        try {
//                int firstDistance = firstCrossMultiplication.get();
//                int secondDistance = secondCrossMultiplication.get();
//                
//                System.out.println("First distance share: " + firstDistance);
//                System.out.println("Second distance share: " + secondDistance);
//                // TODO: Add comparison here
//                
//                List<List<Integer>> bitShares = getBitShares(firstDistance,secondDistance,startpid);
//                
//                result = getComparsionResult(bitShares, startpid);
//                   
//            } catch (InterruptedException ex) {
//                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (ExecutionException ex) {
//                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        return result;
//    }
//    
//    public List<List<Integer>> getBitShares(int firstShare, int secondShare, int startpid){
//        List<List<Integer>> results = new ArrayList<>();
//        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
//       
//        initQueueMap(recQueues, sendQueues, startpid);
//        BitDecomposition firstTask = new BitDecomposition(firstShare,binaryTiShares,
//                                        oneShare, Constants.bitLength, sendQueues.get(startpid), 
//                                        recQueues.get(startpid), clientID, Constants.binaryPrime, startpid);
//        startpid++;
//        initQueueMap(recQueues, sendQueues, startpid);
//        BitDecomposition secondTask = new BitDecomposition(secondShare,binaryTiShares,
//                                        oneShare, Constants.bitLength, sendQueues.get(startpid), 
//                                        recQueues.get(startpid), clientID, Constants.binaryPrime, startpid);
//     
//        //Submit all tasks for execution
//       Future<List<Integer>> firstBitResult = es.submit(firstTask);
//       Future<List<Integer>>  secondBitResult= es.submit(secondTask);
//       es.shutdown();
//       
//       try{
//           List<Integer> firstBitShare = firstBitResult.get();
//           List<Integer> secondBitShare = secondBitResult.get();
//
//           results.add(firstBitShare);
//           results.add(secondBitShare);
//           System.out.println("Firstbitshare: " + results.get(0));
//           System.out.println("Secondbitshare: " + results.get(1));
//           
//           
//        }catch (InterruptedException ex) {
//                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
//        }catch (ExecutionException ex) {
//                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
//        }
//       
//        return results;
//    }
//    
//    public int getComparsionResult(List<List<Integer>> bitShares, int startpid){
//        
//        int comparisonResult = -1;
//        
//        ExecutorService es = Executors.newSingleThreadExecutor();
//        
//        initQueueMap(recQueues, sendQueues, startpid);
//        
//        Comparison comparisonModule = new Comparison(bitShares.get(0),
//                                    bitShares.get(1), binaryTiShares, oneShare,
//                                    sendQueues.get(startpid), recQueues.get(startpid), clientID, 
//                                    Constants.binaryPrime, startpid);
//        
//        Future<Integer> comparisonTask = es.submit(comparisonModule);
//        es.shutdown();
//        
//        try{
//                comparisonResult = comparisonTask.get();
//        }catch (InterruptedException ex) {
//                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
//        }catch (ExecutionException ex) {
//                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
//        }
//       
//        
//        return comparisonResult;
//    }
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
