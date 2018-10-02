/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.OR_XOR;
import Protocol.Utility.CrossMultiplyCompare;
import Protocol.Utility.JaccardDistance;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;
import Utility.ThreadPoolManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author keerthanaa
 */
public class KNNBinarySearch extends Model {
    
    List<List<Integer>> trainingShares;
    List<Integer> testShare;
    List<Integer> classLabels;
    List<List<Integer>> jaccardDistances;
    int pid, attrLength, K, decimalTiIndex, binaryTiIndex, trainingSharesCount;
    int ccTICount, prime, bitLength, comparisonTICount, bitDTICount;
    List<TripleInteger> decimalTiShares;
    List<TripleByte> binaryTiShares;
    ExecutorService es;
    
    public KNNBinarySearch(int asymmetricBit, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue, int clientId, List<TripleByte> binaryTriples,
            List<TripleInteger> decimalTriples, int partyCount, String[] args) {
        
        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount);
        
        pid = 0;
        initalizeModelVariables(args);
        
        this.prime = (int) Math.pow(2.0, bitLength);
        comparisonTICount = (2*bitLength) + ((bitLength*(bitLength-1))/2);
        bitDTICount = bitLength*3 - 2;
        ccTICount = comparisonTICount + 2*bitDTICount;

        this.attrLength = testShare.size();
        this.trainingSharesCount = trainingShares.size();
        
        this.binaryTiShares = binaryTriples;
        this.decimalTiShares = decimalTriples;

        this.decimalTiIndex = 0;
        this.binaryTiIndex = 0;

        es = ThreadPoolManager.getInstance();
    }
    
    private void initalizeModelVariables(String[] args) {

        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.partyUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];

            switch (command) {
                case "trainingShares":
                    trainingShares = FileIO.loadIntListFromFile(value);
                    break;
                case "testShares":
                    testShare = (FileIO.loadIntListFromFile(value)).get(0);
                    break;
                case "classLabels":
                    classLabels = (FileIO.loadIntListFromFile(value)).get(0);
                    break;
                case "K":
                    K = Integer.parseInt(value);
                    break;
                case "bitLength":
                    bitLength = Integer.parseInt(value);
                    break;

            }

        }

        //System.out.println("Test Print: training=" + trainingShares + " test=" +
        //        testShare + " classL=" + classLabels + " K=" + K);
    }
    
    public int binarySearch(int lbound, int ubound) {
        int threshold = asymmetricBit * (lbound + ubound)/2;
        int stoppingBit = 0;
        Integer[] comparisonResults = new Integer[trainingSharesCount];
        
        while(stoppingBit == 0) {
            //compute no. of elements greater than threshold
            List<Future<Integer>> ccTaskList = new ArrayList<>();
            for(int i=0;i<trainingSharesCount;i++) {
                CrossMultiplyCompare ccModule = new CrossMultiplyCompare(jaccardDistances.get(i).get(1),
                        jaccardDistances.get(i).get(0), threshold, asymmetricBit, asymmetricBit,
                        decimalTiShares, binaryTiShares, pidMapper, commonSender,
                        clientId, prime, Constants.binaryPrime, pid,
                        new LinkedList<>(protocolIdQueue), partyCount, bitLength);
                Future<Integer> task = es.submit(ccModule);
                ccTaskList.add(task);
                pid++;
            }
            
            for(int i=0;i<trainingSharesCount;i++) {
                Future<Integer> task = ccTaskList.get(i);
                try {
                    comparisonResults[i] = task.get();
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(KNNBinarySearch.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            List<Integer> dummy = new ArrayList<>(Collections.nCopies(K, 0));
            OR_XOR xorModule;
            // Binary to decimal prime conversion
            if(clientId == 1) {
                xorModule = new OR_XOR(Arrays.asList(comparisonResults),
                        dummy, decimalTiShares, asymmetricBit, 2, pidMapper, commonSender,
                        new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
                
            } else {
                xorModule = new OR_XOR(dummy, Arrays.asList(comparisonResults),
                        decimalTiShares, asymmetricBit, 2, pidMapper, commonSender,
                        new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
            }
            Future<Integer[]> task = es.submit(xorModule);
            try {
                comparisonResults = task.get();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KNNBinarySearch.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            int elementsGreater = 0;
            for(int i: comparisonResults) {
                elementsGreater += comparisonResults[i];
            }
        }
        
        return threshold;
    }
    
    public int KNN_Model() {
        //Jaccard Computation for all the training shares
        //ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        long startTime = System.currentTimeMillis();
        
        int decTICount = attrLength * 2 * trainingSharesCount;
        JaccardDistance jdModule = new JaccardDistance(trainingShares, testShare,
                asymmetricBit, decimalTiShares, pidMapper, commonSender,
                clientId, prime, pid, 
                new LinkedList<>(protocolIdQueue), partyCount);

        Future<List<List<Integer>>> jdTask = es.submit(jdModule);
        pid++;
        //decimalTiIndex += decTICount;
        //es.shutdown();
        try {
            jaccardDistances = jdTask.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KNN.class.getName()).log(Level.SEVERE, null, ex);
        }

        //add class labels to JD data structure (contains OR, XOR and Class Labels)
        for (int i = 0; i < trainingShares.size(); i++) {
            jaccardDistances.get(i).add(classLabels.get(i));
        }

        //System.out.println("jaccarddistances:" + jaccardDistances);
        binarySearch(0, prime);
        
        
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        ThreadPoolManager.shutDownThreadService();
        //System.out.println("Label:" + predictedLabel);
        System.out.println("Time taken:" + elapsedTime + "ms");
        return 0;
    }
    
}
