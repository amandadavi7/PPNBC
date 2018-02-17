/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Party;

import Communication.Connection;
import Communication.PeerClient;
import Communication.PeerServer;
import Model.TestModel;
import Utility.Constants;
import Utility.Logging;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A party involved in computing a function
 *
 * @author anisha
 */
public class Party {

    private static ServerSocket socketServer;       // The socket connection for Peer acting as server

    //TODO Keerthana -> List<Integer[]>
    // TIShares -> int[#offun][value] : value->u/v/w in mentioned order
    private static Integer[][] tiShares;

    // TODO Keerthana -> Do you think we need this datastructure anymore? 
    // TODO change it to a more object oriented structure
    private static HashMap<Integer, HashMap<String, Integer>> partyShares;
    // <function id, <variable name, value>>

    private static int partyId;
    private static int port;
    private static String tiIP;
    private static int tiPort;
    private static String peerIP;
    private static int peerPort;
    private static int noOfFuncToCompute;

    private static Integer[] xShares;
    private static Integer[] yShares;

    /**
     * Initialize class variables
     *
     * @param args
     */
    public static void initalizeVariables(String[] args) {
        xShares = new Integer[3];
        yShares = new Integer[3];
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
                case "no_func":
                    noOfFuncToCompute = Integer.parseInt(value);
                    break;
                case "xShares":
                    int[] xIntShares = Arrays.stream(value.split(",")).
                            mapToInt(Integer::parseInt).toArray();
                    xShares = Arrays.stream(xIntShares).boxed().toArray(Integer[]::new);
                    break;
                case "yShares":
                    int[] yIntShares = Arrays.stream(value.split(",")).
                            mapToInt(Integer::parseInt).toArray();
                    yShares = Arrays.stream(yIntShares).boxed().toArray(Integer[]::new);
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

        TestModel testModel = new TestModel();
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
        PeerTICommunication ticommunicationObj = new PeerTICommunication(socketTI,
                tiShares, noOfFuncToCompute);
        Future<Integer[][]> sharesReceived = tiEs.submit(ticommunicationObj);

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
        ExecutorService peerServerEs = Executors.newCachedThreadPool();
        PeerServer peerServer = new PeerServer(socketServer);
        peerServerEs.submit(peerServer);

    }

    /**
     * Creates a client thread that receives data from socket and saves it to
     * the corresponding receiver queue
     *
     */
    private static void startClient() {
        System.out.println("Client thread starting");
        ExecutorService peerServerEs = Executors.newCachedThreadPool();
        PeerClient peerClient = new PeerClient(socketServer, peerIP, peerPort);
        peerServerEs.submit(peerClient);

    }

}
