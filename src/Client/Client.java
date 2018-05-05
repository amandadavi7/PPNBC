/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import Utility.Constants;
import Utility.Logging;
import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author anisha
 */
public class Client {

    static String sourceFile;
    protected static BigInteger[][] x;
    static BigInteger Zq;
    static int row, col;
    static BigInteger[][][] partyInput;

    public static void main(String[] args) {
        if (args.length < 1) {
            Logging.clientUsage();
            System.exit(0);
        }
        initalizeVariables(args);

        loadCSVFromFile();
        splitInput();
        saveToCSV();
    }

    private static void initalizeVariables(String[] args) {
        sourceFile = args[0];
        Zq = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1).nextProbablePrime();  //Zq must be a prime field
        System.out.println("Field: Zq = " + Zq);

    }

    /**
     * @param x value in reals
     * @param f bit resolution of decimal component
     * @return SMPCVariable of x in Z_q
     */
    static BigInteger realToZq(double x, int f, BigInteger q) {
        // Our integer space must be at least 2^k
        // TODO: Does this need to be larger given more parties?
        BigDecimal X = BigDecimal.valueOf(x);
        BigDecimal fac = BigDecimal.valueOf(2).pow(f);
        X = X.multiply(fac);

        return X.toBigInteger().mod(q);
    }

    static BigDecimal ZqToReal(BigInteger x, int f, BigInteger q) {
        BigInteger partition = q.subtract(BigInteger.ONE).divide(BigInteger.valueOf(2));
        BigInteger inverse = BigInteger.valueOf(2).pow(f);
        BigDecimal Zk;

        // If x is more than half of the field size, it is negative.
        if (x.compareTo(partition) > 0) {
            Zk = new BigDecimal(x.subtract(q));
        } else {
            Zk = new BigDecimal(x);
        }

        BigDecimal Q = Zk.divide(new BigDecimal(inverse), f, RoundingMode.CEILING);
        return Q;
        // return Zk.divide(new BigDecimal(inverse), BigDecimal.ROUND_HALF_UP);
    }

    public static void loadCSVFromFile() {
        File file = new File(sourceFile);
        Scanner inputStream;

        try {
            inputStream = new Scanner(file);
            int row = 0;
            while (inputStream.hasNext()) {
                String line = inputStream.next();
                Double[] doubleValues = Stream.of(line.split(","))
                        .map(Double::valueOf).toArray(Double[]::new);

                /*
                List<Double> list = Stream.of(line.split(","))
                        .map(Double::parseDouble)
                        .collect(Collectors.toList());
                
                List<BigInteger> bigIntegerlist = new ArrayList<>();
                list.forEach((listValue) -> {
                    bigIntegerlist.add(realToZq(listValue,Constants.decimal_precision, Zq));
                });
                x.add(bigIntegerlist);
                 */
                int col = doubleValues.length;
                BigInteger[] bigIntegerlist = new BigInteger[col];
                for (int i = 0; i < col; i++) {
                    bigIntegerlist[i] = realToZq(doubleValues[i],
                            Constants.decimal_precision, Zq);
                }

                x[row++] = bigIntegerlist;

            }

            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        row = x.length;
        col = x[0].length;
        
        partyInput = new BigInteger[Constants.clientCount][row][col];

    }

    private static void splitInput() {
        SecureRandom srng = new SecureRandom();
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                //generate n-1 random variables in the range
                BigInteger totalSum = BigInteger.ZERO;
                for(int k=0;k<Constants.clientCount-1;k++) {
                    BigInteger xK = new BigInteger(Constants.integer_precision
                        + 2 * Constants.decimal_precision, srng).mod(Zq);
                    partyInput[k][row][col] = xK;
                    totalSum = totalSum.add(xK).mod(Zq);
                }
                BigInteger xK = x[row][col].subtract(totalSum).mod(Zq);
                partyInput[Constants.clientCount-1][row][col] = xK;
            }
        }
    }

    private static void saveToCSV() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
