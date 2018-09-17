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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Class to take care of multiplying all numbers in a list in parallel
 *
 * @author keerthanaa
 */
public class ParallelMultiplication extends CompositeProtocol implements Callable<Integer> {

    List<Integer> wRow;
    List<TripleByte> tishares;
    int prime;

    /**
     * Uses n-1 tiShares for n numbers 
     * Takes a list of Integers and returns an Integer
     *
     * @param row
     * @param tishares
     * @param clientID
     * @param prime
     * @param protocolID
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param protocolIdQueue
     * @param partyCount
     */
    public ParallelMultiplication(List<Integer> row, List<TripleByte> tishares,
            int clientID, int prime, int protocolID,
            int asymmetricBit, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue,
            Queue<Integer> protocolIdQueue, int partyCount) {

        super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientID, asymmetricBit, partyCount);
        this.wRow = row;
        this.tishares = tishares;
        this.prime = prime;
    }

    /**
     *
     * @return @throws Exception
     */
    @Override
    public Integer call() throws Exception {
        List<Integer> products = new ArrayList<>(wRow);
        int tiStartIndex = 0;
        
        //iteratively multiply the first half of the list with the second half
        while (products.size() > 1) {
            int size = products.size();
            int push = -1;
            int toIndex1 = size / 2;
            int toIndex2 = size;
            if (size % 2 == 1) {
                toIndex2--;
                push = products.get(size - 1);
            }

            //System.out.println("products size:"+size+",toIndex1 "+toIndex1+",toIndex2 "+toIndex2);
            // TODO: Keerthana, I think we can move it outside the outer while loop
            ExecutorService batchmults = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
            ExecutorCompletionService<Integer[]> multCompletionService = new ExecutorCompletionService<>(batchmults);

            int i1 = 0;
            int i2 = toIndex1;
            int startpid = 0;

            do {

                int tempIndex1 = Math.min(i1 + Constants.BATCH_SIZE, toIndex1);
                int tempIndex2 = Math.min(i2 + Constants.BATCH_SIZE, toIndex2);

                //System.out.println("calling batchmult with pid:"+startpid+",indices:"+tempIndex1+","+tempIndex2);
                multCompletionService.submit(new BatchMultiplicationByte(products.subList(i1, tempIndex1),
                        products.subList(i2, tempIndex2), tishares.subList(tiStartIndex, tiStartIndex + tempIndex1),
                        pidMapper, senderQueue, new LinkedList<>(protocolIdQueue),
                        clientID, prime, startpid, asymmetricBit, protocolId, partyCount));

                tiStartIndex += tempIndex1;
                startpid++;
                i1 = tempIndex1;
                i2 = tempIndex2;

            } while (i1 < toIndex1 && i2 < toIndex2);

            batchmults.shutdown();
            List<Integer> newProducts = new ArrayList<>();
            for (int i = 0; i < startpid; i++) {
                Future<Integer[]> prodFuture = multCompletionService.take();
                Integer[] newProds = prodFuture.get();
                for (int j : newProds) {
                    newProducts.add(j);
                }
            }

            products.clear();
            products = new ArrayList<>(newProducts);

            if (push != -1) {
                products.add(push);
            }

        }
        System.out.println("returning " + products.get(0));
        return products.get(0);

    }

}
