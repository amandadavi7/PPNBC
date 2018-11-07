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
 * Tree Ensemble - Random Forest and Boosted Decision Trees
 *
 * @author keerthanaa
 */
public class TreeEnsemble extends Model {

    String csvPath;
    boolean partyHasTrees;
    String[] propertyFiles;
    int treeCount, pid, bitLength;
    List<TripleByte> binaryTiShares;
    List<Integer[]> treeOutputs;
    List<TripleInteger> decimalTiShares;
    int prime;
    Logger LOGGER;

    /**
     * Constructor:
     *
     * Party 1: contains the decision trees Each tree is stored in a properties
     * file the metadata is passed to party as "randomforeststored" contains
     * number of trees and the names of the property files
     *
     * party 2: csv file, properties file with name "randomforestproperties" -
     * list of properties filenames about all the trees
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
            Queue<Integer> protocolIdQueue, int protocolID) throws IOException {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);

        initializeModelVariables(args);
        pid = 0;
        this.binaryTiShares = binaryTriples;
        this.decimalTiShares = decimalTriples;
        treeOutputs = new ArrayList<>();
        LOGGER = Logger.getLogger(TreeEnsemble.class.getName());
        this.prime = Constants.PRIME;
        if(prime == -1) {
            throw new IllegalArgumentException("Please add a valid prime to the config file");
        }
    }

    /**
     * Initialize variables
     *
     * @param args
     */
    private void initializeModelVariables(String[] args) throws FileNotFoundException, IOException {

        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.partyUsage();
                System.exit(0);
            }

            String command = currInput[0];
            String value = currInput[1];

            switch (command) {
                case "testCsv":
                    //party has feature vector
                    csvPath = value;
                    break;
                case "randomforeststored":
                    //party has the tree
                    partyHasTrees = true;
                    Properties prop = new Properties();
                    InputStream input = new FileInputStream(value);
                    prop.load(input);
                    treeCount = Integer.parseInt(prop.getProperty("treecount"));
                    String str = prop.getProperty("propertyfiles");
                    propertyFiles = str.split(",");
                    bitLength = Integer.parseInt(prop.getProperty("bitLength"));
                    break;
                case "randomforestproperties":
                    //party has feature vector
                    partyHasTrees = false;
                    prop = new Properties();
                    input = new FileInputStream(value);
                    prop.load(input);
                    treeCount = Integer.parseInt(prop.getProperty("treecount"));
                    str = prop.getProperty("propertyfiles");
                    propertyFiles = str.split(",");
                    bitLength = Integer.parseInt(prop.getProperty("bitLength"));
                    break;
            }
        }
    }

    /**
     * Main method
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public void runTreeEnsembles() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // TODO - How to handle TiShares here????
        String args[];
        for (int i = 0; i < treeCount; i++) {
            LOGGER.info("calling RF: " + pid);
            if (partyHasTrees) {
                args = new String[1];
                args[0] = "storedtree=" + propertyFiles[i];
            } else {
                args = new String[2];
                args[0] = "testCsv=" + csvPath;
                args[1] = "treeproperties=" + propertyFiles[i];
            }

            RandomForestDTScoring DTScoreModule = new RandomForestDTScoring(asymmetricBit,
                    pidMapper, commonSender, clientId, binaryTiShares, decimalTiShares,
                    partyCount, args, new LinkedList<>(protocolIdQueue), pid);

            Future<Integer[]> output = es.submit(DTScoreModule);
            taskList.add(output);
            pid++;
        }

        for (int i = 0; i < treeCount; i++) {
            Future<Integer[]> DTScoreTask = taskList.get(i);
            treeOutputs.add(DTScoreTask.get());
            LOGGER.log(Level.INFO, "received: {0}", i);

        }

        int classLabelCount = treeOutputs.get(0).length;
        int[] weightedProbabilityVector = new int[classLabelCount];
        
        for(Integer[] output: treeOutputs) {
            for(int i = 0; i < classLabelCount; i++) {
                weightedProbabilityVector[i] = Math.floorMod(weightedProbabilityVector[i]+output[i], prime);
            }
        }

        LOGGER.log(Level.FINE, "weighted prob vector output{0}", Arrays.toString(weightedProbabilityVector));

        List<Future<List<Integer>>> bitDtaskList = new ArrayList<>();
        for(int i = 0; i < classLabelCount; i++) {
            BitDecomposition bitDModule = new BitDecomposition(weightedProbabilityVector[i], 
                    binaryTiShares, asymmetricBit, bitLength, pidMapper, commonSender, 
                    new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME, pid, partyCount);
            bitDtaskList.add(es.submit(bitDModule));
            pid++;
        }

        List<List<Integer>> bitSharesProbs = new ArrayList<>();
        for (int i = 0; i < classLabelCount; i++) {
            Future<List<Integer>> bitDResult = bitDtaskList.get(i);
            bitSharesProbs.add(bitDResult.get());
        }

        ArgMax argmaxModule = new ArgMax(bitSharesProbs, binaryTiShares, asymmetricBit,
                pidMapper, commonSender, new LinkedList<>(protocolIdQueue), clientId,
                Constants.BINARY_PRIME, pid, partyCount);
        pid++;

        Future<Integer[]> classIndexResult = es.submit(argmaxModule);
        Integer[] finalClassIndex = classIndexResult.get();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        LOGGER.log(Level.INFO, "output final:{0}", Arrays.toString(finalClassIndex));
        LOGGER.log(Level.INFO, "Avg time duration:{0}", elapsedTime);
    }

}
