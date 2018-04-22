/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;
import Communication.Message;
import Utility.Logging;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import TrustedInitializer.Triple;
import Utility.Constants;

/**
 *
 * @author bhagatsanchya
 */
public class BitDecomposition extends CompositeProtocol implements Callable<List<Integer>>{
   
    int a_decimal;
    int b_decimal;
    List<Integer> a;
    List<Integer> b;
    int oneShare;
    List<Triple> tiShares;

    HashMap<Integer, Integer> dShares;
    HashMap<Integer, Integer> eShares;
   // HashMap<Integer, Integer> multiplicationE;
    HashMap<Integer, Integer> cShares;
    //Changed to List from HashMap
    List<Integer> xShares;
    HashMap<Integer, Integer> yShares;
    
    /*parentProtocolId not used for testing*/
    //int parentProtocolId;   
    int bitLength;

    /**
     * Constructor
     *
     * @param a_decimal
     * @param b_decimal
     * @param tiShares
     * @param oneShare
     * @param bitLength
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param prime
     * @param protocolID
     */
    public BitDecomposition(Integer a_decimal, Integer b_decimal, List<Triple> tiShares,
            int oneShare, int bitLength, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, int prime,
            int protocolID) {
        
        super(protocolID, senderQueue, receiverQueue, clientId, prime);

        this.a_decimal = a_decimal;
        this.b_decimal = b_decimal;
        this.bitLength = bitLength;
        System.out.println("a_share" + a_decimal);
        System.out.println("b_share" + b_decimal);
        
        // convert decimal to binary notation
        this.a = decimalToBinary(this.a_decimal);
        this.b = decimalToBinary(this.b_decimal);
        
        //add padding to  make the number of bits same
        int diff = Math.abs(bitLength - b.size());
        
        // add padding to b if a>b
        for(int i = 0; i < diff;i++){
            b.add(0);
        }
        
        diff = Math.abs(bitLength - a.size());
        
        // add padding to a if a<b
        for(int i = 0; i < diff;i++){
            a.add(0);
        }
        
        System.out.println("a size after padding: " + a.size());
        System.out.println("b size after padding:" + b.size());
        this.oneShare = oneShare;
        this.tiShares = tiShares;
        //this.parentProtocolId = protocolID;
        eShares = new HashMap<>();
        dShares = new HashMap<>();
        cShares = new HashMap<>();
        xShares = new ArrayList<>();
        yShares = new HashMap<>();
        //multiplicationE = new HashMap<>();

        System.out.println("bitLength:" + bitLength);

    }

    @Override
    public List<Integer> call() throws Exception {
        
        startHandlers();
        
        // Function to initialze y[i] and put x1 <- y1
        initY();
        
        // Initialize c[1] 
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
//            System.out.println("Values for xShares: [" + xShares+"]"); 
        }
          
          tearDownHandlers();
          return xShares;
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
        xShares.add(0, y0);
        System.out.println("LSB for x: " + xShares);
    }
    
    
    // Calculate step (2)  [c1] = [a1][b1] 
    public int computeByBit(int first_bit, int second_bit, int protocol_id, int subprotocolId)throws InterruptedException, ExecutionException{
        int multiplication_result = -1;
    
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
    
    /**
     * Compute [di] = [ai]*[bi] + oneShare  using distributed multiplication and set 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void computeDShares() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(bitLength);
        List<Future<Integer[]>> taskList = new ArrayList<>();
        System.out.println("In D shares");
        
        int i = 1;
        int startpid = 1;
        
        // **** The protocols for computation of d are assigned id 1-bitLength-1 ***
        do {
            System.out.println("Protocol " + protocolId + " batch " + startpid);
            initQueueMap(recQueues, sendQueues, startpid);

            int toIndex = (i + Constants.batchSize < bitLength)
                    ? (i + Constants.batchSize) : bitLength;

            BatchMultiplication batchMultiplication = new BatchMultiplication(
                    a.subList(i, toIndex),
                    b.subList(i, toIndex),
                    tiShares.subList(i, toIndex),
                    sendQueues.get(startpid), recQueues.get(startpid),
                    clientID, prime, startpid, oneShare, protocolId);

            Future<Integer[]> multiplicationTask = es.submit(batchMultiplication);
            taskList.add(multiplicationTask);

            startpid++;
            i += Constants.batchSize;
        } while (i < bitLength);

        es.shutdown();
        
        // Number of Future Integer[] present after batch multiplications
        int taskLength = taskList.size();
        
        // Save d[i] to dShares
        for (int j = 0; j < taskLength; j++) {
            try {
            Future<Integer[]> dWorkerResponse = taskList.get(j);
            Integer[] d_batch = dWorkerResponse.get();
            
            // Iterate through each batch to do computations and save values in dShares
            for(int index = 0; index < d_batch.length; index++){
                
                int globalIndex = j * 10 + index + 1; // + 1 signifies satring point is bit 1
                int d = d_batch[index];
                //add the oneShare to each d
                d = d + oneShare;
                d = Math.floorMod(d, prime);
                dShares.put(globalIndex, d);
            }
        }catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
        Logging.logShares("dShares", dShares);
        }
        
    }
    /**
     * Converts decimal value to List<> of bits (binary)
     * @param decimal_val
     * @return 
     */
     public static List<Integer> decimalToBinary(int decimal_val){
         List<Integer> bits = new ArrayList<>();
        while(decimal_val > 0){
            
            bits.add(decimal_val % 2);   
            decimal_val = decimal_val / 2;
        }
       return bits;
    }
    /**
     * compute c, e and x in proper sequential order
     * @throws InterruptedException 
     */
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
                xShares.add(i, x_result);
                
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
