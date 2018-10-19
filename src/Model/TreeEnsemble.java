/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.ArgMax;
import Protocol.BitDecomposition;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import Utility.Logging;
import Utility.ThreadPoolManager;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author keerthanaa
 */
public class TreeEnsemble extends Model {

    String csvPath;
    boolean partyHasTrees;
    String[] propertyFiles;
    int treeCount, pid, prime;
    List<TripleByte> binaryTiShares;
    List<Integer[]> treeOutputs;
    List<TripleInteger> decimalTiShares;
    ExecutorService es;

    /**
     * Constructor:
     *
     * Party 1: contains the decision trees Each tree is stored in a properties
     * file the metadata is passed to party as "randomforeststored" contains
     * number of trees and the names of the property files
     *
     * party 2: csv file, properties file with name "randomforestproperties" - list of properties filenames about all
     * the trees
     *
     * @param asymmetricBit
     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param binaryTriples
     * @param decimalTriples
     * @param partyCount
     * @param args
     * @param protocolIdQueue
     * @param protocolID
     */
    public TreeEnsemble(int asymmetricBit,
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, int clientId, List<TripleByte> binaryTriples, 
            List<TripleInteger> decimalTriples, int partyCount, String[] args,
            Queue<Integer> protocolIdQueue, int protocolID) {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);

        initializeModelVariables(args);
        pid = 0;
        this.binaryTiShares = binaryTriples;
        this.decimalTiShares = decimalTriples;
        treeOutputs = new ArrayList<>();
        es = ThreadPoolManager.getInstance();
    }

    private void initializeModelVariables(String[] args) {

        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.partyUsage();
                System.exit(0);
            }

            String command = currInput[0];
            String value = currInput[1];

            switch (command) {
                case "prime":
                    prime = Integer.parseInt(value);
                    break;
                case "testCsv":
                    //party has feature vector
                    csvPath = value;
                    break;
                case "randomforeststored":
                    //party has the tree
                    partyHasTrees = true;
                    Properties prop = new Properties();
                    InputStream input = null;
                    try {
                        input = new FileInputStream(value);
                        prop.load(input);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(DecisionTreeScoring.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(DecisionTreeScoring.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    treeCount = Integer.parseInt(prop.getProperty("treecount"));
                    String str = prop.getProperty("propertyfiles");
                    propertyFiles = str.split(",");
                    break;
                case "randomforestproperties":
                    //party has feature vector
                    partyHasTrees = false;
                    prop = new Properties();
                    input = null;
                    try {
                        input = new FileInputStream(value);
                        prop.load(input);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(DecisionTreeScoring.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(DecisionTreeScoring.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    treeCount = Integer.parseInt(prop.getProperty("treecount"));
                    str = prop.getProperty("propertyfiles");
                    propertyFiles = str.split(",");
                    break;
            }
        }
    }

    public void runTreeEnsembles() {

        List<Future<Integer[]>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // TODO - How to handle TiShares here????
        if (partyHasTrees) {
            for (int i = 0; i < treeCount; i++) {

                System.out.println("calling RF: " + pid);

                String args[] = {"storedtree=" + propertyFiles[i], "prime=" + prime};

                RandomForestDTScoring DTScoreModule = new RandomForestDTScoring(asymmetricBit,
                        pidMapper, commonSender, clientId, binaryTiShares, decimalTiShares,
                        partyCount, args, new LinkedList<>(protocolIdQueue), pid);

                Future<Integer[]> output = es.submit(DTScoreModule);
                taskList.add(output);
                /*Future<Integer[]> output = es.submit(DTScoreModule);
                try {
                    Integer[] result = output.get();
                    treeOutputs.add(result);
                    System.out.println("Output" + Arrays.toString(result));
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(TreeEnsemble.class.getName()).log(Level.SEVERE, null, ex);
                }*/
                pid++;

            }
        } else {
            for (int i = 0; i < treeCount; i++) {

                System.out.println("calling RF: " + pid);

                String args[] = {"testCsv=" + csvPath, "treeproperties=" + propertyFiles[i], "prime=" + prime};

                RandomForestDTScoring DTScoreModule = new RandomForestDTScoring(asymmetricBit,
                        pidMapper, commonSender, clientId, binaryTiShares, decimalTiShares,
                        partyCount, args, new LinkedList<>(protocolIdQueue), pid);

                Future<Integer[]> output = es.submit(DTScoreModule);
                taskList.add(output);
                /*try {
                    Integer[] result = output.get();
                    treeOutputs.add(result);
                    System.out.println("Output" + Arrays.toString(result));
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(TreeEnsemble.class.getName()).log(Level.SEVERE, null, ex);
                }*/
                pid++;
            }
        }

        for (int i = 0; i < treeCount; i++) {
            Future<Integer[]> DTScoreTask = taskList.get(i);
            try {
                treeOutputs.add(DTScoreTask.get());
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TreeEnsemble.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        for(Integer[] output: treeOutputs) {
            System.out.println("output of trees:" + Arrays.toString(output));
        }

        
        
        int classLabelCount = treeOutputs.get(0).length;
        int[] weightedProbabilityVector = new int[classLabelCount];
        
        for(Integer[] output: treeOutputs) {
            for(int i=0; i<classLabelCount;i++) {
                weightedProbabilityVector[i] += output[i];
            }
        }
        
        //System.out.println("weighted prob vector output" + Arrays.toString(weightedProbabilityVector));
        
        List<Future<List<Integer>>> bitDtaskList = new ArrayList<>();
        for(int i=0;i<classLabelCount;i++) {
            BitDecomposition bitDModule = new BitDecomposition(weightedProbabilityVector[i], 
                    binaryTiShares, asymmetricBit, 6, pidMapper, commonSender, 
                    new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, pid, partyCount);
            bitDtaskList.add(es.submit(bitDModule));
            pid++;
        }
        
        List<List<Integer>> bitSharesProbs = new ArrayList<>();
        for(int i=0;i<classLabelCount;i++) {
            Future<List<Integer>> bitDResult = bitDtaskList.get(i);
            try {
                bitSharesProbs.add(bitDResult.get());
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TreeEnsemble.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //System.out.println("bitD result" + bitSharesProbs);
        ArgMax argmaxModule = new ArgMax(bitSharesProbs, binaryTiShares, asymmetricBit,
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue), clientId,
                Constants.binaryPrime, pid, partyCount);
        pid++;
        
        Future<Integer[]> classIndexResult = es.submit(argmaxModule);
        Integer[] finalClassIndex = null;
        try {
            finalClassIndex = classIndexResult.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(TreeEnsemble.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("output final:" + Arrays.toString(finalClassIndex));
        
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        
        System.out.println("Avg time duration:" + elapsedTime);
        ThreadPoolManager.shutDownThreadService();
    }

}
