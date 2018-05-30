/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.BatchMultiplicationByte;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import TrustedInitializer.TripleByte;
import Utility.Constants;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author bhagatsanchya
 */
public class BitDecomposition extends CompositeProtocol implements 
        Callable<List<Integer>> {

    int input, tiStartIndex;
    List<List<Integer>> inputShares;
    List<TripleByte> tiShares;

    Integer[] dShares, eShares, cShares, yShares;

    List<Integer> xShares;

    int bitLength;
    int prime;
    int tiIndex, globalPid;

    /**
     * Constructor
     *
     * @param input
     * @param tiShares
     * @param asymmetricBit
     * @param bitLength
     * @param senderQueue
     * @param receiverQueue
     * @param protocolIdQueue
     * @param clientId
     * @param prime
     * @param protocolID
     */
    public BitDecomposition(Integer input, List<TripleByte> tiShares,
            int asymmetricBit, int bitLength, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, Queue<Integer> protocolIdQueue,
            int clientId, int prime,
            int protocolID, int partyCount) {

        super(protocolID, senderQueue, receiverQueue, protocolIdQueue, clientId, 
                asymmetricBit, partyCount);

        this.input = input;
        this.bitLength = bitLength;
        this.prime = prime;
        this.tiIndex = 0;
        this.globalPid = 0;

        // convert decimal to binary notation: TODO - generalize to n parties with a for loop
        inputShares = new ArrayList<>();
        List<Integer> temp = decimalToBinary(this.input);
        List<Integer> temp0 = new ArrayList<>(Collections.nCopies(bitLength, 0));
        int diff = Math.abs(bitLength - temp.size());
        for (int i = 0; i < diff; i++) {
            temp.add(0);
        }

        // TODO - client ID - change to zero indexed
        for (int i = 0; i < partyCount; i++) {
            if (i + 1 == clientId) {
                inputShares.add(temp);
            } else {
                inputShares.add(temp0);
            }
        }

        this.tiShares = tiShares;
        //this.parentProtocolId = protocolID;
        tiStartIndex = 0;
        eShares = new Integer[bitLength];
        dShares = new Integer[bitLength];
        cShares = new Integer[bitLength];
        xShares = new ArrayList<>();
        yShares = new Integer[bitLength];

    }

    @Override
    public List<Integer> call() throws Exception {

        startHandlers();

        // Function to initialze y[i] and put x1 <- y1
        initY();

        // Initialize c[1] 
        int first_c_share = bitMultiplication(inputShares.get(0).get(0),
                inputShares.get(1).get(0));
        cShares[0] = Math.floorMod(first_c_share, prime);
        System.out.println("the first c share: " + cShares[0]);

        computeDShares();
        for (int i = 1; i < bitLength; i++) {
            System.out.println("The current index " + i);
            computeVariables(i);
        }

        tearDownHandlers();
        return xShares;
    }

    /**
     * computes y locally
     */
    public void initY() {
        for (int i = 0; i < bitLength; i++) {
            int y = inputShares.get(0).get(i) + inputShares.get(1).get(i);
            yShares[i] = Math.floorMod(y, prime);
        }
        System.out.println("Y shares: " + Arrays.toString(yShares));
        // set x[1] <- y[1]
        xShares.add(0, yShares[0]);
        System.out.println("LSB for x: " + xShares);
    }

    // Calculate step (2)  [c1] = [a1][b1] 
    public int bitMultiplication(int first_bit, int second_bit)
            throws InterruptedException, ExecutionException {

        int multiplication_result = -1;

        ExecutorService es = Executors.newSingleThreadExecutor();

        //compute local shares of d and e and add to the message queue
        initQueueMap(recQueues, globalPid);

        MultiplicationByte multiplicationModule = new MultiplicationByte(first_bit,
                second_bit,
                tiShares.get(tiIndex),
                senderQueue,
                recQueues.get(globalPid), new LinkedList<>(protocolIdQueue),
                clientID,
                prime, globalPid, asymmetricBit, 1, partyCount);

        tiIndex++;
        globalPid++;
        // Assign the multiplication task to ExecutorService object.
        Future<Integer> multiplicationTask = es.submit(multiplicationModule);
        es.shutdown();
        try {
            multiplication_result = multiplicationTask.get();

        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
        }

        return multiplication_result;
    }

    /**
     * Compute [di] = [ai]*[bi] + asymmetricBit using distributed multiplication and
 set
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void computeDShares() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();
        System.out.println("In D shares");

        int i = 1;
        //int startpid = 1;

        // **** The protocols for computation of d are assigned id 1-bitLength-1 ***
        do {
            //System.out.println("Protocol " + protocolId);
            initQueueMap(recQueues, globalPid);

            int toIndex = Math.min(i + Constants.batchSize, bitLength);
            int tiCount = toIndex - i;

            BatchMultiplicationByte batchMultiplication = new BatchMultiplicationByte(
                    inputShares.get(0).subList(i, toIndex),
                    inputShares.get(1).subList(i, toIndex),
                    tiShares.subList(tiIndex, tiIndex + tiCount),
                    senderQueue, recQueues.get(globalPid), new LinkedList<>(protocolIdQueue),
                    clientID, prime, globalPid, asymmetricBit, protocolId, partyCount);

            Future<Integer[]> multiplicationTask = es.submit(batchMultiplication);
            taskList.add(multiplicationTask);

            globalPid++;
            i = toIndex;
            tiIndex += tiCount;
        } while (i < bitLength);

        es.shutdown();

        // Number of Future Integer[] present after batch multiplications
        int taskLength = taskList.size();

        // Save d[i] to dShares
        int globalIndex = 1;
        for (int j = 0; j < taskLength; j++) {
            try {
                Future<Integer[]> dWorkerResponse = taskList.get(j);
                Integer[] d_batch = dWorkerResponse.get();

                // Iterate through each batch to do computations and save values in dShares
                for (int index = 0; index < d_batch.length; index++) {
                    int d = d_batch[index] + asymmetricBit;
                    dShares[globalIndex++] = Math.floorMod(d, prime);
                }
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
        }

    }

    /**
     * Converts decimal value to List<> of bits (binary)
     *
     * @param decimal_val
     * @return
     */
    public static List<Integer> decimalToBinary(int decimal_val) {
        List<Integer> bits = new ArrayList<>();
        while (decimal_val > 0) {
            bits.add(decimal_val % 2);
            decimal_val = decimal_val / 2;
        }
        return bits;
    }

    /**
     * compute c, e and x in proper sequential order
     *
     * @throws InterruptedException
     */
    private void computeVariables(int index) {
        ExecutorService es = Executors.newFixedThreadPool(2);

        Runnable eThread = new Runnable() {
            @Override
            public void run() {
                try {
                    int e_result = bitMultiplication(yShares[index],
                            cShares[index - 1]) + asymmetricBit;
                    e_result = Math.floorMod(e_result, prime);
                    eShares[index] = e_result;
                    System.out.println("e result for id: " + e_result);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Comparison.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(BitDecomposition.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        es.submit(eThread);

        Runnable xThread = new Runnable() {
            @Override
            public void run() {
                int x_result = yShares[index] + cShares[index - 1];
                x_result = Math.floorMod(x_result, prime);
                xShares.add(index, x_result);
            }
        };

        es.submit(xThread);

        es.shutdown();

        // Compute c and w sequentially when both threads end
        // TODO Anisha: Optimize this further
        boolean threadsCompleted;
        try {
            threadsCompleted = es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (threadsCompleted) {
                int c_result = bitMultiplication(eShares[index],
                        dShares[index]) + asymmetricBit;
                c_result = Math.floorMod(c_result, prime);
                cShares[index] = c_result;
                System.out.println("c result for id: " + c_result);
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(BitDecomposition.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
