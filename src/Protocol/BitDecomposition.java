
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * The class takes as input shares of an value and converts it to the
 * corresponding shares of bits of the value
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
     * Takes 3(bitLength) - 2 binary ti shares
     *
     * @param input
     * @param tiShares
     * @param asymmetricBit
     * @param bitLength
     * @param pidMapper
     * @param senderQueue
<<<<<<< HEAD
     * @param receiverQueue
=======
>>>>>>> master
     * @param protocolIdQueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param partyCount
     */
    public BitDecomposition(Integer input, List<TripleByte> tiShares,
            int asymmetricBit, int bitLength,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, int prime, int protocolID, int partyCount) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId,
                asymmetricBit, partyCount);

        this.input = input;
        this.bitLength = bitLength;
        this.prime = prime;
        this.tiIndex = 0;
        this.globalPid = 0;

        // convert decimal to binary notation
        // TODO - generalize to n parties with a for loop
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
        tiStartIndex = 0;
        eShares = new Integer[bitLength];
        dShares = new Integer[bitLength];
        cShares = new Integer[bitLength];
        xShares = new ArrayList<>();
        yShares = new Integer[bitLength];

    }

    /**
     * Convert shares of the value to shares of bit representation of the value
     *
     * @return
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Override
    public List<Integer> call() throws InterruptedException, ExecutionException {

        // Function to initialze y[i] and put x1 <- y1
        initY();

        // Initialize c[1] 
        int first_c_share = bitMultiplication(inputShares.get(0).get(0),
                inputShares.get(1).get(0));
        cShares[0] = Math.floorMod(first_c_share, prime);

        computeDShares();
        for (int i = 1; i < bitLength; i++) {
            computeVariables(i);
        }

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

        // set x[1] <- y[1]
        xShares.add(0, yShares[0]);
    }

    /**
     * Compute [c1] = [a1][b1]
     *
     * @param first_bit
     * @param second_bit
     * @return
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public int bitMultiplication(int first_bit, int second_bit)
            throws InterruptedException, ExecutionException {

        int multiplication_result = -1;

        ExecutorService es = Executors.newSingleThreadExecutor();

        //compute local shares of d and e and add to the message queue
        MultiplicationByte multiplicationModule
                = new MultiplicationByte(first_bit, second_bit,
                        tiShares.get(tiIndex), pidMapper, senderQueue,
                        new LinkedList<>(protocolIdQueue), clientID,
                        prime, globalPid, asymmetricBit, 1, partyCount);

        tiIndex++;
        globalPid++;
        // Assign the multiplication task to ExecutorService object.
        Future<Integer> multiplicationTask = es.submit(multiplicationModule);
        es.shutdown();
        multiplication_result = multiplicationTask.get();

        return multiplication_result;
    }

    /**
     * Compute [di] = [ai]*[bi] + asymmetricBit using distributed multiplication
     * and set
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void computeDShares() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        int i = 1;

        // The protocols for computation of d are assigned id 1-bitLength-1
        do {
            int toIndex = Math.min(i + Constants.BATCH_SIZE, bitLength);
            int tiCount = toIndex - i;

            BatchMultiplicationByte batchMultiplication
                    = new BatchMultiplicationByte(
                            inputShares.get(0).subList(i, toIndex),
                            inputShares.get(1).subList(i, toIndex),
                            tiShares.subList(tiIndex, tiIndex + tiCount),
                            pidMapper, senderQueue,
                            new LinkedList<>(protocolIdQueue),
                            clientID, prime, globalPid, asymmetricBit,
                            protocolId, partyCount);

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
            Future<Integer[]> dWorkerResponse = taskList.get(j);
            Integer[] d_batch = dWorkerResponse.get();

            // Iterate through each batch to do computations and save values in dShares
            for (Integer d_batch1 : d_batch) {
                int d = d_batch1 + asymmetricBit;
                dShares[globalIndex++] = Math.floorMod(d, prime);
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
    private void computeVariables(int index) throws InterruptedException,
            ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(2);

        Runnable eThread = () -> {
            try {
                int e_result = bitMultiplication(yShares[index],
                        cShares[index - 1]) + asymmetricBit;
                e_result = Math.floorMod(e_result, prime);
                eShares[index] = e_result;
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(BitDecomposition.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        };

        es.submit(eThread);

        Runnable xThread = () -> {
            int x_result = yShares[index] + cShares[index - 1];
            x_result = Math.floorMod(x_result, prime);
            xShares.add(index, x_result);
        };

        es.submit(xThread);

        es.shutdown();
        // Compute c and w sequentially when both threads end
        boolean threadsCompleted = es.awaitTermination(Long.MAX_VALUE,
                TimeUnit.NANOSECONDS);
        if (threadsCompleted) {
            int c_result = bitMultiplication(eShares[index], dShares[index])
                    + asymmetricBit;
            c_result = Math.floorMod(c_result, prime);
            cShares[index] = c_result;
        }
    }
}
