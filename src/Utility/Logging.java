/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains all logging functions for cleaner code, and easy debugging.
 *
 * @author anisha
 */
public class Logging {

    /**
     * Log shares from a list
     *
     * @param variableName
     * @param shares
     */
    public static void logShares(String variableName, ArrayList<Integer> shares) {
        int n = 0;
        System.out.print(variableName + ": ");
        for (int share : shares) {
            System.out.print(share + " ");
        }
        System.out.println("");
    }

    /**
     * Log shares from a list
     *
     * @param variableName
     * @param shares
     */
    public static void logShares(String variableName, HashMap<Integer, Integer> shares) {
        int n = 0;
        System.out.print(variableName + ": ");
        for (Map.Entry<Integer, Integer> share : shares.entrySet()) {
            System.out.print(share.getValue() + " ");
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
        System.out.println("Usage: \n java Party.Party party_port=<port> "
                + "ti=<TI IP:port> \n"
                + "ba=<BA IP:port> \n"
                + "partyCount=<total number of parties involved> \n"
                + "party_id=<Party Id> \n"
                + "asymmetricBit=<asymmetric bit 0/1> \n"
                + "model=<Model Id(1: DT Scoring 2:LR Evaluation 3:KNN "
                + "4:DT Learning default:TestModel)> ");
    }

    /**
     * Guideline to use the TI class
     */
    public static void tiUsage() {
        System.out.println("java TI port=<port> partyCount=<no. of parties> \n"
                + "decimal=<no. of decimal triples> \n"
                + "binary=<no. of binary triples> \n"
                + "real=<no. of realnumber triples> \n"
                + "truncation= <no. of truncation pairs>");
    }

    /**
     * Guideline to use the BA class
     */
    public static void baUsage() {
        System.out.println("java BA port=<port> partyCount=<no. of parties> ");
    }

    /**
     * Guideline to use the Client class
     */
    public static void clientUsage() {
        System.out.println("java LinearRegressionClient <number of parties> "
                + "<inputfilePath> <output path>");
    }
    
    /**
     * Guidelines to use the LinearRegression Training
     */
    public static void lrTrainingUsage() {
        System.out.println("Required fields: xCsv=<input file path for xShares> \n"
                + "yCsv=<input file path for yShares> \n"
                + "output=<output file path for betaShares>");
    }

}
