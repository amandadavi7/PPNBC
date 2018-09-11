/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import Communication.Message;
import Model.DecisionTreeScoring;
import Model.LinearRegressionEvaluation;
import Model.LinearRegressionTraining;
import Utility.Connection;
import Model.TestModel;
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
 * A party involved in computing a function 
 * Starting point of the application
 *
 * @author anisha
 */
public class Party {

    private static ExecutorService partySocketEs;
    private static List<Future<?>> socketFutureList;
    private static Socket clientSocket;
    private static TIShare tiShares;

    private static BlockingQueue<Message> senderQueue;
    private static BlockingQueue<Message> receiverQueue;

    private static int partyId;
    private static int port;
    private static int partyCount;

    private static String tiIP;
    private static int tiPort;

    private static String baIP;
    private static int baPort;

    private static int asymmetricBit;
    private static int modelId;
    
    private static ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper;

    /**
     * Initialize class variables
     *
     * @param args
     */
    public static void initalizeVariables(String[] args) {
        senderQueue = new LinkedBlockingQueue<>();
        receiverQueue = new LinkedBlockingQueue<>();
        partySocketEs = Executors.newFixedThreadPool(2);
        socketFutureList = new ArrayList<>();
        tiShares = new TIShare();
        partyId = -1;
        asymmetricBit = -1;
        port = -1;
        partyCount = -1;
        tiIP = null;
        tiPort = -1;
        baIP = null;
        baPort = -1;
        
        pidMapper = new ConcurrentHashMap<>();

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
                case "ba":
                    baIP = value.split(":")[0];
                    baPort = Integer.parseInt(value.split(":")[1]);
                    break;
                case "party_id":
                    partyId = Integer.parseInt(value);
                    break;
                case "asymmetricBit":
                    asymmetricBit = Integer.parseInt(value);
                    break;
                case "model":
                    modelId = Integer.parseInt(value);
                    break;
                case "partyCount":
                    partyCount = Integer.parseInt(value);
            }

        }

        if (tiIP == null || baIP == null || tiPort == -1 || baPort == -1 
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

        getSharesFromTI();  // This is a blocking call

        startPartyConnections();

        callModel(args);

        tearDownSocket();
    }

    /**
     * Gets shares from TI in a separate blocking thread and saves it.
     */
    private static void getSharesFromTI() {
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
    private static void startPartyConnections() {
        System.out.println("creating a party socket");
        clientSocket = Connection.initializeClientConnection(
                baIP, baPort);
        ObjectOutputStream oStream = null;
        ObjectInputStream iStream = null;

        try {
            oStream = new ObjectOutputStream(clientSocket.getOutputStream());
            iStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Client thread starting");
        PartyClient partyClient = new PartyClient(pidMapper, clientSocket, iStream);
        // TODO cleanup
        //PartyClient partyClient = new PartyClient(receiverQueue, clientSocket, iStream);
        socketFutureList.add(partySocketEs.submit(partyClient));

        System.out.println("Server thread starting");
        PartyServer partyServer = new PartyServer(clientSocket, senderQueue, oStream);
        socketFutureList.add(partySocketEs.submit(partyServer));
    }

    /**
     * shut down the party sockets
     */
    private static void tearDownSocket() {
        partySocketEs.shutdownNow();
    }

    /**
     * Call the model class with the input args
     * @param args 
     */
    private static void callModel(String[] args) {
        switch (modelId) {
            case 1:
                // DT Scoring
                DecisionTreeScoring DTree = new DecisionTreeScoring(asymmetricBit,
                        senderQueue, receiverQueue, partyId, tiShares.binaryShares,
                        partyCount, args);
                DTree.ScoreDecisionTree();
                break;

            case 2:
                // LR Evaluation
                LinearRegressionEvaluation regressionEvaluationModel
                        = new LinearRegressionEvaluation(tiShares.bigIntShares,
                                tiShares.truncationPair,
                                asymmetricBit, senderQueue,
                                receiverQueue, partyId, partyCount, args);

                regressionEvaluationModel.predictValues();
                break;

            case 3:
                // LR Evaluation
                LinearRegressionTraining regressionTrainingModel
                        = new LinearRegressionTraining(tiShares.bigIntShares,
                                tiShares.truncationPair,
                                senderQueue, receiverQueue, partyId,
                                asymmetricBit, partyCount, args);

                regressionTrainingModel.trainModel();
                break;

            default:
                // test model
                TestModel testModel = new TestModel(tiShares.binaryShares,
                        tiShares.decimalShares, tiShares.bigIntShares,
                        tiShares.truncationPair,
                        asymmetricBit, senderQueue, receiverQueue, partyId,
                        partyCount, args);

                testModel.compute();
                break;
        }
    }
}
