/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import Communication.Message;
import Utility.Connection;
import Model.TestModel;
import TrustedInitializer.TIShare;
import Utility.Logging;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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

    private static TIShare tiShares;

    // TODO Keerthana -> Do you think we need this datastructure anymore? 
    // TODO change it to a more object oriented structure
    private static HashMap<Integer, HashMap<String, Integer>> partyShares;
    // <function id, <variable name, value>>

    private static BlockingQueue<Message> senderQueue;
    private static BlockingQueue<Message> receiverQueue;
    private static int partyId;
    private static int port;
    private static String tiIP;
    private static int tiPort;
    private static String peerIP;
    private static int peerPort;

    private static List<List<Integer> > xShares;
    private static List<List<Integer> > yShares;
    private static List<List<Integer> > vShares;
    private static int oneShares; 

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
        tiShares = new TIShare();
        partyShares = new HashMap<>();
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
                        while((line = buf.readLine()) != null){
                            int lineInt[] = Arrays.stream(line.split(",")).mapToInt(Integer::parseInt).toArray();
                            List<Integer> xline = Arrays.stream(lineInt).boxed().collect(Collectors.toList());
                            xShares.add(xline);
                            //System.out.println(xline);
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
                            //System.out.println(yline);
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
                        while((line = buf.readLine()) != null){
                            int lineInt[] = Arrays.stream(line.split(",")).mapToInt(Integer::parseInt).toArray();
                            List<Integer> vline = Arrays.stream(lineInt).boxed().collect(Collectors.toList());
                            vShares.add(vline);
                            //System.out.println(vline);
                        }                        
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
                    }
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

        TestModel testModel = new TestModel(xShares, yShares, vShares, 
              tiShares.binaryShares, oneShares, senderQueue, receiverQueue, partyId);
        testModel.compute();
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
        ExecutorService partyServerEs = Executors.newCachedThreadPool();
        PartyServer partyServer = new PartyServer(socketServer, senderQueue);
        partyServerEs.submit(partyServer);

    }

    /**
     * Creates a client thread that receives data from socket and saves it to
     * the corresponding receiver queue
     *
     */
    private static void startClient() {
        System.out.println("Client thread starting");
        ExecutorService partyServerEs = Executors.newCachedThreadPool();
        PartyClient partyrClient = new PartyClient(receiverQueue, peerIP, peerPort);
        partyServerEs.submit(partyrClient);

    }

}
