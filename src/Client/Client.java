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
import java.util.ArrayList;
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
    protected static List<List<BigInteger>> x;
    static BigInteger Zq;

    public static void main(String[] args) {
        if (args.length < 1) {
            Logging.clientUsage();
            System.exit(0);
        }
        initalizeVariables(args);

        loadCSVFromFile();
        splitData();
    }

    private static void initalizeVariables(String[] args) {
        sourceFile = args[0];
        List<List<Double>> x = new ArrayList<>();
        Zq = BigInteger.valueOf(2).pow(Constants.integer_precision + 
                2 * Constants.decimal_precision + 1).nextProbablePrime();  //Zq must be a prime field
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
            while (inputStream.hasNext()) {
                String line = inputStream.next();
                List<Double> list = Stream.of(line.split(","))
                        .map(Double::parseDouble)
                        .collect(Collectors.toList());
                
                List<BigInteger> bigIntegerlist = new ArrayList<>();
                list.forEach((listValue) -> {
                    bigIntegerlist.add(realToZq(listValue,Constants.decimal_precision, Zq));
                });
                x.add(bigIntegerlist);
            }

            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private static void splitData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
