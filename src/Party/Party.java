/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import Communication.Message;
import Model.DecisionTreeScoring;
import Model.KNN;
import Model.LinearRegressionEvaluation;
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
 *
 * @author anisha
 */
public class Party {

    private static ServerSocket socketServer;       // The socket connection for Peer acting as server

    private static ExecutorService partySocketEs;
    private static List<Future<?>> socketFutureList;
    private static TIShare tiShares;

    private static BlockingQueue<Message> senderQueue;
    private static BlockingQueue<Message> receiverQueue;
    private static int partyId;
    private static int port;
    private static String tiIP;
    private static int tiPort;
    private static String baIP;
    private static int baPort;
    private static int partyCount;

    private static List<List<List<Integer>>> vShares;
    private static int asymmetricBit;

    private static int modelId;
    private static Socket clientSocket;

    /**
     * Initialize class variables
     *
     * @param args
     */
    public static void initalizeVariables(String[] args) {
        vShares = new ArrayList<>();
        senderQueue = new LinkedBlockingQueue<>();
        receiverQueue = new LinkedBlockingQueue<>();
        partySocketEs = Executors.newFixedThreadPool(2);
        socketFutureList = new ArrayList<>();
        tiShares = new TIShare();
        partyId = -1;

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

    }

    public static void main(String[] args) {
        if (args.length < 6) {
            Logging.partyUsage();
            System.exit(0);
        }

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

    private static void startPartyConnections() {
        System.out.println("creating a party socket");
        ObjectOutputStream oStream = null;
        ObjectInputStream iStream = null;

        if(partyCount==2 && asymmetricBit==1) {
            socketServer = Connection.createServerSocket(port);
            if (socketServer == null) {
                System.out.println("Socket creation error");
                System.exit(0);
            }
            try {
                clientSocket = socketServer.accept();
            } catch (IOException ex) {
                Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } else {
            clientSocket = Connection.initializeClientConnection(
                        baIP, baPort);   
        }
        
        try {
            oStream = new ObjectOutputStream(clientSocket.getOutputStream());
            iStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Client thread starting");
        PartyClient partyClient = new PartyClient(receiverQueue, clientSocket, iStream);
        socketFutureList.add(partySocketEs.submit(partyClient));

        System.out.println("Server thread starting");
        PartyServer partyServer = new PartyServer(clientSocket, senderQueue, oStream);
        socketFutureList.add(partySocketEs.submit(partyServer));
    }

    private static void tearDownSocket() {
        partySocketEs.shutdownNow();
    }

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
                LinearRegressionEvaluation regressionModel
                        = new LinearRegressionEvaluation(tiShares.bigIntShares,
                                asymmetricBit, senderQueue,
                                receiverQueue, partyId, partyCount, args);

                regressionModel.predictValues();
                break;
                
            case 3:
                // KNN
                
                /*KNN knnModel = new KNN(asymmetricBit, senderQueue, receiverQueue, partyId, 
                        tiShares.binaryShares, tiShares.decimalShares, xShares, yShares.get(0), 
                        yShares.get(1), 8, partyCount);
        
                knnModel.KNN_Model();*/
                break;
                
            default:
                // test model
                TestModel testModel = new TestModel(tiShares.binaryShares,
                        tiShares.decimalShares, tiShares.bigIntShares,
                        asymmetricBit, senderQueue, receiverQueue, partyId,
                        partyCount, args);

                testModel.compute();
                break;
        }
    }
}
