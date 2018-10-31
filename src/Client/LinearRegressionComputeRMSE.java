/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import Utility.Constants;
import Utility.FileIO;
import Utility.LocalMath;
import Utility.Logging;
import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The client is responsible to split the dataset among n parties over Zq. The
 * shares are then sent to the parties to evaluate the model
 *
 * @author anisha
 */
public class LinearRegressionComputeRMSE {

    private static final Logger LOGGER = Logger.getLogger(
            LinearRegressionComputeRMSE.class.getName());
    static BigInteger Zq;
    static int noOfParties;
    
    static List<Double> actualYList;
    static List<Double> predictedYList;

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            Logging.clientUsage();
            System.exit(0);
        }
        initalizeVariables(args);

        double rmse = LocalMath.computeRMSE(predictedYList, actualYList);
        LOGGER.log(Level.INFO, "rmse:{0}", rmse);
    }

    /**
     * initialize input variables from command line
     *
     * @param args command line arguments
     */
    private static void initalizeVariables(String[] args) {
        noOfParties = Integer.parseInt(args[0]);
        System.out.println("Num of parties:" + noOfParties);

        String predictedYFiles = args[1];
        String actualY = args[2];

        Zq = BigInteger.valueOf(2).pow(Constants.INTEGER_PRECISION
                + 2 * Constants.DECIMAL_PRECISION + 1).nextProbablePrime();  //Zq must be a prime field

        actualYList = loadListFromFile(actualY);

        int datasetSize = actualYList.size();

        List<BigInteger> predictedYListBigInt = new ArrayList<>(actualYList.size());
        for(int i=0;i<datasetSize;i++) {
            predictedYListBigInt.add(BigInteger.ZERO);
        }
        
        for (String yFile : predictedYFiles.split(";")) {
            List<BigInteger> partyPredictedList = FileIO.loadListFromFile(yFile);
            for (int i = 0; i < datasetSize; i++) {
                predictedYListBigInt.set(i, predictedYListBigInt.get(i).add(partyPredictedList.get(i)).mod(Zq));
            }
        }

        predictedYList = new ArrayList<>(actualYList.size());
        for (int i = 0; i < datasetSize; i++) {
            predictedYList.add(LocalMath.ZqToReal(predictedYListBigInt.get(i), 
                    Constants.DECIMAL_PRECISION, Zq).doubleValue());
            
        }

    }
    
    /**
     * Load list of actual values from file
     * TODO: move it to FileIO 
     * @param sourceFile
     * @return 
     */
    public static List<Double> loadListFromFile(String sourceFile) {

        File file = new File(sourceFile);
        Scanner inputStream;
        List<Double> x = new ArrayList<>();

        try {
            inputStream = new Scanner(file);
            while (inputStream.hasNext()) {
                String line = inputStream.next();
                Double value = new Double(line.split(",")[0]);
                x.add(value);

            }

            inputStream.close();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, null, e);
        }

        return x;

    }

}
