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
 *
 * @author anisha
 */
public class LrTrainingValidation {

    static String sourceFile;
    static String destDir;
    protected static List<List<BigInteger>> x;
    static BigInteger Zq;
    static int row, col;
    static BigInteger[][] totalInput;
    static int noOfParties;

    public static void main(String[] args) {
        if (args.length < 3) {
            Logging.clientUsage();
            System.exit(0);
        }
        initalizeVariables(args);

        String[] files = sourceFile.split(",");

        for (String file : files) {
            System.out.println("Reading file:" + file);
            List<List<BigInteger>> x = FileIO.loadMatrixFromFile(file, Zq);
            row = x.size();
            col = x.get(0).size();
            if (totalInput == null) {
                totalInput = new BigInteger[row][col];
            }
            System.out.println("totalinput:" + totalInput);
            for (int k = 0; k < row; k++) {
                for (int j = 0; j < col; j++) {
                    //generate n-1 random variables in the range
                    System.out.println("");
                    if (totalInput[k][j] == null) {
                        totalInput[k][j] = x.get(k).get(j);
                    } else {
                        totalInput[k][j] = totalInput[k][j].add(x.get(k).get(j)).mod(Zq);
                    }

                }
            }
        }

        saveToCSV();
    }

    /**
     * initialize input variables from command line
     *
     * @param args command line arguments
     */
    private static void initalizeVariables(String[] args) {
        noOfParties = Integer.parseInt(args[0]);
        System.out.println("Num of parties:" + noOfParties);
        sourceFile = args[1];
        destDir = args[2];
        Zq = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1).nextProbablePrime();  //Zq must be a prime field
        x = new ArrayList<>();

    }

    /**
     * Save shares of input for n parties
     */
    private static void saveToCSV() {
        // TODO: add proper directory location
        String baseFileName = destDir + "/predictedbeta_";
        try (BufferedWriter br = new BufferedWriter(new FileWriter(
                baseFileName + ".csv"))) {
            for (int rowIndex = 0; rowIndex < row; rowIndex++) {
                for (int colIndex = 0; colIndex < col; colIndex++) {
                    br.append(FileIO.ZqToReal(totalInput[rowIndex][colIndex], Constants.decimal_precision, Zq).toPlainString() + ",");
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
