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

    private static final Logger LOGGER = Logger.getLogger(FileIO.class.getName());

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
                    bigIntegerlist.add(LocalMath.realToZq(doubleValues[i],
                            Constants.DECIMAL_PRECISION, Zq));
                }

                x.add(bigIntegerlist);

            }

            inputStream.close();
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "File not found: ", ex);
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
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "File not found: ", ex);
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

                x.add(LocalMath.realToZq(doubleValues[0],
                        Constants.DECIMAL_PRECISION, Zq));

            }

            inputStream.close();
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "File not found: ", ex);
        }

        return x;

    }

    /**
     * Load list of values from file as BigInteger
     *
     * @param sourceFile
     * @return
     */
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
                for (String value : line.split(",")) {
                    x.add(new BigInteger(value));
                }
            }

            inputStream.close();
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "File not found: ", ex);
        }

        return x;

    }

    public static void writeToCSV(BigInteger[] y, String outputPath,
            String filePrefix, int clientId) {
        int len = y.length;
        try {
            try (FileWriter writer = new FileWriter(outputPath + filePrefix
                    + "_" + clientId + ".csv")) {
                for (int i = 0; i < len; i++) {
                    writer.write(y[i].toString());
                    writer.write("\n");
                }
            }
            System.out.println("Written all lines");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "File write exception: ", ex);
        }
    }

    public static void writeToCSV(List<BigInteger> y, String outputPath,
            String filePrefix, int clientId) {
        int len = y.size();
        try {
            try (FileWriter writer = new FileWriter(outputPath + filePrefix
                    + "_" + clientId + ".csv")) {
                for (int i = 0; i < len; i++) {
                    writer.write(y.get(i).toString());
                    writer.write("\n");
                }
            }
            System.out.println("Written all lines");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "File write exception: ", ex);
        }

    }

    public static void writeToCSV(BigInteger[][] y, String outputPath,
            String filePrefix, int clientId) {
        int rows = y.length;
        int cols = y[0].length;
        try {
            try (FileWriter writer = new FileWriter(outputPath + filePrefix
                    + "_" + clientId + ".csv")) {
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        writer.write(y[i][j].toString() + ",");
                    }
                    writer.write("\n");
                }
            }
            System.out.println("Written all lines");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "File write exception: ", ex);
        }
    }

    public static List<List<Integer>> loadIntListFromFile(String csvFile) {
        BufferedReader buf = null;
        List<List<Integer>> value = new ArrayList<>();
        try {
            buf = new BufferedReader(new FileReader(csvFile));
            String line = null;
            while ((line = buf.readLine()) != null) {
                int lineInt[] = Arrays.stream(line.split(","))
                        .mapToInt(Integer::parseInt).toArray();
                List<Integer> yline = Arrays.stream(lineInt)
                        .boxed().collect(Collectors.toList());
                value.add(yline);
            }
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "File not found: ", ex);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error reading line: ", ex);
        } finally {
            try {
                buf.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "File close error: ", ex);
            }
        }
        return value;
    }

}
