/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.Utility.BatcherSortKNN;
import Protocol.Utility.JaccardDistance;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.FileIO;
import Utility.Logging;
import Utility.ThreadPoolManager;
import java.util.ArrayList;
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

        
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        ThreadPoolManager.shutDownThreadService();
        //System.out.println("Label:" + predictedLabel);
        System.out.println("Time taken:" + elapsedTime + "ms");
        return 0;
    }
    
}
