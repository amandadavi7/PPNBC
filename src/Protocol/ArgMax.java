/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
import Protocol.Utility.ParallelMultiplication;
import TrustedInitializer.TripleByte;
import Utility.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Computes the ArgMax of a given set of numbers (binary representation)
 *
 * @author keerthanaa
 */
public class ArgMax extends CompositeProtocol implements Callable<Integer[]> {

    List<List<Integer>> vShares;
    List<TripleByte> tiShares;
    int bitLength, numberCount, prime;

    HashMap<Integer, ArrayList<Integer>> wIntermediate;
    Integer[] wOutput;

    /**
     * vShares - shares of all the numbers to be compared (k numbers) Each
     * element in vShares contains l bits (binary representation) Returns shares
     * of k bits as an integer array (0 if not ArgMax and 1 if ArgMax)
     *
     * Takes k*(k-1)*(2 * l + ((l * (l - 1))/ 2)) + k*(k-2) binaryTiShares
     *
     * @param vShares
     * @param tiShares
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param protocolIdQueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param partyCount
     */
    public ArgMax(List<List<Integer>> vShares, List<TripleByte> tiShares,
            int asymmetricBit, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, int prime,
            int protocolID, int partyCount) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId, asymmetricBit, partyCount);

        this.vShares = vShares;
        this.tiShares = tiShares;
        this.prime = prime;

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
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Override
    public Integer[] call() throws InterruptedException, ExecutionException {

        if (numberCount == 1) {
            wOutput[0] = 1;
            return wOutput;
        }

        int tiIndex = computeComparisons();
        computeW(tiIndex);

        return wOutput;
    }

    /**
     * each number in (1,..,k) to be compared with (k-1) other numbers does
     * multi-threaded k*(k-1) comparisons and stores the results in wIntermediate
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private int computeComparisons() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> taskList = new ArrayList<>();
        int tiIndex = 0;

        int tiCount = 2 * bitLength + ((bitLength * (bitLength - 1)) / 2);
        for (int i = 0; i < numberCount; i++) {
            for (int j = 0; j < numberCount; j++) {
                if (i != j) {
                    int key = (i * numberCount) + j;

                    //Extract the required number of tiShares and pass it to comparison protocol
                    //each comparison needs tiCount shares
                    Comparison comparisonModule = new Comparison(vShares.get(i), vShares.get(j),
                            tiShares.subList(tiIndex, tiIndex + tiCount), asymmetricBit,
                            pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                            clientID, prime, key, partyCount);
                    tiIndex += tiCount;
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
            //System.out.println("w[" + Integer.toString(i) + "]:" + wIntermediate.get(i));
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
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> taskList = new ArrayList<>();

        //Each row has n-2 multiplications to do for n-1 numbers
        int tiCount = numberCount - 2;

        for (int i = 0; i < numberCount; i++) {

            ParallelMultiplication rowMultiplication = new ParallelMultiplication(
                    wIntermediate.get(i), tiShares.subList(tiIndex, tiIndex + tiCount), clientID, prime, i,
                    asymmetricBit, pidMapper, senderQueue, 
                    new LinkedList<>(protocolIdQueue), partyCount);
            tiIndex += tiCount;

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
