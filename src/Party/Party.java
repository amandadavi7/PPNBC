/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import Communication.Message;

import Model.NaiveBayesScoring;
import Utility.Connection;
import TrustedInitializer.TIShare;
import Utility.Logging;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    
    private static int partyId = -1;
    private static int port = -1;
    private static int partyCount = -1;

    private static String tiIP = null;
    private static int tiPort = -1;

    private static String baIP = null;
    private static int baPort = -1;

    private static int asymmetricBit = -1;
    private static String modelName;
    
    private static String protocolName;
    
    private static ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper;

    private static Queue<Integer> protocolIdQueue;

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
                    modelName = value;
                    break;
                case "partyCount":
                    partyCount = Integer.parseInt(value);
                case "protocolName":
                    protocolName = value;
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

        try {
            getSharesFromTI();  // This is a blocking call
        } catch (IOException ex) {
            Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            startPartyConnections();
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
    private static void startPartyConnections() throws IOException {
        System.out.println("creating a party socket");
        ObjectOutputStream oStream = null;
        ObjectInputStream iStream = null;

        clientSocket = Connection.initializeClientConnection(
                        baIP, baPort);   
        
        try {
            oStream = new ObjectOutputStream(clientSocket.getOutputStream());
            iStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("Client thread starting");
        PartyClient partyClient = new PartyClient(pidMapper, clientSocket, iStream);
        socketFutureList.add(partySocketEs.submit(partyClient));

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
        while(!senderQueue.isEmpty()){
        }
        partySocketEs.shutdownNow();
    }

    /**
     * Call the model class with the input args
     * @param args 
     */
    private static void callModel(String[] args) throws InterruptedException, ExecutionException, IOException {
        int modelId = 1;
               
        switch (modelName) {
                
            case "NaiveBayesScoring":
                NaiveBayesScoring bayesScoring = new NaiveBayesScoring(pidMapper, senderQueue, partyId, 
                        asymmetricBit, partyCount, protocolIdQueue, modelId, args, tiShares.binaryShares, tiShares.decimalShares);

                bayesScoring.scoreNaiveBayes();
                
                break;
           
        }
    }
}
