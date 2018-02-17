/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Communication;

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
    //TODO handle all exceptions here

    /**
     * Given a port, create a socket for the server
     *
     * @param port
     * @return socket
     */
    public static ServerSocket createServerSocket(int port) {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(port);
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return socket;
    }

    /**
     * Close the socket. Return false in case of error
     *
     * @param serverSocket
     * @return true - if successfully closed
     */
    public static boolean closeServerSocket(ServerSocket serverSocket) {
        System.out.println("Closing server socket");
        try {
            serverSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;

    }

    /**
     * Create a socket for the client, given the server IP and port
     *
     * @param ip
     * @param port
     * @return socket
     */
    public static Socket initializeClientConnection(String ip, int port) {
        Socket socket = null;
        try {
            socket = new Socket(ip, port);

        } catch (ConnectException ex) {
            socket = null;
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return socket;

    }

}
