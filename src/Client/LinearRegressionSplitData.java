/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The client is responsible to split the dataset among n parties over Zq. The
 * shares are then sent to the parties to evaluate the model
 *
 * @author anisha
 */
public class LinearRegressionSplitData {

    protected static List<List<BigInteger>> x;

    static String sourceFile;
    static String destDir;

    static BigInteger Zq;
    static int row, col;
    static int noOfParties;

    static BigInteger[][][] partyInput;

    public static void main(String[] args) {
        if (args.length < 3) {
            Logging.clientUsage();
            System.exit(0);
        }
        initalizeVariables(args);

        x = FileIO.loadMatrixFromFile(sourceFile, Zq);

        row = x.size();
        col = x.get(0).size();

        partyInput = new BigInteger[noOfParties][row][col];

        splitInput();
        saveToCSV();
    }

    /**
     * initialize input variables from command line
     *
     * @param args command line arguments
     */
    private static void initalizeVariables(String[] args) {

        noOfParties = -1;
        sourceFile = null;
        destDir = null;
        x = new ArrayList<>();
        Zq = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1)
                .nextProbablePrime();  //Zq must be a prime field

        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.baUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];
            switch (command) {
                case "sourceFile":
                    sourceFile = value;
                    break;
                case "destPath":
                    destDir = value;
                    break;
                case "partyCount":
                    noOfParties = Integer.valueOf(value);
                    break;

            }
        }

        if (noOfParties == -1 || sourceFile == null || destDir == null) {
            Logging.clientUsage();
            System.exit(0);
        }

        System.out.println("Num of parties:" + noOfParties);
    }

    /**
     * Convert the input to Zq and split it between n parties
     */
    private static void splitInput() {
        SecureRandom srng = new SecureRandom();
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                //generate n-1 random variables in the range
                BigInteger totalSum = BigInteger.ZERO;
                for (int k = 0; k < noOfParties - 1; k++) {
                    BigInteger xK = new BigInteger(Constants.integer_precision
                            + 2 * Constants.decimal_precision, srng).mod(Zq);
                    partyInput[k][i][j] = xK;
                    totalSum = totalSum.add(xK).mod(Zq);
                }
                BigInteger xK = x.get(i).get(j).subtract(totalSum).mod(Zq);
                partyInput[noOfParties - 1][i][j] = xK;
            }
        }
    }

    /**
     * Save shares of input for n parties
     */
    private static void saveToCSV() {
        // TODO: add proper directory location
        String baseFileName = destDir + "/thetaPower_";
        for (int partyId = 0; partyId < noOfParties; partyId++) {
            try (BufferedWriter br = new BufferedWriter(new FileWriter(
                    baseFileName + partyId + ".csv"))) {
                for (int rowIndex = 0; rowIndex < row; rowIndex++) {
                    for (int colIndex = 0; colIndex < col; colIndex++) {
                        br.append(partyInput[partyId][rowIndex][colIndex].toString()+",");
                    }
                    br.append("\n");
                    br.flush();
                }
                br.close();

            } catch (IOException ex) {
                Logger.getLogger(LinearRegressionSplitData.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

}
