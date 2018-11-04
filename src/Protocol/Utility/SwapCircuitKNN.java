/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.CompositeProtocol;
import TrustedInitializer.TripleInteger;
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
 * Implements the swap circuit for KNN Sort and Swap algorithm
 *
 * return jdj = CM.notC.jdi + notCj-1.jdj-1 + C.jdj
 *
 * @author keerthanaa
 */
public class SwapCircuitKNN extends CompositeProtocol implements Callable<Integer[]> {

    int trainingIndex;
    int position;
    Integer[] comparisonResults;
    Integer[] comparisonMultiplications;
    List<TripleInteger> decimalTiShares;
    int prime;
    List<Integer> jaccardDistanceTraining, jaccardDistanceSorted, jaccardDistanceSortedPrevious;

    /**
     *
     * @param trainingIndex
     * @param position
     * @param comparisonResults
     * @param comparisonMultiplications
     * @param protocolID
     * @param protocolIdQueue
     * @param prime
     * @param oneShare
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param tiShares
     * @param jaccardDistanceTraining
     * @param jaccardDistanceSorted
     * @param jaccardDistanceSortedprev
     * @param partyCount
     */
    public SwapCircuitKNN(int trainingIndex, int position,
            Integer[] comparisonResults, Integer[] comparisonMultiplications,
            int protocolID, Queue<Integer> protocolIdQueue, int prime, int oneShare,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, int clientId, List<TripleInteger> tiShares,
            List<Integer> jaccardDistanceTraining, List<Integer> jaccardDistanceSorted,
            List<Integer> jaccardDistanceSortedprev, int partyCount) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId, oneShare, partyCount);
        this.trainingIndex = trainingIndex;
        this.position = position;
        this.comparisonResults = comparisonResults;
        this.comparisonMultiplications = comparisonMultiplications;
        this.decimalTiShares = tiShares;
        this.jaccardDistanceSorted = jaccardDistanceSorted;
        this.jaccardDistanceTraining = jaccardDistanceTraining;
        this.jaccardDistanceSortedPrevious = jaccardDistanceSortedprev;
        this.prime = prime;
    }

    /**
     *
     * @return @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Override
    public Integer[] call() throws InterruptedException, ExecutionException {
        int pid = 0, decimalTiIndex = 0;
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);

        //first mult
        List<Integer> productComps = new ArrayList<>(Collections.nCopies(3, comparisonMultiplications[position]));
        BatchMultiplicationInteger cmNotCJdi = new BatchMultiplicationInteger(jaccardDistanceTraining,
                productComps, decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3),
                pidMapper, senderQueue,
                new LinkedList<>(protocolIdQueue), clientID, prime,
                pid, asymmetricBit, 0, partyCount);
        Future<Integer[]> multTask1 = es.submit(cmNotCJdi);
        pid++;
        decimalTiIndex += 3;

        //third mult
        List<Integer> C = new ArrayList<>(Collections.nCopies(3, comparisonResults[position]));
        BatchMultiplicationInteger CJdj = new BatchMultiplicationInteger(jaccardDistanceSorted,
                C, decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3),
                pidMapper, senderQueue,
                new LinkedList<>(protocolIdQueue), clientID, prime,
                pid, asymmetricBit, 0, partyCount);
        Future<Integer[]> multTask3 = es.submit(CJdj);
        pid++;
        //decimalTiIndex += 3;

        Integer[] results = new Integer[3];
        Arrays.fill(results, 0);

        if (position != 0) {
            //second mult
            List<Integer> notC = new ArrayList<>(Collections.nCopies(3,
                    Math.floorMod(asymmetricBit - comparisonResults[position - 1], prime)));
            BatchMultiplicationInteger notCprevJdprev = new BatchMultiplicationInteger(jaccardDistanceSortedPrevious,
                    notC, decimalTiShares.subList(decimalTiIndex, decimalTiIndex + 3),
                    pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue), clientID, prime,
                    pid, asymmetricBit, 0, partyCount);
            Future<Integer[]> multTask2 = es.submit(notCprevJdprev);
            pid++;
            //decimalTiIndex += 3;

            Integer[] Mults = multTask2.get();
            for (int i = 0; i < 3; i++) {
                results[i] += Mults[i];
            }
        }
        es.shutdown();
        Integer[] Mults = multTask1.get();
        for (int i = 0; i < 3; i++) {
            results[i] += Mults[i];
        }
        Mults = multTask3.get();
        for (int i = 0; i < 3; i++) {
            results[i] = Math.floorMod(results[i] + Mults[i], prime);
        }

        //System.out.println("results retuning: " + Arrays.toString(results));
        return results;
    }
}
