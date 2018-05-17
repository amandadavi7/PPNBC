/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BroadcastAgent;

import Communication.Message;
import Utility.Connection;
import Utility.Constants;
import Utility.Logging;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anisha
 */
public class BA {

    private static ServerSocket socketServer;       // Server to broadcast messages
    private static ExecutorService clientHandlerThreads, es;
    private static List<Future<Boolean>> senderFutureList;
    private static List<Future<Boolean>> receiverFutureList;

    static int baPort, partyCount;
    static List<String[]> partyAddress;
    private static BlockingQueue<BaMessagePacket> receiverQueue;
    protected static ConcurrentHashMap<Integer, BlockingQueue<Message>> senderQueues;
    private static int clientId;
    
    public static void main(String[] args) {
        if (args.length < 3) {
            Logging.baUsage();
            System.exit(0);
        }

        initializeVariables(args);
        startQueueMappingThread();
        initiateConnections();
        
        //TODO handle teardown flow
        checkAndTeardownThreads();

    }

    private static void initializeVariables(String[] args) {
        clientId = 0;
        partyAddress = new ArrayList<>();
        receiverQueue = new LinkedBlockingQueue<>();
        senderQueues = new ConcurrentHashMap<>();
        clientHandlerThreads = Executors.newFixedThreadPool(Constants.threadCount);
        senderFutureList = new ArrayList<>();
        receiverFutureList = new ArrayList<>();

        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.baUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];
            switch (command) {
                case "port":
                    baPort = Integer.parseInt(value);
                    break;
                case "partyCount":
                    partyCount = Integer.valueOf(value);
                    break;
                case "partyIP":
                    String[] address = value.split(";");
                    for (String curr : address) {
                        String[] currAddress = new String[2];
                        currAddress[0] = curr.split(":")[0];
                        currAddress[1] = curr.split(":")[1];
                        partyAddress.add(currAddress);
                    }
            }
        }

        socketServer = Connection.createServerSocket(baPort);
        if (socketServer == null) {
            System.out.println("Socket creation error");
            System.exit(0);
        }

    }

    private static void initiateConnections() {
        //start n server threads to broadcast
        System.out.println("no. of clients:"+partyAddress.size());
        Socket socket;
        
        for(int i=0;i<partyCount;i++) {
            try {
                socket = socketServer.accept();
                ObjectOutputStream oStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream iStream = new ObjectInputStream(socket.getInputStream());
                // Start BaServerHandler thread for this client
                BaClientReceiver receiverHandler = new BaClientReceiver(socket, 
                        iStream, receiverQueue, clientId, partyCount);
                
                senderQueues.putIfAbsent(clientId, new LinkedBlockingQueue<>());
                BaClientSender senderHandler = new BaClientSender(socket, 
                        oStream, senderQueues.get(clientId));
                
                
                receiverFutureList.add(clientHandlerThreads.submit(receiverHandler));
                senderFutureList.add(clientHandlerThreads.submit(senderHandler));
                System.out.println("client ID:"+clientId+" connected to BA");
                clientId++;

            } catch (IOException ex) {
                Logger.getLogger(BA.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

    private static void startQueueMappingThread() {
        es = Executors.newSingleThreadExecutor();
        BaQueueHandler queueHandler = new BaQueueHandler(partyCount,receiverQueue, senderQueues);
        es.submit(queueHandler);
    }
    
    private static void checkAndTeardownThreads() {
        System.out.println("checking...");
        for(int i=0;i<partyCount;i++) {
            Future<Boolean> recThread = receiverFutureList.get(i);
            try {
                Boolean done = recThread.get();
                System.out.println("cancelling sender");
                senderFutureList.get(i).cancel(true);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(BA.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        es.shutdownNow();
        clientHandlerThreads.shutdownNow();
    }
}
