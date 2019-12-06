/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import Communication.Message;
import Model.*;
import Utility.Connection;
import TrustedInitializer.TIShare;
import Utility.Logging;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * A party involved in computing a function Starting point of the application
 *
 * @author anisha
 */
public class Party {

    private static ExecutorService partySocketEs;
    private static List<Future<?>> socketFutureList;
    private static ServerSocket serverSocket;
    private static Socket clientSocket;
    private static TIShare tiShares;

    private static BlockingQueue<Message> senderQueue;

    private static int partyId = -1;
    private static int port = -1;
    private static int partyCount = -1;

    private static String tiIP = null;
    private static int tiPort = -1;

    private static String otherPartyIP = null;
    private static int otherPartyPort = -1;

    private static int asymmetricBit = -1;
    private static String modelName;

    private static String protocolName;

    private static ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper;

    private static Queue<Integer> protocolIdQueue;
    private static List<ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>>> pidMapperList;
    private static int ensembleRounds = 0, treeCount = 0;

    /**
     * Initialize class variables
     *
     * @param args
     */
    public static void initalizeVariables(String[] args) {
        senderQueue = new LinkedBlockingQueue<>();
        partySocketEs = Executors.newFixedThreadPool(2);
        socketFutureList = new ArrayList<>();
        tiShares = new TIShare();
        protocolName = "";
        modelName = "";

        pidMapper = new ConcurrentHashMap<>();

        protocolIdQueue = new LinkedList<>();

        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.partyUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];

            switch (command) {
                case "ti":
                    tiIP = value.split(":")[0];
                    tiPort = Integer.parseInt(value.split(":")[1]);
                    break;
                case "party_port":
                    port = Integer.parseInt(value);
                    break;
                case "other_party":
                    otherPartyIP = value.split(":")[0];
                    otherPartyPort = Integer.parseInt(value.split(":")[1]);
                    break;
                case "party_id":
                    partyId = Integer.parseInt(value);
                    break;
                case "asymmetricBit":
                    asymmetricBit = Integer.parseInt(value);
                    break;
                case "model":
                    modelName = value;
                    break;
                case "partyCount":
                    partyCount = Integer.parseInt(value);
                    break;
                case "protocolName":
                    protocolName = value;
                    break;
                case "ensembleRounds":
                    ensembleRounds = Integer.parseInt(value);
                    break;
                case "treeCount":
                    treeCount = Integer.parseInt(value);
                    break;
            }

        }

        if (tiIP == null || otherPartyIP == null || tiPort == -1 || otherPartyPort == -1
                || port <= 0 || asymmetricBit == -1 || partyCount <= 0
                || partyId < 0) {
            Logging.partyUsage();
            System.exit(0);
        }

    }

    /**
     * Main method
     *
     * @param args
     */
    public static void main(String[] args) {
        initalizeVariables(args);

        try {
            getSharesFromTI();  // This is a blocking call
        } catch (IOException ex) {
            Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            if (modelName.equals("RandomForestClassifierTraining") || modelName.equals("ExtraTrees") || modelName.equals("DecisionTreeTraining")) {
                startPartyConnections(true);
            } else {
                startPartyConnections(false);
            }
        } catch (IOException ex) {
            Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            callModel(args);
        } catch (InterruptedException | ExecutionException | IOException ex) {
            Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
        }

        checkAndTearDownSocket();
    }

    /**
     * Gets shares from TI in a separate blocking thread and saves it.
     */
    private static void getSharesFromTI() throws IOException {
        // Initialize TI socket and receive shares
        Socket socketTI = null;

        System.out.println("Receiving shares from TI");

        // client for TI
        socketTI = Connection.initializeClientConnection(tiIP, tiPort);
        ExecutorService tiEs = Executors.newSingleThreadScheduledExecutor();
        PeerTICommunication ticommunicationObj = new PeerTICommunication(socketTI, tiShares);
        Future<TIShare> sharesReceived = tiEs.submit(ticommunicationObj);

        try {
            tiShares = sharesReceived.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
        }

        tiEs.shutdown();

        try {
            socketTI.close();
        } catch (IOException ex) {
            Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Initiate connections with the BA as a client
     */
    private static void startPartyConnections(boolean multiThread) throws IOException {
        System.out.println("creating a party socket");
        ObjectOutputStream oStream = null;
        ObjectInputStream iStream = null;

        if (asymmetricBit == 0) {
            serverSocket = Connection.createServerSocket(port);
            clientSocket = serverSocket.accept();
        } else {
            clientSocket = Connection.initializeClientConnection(otherPartyIP, otherPartyPort);
        }
        
        try {
            oStream = new ObjectOutputStream(clientSocket.getOutputStream());
            iStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Client thread starting");
        if (!multiThread) {
            PartyClient partyClient = new PartyClient(pidMapper, clientSocket, iStream);
            socketFutureList.add(partySocketEs.submit(partyClient));
        } else {
            System.out.println("Multithreaded mode");
            //ensemble rounds or tree count plus the main thread
            pidMapperList = new LinkedList<>();
            for (int i = 0; i <= Math.max(ensembleRounds, treeCount); i++) {
                pidMapperList.add(new ConcurrentHashMap<>());
            }
            PartyClient partyClient = new PartyClient(pidMapperList, clientSocket, iStream);
            socketFutureList.add(partySocketEs.submit(partyClient));
        }

        System.out.println("Server thread starting");
        PartyServer partyServer = new PartyServer(clientSocket, senderQueue, oStream,
                partyId, asymmetricBit);
        socketFutureList.add(partySocketEs.submit(partyServer));
    }

    /**
     * shut down the party sockets
     */
    private static void checkAndTearDownSocket() {
        // wait for the party to send all messages before closing the socket.
        while (!senderQueue.isEmpty()) {
        }
        partySocketEs.shutdownNow();
    }

    /**
     * Call the model class with the input args
     *
     * @param args
     */
    private static void callModel(String[] args) throws InterruptedException, ExecutionException, IOException {
        int modelId = 1;

        switch (modelName) {
            case "DecisionTreeScoring":
                // DT Scoring
                DecisionTreeScoring DTree = new DecisionTreeScoring(asymmetricBit,
                        pidMapper, senderQueue, partyId, tiShares.binaryShares,
                        partyCount, args, protocolIdQueue, modelId, 0);
                DTree.scoreDecisionTree();
                break;

            case "LinearRegressionEvaluation":
                // LR Evaluation
                LinearRegressionEvaluation regressionEvaluationModel
                        = new LinearRegressionEvaluation(tiShares.realShares,
                                tiShares.truncationPair, asymmetricBit,
                                pidMapper, senderQueue, partyId, partyCount,
                                args, protocolIdQueue, modelId, 0);

                regressionEvaluationModel.predictValues();
                break;

            case "LinearRegressionTraining":
                // LR Training
                LinearRegressionTraining regressionTrainingModel
                        = new LinearRegressionTraining(tiShares.realShares,
                                tiShares.truncationPair, pidMapper, senderQueue, partyId,
                                asymmetricBit, partyCount, args, protocolIdQueue, modelId, 0);

                regressionTrainingModel.trainModel();
                break;

            case "KNNSortAndSwap":
                // KNN

                KNNSortAndSwap knnModel = new KNNSortAndSwap(asymmetricBit, pidMapper,
                        senderQueue, partyId, tiShares.binaryShares,
                        tiShares.decimalShares, partyCount, args, protocolIdQueue, modelId, 0);

                knnModel.runModel();
                break;

            case "KNNThresholdKSelect":
                // KNN threshold K Select (binary search approach)
                KNNThresholdKSelect knnThresholdSelectModel = new KNNThresholdKSelect(
                        asymmetricBit, pidMapper, senderQueue, partyId,
                        tiShares.binaryShares, tiShares.decimalShares, partyCount,
                        args, protocolIdQueue, modelId, 0);
                knnThresholdSelectModel.runModel();
                break;

            case "TreeEnsemble":
                //Random Forest
                TreeEnsemble TEModel = new TreeEnsemble(asymmetricBit, pidMapper,
                        senderQueue, partyId, tiShares.binaryShares,
                        tiShares.decimalShares, partyCount, args, protocolIdQueue, modelId, 0);
                TEModel.runTreeEnsembles();
                break;

            case "LinearRegressionDAMFPrediction":
                // LR Evaluation
                LinearRegressionEvaluationDAMF regressionEvaluationModelDAMF
                        = new LinearRegressionEvaluationDAMF(asymmetricBit, pidMapper, senderQueue,
                                partyId, partyCount, args, protocolIdQueue, modelId, 0);

                regressionEvaluationModelDAMF.predictValues();
                break;

            case "DecisionTreeTraining":
                // Decision Tree Training
                DecisionTreeTraining dtModel = new DecisionTreeTraining(
                        asymmetricBit, pidMapperList.get(0), senderQueue, partyId,
                        tiShares.binaryShares, tiShares.bigIntShares,
                        tiShares.decimalShares,
                        tiShares.bigIntEqualityShares, args, partyCount,
                        protocolIdQueue, modelId, 0);

                dtModel.trainDecisionTree();
                break;

            case "LogisticRegressionTraining":

                LogisticRegressionTraining lrModel = new LogisticRegressionTraining(
                        tiShares.bigIntShares, tiShares.decimalShares,
                        tiShares.binaryShares, pidMapper, senderQueue, partyId, asymmetricBit,
                        partyCount, args, protocolIdQueue, modelId, 0);

                lrModel.trainLogisticRegression();
                break;
            case "RandomForestClassifierTraining":
                RandomForestClassifierTraining randomForestClassifierTraining = new RandomForestClassifierTraining(
                        asymmetricBit, pidMapperList.get(0), senderQueue, partyId,
                        tiShares, args, partyCount,
                        protocolIdQueue, modelId, pidMapperList, 0
                );
                randomForestClassifierTraining.trainRandomForestClassifier();
                break;

            case "ExtraTrees":
                //Extra Trees
                ExtraTreesTraining extraTreesModel = new ExtraTreesTraining(asymmetricBit, pidMapperList.get(0), senderQueue, partyId, partyCount, tiShares.rowShares, tiShares.colShares, tiShares.decimalShares, tiShares.binaryShares, tiShares.bigIntShares, tiShares.wholeNumShares, tiShares.bigIntEqualityShares, args, protocolIdQueue, modelId, pidMapperList, 0);
                extraTreesModel.runModel();
                break;
                
            default:
                // test model
                TestModel testModel = new TestModel(tiShares.binaryShares,
                        tiShares.decimalShares, tiShares.realShares,
                        tiShares.bigIntShares, tiShares.equalityShares,
                        tiShares.truncationPair, asymmetricBit, pidMapper,
                        senderQueue, partyId, partyCount, args, protocolIdQueue,
                        modelId, 0);
                testModel.compute(protocolName);
                break;
        }
    }
}
