/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import TrustedInitializer.TripleByte;
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
import java.util.concurrent.BlockingQueue;
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
    int treeCount, pid;
    List<TripleByte> binaryTiShares;
    List<Integer[]> treeOutputs;

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
     * @param senderQueue
     * @param receiverQueue
     * @param clientId
     * @param binaryTriples
     * @param partyCount
     * @param args
     * @param protocolIdQueue
     * @param protocolID
     */
    public TreeEnsemble(int asymmetricBit, BlockingQueue<Message> senderQueue,
            BlockingQueue<Message> receiverQueue, int clientId, List<TripleByte> binaryTriples,
            int partyCount, String[] args, LinkedList<Integer> protocolIdQueue, int protocolID) {

        super(senderQueue, receiverQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);

        initializeModelVariables(args);
        pid = 0;
        this.binaryTiShares = binaryTriples;
        treeOutputs = new ArrayList<>();

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

        startModelHandlers();

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // TODO - How to handle TiShares here????
        if (partyHasTrees) {
            for (int i = 0; i < treeCount; i++) {

                initQueueMap(recQueues, pid);

                String args[] = {"storedtree=" + propertyFiles[i]};

                DecisionTreeScoring DTScoreModule = new DecisionTreeScoring(asymmetricBit,
                        commonSender, recQueues.get(pid), clientId,
                        binaryTiShares, partyCount, args, new LinkedList<>(protocolIdQueue), pid);

                taskList.add(es.submit(DTScoreModule));
                pid++;

            }
        } else {
            for (int i = 0; i < treeCount; i++) {

                initQueueMap(recQueues, pid);

                String args[] = {"testCsv=" + csvPath, "treeproperties=" + propertyFiles[i]};

                DecisionTreeScoring DTScoreModule = new DecisionTreeScoring(asymmetricBit,
                        commonSender, recQueues.get(pid), clientId,
                        binaryTiShares, partyCount, args, new LinkedList<>(protocolIdQueue), pid);

                taskList.add(es.submit(DTScoreModule));
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
        
        //bitwise addition and argmax

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;

        for(Integer[] output: treeOutputs) {
            System.out.println("output:" + Arrays.toString(output));
        }
        System.out.println("Avg time duration:" + elapsedTime);

        teardownModelHandlers();
    }

}
