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
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.stream.Collectors;

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
    private static int oneShares;

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
                case "oneShares":
                    oneShares = Integer.parseInt(value);
                    break;
                case "model":
                    modelId = Integer.parseInt(value);
                    break;
                case "partyCount":
                    partyCount = Integer.parseInt(value);
            }

        }

        socketServer = Connection.createServerSocket(port);
        if (socketServer == null) {
            System.out.println("Socket creation error");
            System.exit(0);
        }

    }

    public static void main(String[] args) {
        if (args.length < 6) {
            Logging.partyUsage();
            System.exit(0);
        }

        initalizeVariables(args);

        initalizeModelVariables(args);

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
                if (partyId == 1) {
                    int[] leafToClassIndexMapping = new int[5];
                    leafToClassIndexMapping[1] = 1;
                    leafToClassIndexMapping[2] = 2;
                    leafToClassIndexMapping[3] = 3;
                    leafToClassIndexMapping[4] = 1;
                    int[] nodeToAttributeIndexMapping = new int[3];
                    nodeToAttributeIndexMapping[0] = 0;
                    nodeToAttributeIndexMapping[1] = 1;
                    nodeToAttributeIndexMapping[2] = 2;
                    int[] attributeThresholds = new int[3];
                    attributeThresholds[0] = 10;
                    attributeThresholds[1] = 5;
                    attributeThresholds[2] = 20;
                    DecisionTreeScoring DTree = new DecisionTreeScoring(oneShares, senderQueue, receiverQueue, partyId, tiShares.binaryShares,
                            tiShares.decimalShares, 2, 3, 5, leafToClassIndexMapping, nodeToAttributeIndexMapping, attributeThresholds, 3, partyCount);
                    DTree.ScoreDecisionTree();

                } else if (partyId == 2) {

                    DecisionTreeScoring DScore = new DecisionTreeScoring(oneShares, senderQueue, receiverQueue, partyId, tiShares.binaryShares,
                            tiShares.decimalShares, 2, 3, 5, vShares.get(0), 3, partyCount);

                    DScore.ScoreDecisionTree();
                }
                break;

            case 2:
                // LR Evaluation
                LinearRegressionEvaluation regressionEvaluationModel
                        = new LinearRegressionEvaluation(tiShares.bigIntShares,
                                oneShares,
                                senderQueue, receiverQueue, partyId,
                                partyCount, args);

                regressionEvaluationModel.predictValues();
                break;

            case 3:
                // LR Evaluation
                LinearRegressionTraining regressionTrainingModel
                        = new LinearRegressionTraining(tiShares.bigIntShares,
                                senderQueue, receiverQueue, partyId,
                                oneShares, partyCount, args);

                regressionTrainingModel.trainModel();
                break;

            default:
                // test model
                TestModel testModel = new TestModel(tiShares.binaryShares, 
                        tiShares.decimalShares, tiShares.bigIntShares,
                        oneShares, senderQueue, receiverQueue, partyId, 
                        partyCount, args);

                testModel.compute();
                break;
        }
    }

    private static void initalizeModelVariables(String[] args) {

        switch (modelId) {
            case 1:
                // DT Scoring
                for (String arg : args) {
                    String[] currInput = arg.split("=");
                    if (currInput.length < 2) {
                        Logging.partyUsage();
                        System.exit(0);
                    }
                    String command = currInput[0];
                    String value = currInput[1];

                    switch (command) {
                        case "vShares":
                            try {
                                BufferedReader buf = new BufferedReader(new FileReader(value));
                                String line = null;
                                while ((line = buf.readLine()) != null) {
                                    String[] vListShares = line.split(";");
                                    List<List<Integer>> vline = new ArrayList<>();
                                    for (String str : vListShares) {
                                        int lineInt[] = Arrays.stream(str.split(",")).mapToInt(Integer::parseInt).toArray();
                                        vline.add(Arrays.stream(lineInt).boxed().collect(Collectors.toList()));
                                    }
                                    vShares.add(vline);
                                }
                            } catch (FileNotFoundException ex) {
                                Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            break;
                    }

                }

                break;

        }
    }

}
