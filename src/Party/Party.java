/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import Communication.Message;
import Model.DecisionTreeScoring;
import Model.LinearRegressionEvaluation;
import Utility.Connection;
import Model.TestModel;
import TrustedInitializer.TIShare;
import TrustedInitializer.Triple;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
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
    private static TIShare tiShares;

    private static BlockingQueue<Message> senderQueue;
    private static BlockingQueue<Message> receiverQueue;
    private static int partyId;
    private static int port;
    private static String tiIP;
    private static int tiPort;
    private static String peerIP;
    private static int peerPort;

    private static List<List<Integer>> xShares;
    private static List<List<Integer>> yShares;
    private static List<List<BigInteger>> xSharesBigInt;
    private static List<BigInteger> ySharesBigInt;

    private static List<List<List<Integer>>> vShares;
    private static int oneShares;
    private static BigInteger Zq;

    /**
     * Initialize class variables
     *
     * @param args
     */
    public static void initalizeVariables(String[] args) {
        xShares = new ArrayList<>();
        yShares = new ArrayList<>();
        vShares = new ArrayList<>();
        senderQueue = new LinkedBlockingQueue<>();
        receiverQueue = new LinkedBlockingQueue<>();
        partySocketEs = Executors.newFixedThreadPool(2);
        tiShares = new TIShare();
        partyId = -1;

        Zq = BigInteger.valueOf(2).pow(Constants.integer_precision 
                + 2 * Constants.decimal_precision + 1).nextProbablePrime();  //Zq must be a prime field

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
                case "peer_port":
                    peerIP = value.split(":")[0];
                    peerPort = Integer.parseInt(value.split(":")[1]);
                    break;
                case "party_id":
                    partyId = Integer.parseInt(value);
                    break;
                case "xShares":
                    String csvFile = value;

                    BufferedReader buf;
                    try {
                        buf = new BufferedReader(new FileReader(csvFile));
                        String line = null;
                        while ((line = buf.readLine()) != null) {
                            int lineInt[] = Arrays.stream(line.split(",")).mapToInt(Integer::parseInt).toArray();
                            List<Integer> xline = Arrays.stream(lineInt).boxed().collect(Collectors.toList());
                            xShares.add(xline);
                        }
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                case "oneShares":
                    oneShares = Integer.parseInt(value);
                    break;
                case "yShares":
                    csvFile = value;

                    try {
                        buf = new BufferedReader(new FileReader(csvFile));
                        String line = null;
                        while((line = buf.readLine()) != null){
                            int lineInt[] = Arrays.stream(line.split(",")).mapToInt(Integer::parseInt).toArray();
                            List<Integer> yline = Arrays.stream(lineInt).boxed().collect(Collectors.toList());
                            yShares.add(yline);
                        }
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                case "vShares":
                    csvFile = value;
                    try {
                        buf = new BufferedReader(new FileReader(csvFile));
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
                case "xCsv":
                    csvFile = value;
                    xSharesBigInt = FileIO.loadMatrixFromFile(csvFile);
                    break;
                case "yCsv":
                    csvFile = value;
                    //TODO generalize it
                    ySharesBigInt = FileIO.loadListFromFile(csvFile, Zq);
                    break;
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

        getSharesFromTI();  // This is a blocking call

        startServer();
        startClient();

        /*
        LinearRegressionEvaluation regressionModel
                = new LinearRegressionEvaluation(xSharesBigInt, ySharesBigInt,
                        tiShares.decimalShares, oneShares, senderQueue,
                        receiverQueue, partyId, Zq);

        regressionModel.predictValues();*/

        /*
        KNN knnModel = new KNN(oneShares, senderQueue, receiverQueue, partyId, 
                tiShares.binaryShares, tiShares.decimalShares, xShares, yShares.get(0), 
                yShares.get(1), 8);
        
        knnModel.KNN_Model();
        */
        
        
        TestModel testModel = new TestModel(xShares, yShares, vShares, 
              tiShares.binaryShares, tiShares.decimalShares,oneShares, senderQueue, receiverQueue, partyId);
        
        testModel.compute();
         
//DTSCORING:
//        if(partyId==1) {
//            
//            int[] leafToClassIndexMapping = new int[5];
//            leafToClassIndexMapping[1] = 1;
//            leafToClassIndexMapping[2] = 2;
//            leafToClassIndexMapping[3] = 3;
//            leafToClassIndexMapping[4] = 1;
//            int[] nodeToAttributeIndexMapping = new int[3];
//            nodeToAttributeIndexMapping[0] = 0;
//            nodeToAttributeIndexMapping[1] = 1;
//            nodeToAttributeIndexMapping[2] = 2;
//            int[] attributeThresholds = new int[3];
//            attributeThresholds[0] = 10;
//            attributeThresholds[1] = 5;
//            attributeThresholds[2] = 20;
//            DecisionTreeScoring DTree = new DecisionTreeScoring(oneShares, senderQueue, receiverQueue, partyId, tiShares.binaryShares, 
//                    tiShares.decimalShares, 2, 3, 5, leafToClassIndexMapping, nodeToAttributeIndexMapping, attributeThresholds, 3);
//            DTree.ScoreDecisionTree();
//        
//        } else if(partyId==2) {
//            
//            DecisionTreeScoring DScore = new DecisionTreeScoring(oneShares, senderQueue, receiverQueue, partyId, tiShares.binaryShares, 
//                    tiShares.decimalShares, 2, 3, 5, vShares.get(0), 3);
//            
//            
//            DScore.ScoreDecisionTree();
//        }

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
     * Creates a server thread that sends data from sender queue to the given
     * peer/ set of peers
     */
    private static void startServer() {
        System.out.println("Server thread starting");
        PartyServer partyServer = new PartyServer(socketServer, senderQueue);
        partySocketEs.submit(partyServer);

    }

    /**
     * Creates a client thread that receives data from socket and saves it to
     * the corresponding receiver queue
     *
     */
    private static void startClient() {
        System.out.println("Client thread starting");
        PartyClient partyClient = new PartyClient(receiverQueue, peerIP, peerPort);
        partySocketEs.submit(partyClient);

    }

}
