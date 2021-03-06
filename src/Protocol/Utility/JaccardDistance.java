/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol.Utility;

import Communication.Message;
import Protocol.CompositeProtocol;
import Protocol.OR_XOR;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import java.util.ArrayList;
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
 * Takes List of training rows and test row and computes Jaccard Distances
 * between the two
 *
 * @author bhagatsanchya
 */
public class JaccardDistance extends CompositeProtocol implements Callable<List<List<Integer>>> {

    List<List<Integer>> trainingShares;
    List<Integer> testShare;
    List<TripleInteger> decimalTiShares;
    int prime;

    /**
     *
     * @param trainingShares
     * @param testShare
     * @param asymmetricBit
     * @param tiShares
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param partyCount
     * @param protocolIdQueue
     */
    public JaccardDistance(List<List<Integer>> trainingShares,
            List<Integer> testShare, int asymmetricBit, List<TripleInteger> tiShares,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, BlockingQueue<Message> senderQueue,
            int clientId, int prime,
            int protocolID, Queue<Integer> protocolIdQueue, int partyCount) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId, asymmetricBit, partyCount);

        this.trainingShares = trainingShares;
        this.testShare = testShare;
        this.decimalTiShares = tiShares;
        this.prime = prime;
    }

    /**
     *
     * @return @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Override
    public List<List<Integer>> call() throws InterruptedException, ExecutionException {

        List<List<Integer>> result = new ArrayList<>();
        int startpid = 0;
        int attrLength = testShare.size(), tiStartIndex = 0;
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        
        List<Future<Integer[]>> orTaskList = new ArrayList<>();
        List<Future<Integer[]>> xorTaskList = new ArrayList<>();

        for (int i = 0; i < trainingShares.size(); i++) {
            OR_XOR orModule = new OR_XOR(trainingShares.get(i), testShare,
                    decimalTiShares.subList(tiStartIndex, tiStartIndex + attrLength),
                    asymmetricBit, 1, pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue), clientID, prime, startpid, partyCount);

            Future<Integer[]> orTask = es.submit(orModule);
            orTaskList.add(orTask);
            startpid++;
            //tiStartIndex += attrLength;

            OR_XOR xorModule = new OR_XOR(trainingShares.get(i), testShare,
                    decimalTiShares.subList(tiStartIndex, tiStartIndex + attrLength),
                    asymmetricBit, 2, pidMapper, senderQueue,
                    new LinkedList<>(protocolIdQueue), clientID, prime, startpid, partyCount);

            Future<Integer[]> xorTask = es.submit(xorModule);
            xorTaskList.add(xorTask);
            startpid++;
            //tiStartIndex += attrLength;
        }

        es.shutdown();

        // These scores are additive shares over prime (prime)
        // We eventually need sum of individual list over prime
        for (int i = 0; i < trainingShares.size(); i++) {
            Future<Integer[]> orTask = orTaskList.get(i);
            Future<Integer[]> xorTask = xorTaskList.get(i);

            Integer[] orOutput = orTask.get();
            Integer[] xorOutput = xorTask.get();

            //row stores or, xor outputs for a training example
            List<Integer> row = new ArrayList<>();
            row.add(getScoreFromList(orOutput));
            row.add(getScoreFromList(xorOutput));
            result.add(row);
        }
        return result;
    }

    public int getScoreFromList(Integer[] scoreList) {

        int sum = 0;
        for (int element : scoreList) {

            sum = sum + element;
        }

        return Math.floorMod(sum, prime);
    }

}
