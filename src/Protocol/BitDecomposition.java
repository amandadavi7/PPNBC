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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import TrustedInitializer.Triple;
/**
 *
 * @author bhagatsanchya
 */
public class BitDecomposition implements Callable<Integer>{
   
    ConcurrentHashMap<Integer, BlockingQueue<Message>> recQueues;
    ConcurrentHashMap<Integer, BlockingQueue<Message>> sendQueues;

    ExecutorService sendqueueHandler;
    ExecutorService recvqueueHandler;

    private BlockingQueue<Message> commonSender;
    private BlockingQueue<Message> commonReceiver;
    List<Integer> a;
    List<Integer> b;
   // List<Integer> x;
   //List<Integer> y;
    int oneShare;
    List<Triple> tiShares;

    HashMap<Integer, Integer> dShares;
    HashMap<Integer, Integer> eShares;
   // HashMap<Integer, Integer> multiplicationE;
    HashMap<Integer, Integer> cShares;
    HashMap<Integer,Integer> xShares;
    HashMap<Integer, Integer> yShares;
    
    /*parentProtocolId not used for testing*/
    int parentProtocolId;   
    int clientID;
    int prime;
    int bitLength;

    /**
     * Constructor
     *
     * @param a
     * @param b
     * @param tiShares
     * @param oneShare
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param prime
     * @param protocolID
     */
    public BitDecomposition(List<Integer> a, List<Integer> b, List<Triple> tiShares,
            int oneShare, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID) {

        this.a = a;
        this.b = b;
        this.oneShare = oneShare;
        this.tiShares = tiShares;
        this.commonSender = senderQueue;
        this.commonReceiver = receiverQueue;
        this.clientID = clientId;
        this.prime = prime;
        this.parentProtocolId = protocolID;

        bitLength = Math.max(a.size(), b.size());
        eShares = new HashMap<>();
        dShares = new HashMap<>();
        cShares = new HashMap<>();
        xShares = new HashMap<>();
        yShares = new HashMap<>();
        //multiplicationE = new HashMap<>();

        System.out.println("bitLength:" + bitLength);
        // Communication between the parent and the sub protocols
        recQueues = new ConcurrentHashMap<>();
        sendQueues = new ConcurrentHashMap<>();

        sendqueueHandler = Executors.newSingleThreadExecutor();
        recvqueueHandler = Executors.newSingleThreadExecutor();

        sendqueueHandler.execute(new SenderQueueHandler(protocolID, commonSender, sendQueues));
        recvqueueHandler.execute(new ReceiverQueueHandler(protocolID,commonReceiver, recQueues));

    }

    @Override
    public Integer call() throws Exception {
          
        // Function to initialze y[i] and put x1 <- y1
        initY();
        
        for(int j=0; j<100;j++){
            //just wait
        }
         
        
        // Initialize c[1] 
        // TODO : Check for parameters here.
        int first_c_share = computeByBit(a.get(0),b.get(0),0,0);
        first_c_share = Math.floorMod(first_c_share, prime);
        cShares.put(0,first_c_share);
        System.out.println("the first c share: " + cShares.get(0));
          
        ExecutorService threadService = Executors.newCachedThreadPool();
        Runnable dThread = new Runnable() {
            @Override
            public void run() {
                try {
                    computeDShares();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        threadService.submit(dThread);       

        threadService.shutdown();
        
        boolean threadsCompleted = threadService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        // once we have d and e, we can compute c and x Sequentially
        if(threadsCompleted){
            computeVariables();
            System.out.println("Values for xShares: [" + xShares+"]"); 
        }
          
          tearDownHandlers();
          return 1;
    }
    
    public void initY(){
        
        for(int i = 0; i < bitLength ; i++){
            
            int y = a.get(i) + b.get(i);
            y = Math.floorMod(y, prime);
            yShares.put(i, y);
        }
        System.out.println("Y shares: " + yShares);
        
        // set x[1] <- y[1]
        int y0 = yShares.get(0);
        xShares.put(0, y0);
        System.out.println("LSB for x: " + xShares);
    }
    
    
    // Calculate step (2)  [c1] = [a1][b1] 
    public int computeByBit(int first_bit, int second_bit, int protocol_id, int subprotocolId)throws InterruptedException, ExecutionException{
        int multiplication_result = -1;
        
//        int first_bit = getBitFromId(first_name,first_index);
//        int second_bit = getBitFromId(second_name,second_index);
        System.out.println("Computing prtocol id: " + (protocol_id + subprotocolId) );
        // System.out.println("In BitDecomposition -> ComputeInit()");
        ExecutorService es = Executors.newSingleThreadExecutor();
        
         //compute local shares of d and e and add to the message queue
            if (!recQueues.containsKey(subprotocolId + protocol_id)) {
                BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                recQueues.put(subprotocolId + protocol_id, temp);
            }

            if (!sendQueues.containsKey(protocol_id + subprotocolId )) {
                BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                sendQueues.put(subprotocolId + protocol_id , temp2);
            }
            
//        System.out.println("A shares " + a.get(0));
//        System.out.println("B shares " + b.get(0));
//        System.out.println("ti shares " + tiShares.get(0));
      
        Multiplication multiplicationModule = new Multiplication(first_bit,
                    second_bit, 
                tiShares.get(protocol_id + subprotocolId),
                    sendQueues.get(protocol_id + subprotocolId), recQueues.get(protocol_id + subprotocolId), clientID,
                    prime, subprotocolId + protocol_id, oneShare, 1);
        
        // Assign the multiplication task to ExecutorService object.
        Future<Integer> multiplicationTask = es.submit(multiplicationModule);
        es.shutdown();
            try {
                    multiplication_result = multiplicationTask.get();
                    
                    
            } catch (InterruptedException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            }
          
        return multiplication_result;
    }
    
    private int getBitFromId(char name, int index){
        
        int return_val = -1;
        
        switch(name){
            case 'a':
                return_val =  a.get(index);
                break;
            case 'b':
                return_val =  b.get(index);
                break;
            case 'e':
                return_val =  eShares.get(index);
                break;
            case 'd':
                return_val =  dShares.get(index);
                break;
            case 'c':
                return_val =  cShares.get(index);
                break;
            case 'x':
                return_val =  xShares.get(index);
                break;
            case 'y':
                return_val =  yShares.get(index);
                break;    
            
        }
        return return_val;
    }
    /**
     * Teardown all local threads 
     */
    private void tearDownHandlers() {
        recvqueueHandler.shutdownNow();
        sendqueueHandler.shutdownNow();
    }
    
    /**
     * Compute [di] = [ai]*[bi] + 1  using distributed multiplication and set 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void computeDShares() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(bitLength);
        List<Future<Integer>> taskList = new ArrayList<Future<Integer>>();
        System.out.println("In D shares");
        // **** The protocols for computation of d are assigned id 1-bitLength-1 ***
        // We already calculated the LSB, so we don't repeat that.
        for (int i = 1; i < bitLength; i++) {
            //compute local shares of d and e and add to the message queue
            
            if (!recQueues.containsKey(i)) {
                BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                recQueues.put(i, temp);
            }

            if (!sendQueues.containsKey(i)) {
                BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                sendQueues.put(i, temp2);
            }
            
            Multiplication multiplicationModule = new Multiplication(a.get(i),
                    b.get(i), tiShares.get(i),
                    sendQueues.get(i), recQueues.get(i), clientID,
                    prime, i, oneShare, 1);
            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);

        }

        es.shutdown();

        // Save d[i] to dShares
        for (int i = 1; i < bitLength; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i-1);
            int d = dWorkerResponse.get() + oneShare;
            d = Math.floorMod(d, prime);
            dShares.put(i, d);
            System.out.println("d share + " + i + " is: " + d);
        }
        
        Logging.logShares("dShares", dShares);
    }
    
    /**
     * Compute [ei] = [yi]*[c(i-1)] + 1  using distributed multiplication and set 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void computeEShares() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(bitLength);
        List<Future<Integer>> taskList = new ArrayList<Future<Integer>>();
        int subProtocolID = bitLength + 1;
        // **** The protocols for computation of e are assigned id 1-bitLength-1 ***
        // We already calculated the LSB, so we don't repeat that.
        for (int i = 1; i < bitLength; i++) {
            //compute local shares of d and e and add to the message queue
            
            if (!recQueues.containsKey(subProtocolID + i)) {
                BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                recQueues.put(subProtocolID + i, temp);
            }

            if (!sendQueues.containsKey(subProtocolID + i)) {
                BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                sendQueues.put(subProtocolID + i, temp2);
            }
            
            Multiplication multiplicationModule = new Multiplication(yShares.get(i),
                    cShares.get(i-1), tiShares.get(subProtocolID + i),
                    sendQueues.get(subProtocolID + i), recQueues.get(subProtocolID + i), clientID,
                    prime, subProtocolID + i, oneShare, 1);
            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);

        }

        es.shutdown();

        // Save e[i] to eShares
        for (int i = 1; i < bitLength; i++) {
            Future<Integer> eWorkerResponse = taskList.get(i-1);
            int e = eWorkerResponse.get() + 1;
            e = Math.floorMod(e, prime);
            eShares.put(i, e);
            System.out.println("E shares " + i + " ");
        }
        
        Logging.logShares("eShares", eShares);
    }
    
    
    /**
     * Compute [ci] = [ei]*[di] + 1  using distributed multiplication and set 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void computeCShares() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(bitLength);
        List<Future<Integer>> taskList = new ArrayList<Future<Integer>>();
        
        // TODO Check the bitLength to use here
        int subProtocolID = (bitLength * 2) + 1;
        
        // **** The protocols for computation of e are assigned id 1-bitLength-1 ***
        // We already calculated the LSB, so we don't repeat that.
        for (int i = 1; i < bitLength; i++) {
            //compute local shares of d and e and add to the message queue
            
            if (!recQueues.containsKey(subProtocolID + i)) {
                BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                recQueues.put(subProtocolID + i, temp);
            }

            if (!sendQueues.containsKey(subProtocolID + i)) {
                BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                sendQueues.put(subProtocolID + i, temp2);
            }
            
            Multiplication multiplicationModule = new Multiplication(eShares.get(i),
                    dShares.get(i), tiShares.get(subProtocolID + i),
                    sendQueues.get(subProtocolID + i), recQueues.get(subProtocolID + i), clientID,
                    prime, subProtocolID + i, oneShare, 1);
            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);

        }

        es.shutdown();

        // Save c[i] to cShares
        for (int i = 1; i < bitLength; i++) {
            Future<Integer> cWorkerResponse = taskList.get(i-1);
            int c = cWorkerResponse.get() + 1;
            c = Math.floorMod(c, prime);
            cShares.put(i, c);
        }
        
        Logging.logShares("eShares", eShares);
    }
    
    /**
     * Compute [xi] = [yi]*[c(i-1)] using distributed multiplication and set 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void computeXShares() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(bitLength);
        List<Future<Integer>> taskList = new ArrayList<Future<Integer>>();
        
        // TODO Check the bitLength to use here
        int subProtocolID = (bitLength * 3) + 1;
        
        // **** The protocols for computation of x are assigned id 1-bitLength-1 ***
        // We already calculated the LSB, so we don't repeat that.
        for (int i = 1; i < bitLength; i++) {
            //compute local shares of d and e and add to the message queue
            
            if (!recQueues.containsKey(subProtocolID + i)) {
                BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                recQueues.put(subProtocolID + i, temp);
            }

            if (!sendQueues.containsKey(subProtocolID + i)) {
                BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                sendQueues.put(subProtocolID + i, temp2);
            }
            
            Multiplication multiplicationModule = new Multiplication(yShares.get(i),
                    cShares.get(i-1), tiShares.get(subProtocolID + i),
                    sendQueues.get(subProtocolID + i), recQueues.get(subProtocolID + i), clientID,
                    prime, subProtocolID + i, oneShare, 1);
            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);

        }

        es.shutdown();

        // Save x[i] to xShares
        for (int i = 1; i < bitLength; i++) {
            Future<Integer> xWorkerResponse = taskList.get(i-1);
            int x = xWorkerResponse.get();
            x = Math.floorMod(x, prime);
            xShares.put(i, x);
        }
        
        //Logging.logShares("xShares", xShares);
    }
    
    private void computeVariables() throws InterruptedException{
        
        for(int i = 1; i < bitLength; i++){
            System.out.println("The current index " + i);
           ExecutorService threadService = Executors.newSingleThreadExecutor();
            
           ComputeThread compute = new ComputeThread(i);
           threadService.submit(compute);
           
            threadService.shutdown();
           
           boolean threadsCompleted = threadService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        // once we have d and e, we can compute c and x Sequentially
        if(threadsCompleted){
                int x_result = yShares.get(i);
                x_result = x_result + cShares.get(i-1); 
                x_result = Math.floorMod(x_result, prime);
                xShares.put(i, x_result);
                
                //continue;
        }
           
           
           
          
            
           
           
        
        }
    }
    
    private class ComputeThread implements Runnable{
       
        int protocolId;
        

        public ComputeThread(int protocolId) {
            this.protocolId = protocolId;
        }
        
    @Override
    public void run(){
        
            try {
                int e_result =
                        computeByBit(yShares.get(protocolId),cShares.get(protocolId - 1),protocolId, bitLength);
                e_result = e_result + oneShare;
                e_result = Math.floorMod(e_result, prime);
                eShares.put(protocolId,e_result);
                System.out.println("e result for id: " + e_result );
                
                int c_result = computeByBit(eShares.get(protocolId),dShares.get(protocolId),protocolId,bitLength*2 );
                c_result = c_result + oneShare;
                c_result = Math.floorMod(c_result, prime);
                cShares.put(protocolId, c_result);
                System.out.println("c result for id: " + c_result );
                
            } catch (InterruptedException ex) {
                Logger.getLogger(BitDecomposition.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(BitDecomposition.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        
        }
    
    }
    
}
