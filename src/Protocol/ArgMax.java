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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Class to take care of multiplying all w[j,n] for each j = 0 to numberCount-1
 * @author keerthanaa
 */
class ParallelMultiplication extends Protocol implements Callable<Integer> {
    
    List<Integer> wRow;
    List<Triple> tishares;
    int startProtocolID, clientID, prime, oneShare;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    
    public ParallelMultiplication(List<Integer> row, List<Triple> tishares, 
            int clientID, int prime, int protocolID, int startProtocolID, 
            int oneShare, ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues, 
            ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues,
            BlockingQueue<Message> senderQueue, BlockingQueue<Message> receiverQueue) {
        
        super(protocolID,senderQueue,receiverQueue);
        this.wRow = row;
        this.tishares = tishares;
        this.clientID = clientID;
        this.prime = prime;
        this.startProtocolID = startProtocolID;
        this.oneShare = oneShare;   
        this.sendQueues = sendQueues;
        this.recQueues = recQueues;
    }
    
    /**
     * 
     * @return
     * @throws Exception 
     */
    @Override
    public Integer call() throws Exception {
        List<Integer> products = new ArrayList<>(wRow);
        int tiStartIndex = 0;
        
        while(products.size()>1){
            int size = products.size();
            int push = -1;
            int toIndex1 = size/2;
            int toIndex2 = size;
            if(size%2==1){
                toIndex2--;
                push = products.get(size-1);
            }
            
            System.out.println("products size:"+size+",toIndex1 "+toIndex1+",toIndex2 "+toIndex2);
            
            ExecutorService batchmults = Executors.newFixedThreadPool(Constants.threadCount);
            ExecutorCompletionService<Integer[]> multCompletionService = new ExecutorCompletionService<>(batchmults);
            
            int i1=0;
            int i2=toIndex1;
            int startpid = startProtocolID;
            
            do {
                
                int tempIndex1 = Math.min(i1+Constants.batchSize, toIndex1);
                int tempIndex2 = Math.min(i2+Constants.batchSize, toIndex2);
                
                initQueueMap(recQueues, sendQueues, startpid);
                System.out.println("calling batchmult with pid:"+startpid+",indices:"+tempIndex1+","+tempIndex2);
                
                multCompletionService.submit(new BatchMultiplication(products.subList(i1, tempIndex1), 
                    products.subList(i2, tempIndex2), tishares.subList(tiStartIndex, tiStartIndex+tempIndex1), 
                    sendQueues.get(startpid), recQueues.get(startpid), clientID, prime, startpid, oneShare, protocolId));
                
                tiStartIndex += tempIndex1;
                startpid++;
                i1 = tempIndex1;
                i2 = tempIndex2;                
                
            } while(i1<toIndex1 && i2<toIndex2);
            
            batchmults.shutdown();
            List<Integer> newProducts = new ArrayList<>();
            for(int i=0;i<startpid-startProtocolID;i++) {
                    Future<Integer[]> prodFuture = multCompletionService.take();
                    Integer[] newProds = prodFuture.get();
                    for(int j: newProds){
                        newProducts.add(j);
                    }
            }
            
            products.clear();
            products = new ArrayList<>(newProducts);
            
            if(push!=-1) {
                products.add(push);
            }
            
        }
        System.out.println("returning "+products.get(0));
        return products.get(0);
        
        
        /*
        int product = wRow.get(0);

        for (int i = 0; i < wRow.size() - 1; i++) {
            Multiplication multiplicationTask = new Multiplication(product, wRow.get(i + 1),
                    tishares.get(i), senderQueue, receiverQueue,
                    clientID, prime, startProtocolID, oneShare,protocolId);
            product = (int) multiplicationTask.call();

        }
        //System.out.println("returning "+product);
        return product;   */     
    }
    
}

/**
 *
 * @author keerthanaa
 */
public class ArgMax extends CompositeProtocol implements Callable<Integer[]> {

    List<List<Integer>> vShares;
    int oneShare;
    List<Triple> tiShares;    
    int bitLength, numberCount;

    HashMap<Integer, ArrayList<Integer>> wIntermediate;
    Integer[] wOutput;

    /**
     * vShares - shares of all the numbers to be compared (k numbers) each share
     * vShare(j) contains l bits
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
    public ArgMax(List<List<Integer>> vShares, List<Triple> tiShares,
            int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID) {
        
        super(protocolID, senderQueue, receiverQueue, clientId, prime);
        
        this.vShares = vShares;
        this.oneShare = oneShare;
        this.tiShares = tiShares;

        numberCount = vShares.size();
        bitLength = 0;
        for (List<Integer> row : vShares) {
            bitLength = Math.max(bitLength, row.size());
        }

        wIntermediate = new HashMap<>();
        for (int i = 0; i < numberCount; i++) {
            wIntermediate.put(i, new ArrayList<>());
        }

        wOutput = new Integer[numberCount];

    }

    /**
     * Computes ArgMax by invoking comparison and multiplication protocols
     *
     * @return
     * @throws Exception
     */
    @Override
    public Integer[] call() throws Exception {
        
        if (numberCount == 1) {
            wOutput[0] = 1;
            return wOutput;
        }
        
        startHandlers();

        int tiIndex = computeComparisons();
        computeW(tiIndex);

        tearDownHandlers();
        return wOutput;
    }

    /**
     * each number in (1,..,k) to be compared with (k-1) other numbers does
     * multithreaded k*(k-1) comparisons and stores the results in wIntermediate
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private int computeComparisons() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();
        int tiIndex = 0;
        int tiCount = 3 * bitLength - 3;
        for (int i = 0; i < numberCount; i++) {
            for (int j = 0; j < numberCount; j++) {
                if (i != j) {
                    int key = (i * numberCount) + j;
                    
                    initQueueMap(recQueues, sendQueues, key);
                    
                    //Extract the required number of tiShares and pass it to comparison protocol
                    //each comparison needs 3(bitlength)-3 shares
                    List<Triple> tiComparsion = tiShares.subList(tiIndex, tiIndex + tiCount);
                    tiIndex += tiCount;
                    Comparison comparisonModule = new Comparison(vShares.get(i), vShares.get(j), tiComparsion,
                            oneShare, sendQueues.get(key), recQueues.get(key), clientID, prime, key);
                    Future<Integer> comparisonTask = es.submit(comparisonModule);
                    taskList.add(comparisonTask);
                }
            }
        }

        es.shutdown();

        for (int i = 0; i < numberCount * (numberCount - 1); i++) {
            Future<Integer> w_temp = taskList.get(i);
            int key = i / (numberCount - 1);
            wIntermediate.get(key).add(w_temp.get());
        }

        for (int i = 0; i < numberCount; i++) {
            System.out.println("w[" + Integer.toString(i) + "]:" + wIntermediate.get(i));
        }

        return tiIndex;
    }

    /**
     * Create numberCount threads to multiply all w values for each entry in
     * HashMap store the result in wOutput
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void computeW(int tiIndex) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer>> taskList = new ArrayList<>();

        //Each row has n-2 sequential multiplications to do for n-1 numbers
        int tiCount = numberCount - 2;
        int startProtocolID = numberCount * numberCount + 1;
        int IDSizePerRow = (int) Math.ceil(((double)numberCount-1)/10.0);
        
        for (int i = 0; i < numberCount; i++) {
            
            List<Triple> tishares = tiShares.subList(tiIndex, tiIndex + tiCount);
            tiIndex += tiCount;
            
            ParallelMultiplication rowMultiplication = new ParallelMultiplication(
                    wIntermediate.get(i), tishares, clientID, prime, protocolId, 
                    startProtocolID, oneShare, sendQueues, recQueues, 
                    senderQueue, receiverQueue);
            
            startProtocolID+=IDSizePerRow;
            
            Future<Integer> wRowProduct = es.submit(rowMultiplication);
            taskList.add(wRowProduct);
        }

        for (int i = 0; i < numberCount; i++) {
            Future<Integer> w_temp = taskList.get(i);
            wOutput[i] = w_temp.get();
        }

        es.shutdown();
    }

}
