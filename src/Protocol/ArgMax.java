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


class sequentialMultiplication implements Callable<Integer>{

    List<Integer> wRow;
    List<Triple> tishares;
    int startProtocolID, clientID, prime, oneShare;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;
    
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
    
    public Integer call() throws Exception{
        int product = 1;        
        for(int i=0;i<wRow.size();i++){
            if (!recQueues.containsKey(i+startProtocolID)) {
                BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                recQueues.put(i+startProtocolID, temp);
            }

            if (!sendQueues.containsKey(i+startProtocolID)) {
                BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                sendQueues.put(i+startProtocolID, temp2);
            }
            Multiplication multiplicationTask = new Multiplication(product, wRow.get(i), 
                    tishares.get(i), sendQueues.get(i+startProtocolID), recQueues.get(i+startProtocolID), 
                    clientID, prime, startProtocolID+i, oneShare);
            int tempProduct = (int) multiplicationTask.call();
            product*=tempProduct;
        }
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
        
        computeComparisons();
        computeW();
        
        tearDownHandlers();
        return wOutput;
    }
    
    /**
     * each number in (1,..,k) to be compared with (k-1) other numbers
     * does multithreaded k*(k-1) comparisons and stores the results in wIntermediate
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void computeComparisons() throws InterruptedException, ExecutionException{
        
        ExecutorService es = Executors.newFixedThreadPool(numberCount*numberCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        
        for(int i=0;i<numberCount;i++){
            for(int j=0;j<numberCount;j++){
                if(i!=j){
                    int key = (i*numberCount)+j;
                    if(!recQueues.containsKey(key)) {
                        BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                        recQueues.put((i*numberCount)+j, temp);
                    }
                    if(!sendQueues.containsKey(key)) {
                        BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                        sendQueues.put((i*numberCount)+j, temp2);
                    }
                    //Extract the required number of tiShares and pass it to comparison protocol
                    Comparison comparisonModule = new Comparison(vShares.get(i), vShares.get(j), tiShares, 
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
        
        for(int i=0;i<numberCount;i++){
            Logging.logShares("w["+Integer.toString(i)+"]", wIntermediate.get(i));
        } 
    }
    
    /**
     * Create numberCount threads to multiply all w values for each entry in HashMap
     * store the result in wOutput
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void computeW() throws InterruptedException, ExecutionException{
        ExecutorService es = Executors.newFixedThreadPool(numberCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        List<Triple> tishares = null;
        int startProtocolID = numberCount*numberCount + 1;
        for(int i=0;i<numberCount;i++){
            sequentialMultiplication rowMultiplication = new sequentialMultiplication(wIntermediate.get(i),tishares,
                    clientID, prime, startProtocolID, oneShare, sendQueues, recQueues);
            Future<Integer> wRowProduct = es.submit(rowMultiplication);
            taskList.add(wRowProduct);
            startProtocolID+=numberCount;
        }

        for(int i=0;i<numberCount;i++){
            Future<Integer> w_temp = taskList.get(i);
            wOutput[i]=w_temp.get();
        }
    }
    
    private void tearDownHandlers() {
        recvqueueHandler.shutdownNow();
        sendqueueHandler.shutdownNow();
    }
    

    
}
