/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import Communication.Message;
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
 * Takes a feature vector, k (index (0 index) of the feature that needs to be
 * selected and returns shares of xk asymmetric computation 
 *
 * One party sends k=-1 and feature vector, 
 * Another party sends k and featurevector = null
 *
 * Uses bitlength*count binaryTiShares
 *
 * @author keerthanaa
 */
public class OIS extends CompositeProtocol implements Callable<Integer[]> {

    List<List<Integer>> featureVectorTransposed;
    List<Integer> yShares;
    List<TripleByte> tiShares;
    int numberCount, bitLength, prime;
    private static final Logger LOGGER = Logger.getLogger(OIS.class.getName());;

    /**
     * Constructor
     *
     * Uses bitLength*count no. of binaryTIShares
     *
     * @param features
     * @param tiShares
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param protocolIdQueue
     * @param clientId
     * @param prime
     * @param protocolID
     * @param bitLength
     * @param k
     * @param numberCount
     * @param partyCount
     */
    public OIS(List<List<Integer>> features, List<TripleByte> tiShares,
            int asymmetricBit, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue,
            int clientId, int prime,
            int protocolID, int bitLength, int k, int numberCount, int partyCount) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId, asymmetricBit, partyCount);
        this.numberCount = numberCount;
        this.bitLength = bitLength;
        this.tiShares = tiShares;
        this.prime = prime;

        featureVectorTransposed = new ArrayList<>();
        if (features == null) {
            for (int i = 0; i < bitLength; i++) {
                List<Integer> temp = new ArrayList<>();
                for (int j = 0; j < numberCount; j++) {
                    temp.add(0);
                }
                featureVectorTransposed.add(temp);
            }
        } else {
            for (int i = 0; i < bitLength; i++) {
                featureVectorTransposed.add(new ArrayList<>());
            }
            for (int j = 0; j < numberCount; j++) {
                for (int i = 0; i < bitLength; i++) {
                    featureVectorTransposed.get(i).add(features.get(j).get(i));
                }
            }

        }

        yShares = new ArrayList<>(Collections.nCopies(numberCount, 0));
        if (k != -1) {
            yShares.set(k, 1);
        }
        
    }

    /**
     * for each bit, call dot product between all the nth bits of the features
     * and yShares vector
     *
     * @return
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    @Override
    public Integer[] call() throws InterruptedException, ExecutionException {
        Integer[] output = new Integer[bitLength];
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> taskList = new ArrayList<>();
        int tiStartIndex = 0;

        for (int i = 0; i < bitLength; i++) {

            DotProductByte dp = new DotProductByte(featureVectorTransposed.get(i), yShares,
                    tiShares.subList(tiStartIndex, tiStartIndex + numberCount), pidMapper, 
                    senderQueue, new LinkedList<>(protocolIdQueue),
                    clientID, prime, i, asymmetricBit, partyCount);

            Future<Integer> dpTask = es.submit(dp);
            taskList.add(dpTask);

            tiStartIndex += numberCount;
        }

        es.shutdown();

        for (int i = 0; i < bitLength; i++) {
            Future<Integer> dotprod = taskList.get(i);
            output[i] = dotprod.get();
        }

        LOGGER.log(Level.FINE, "OIS PID: {0} - returning result: {1}", new Object[]{protocolId, Arrays.toString(output)});
        return output;
    }

}
