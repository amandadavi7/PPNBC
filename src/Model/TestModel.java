/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.ArgMax;
import Protocol.BitDecomposition;
import Protocol.Comparison;
import Protocol.DotProductNumber;
import Protocol.Multiplication;
import Protocol.OIS;
import Protocol.OR_XOR;
import TrustedInitializer.Triple;
import Utility.Constants;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 *
 * @author anisha
 */
public class TestModel extends Model {

    List<List<Integer>> x;
    List<List<Integer>> y;
    List<List<List<Integer>>> v;

    public TestModel(List<List<Integer>> x, List<List<Integer>> y,
            List<List<List<Integer>>> v, List<Triple> binaryTriples, List<Triple> decimalTriples,
            int oneShares, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId) {

        super(senderQueue, receiverQueue, clientId, oneShares, binaryTriples, decimalTriples);

        this.x = x;
        this.y = y;
        this.v = v;

    }

    public void callBitDecomposition() {

        ExecutorService es = Executors.newFixedThreadPool(1);
        initQueueMap(recQueues, 1);
        //TODO: change this to just take integers instead of wasting memory on List<Integer> 
//        BitDecomposition bitTest = new BitDecomposition(x.get(0).get(0), y.get(0).get(0),
//                binaryTiShares, oneShare, Constants.bitLength, sendQueues.get(1),
//                recQueues.get(1), clientId, Constants.binaryPrime, 1);

        BitDecomposition bitTest = new BitDecomposition(2, binaryTiShares,
                oneShare, Constants.bitLength, commonSender,
                recQueues.get(1), new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, 1);

        Future<List<Integer>> bitdecompositionTask = es.submit(bitTest);

        try {
            List<Integer> result = bitdecompositionTask.get();
            System.out.println("result of bitDecomposition: " + result);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }

        teardownModelHandlers();
    }

    public void callArgMax() {

        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<Integer[]>> taskList = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        int totalCases = v.size();
        
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {

            initQueueMap(recQueues, i);

            ArgMax argmaxModule = new ArgMax(v.get(i), binaryTiShares, oneShare, commonSender,
                    recQueues.get(i), new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, i);

            System.out.println("submitted " + i + " argmax");

            Future<Integer[]> argmaxTask = es.submit(argmaxModule);
            taskList.add(argmaxTask);

        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer[]> dWorkerResponse = taskList.get(i);
            try {
                Integer[] result = dWorkerResponse.get();
                System.out.println("result:" + Arrays.toString(result) + ", #:" + i);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        teardownModelHandlers();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);

    }

    public void callOR_XOR() {

        System.out.println("calling or_xor with x=" + x + " y=" + y);

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();

        for (int i = 0; i < totalCases; i++) {

            initQueueMap(recQueues, i);
            OR_XOR or_xor = new OR_XOR(x.get(i), y.get(i), decimalTiShares, oneShare, 1, commonSender,
                    recQueues.get(i), new LinkedList<>(protocolIdQueue), clientId, Constants.prime, i);

            Future<Integer[]> task = es.submit(or_xor);
            taskList.add(task);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            try {
                Future<Integer[]> task = taskList.get(i);
                Integer[] result = task.get();
                System.out.println("result: " + i + ": " + Arrays.toString(result));
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        teardownModelHandlers();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);

    }

    public void callOIS() {

        System.out.println("calling OIS with v" + v);

        ExecutorService es = Executors.newSingleThreadExecutor();

        long startTime = System.currentTimeMillis();

        initQueueMap(recQueues, clientId);

        OIS ois;

        if (v.isEmpty()) {
            System.out.println("v is null");
            ois = new OIS(null, binaryTiShares, oneShare, commonSender, recQueues.get(0), new LinkedList<>(protocolIdQueue), clientId,
                    Constants.binaryPrime, 0, 4, 1, 3);
        } else {
            System.out.println("v is not null");
            ois = new OIS(v.get(0), binaryTiShares, oneShare, commonSender, recQueues.get(0), new LinkedList<>(protocolIdQueue), clientId,
                    Constants.binaryPrime, 0, 4, -1, 3);
        }

        Future<Integer[]> task = es.submit(ois);

        es.shutdown();

        try {
            Integer[] result = task.get();
            System.out.println("result:" + Arrays.toString(result));
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }

        teardownModelHandlers();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }

    public void callProtocol(int protocolType) {
        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        // totalcases number of protocols are submitted to the executorservice
        switch (protocolType) {
            case 1:
                for (int i = 0; i < totalCases; i++) {

                    initQueueMap(recQueues, i);

                    Multiplication multiplicationModule = new Multiplication(
                            x.get(i).get(0), y.get(i).get(0), 
                            decimalTiShares.get(i), commonSender, recQueues.get(i), 
                            new LinkedList<>(protocolIdQueue), clientId, Constants.prime, i, oneShare, 0);

                    System.out.println("Submitted " + i + " multiplication");

                    Future<Integer> multiplicationTask = es.submit(multiplicationModule);
                    taskList.add(multiplicationTask);
                }
                break;
            case 2:
                for (int i = 0; i < totalCases; i++) {

                    initQueueMap(recQueues, i);

                    DotProductNumber DPModule = new DotProductNumber(x.get(i),
                            y.get(i), decimalTiShares, commonSender, recQueues.get(i),
                            new LinkedList<>(protocolIdQueue),clientId, Constants.prime, i, oneShare);

                    System.out.println("Submitted " + i + " dotproduct");

                    Future<Integer> DPTask = es.submit(DPModule);
                    taskList.add(DPTask);
                }
                break;
            case 3:
                for (int i = 0; i < totalCases; i++) {

                    initQueueMap(recQueues, i);

                    Comparison comparisonModule = new Comparison(x.get(i), y.get(i),
                            binaryTiShares, oneShare, commonSender,
                            recQueues.get(i), new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, i);

                    System.out.println("submitted " + i + " comparison");

                    Future<Integer> comparisonTask = es.submit(comparisonModule);
                    taskList.add(comparisonTask);
                }
                break;
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            try {
                Integer result = dWorkerResponse.get();
                System.out.println("result:" + result + ", #:" + i);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        teardownModelHandlers();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }

    public void compute() {

        startModelHandlers();

        callArgMax();
        //callOIS();
        //callOR_XOR();
        //callBitDecomposition();

        // pass 1 - multiplication, 2 - dot product and 3 - comparison
        //callProtocol(3);
        teardownModelHandlers();

    }

}
