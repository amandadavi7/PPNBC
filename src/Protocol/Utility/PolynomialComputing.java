/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
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

/**
 * Polynomial circuit for scoring Decision Trees
 *
 * @author keerthanaa
 */
public class PolynomialComputing extends CompositeProtocol implements Callable<Integer[]> {

    int level, nodeIndex, alpha;
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
            int protocolID, int clientId, int asymmetricBit, int partyCount,int threadID) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId,
                asymmetricBit, partyCount,threadID);

        this.level = depth;
        nodeIndex = 1;
        this.alpha = alpha;
        this.y_j = y_j;
        this.comparisonOutputs = zOutputs;
        this.jBinary = jBinary;
        this.tiShares = tiShares;
    }

    /**
     *
     * @return @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Override
    public Integer[] call() throws InterruptedException, ExecutionException {

        int pid = 0;

        while (level > 0) {
            ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
            List<Future<Integer[]>> taskList = new ArrayList<>();

            List<Integer> yj = Arrays.asList(y_j);
            List<Integer> compResultNode = new ArrayList<>(Collections.<Integer>nCopies(Constants.BATCH_SIZE,
                    (comparisonOutputs[nodeIndex - 1] + asymmetricBit * jBinary[level - 1]) % Constants.BINARY_PRIME));

            int i = 0;
            do {
                int toIndex = Math.min(i + Constants.BATCH_SIZE, alpha);

                BatchMultiplicationByte mults = new BatchMultiplicationByte(
                        yj.subList(i, toIndex), compResultNode, tiShares.subList(i, toIndex),
                        pidMapper, senderQueue,
                        new LinkedList<>(protocolIdQueue), clientID,
                        Constants.BINARY_PRIME, pid, asymmetricBit, protocolId,
                        partyCount,threadID);

                Future<Integer[]> task = es.submit(mults);
                taskList.add(task);
                i = toIndex;
                pid++;

            } while (i < alpha);

            es.shutdown();
            int batches = taskList.size();
            int globalIndex = 0;
            for (i = 0; i < batches; i++) {
                Future<Integer[]> taskResponse = taskList.get(i);
                Integer[] arr = taskResponse.get();
                for (int l = 0; l < arr.length; l++) {
                    y_j[globalIndex] = arr[l];
                    globalIndex++;
                }
            }

            nodeIndex = 2 * nodeIndex + jBinary[level - 1];
            level--;
        }
        return y_j;
    }
}
