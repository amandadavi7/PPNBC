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
 * The client is responsible to split the dataset among n parties over Zq. 
 * The shares are then sent to the parties to evaluate the model
 * @author anisha
 */
public class LinearRegressionClient {

    static String sourceFile;
    protected static List<List<BigInteger>> x;
    static BigInteger Zq;
    static int row, col;
    static BigInteger[][][] partyInput;

    public static void main(String[] args) {
        if (args.length < 1) {
            Logging.clientUsage();
            System.exit(0);
        }
        initalizeVariables(args);

        x = FileIO.loadMatrixFromFile(sourceFile, Zq);
        
        row = x.size();
        System.out.println("row:"+row);
        col = x.get(0).size();

        partyInput = new BigInteger[Constants.clientCount][row][col];
        
        splitInput();
        saveToCSV();
    }

    private static void initalizeVariables(String[] args) {
        sourceFile = args[0];
        Zq = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1).nextProbablePrime();  //Zq must be a prime field
        System.out.println("Field: Zq = " + Zq);
        x = new ArrayList<>();

    }

    private static void splitInput() {
        SecureRandom srng = new SecureRandom();
        System.out.println("row:"+row+", col:"+col);
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                //generate n-1 random variables in the range
                BigInteger totalSum = BigInteger.ZERO;
                for (int k = 0; k < Constants.clientCount - 1; k++) {
                    BigInteger xK = new BigInteger(Constants.integer_precision
                            + 2 * Constants.decimal_precision, srng).mod(Zq);
                    partyInput[k][i][j] = xK;
                    totalSum = totalSum.add(xK).mod(Zq);
                }
                BigInteger xK = x.get(i).get(j).subtract(totalSum).mod(Zq);
                partyInput[Constants.clientCount - 1][i][j] = xK;
            }
        }
    }

    private static void saveToCSV() {
        // TODO: add proper directory location
        String baseFileName = "thetaPower_";
        for (int partyId = 0; partyId < Constants.clientCount; partyId++) {
            try (BufferedWriter br = new BufferedWriter(new FileWriter(baseFileName + partyId+".csv"))) {
                for (int rowIndex = 0; rowIndex < row; rowIndex++) {
                    for (int colIndex = 0; colIndex < col; colIndex++) {
                        br.append(partyInput[partyId][rowIndex][colIndex]+",");
                    }
                    br.append("\n");
                    br.flush();
                }
                br.close();

            } catch (IOException ex) {
                Logger.getLogger(LinearRegressionClient.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        
    }

}
