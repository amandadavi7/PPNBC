/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utility;

/**
 * Contains all logging functions for cleaner code, and easy debugging.
 * @author anisha
 */
public class Logging {

    /**
     * Log shares from a list
     *
     * @param variableName
     * @param shares
     */
    public static void logShares(String variableName, Integer[] shares) {
        int n = 0;
        System.out.print(variableName + ": ");
        for (int share : shares) {
            System.out.print(share + " ");
        }
        System.out.println("");
    }

    /**
     * Log a single value
     *
     * @param variableName
     * @param value
     */
    public static void logValue(String variableName, int value) {
        System.out.println(variableName + ": " + value);
    }

    /**
     * Guidelines to use the Party class
     */
    public static void partyUsage() {
        System.out.println("java Party.Party party_port=<port> ti=<TI port> "
                + "peer_port=<peer port> party_id=<Party Id> "
                + "values=<secret share>");
    }

    /**
     * Guideline to use the TI class
     */
    public static void tiUsage() {
        System.out.println("java TI <port>");
    }

}
