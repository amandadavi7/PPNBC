/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Model.TestModel;
import Protocol.CompositeProtocol;
import TrustedInitializer.TripleByte;
import Utility.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Polynomial circuit for scoring Decision Trees
 *
 * @author keerthanaa
 */
public class PolynomialComputing extends CompositeProtocol implements Callable<Integer[]> {

    int s, u, alpha;
    int[] comparisonOutputs;
    Integer[] y_j, jBinary;
    List<TripleByte> tiShares;

    /**
     * Constructor (takes initial y[j][r] for a given j and computes the final
     * y[j][r] output
     *
     * @param y_j
     * @param jBinary
     * @param alpha
     * @param depth
     * @param zOutputs
     * @param tiShares
     * @param protocolIdQueue
     * @param pidMapper
     * @param senderQueue
     * @param protocolID
     * @param clientId
     * @param asymmetricBit
     * @param partyCount
     */
    public PolynomialComputing(Integer[] y_j, Integer[] jBinary, int alpha, int depth,
            int[] zOutputs, List<TripleByte> tiShares,
            Queue<Integer> protocolIdQueue,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue, 
            int protocolID, int clientId, int asymmetricBit, int partyCount) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId, 
                asymmetricBit, partyCount);

        this.s = depth;
        u = 1;
        this.alpha = alpha;
        this.y_j = y_j;
        this.comparisonOutputs = zOutputs;
        this.jBinary = jBinary;
        this.tiShares = tiShares;
    }

    /**
     *
     * @return @throws Exception
     */
    @Override
    public Integer[] call() throws Exception {

        int pid = 0;

        while (s > 0) {
            ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
            List<Future<Integer[]>> taskList = new ArrayList<>();

            List<Integer> yj = Arrays.asList(y_j);
            List<Integer> z_u = new ArrayList<>(Collections.<Integer>nCopies(Constants.BATCH_SIZE,
                    (comparisonOutputs[u - 1] + asymmetricBit * jBinary[s - 1]) % Constants.binaryPrime));

            int i = 0;
            do {
                int toIndex = Math.min(i + Constants.BATCH_SIZE, alpha);
                
                BatchMultiplicationByte mults = new BatchMultiplicationByte(
                        yj.subList(i, toIndex), z_u, tiShares.subList(i, toIndex),
                        pidMapper, senderQueue, 
                        new LinkedList<>(protocolIdQueue), clientID,
                        Constants.binaryPrime, pid, asymmetricBit, protocolId,
                        partyCount);

                Future<Integer[]> task = es.submit(mults);
                taskList.add(task);
                i = toIndex;
                pid++;

            } while (i < alpha);

            int batches = taskList.size();
            int globalIndex = 0;
            for (i = 0; i < batches; i++) {
                Future<Integer[]> taskResponse = taskList.get(i);
                try {
                    Integer[] arr = taskResponse.get();
                    for (int l = 0; l < arr.length; l++) {
                        y_j[globalIndex] = arr[l];
                        globalIndex++;
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            u = 2 * u + jBinary[s - 1];
            s--;
        }

        System.out.println("pid:" + protocolId + " returning" + Arrays.toString(y_j));
        return y_j;

    }
}
