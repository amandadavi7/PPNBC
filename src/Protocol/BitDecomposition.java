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
    int oneShare;
    List<Triple> tiShares;

    HashMap<Integer, Integer> dShares;
    HashMap<Integer, Integer> eShares;
   // HashMap<Integer, Integer> multiplicationE;
    HashMap<Integer, Integer> cShares;
    HashMap<Integer, Integer> xShares;
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
        //multiplicationE = new HashMap<>();

        System.out.println("bitLength:" + bitLength);
        // Communication between the parent and the sub protocols
        recQueues = new ConcurrentHashMap<>();
        sendQueues = new ConcurrentHashMap<>();

        sendqueueHandler = Executors.newSingleThreadExecutor();
        recvqueueHandler = Executors.newSingleThreadExecutor();

        sendqueueHandler.execute(new SenderQueueHandler(protocolID, commonSender, sendQueues));
        recvqueueHandler.execute(new ReceiverQueueHandler(commonReceiver, recQueues));

    }

    @Override
    public Integer call() throws Exception {
            
          computeInit();
          System.out.println("the first c share: " + cShares.get(0));
//        ExecutorService threadService = Executors.newCachedThreadPool();
//        Runnable dThread = new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    computeInit();
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
//                } catch (ExecutionException ex) {
//                    Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//        };
//
//        threadService.submit(dThread);
//        threadService.shutdown();
//        
//        boolean threadsCompleted = threadService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//        if(threadsCompleted){
//            System.out.println("The first c Share" + cShares.get(0));
//             return 3;
//        }
//        
//        //tearDownHandlers();
//        return 1;
          return 1;
    }
    
    
    // Calculate step (2)  [c1] = [a1][b1] and set [x1] <- [y1]
    public void computeInit()throws InterruptedException, ExecutionException{
        
        List<Future<Integer>> taskList = new ArrayList<>();
        System.out.println("In BitDecomposition -> ComputeInit()");
//        // Using just one thread in the executor service
            ExecutorService es = Executors.newCachedThreadPool();
        
         //compute local shares of d and e and add to the message queue
            if (!recQueues.containsKey(0)) {
                BlockingQueue<Message> temp = new LinkedBlockingQueue<>();
                recQueues.put(0, temp);
            }

            if (!sendQueues.containsKey(0)) {
                BlockingQueue<Message> temp2 = new LinkedBlockingQueue<>();
                sendQueues.put(0, temp2);
            }
            
//        System.out.println("A shares " + a.get(0));
//        System.out.println("B shares " + b.get(0));
//       // System.out.println("ti shares " + tiShares.get(0));
      
        Multiplication multiplicationModule = new Multiplication(a.get(0),
                    b.get(0), tiShares.get(0),
                    sendQueues.get(0), recQueues.get(0), clientID,
                    prime, 0);
        
        // Assign the multiplication task to ExecutorService object.
        Future<Integer> multiplicationTask = es.submit(multiplicationModule);
        taskList.add(multiplicationTask);
        es.shutdown();
        
        Future<Integer> dWorkerResponse = taskList.get(0);
            try {
                cShares.put(0, dWorkerResponse.get());
            } catch (InterruptedException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
            }
//        int c_share_first = multiplicationTask.get();
//        // put the result in c[0]
//        System.out.println("In compute init, value of c_share is = " + c_share_first);
//        cShares.put(0, c_share_first);
     
    }
    
    /**
     * Teardown all local threads 
     */
    private void tearDownHandlers() {
        recvqueueHandler.shutdownNow();
        sendqueueHandler.shutdownNow();
    }
    
    
    
    
}
