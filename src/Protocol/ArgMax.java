/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Communication.ReceiverQueueHandler;
import Communication.SenderQueueHandler;
import TrustedInitializer.Triple;
import Utility.Logging;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * class to take care of multiplying all w[j,n] for each j = 0 to numberCount-1
 * @author keerthanaa
 */
class sequentialMultiplication implements Callable<Integer>{

    List<Integer> wRow;
    List<Triple> tishares;
    int startProtocolID, clientID, prime, oneShare;
    
    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;
    
    /**
     * tiShares size = numberCount-2
     * each row has numberCount-1 numbers and needs numberCount-2 triplets
     * 
     * @param row
     * @param tishares
     * @param clientID
     * @param prime
     * @param startProtocolID
     * @param oneShare
     * @param recQueues
     * @param sendQueues 
     */
    public sequentialMultiplication(List<Integer> row, List<Triple> tishares, int clientID, int prime,
            int startProtocolID, int oneShare, ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues, 
            ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues) {
        this.wRow = row;
        this.tishares = tishares;
        this.clientID = clientID;
        this.prime = prime;
        this.startProtocolID = startProtocolID;
        this.sendQueues = sendQueues;
        this.recQueues = recQueues;
        this.oneShare = oneShare;
    }
    
    /**
     * calls multiplication protocol sequentially with previous product and next value in the row
     * @return
     * @throws Exception 
     */
    @Override
    public Integer call() throws Exception{
        int product = wRow.get(0);        
        for(int i=0;i<wRow.size()-1;i++){
            if (!recQueues.containsKey(i+startProtocolID)) {
                BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                recQueues.put(i+startProtocolID, temp);
            }

            if (!sendQueues.containsKey(i+startProtocolID)) {
                BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                sendQueues.put(i+startProtocolID, temp2);
            }
            
            Multiplication multiplicationTask = new Multiplication(product, wRow.get(i+1), 
                    tishares.get(i), sendQueues.get(i+startProtocolID), recQueues.get(i+startProtocolID), 
                    clientID, prime, startProtocolID+i, oneShare);
            product = (int) multiplicationTask.call();
        }
        //System.out.println("returning "+product);
        return product;        
    }
}


/**
 *
 * @author keerthanaa
 */

public class ArgMax implements Callable<Integer[]>{
    
    List<List<Integer> > vShares;
    BlockingQueue<Message> commonReceiver;
    BlockingQueue<Message> commonSender;
    int prime,clientID,protocolID,oneShare;
    List<Triple> tiShares;
        
    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;
    ExecutorService sendqueueHandler;
    ExecutorService recvqueueHandler;
    int bitLength, numberCount;
    
    HashMap<Integer,ArrayList<Integer> > wIntermediate;
    Integer[] wOutput;
    
    /**
     * vShares - shares of all the numbers to be compared (k numbers)
     * each share vShare(j) contains l bits
     * 
     * @param vShares
     * @param tiShares
     * @param oneShare
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param prime
     * @param protocolID 
     */
    public ArgMax(List<List<Integer> > vShares, List<Triple> tiShares,
            int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID) {
        this.commonReceiver = receiverQueue;
        this.commonSender =  senderQueue;
        this.clientID = clientId;
        this.prime = prime;
        this.protocolID = protocolID;
        this.vShares = vShares;
        this.oneShare = oneShare;
        this.tiShares = tiShares;
        
        numberCount = vShares.size();
        bitLength = 0;
        for (List<Integer> row : vShares) {
            bitLength = Math.max(bitLength,row.size());
        }
        
        wIntermediate = new HashMap<>();
        for(int i=0;i<numberCount;i++){
            wIntermediate.put(i,new ArrayList<>());
        }
        
        wOutput = new Integer[numberCount];
        
        recQueues = new ConcurrentHashMap<>();
        sendQueues = new ConcurrentHashMap<>();
        
        sendqueueHandler = Executors.newSingleThreadExecutor();
        recvqueueHandler = Executors.newSingleThreadExecutor();
        
        sendqueueHandler.execute(new SenderQueueHandler(protocolID,commonSender,sendQueues));
        recvqueueHandler.execute(new ReceiverQueueHandler(commonReceiver, recQueues));
        
    }
    
    /**
     * Computes ArgMax by invoking comparison and multiplication protocols
     * @return
     * @throws Exception 
     */
    @Override
    public Integer[] call() throws Exception  {
        if (numberCount==1){
            wOutput[0]=1;
            return wOutput;
        }
        
        int tiIndex = computeComparisons();
        computeW(tiIndex);
        
        tearDownHandlers();
        return wOutput;
    }
    
    /**
     * each number in (1,..,k) to be compared with (k-1) other numbers
     * does multithreaded k*(k-1) comparisons and stores the results in wIntermediate
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private int computeComparisons() throws InterruptedException, ExecutionException{
        
        ExecutorService es = Executors.newFixedThreadPool(numberCount*numberCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        int tiIndex = 0;
        int tiCount = 3*bitLength - 3;
        for(int i=0;i<numberCount;i++){
            for(int j=0;j<numberCount;j++){
                if(i!=j){
                    int key = (i*numberCount)+j;
                    if(!recQueues.containsKey(key)) {
                        BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                        recQueues.put(key, temp);
                    }
                    if(!sendQueues.containsKey(key)) {
                        BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                        sendQueues.put(key, temp2);
                    }
                    //Extract the required number of tiShares and pass it to comparison protocol
                    //each comparison needs 3(bitlength)-3 shares
                    List<Triple> tiComparsion = tiShares.subList(tiIndex, tiIndex+tiCount);
                    tiIndex += tiCount;
                    Comparison comparisonModule = new Comparison(vShares.get(i), vShares.get(j), tiComparsion, 
                            oneShare, sendQueues.get(key), recQueues.get(key), clientID, prime, key);
                    Future<Integer> comparisonTask = es.submit(comparisonModule);
                    taskList.add(comparisonTask);
                }
            }
        }
        
        es.shutdown();
        
        for(int i=0;i<numberCount*(numberCount-1);i++){
            Future<Integer> w_temp = taskList.get(i);
            int key = i/(numberCount-1);
            wIntermediate.get(key).add(w_temp.get());
        }
        
        es.shutdownNow();
        
        for(int i=0;i<numberCount;i++){
            System.out.println("w["+Integer.toString(i)+"]:"+ wIntermediate.get(i));
        } 
        
        return tiIndex;
    }
    
    /**
     * Create numberCount threads to multiply all w values for each entry in HashMap
     * store the result in wOutput
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void computeW(int tiIndex) throws InterruptedException, ExecutionException{
        ExecutorService es = Executors.newFixedThreadPool(numberCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        
        //Each row has n-2 sequential multiplications to do for n-1 numbers
        int tiCount = numberCount-2;
        int startProtocolID = numberCount*numberCount + 1;
        
        for(int i=0;i<numberCount;i++){
            List<Triple> tishares = tiShares.subList(tiIndex, tiIndex+tiCount);
            tiIndex+=tiCount;
            sequentialMultiplication rowMultiplication = new sequentialMultiplication(wIntermediate.get(i),tishares,
                    clientID, prime, startProtocolID, oneShare, recQueues, sendQueues);
            Future<Integer> wRowProduct = es.submit(rowMultiplication);
            taskList.add(wRowProduct);
            startProtocolID+=numberCount-2;
        }

        for(int i=0;i<numberCount;i++){
            Future<Integer> w_temp = taskList.get(i);
            wOutput[i]=w_temp.get();
        }
        
        es.shutdownNow();
    }
    
    /**
     * shut send and receive queue handlers
     */
    private void tearDownHandlers() {
        recvqueueHandler.shutdownNow();
        sendqueueHandler.shutdownNow();
    }
    
}
