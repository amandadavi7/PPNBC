/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utility;

import Party.Party;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The class deals with all input/ output utils. Have functions related to
 * reading from files, saving to files, and converting data to relevant forms.
 *
 * @author anisha
 */
public class FileIO {

    /**
     * @param x value in reals
     * @param f bit resolution of decimal component
     * @return SMPCVariable of x in Z_q
     */
    public static BigInteger realToZq(double x, int f, BigInteger q) {
        // Our integer space must be at least 2^k
        // TODO: Does this need to be larger given more parties?
        BigDecimal X = BigDecimal.valueOf(x);
        BigDecimal fac = BigDecimal.valueOf(2).pow(f);
        X = X.multiply(fac);

        return X.toBigInteger().mod(q);
    }

    public static BigDecimal ZqToReal(BigInteger x, int f, BigInteger q) {
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
    }

    /**
     * Reads a matrix from a csv and converts it to Zq.
     *
     * @param sourceFile
     * @param Zq
     * @return
     */
    public static List<List<BigInteger>> loadMatrixFromFile(String sourceFile,
            BigInteger Zq) {

        File file = new File(sourceFile);
        Scanner inputStream;
        List<List<BigInteger>> x = new ArrayList<>();

        try {
            inputStream = new Scanner(file);
            int row = 0;
            while (inputStream.hasNext()) {
                String line = inputStream.next();
                Double[] doubleValues = Stream.of(line.split(","))
                        .map(Double::valueOf).toArray(Double[]::new);

                int col = doubleValues.length;
                List<BigInteger> bigIntegerlist = new ArrayList<>();
                for (int i = 0; i < col; i++) {
                    bigIntegerlist.add(realToZq(doubleValues[i],
                            Constants.decimal_precision, Zq));
                }

                x.add(bigIntegerlist);

            }

            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return x;

    }

    public static List<List<BigInteger>> loadMatrixFromFile(String sourceFile) {

        File file = new File(sourceFile);
        Scanner inputStream;
        List<List<BigInteger>> x = new ArrayList<>();

        try {
            inputStream = new Scanner(file);
            int row = 0;
            while (inputStream.hasNext()) {
                String line = inputStream.next();
                String[] values = line.split(",");
                List<BigInteger> bigIntegerlist = new ArrayList<>();
                for (int i = 0; i < values.length; i++) {
                    bigIntegerlist.add(new BigInteger(values[i]));
                }

                x.add(bigIntegerlist);

            }

            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return x;

    }

    /**
     * Reads a matrix from a csv and converts it to Zq.
     *
     * @param sourceFile
     * @param Zq
     * @return
     */
    public static List<BigInteger> loadListFromFile(String sourceFile,
            BigInteger Zq) {

        File file = new File(sourceFile);
        Scanner inputStream;
        List<BigInteger> x = new ArrayList<>();

        try {
            inputStream = new Scanner(file);
            int row = 0;
            while (inputStream.hasNext()) {
                String line = inputStream.next();
                Double[] doubleValues = Stream.of(line.split(","))
                        .map(Double::valueOf).toArray(Double[]::new);

                int col = doubleValues.length;

                x.add(realToZq(doubleValues[0],
                        Constants.decimal_precision, Zq));

            }

            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return x;

    }
    
    public static List<BigInteger> loadListFromFile(String sourceFile) {

        File file = new File(sourceFile);
        Scanner inputStream;
        List<BigInteger> x = new ArrayList<>();

        try {
            inputStream = new Scanner(file);
            int row = 0;
            while (inputStream.hasNext()) {
                String line = inputStream.next();
                int i = 0;
                for(String value: line.split(",")) {
                    x.add(new BigInteger(value));
                }
            }

            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return x;

    }

    public static void writeToCSV(BigInteger[] y, String outputPath,
            String filePrefix, int clientId) {
        int len = y.length;
        try {
            try (FileWriter writer = new FileWriter(outputPath + filePrefix + 
                    "_" + clientId + ".csv")) {
                for (int i = 0; i < len; i++) {
                    writer.write(y[i].toString());
                    writer.write("\n");
                }
            }
            System.out.println("Written all lines");
        } catch (IOException ex) {
            Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void writeToCSV(BigInteger[][] y, String outputPath, 
            String filePrefix, int clientId) {
        int rows = y.length;
        int cols = y[0].length;
        try {
            try (FileWriter writer = new FileWriter(outputPath + filePrefix + 
                    "_" + clientId + ".csv")) {
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        writer.write(y[i][j].toString() + ",");
                    }
                    writer.write("\n");
                }
            }
            System.out.println("Written all lines");
        } catch (IOException ex) {
            Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static List<List<Integer>> loadIntListFromFile(String csvFile) {
        BufferedReader buf = null;
        List<List<Integer>> value = new ArrayList<>();
        try {
            buf = new BufferedReader(new FileReader(csvFile));

            try {
                buf = new BufferedReader(new FileReader(csvFile));
                String line = null;
                while ((line = buf.readLine()) != null) {
                    int lineInt[] = Arrays.stream(line.split(",")).mapToInt(Integer::parseInt).toArray();
                    List<Integer> yline = Arrays.stream(lineInt).boxed().collect(Collectors.toList());
                    value.add(yline);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Party.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                buf.close();
            } catch (IOException ex) {
                Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return value;
    }

}
