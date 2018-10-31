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
import java.math.BigInteger;
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
public class LinearRegressionComputeDamfRMSE {
    
    private static final Logger LOGGER = Logger.getLogger(
            LinearRegressionComputeDamfRMSE.class.getName());

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
        String predictedYFiles = null;
        String maskedR = null;
        String actualY = null;
        
        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.clientUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];
            switch (command) {
                case "predictedYFiles":
                    predictedYFiles = value;
                    break;
                case "maskedR":
                    maskedR = value;
                    break;
                case "actualY":
                    actualY = value;
                    break;
                case "partyCount":
                    noOfParties = Integer.valueOf(value);
                    break;

            }
        }
        
        if(predictedYFiles == null || actualY == null || maskedR == null || 
                noOfParties < 0) {
            Logging.clientUsage();
            System.exit(1);
        }

        LOGGER.log(Level.INFO, "Num of parties:{0}", noOfParties);
        
        Zq = BigInteger.valueOf(2).pow(Constants.INTEGER_PRECISION
                + 2 * Constants.DECIMAL_PRECISION + 1).nextProbablePrime();  //Zq must be a prime field

        actualYList = FileIO.loadDoubleListFromFile(actualY);

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
        
        List<BigInteger> maskedRList = FileIO.loadListFromFile(maskedR);
        predictedYList = new ArrayList<>(actualYList.size());
        for (int i = 0; i < datasetSize; i++) {
            predictedYListBigInt.set(i, predictedYListBigInt.get(i).
                    subtract(maskedRList.get(i)).mod(Zq));
            predictedYList.add(LocalMath.ZqToReal(predictedYListBigInt.get(i), 
                    Constants.DECIMAL_PRECISION, Zq).doubleValue() / noOfParties); 
        }

    }
    
}
