/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utility;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class deals with socket utils. Have functions related to creation/
 * teardown of sockets, Handle error and connection elements initialization.
 *
 * @author anisha
 */
public class Connection {
    
    /**
     * Given a port, create a socket for the server
     *
     * @param port
     * @return socket
     * @throws java.io.IOException
     */
    public static ServerSocket createServerSocket(int port) throws IOException {
        ServerSocket socket = new ServerSocket(port);
        return socket;
    }

    /**
     * Close the socket. Return false in case of error
     *
     * @param serverSocket
     * @return true - if successfully closed
     * @throws java.io.IOException
     */
    public static boolean closeServerSocket(ServerSocket serverSocket) throws IOException {
        System.out.println("Closing server socket");
        serverSocket.close();
        return true;
    }

    /**
     * Create a socket for the client, given the server IP and port
     *
     * @param ip
     * @param port
     * @return socket
     * @throws java.io.IOException
     */
    public static Socket initializeClientConnection(String ip, int port) throws IOException {
        Socket socket = new Socket(ip, port);
        return socket;
    }

}
