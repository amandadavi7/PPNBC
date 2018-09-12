/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.ArgMax;
import Protocol.BitDecomposition;
import Protocol.Comparison;
import Protocol.DotProductInteger;
import Protocol.MatrixInversion;
import Protocol.MultiplicationInteger;
import Protocol.OIS;
import Protocol.OR_XOR;
import Protocol.Utility.BatchTruncation;
import Protocol.Utility.MatrixMultiplication;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import TrustedInitializer.TripleReal;
import TrustedInitializer.TruncationPair;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;
import Utility.LocalMath;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * A Dummy Model class to test all protocols
 *
 * @author anisha
 */
public class TestModel extends Model {

    List<List<Integer>> x;
    BigInteger[][] xBigInt;
    List<List<Integer>> y;
    List<List<List<Integer>>> v;
    double a, b;

    List<TruncationPair> tiTruncationPair;
    BigInteger prime;
    String outputPath;
    List<TripleByte> binaryTiShares;
    List<TripleInteger> decimalTiShares;
    List<TripleReal> realTiShares;

    /**
     * Constructor
     *
     * @param binaryTriples
     * @param decimalTriples
     * @param realTiShares
     * @param tiTruncationPair
     * @param asymmetricBit
     * @param senderQueue
     * @param pidMapper
     * @param clientId
     * @param partyCount
     * @param args
     */
    public TestModel(List<TripleByte> binaryTriples, List<TripleInteger> decimalTriples,
            List<TripleReal> realTiShares, List<TruncationPair> tiTruncationPair,
            int asymmetricBit, 
            ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue,
            int clientId, int partyCount, String[] args) {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount);

        this.tiTruncationPair = tiTruncationPair;
        prime = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1).nextProbablePrime();
        v = new ArrayList<>();
        this.binaryTiShares = binaryTriples;
        this.decimalTiShares = decimalTriples;
        this.realTiShares = realTiShares;
        initalizeModelVariables(args);

    }

    /**
     * Call bitD protocol for testing 1 test case
     */
    public void callBitDecomposition() {

        ExecutorService es = Executors.newFixedThreadPool(1);
        
        BitDecomposition bitTest = new BitDecomposition(2, binaryTiShares,
                asymmetricBit, Constants.bitLength, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId,
                Constants.binaryPrime, 1, partyCount);

        Future<List<Integer>> bitdecompositionTask = es.submit(bitTest);

        try {
            List<Integer> result = bitdecompositionTask.get();
            System.out.println("result of bitDecomposition: " + result);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Calling ArgMax protocol in parallel for n test cases
     */
    public void callArgMax() {

        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<Integer[]>> taskList = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        int totalCases = v.size();

        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {

            ArgMax argmaxModule = new ArgMax(v.get(i), binaryTiShares, asymmetricBit,
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, Constants.binaryPrime, i, partyCount);

            System.out.println("submitted " + i + " argmax");

            Future<Integer[]> argmaxTask = es.submit(argmaxModule);
            taskList.add(argmaxTask);

        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer[]> dWorkerResponse = taskList.get(i);
            try {
                Integer[] result = dWorkerResponse.get();
                System.out.println("result:" + Arrays.toString(result) + ", #:" + i);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);

    }

    /**
     * Call OR_XOR protocol in parallel for n test cases
     */
    public void callOR_XOR() {

        System.out.println("calling or_xor with x=" + x + " y=" + y);

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();

        for (int i = 0; i < totalCases; i++) {

            OR_XOR or_xor = new OR_XOR(x.get(i), y.get(i), decimalTiShares, 
                    asymmetricBit, 1, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, Constants.prime, i, partyCount);

            Future<Integer[]> task = es.submit(or_xor);
            taskList.add(task);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            try {
                Future<Integer[]> task = taskList.get(i);
                Integer[] result = task.get();
                System.out.println("result: " + i + ": " + Arrays.toString(result));
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);

    }

    /**
     * Call Oblivious Input Selection for 1 test case
     */
    public void callOIS() {

        System.out.println("calling OIS with v" + v);

        ExecutorService es = Executors.newSingleThreadExecutor();

        long startTime = System.currentTimeMillis();

        OIS ois;

        if (v.isEmpty()) {
            System.out.println("v is null");
            ois = new OIS(null, binaryTiShares, asymmetricBit, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId,
                    Constants.binaryPrime, 0, 4, 1, 3, partyCount);
        } else {
            System.out.println("v is not null");
            ois = new OIS(v.get(0), binaryTiShares, asymmetricBit, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId,
                    Constants.binaryPrime, 0, 4, -1, 3, partyCount);
        }

        Future<Integer[]> task = es.submit(ois);

        es.shutdown();

        try {
            Integer[] result = task.get();
            System.out.println("result:" + Arrays.toString(result));
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }

    /**
     * Call multiplication/dot product/comparison protocol for n test cases in
     * parallel
     *
     * @param protocolType
     */
    public void callProtocol(int protocolType) {
        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        // totalcases number of protocols are submitted to the executorservice
        switch (protocolType) {
            case 1:
                for (int i = 0; i < totalCases; i++) {

                    MultiplicationInteger multiplicationModule = new MultiplicationInteger(
                            x.get(i).get(0), y.get(i).get(0),
                            decimalTiShares.get(i), pidMapper, commonSender, 
                            new LinkedList<>(protocolIdQueue), clientId, Constants.prime, i, asymmetricBit, 0, partyCount);

                    System.out.println("Submitted " + i + " multiplication");

                    Future<Integer> multiplicationTask = es.submit(multiplicationModule);
                    taskList.add(multiplicationTask);
                }
                break;
            case 2:
                for (int i = 0; i < totalCases; i++) {

                    DotProductInteger DPModule = new DotProductInteger(x.get(i),
                            y.get(i), decimalTiShares, pidMapper, commonSender, 
                            new LinkedList<>(protocolIdQueue), clientId, Constants.prime, i, asymmetricBit, partyCount);

                    System.out.println("Submitted " + i + " dotproduct");

                    Future<Integer> DPTask = es.submit(DPModule);
                    taskList.add(DPTask);
                }
                break;
            case 3:
                for (int i = 0; i < totalCases; i++) {

                    Comparison comparisonModule = new Comparison(x.get(i), y.get(i),
                            binaryTiShares, asymmetricBit, pidMapper, commonSender,
                            new LinkedList<>(protocolIdQueue), clientId, Constants.binaryPrime, i, partyCount);

                    System.out.println("submitted " + i + " comparison");

                    Future<Integer> comparisonTask = es.submit(comparisonModule);
                    taskList.add(comparisonTask);
                }
                break;
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            try {
                Integer result = dWorkerResponse.get();
                System.out.println("result:" + result + ", #:" + i);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }

    /**
     * Main compute model function for the protocols
     */
    public void compute() {

        callArgMax();
        //callOIS();
        //callOR_XOR();
        //callBitDecomposition();
        // pass 1 - multiplication, 2 - dot product and 3 - comparison
        //callProtocol(1);
        
    }

    /**
     * Input variable initializations
     * @param args 
     */
    private void initalizeModelVariables(String[] args) {

        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.partyUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];

            switch (command) {
                case "xShares":
                    x = FileIO.loadIntListFromFile(value);
                    break;
                case "yShares":
                    y = FileIO.loadIntListFromFile(value);
                    break;
                case "vShares":
                    try {
                        BufferedReader buf = new BufferedReader(new FileReader(value));
                        String line = null;
                        while ((line = buf.readLine()) != null) {
                            String[] vListShares = line.split(";");
                            List<List<Integer>> vline = new ArrayList<>();
                            for (String str : vListShares) {
                                int lineInt[] = Arrays.stream(str.split(",")).mapToInt(Integer::parseInt).toArray();
                                vline.add(Arrays.stream(lineInt).boxed().collect(Collectors.toList()));
                            }
                            v.add(vline);
                        }
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                case "xCsv":
                    List<List<BigInteger>> xList = FileIO.loadMatrixFromFile(value, prime);
                    int row = xList.size();
                    int col = xList.get(0).size();
                    xBigInt = new BigInteger[row][col];
                    for (int i = 0; i < row; i++) {
                        for (int j = 0; j < col; j++) {
                            xBigInt[i][j] = xList.get(i).get(j);
                        }
                    }
                    break;
                case "output":
                    outputPath = value;
                    break;
                case "a":
                    a = Double.parseDouble(value);
                    break;
                case "b":
                    b = Double.parseDouble(value);
                    break;

            }

        }
    }

    private void callMatrixInversion() {
        ExecutorService es = Executors.newFixedThreadPool(1);
        
        long startTime = System.currentTimeMillis();
        MatrixInversion matrixInversion = new MatrixInversion(xBigInt, realTiShares,
                tiTruncationPair,
                1, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, asymmetricBit, partyCount, prime);

        Future<BigInteger[][]> matrixInversionTask = es.submit(matrixInversion);

        BigInteger[][] result = null;
        try {
            result = matrixInversionTask.get();

        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);

        FileIO.writeToCSV(result, outputPath, "matrixInversion", clientId);
    }

    private void callMatrixMultiplication() {
        ExecutorService es = Executors.newFixedThreadPool(1);
        
        int n = xBigInt.length;
        int l = xBigInt[0].length;

        BigInteger xT[][] = LocalMath.transposeMatrix(xBigInt);
        int m = xT[0].length;

        long startTime = System.currentTimeMillis();

        //TODO fix ti share count
        MatrixMultiplication matrixMultiplication = new MatrixMultiplication(
                xBigInt, xT, realTiShares,
                tiTruncationPair,
                clientId, prime, 1, asymmetricBit, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue),
                partyCount);

        Future<BigInteger[][]> matrixMultiplicationTask = es.submit(matrixMultiplication);

        BigInteger[][] result = null;
        try {
            result = matrixMultiplicationTask.get();

        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);

        FileIO.writeToCSV(result, outputPath, "matrixMultiplication", clientId);

    }

    private void callTruncation() {
        System.out.println("calling truncation");

        //Prepare matrix for truncation. Multiply the elements with 2^f
        int rows = xBigInt.length;
        int cols = xBigInt[0].length;
        BigInteger fac = BigInteger.valueOf(2).pow(Constants.decimal_precision);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                xBigInt[i][j] = xBigInt[i][j].multiply(fac).mod(prime);

            }
        }
        BigInteger[][] truncationOutput = new BigInteger[rows][cols];

        ExecutorService es = Executors.newFixedThreadPool(Constants.threadCount);
        List<Future<BigInteger[]>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = xBigInt.length;
        int tiTruncationStartIndex = 0;

        System.out.println("Total testcases:" + totalCases);
        for (int i = 0; i < totalCases; i++) {

            BatchTruncation truncationPair = new BatchTruncation(xBigInt[i],
                    tiTruncationPair.subList(tiTruncationStartIndex,
                            tiTruncationStartIndex + xBigInt[i].length),
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, prime, i, asymmetricBit, partyCount);

            Future<BigInteger[]> task = es.submit(truncationPair);
            taskList.add(task);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            try {
                Future<BigInteger[]> task = taskList.get(i);
                truncationOutput[i] = task.get();

            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);

        FileIO.writeToCSV(truncationOutput, outputPath, "truncation", clientId);

    }

}
