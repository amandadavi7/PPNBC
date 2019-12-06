/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.ArgMax;
import Protocol.BitDecomposition;
import Protocol.BitDecompositionBigInteger;
import Protocol.Comparison;
import Protocol.DotProductInteger;
import Protocol.DotProductBigInteger;
import Protocol.Equality;
import Protocol.MatrixInversion;
import Protocol.MultiplicationInteger;
import Protocol.MultiplicationBigInteger;
import Protocol.OIS;
import Protocol.OR_XOR;
import Protocol.OR_XOR_BigInteger;
import Protocol.Utility.JaccardDistance;
import Protocol.Utility.BatchTruncation;
import Protocol.Utility.MatrixMultiplication;
import Protocol.Utility.BatcherSortKNN;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import TrustedInitializer.TruncationPair;
import TrustedInitializer.TripleBigInteger;
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
    BigInteger[][] xBigIntArr;
    List<List<Integer>> y;
    List<List<List<Integer>>> v;

    List<BigInteger> xBigInt;
    List<BigInteger> yBigInt;


    List<TruncationPair> tiTruncationPair;
    BigInteger bigIntPrime;
    int decPrime;
    String outputPath;
    List<TripleByte> binaryTiShares;
    List<TripleInteger> decimalTiShares;
    List<TripleBigInteger> realTiShares;
    List<Integer> equalityTiShares;
    List<TripleBigInteger> bigIntTiShares;

/* sorting */
    List<List<Integer>> numDenomPairs;
    Integer k;


    /**
     * Constructor
     *
     * @param binaryTriples
     * @param decimalTriples
     * @param realTiShares
     * @param bigIntShares
     * @param equalityTiShares
     * @param tiTruncationPair
     * @param asymmetricBit
     * @param senderQueue
     * @param pidMapper
     * @param clientId
     * @param partyCount
     * @param args
     * @param protocolIdQueue
     * @param protocolID
     */
    public TestModel(List<TripleByte> binaryTriples, List<TripleInteger> decimalTriples,
            List<TripleBigInteger> realTiShares, List<TripleBigInteger> bigIntShares, List<Integer> equalityTiShares, List<TruncationPair> tiTruncationPair,
            int asymmetricBit, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
            BlockingQueue<Message> senderQueue, int clientId, int partyCount, String[] args,
            Queue<Integer> protocolIdQueue, int protocolID, int threadID) {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID, threadID);
        this.tiTruncationPair = tiTruncationPair;
        bigIntPrime = BigInteger.valueOf(2).pow(Constants.INTEGER_PRECISION
                + 2 * Constants.DECIMAL_PRECISION + 1).nextProbablePrime();
        v = new ArrayList<>();
        this.decPrime = Constants.PRIME;
        this.binaryTiShares = binaryTriples;
        this.decimalTiShares = decimalTriples;
        this.realTiShares = realTiShares;
        this.equalityTiShares = equalityTiShares;
        this.bigIntTiShares = bigIntShares;

        initalizeModelVariables(args);
    }

    /**
     * Call bitD protocol for testing 1 test case
     */
    public void callBitDecomposition() {

        ExecutorService es = Executors.newFixedThreadPool(1);

        BitDecomposition bitTest = new BitDecomposition(2, binaryTiShares,
                asymmetricBit, 5, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId,
                Constants.BINARY_PRIME, 1, partyCount, threadID);

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
                    clientId, Constants.BINARY_PRIME, i, partyCount, threadID);

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

        checkPrimeValidity();

        System.out.println("calling or_xor with x=" + x + " y=" + y);

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();

        for (int i = 0; i < totalCases; i++) {

            OR_XOR or_xor = new OR_XOR(x.get(i), y.get(i), decimalTiShares,
                    asymmetricBit, 1, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, decPrime, i, partyCount, threadID);

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
                    Constants.BINARY_PRIME, 0, 4, 1, 3, partyCount, threadID);
        } else {
            System.out.println("v is not null");
            ois = new OIS(v.get(0), binaryTiShares, asymmetricBit, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId,
                    Constants.BINARY_PRIME, 0, 4, -1, 3, partyCount, threadID);
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
     * Call comparison protocol for n test cases in parallel
     *
     */
    public void callComparison() {
        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {

            Comparison comparisonModule = new Comparison(x.get(i), y.get(i),
                    binaryTiShares, asymmetricBit, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME, i, partyCount, threadID);

            Future<Integer> comparisonTask = es.submit(comparisonModule);
            taskList.add(comparisonTask);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            try {
                Integer result = dWorkerResponse.get();
                //System.out.println("result:" + result + ", #:" + i);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }

    public void callJaccard(){
        checkPrimeValidity();

        ExecutorService es = Executors.newFixedThreadPool(1);

        JaccardDistance jdistance = new JaccardDistance(x, y.get(0), asymmetricBit,
                                    decimalTiShares, pidMapper, commonSender,
                                    clientId, decPrime, 0,
                                    new LinkedList<>(protocolIdQueue),partyCount, threadID);

        Future<List<List<Integer>>> jaccardTask = es.submit(jdistance);
        es.shutdown();

        try {
            List<List<Integer>> result = jaccardTask.get();
            System.out.println("result of jaccard distance comparison: " + result);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Main compute model function for the protocols
     *
     * @param protocolName
     */
    public void compute(String protocolName) throws InterruptedException, ExecutionException {

        switch (protocolName) {
            case "Truncation":
                callTruncation();
                break;
            case "MatrixInversion":
                callMatrixInversion();
                break;
            case "MatrixMultiplication":
                callMatrixMultiplication();
                break;
            case "ArgMax":
                callArgMax();
                break;
            case "OIS":
                callOIS();
                break;
            case "OR_XOR":
                callOR_XOR();
                break;
            case "BitDecomposition":
                callBitDecomposition();
                break;
            case "Multiplication":
                callMultiplication();
                break;
            case "DotProduct":
                callDotProduct();
                break;
            case "Comparison":
                callComparison();
                break;
            case "Unicast":
                callUnicast();
                break;
            case "Equality":
                callEquality();
                break;
            case "MultiplicationBigInt":
                callBigIntMultiplication();
                break;
            case "DotProductBigInt":
                callBigIntDotProduct();
                break;
            case "BitDecompositionBigInt":
                callBitDecompositionBigInt();
                break;
            case "OR_XOR_BigInteger":
                callOR_XOR_BigInteger();
                break;
            case "ChangeBinaryToBigIntegerField":
                callChangeBinaryToBigIntegerField();
                break;
            case "BatcherSort":
                callBatcherSort();
            default:
                break;
        }
    }

    private void callBatcherSort() throws ExecutionException, InterruptedException {

        for(long i=0; i<Long.MAX_VALUE/Long.valueOf("100000000000"); i++);

        System.out.println("TI Shares (dec): " + decimalTiShares.size());
        System.out.println("TI Shares (bin): " + binaryTiShares.size());
        System.out.println("Prime: " + decPrime);
        BatcherSortKNN sortModule = new BatcherSortKNN(
            numDenomPairs, asymmetricBit,
            decimalTiShares, binaryTiShares,
            pidMapper,
            commonSender, clientId, decPrime,
            0, new LinkedList<>(protocolIdQueue), 2, numDenomPairs.size(),
            (int) Math.ceil(Math.log(decPrime)/Math.log(2)), threadID);

        List<List<Integer>> ret = sortModule.call();

        System.out.println("Sorted Num/Denom Pairs:");
        for(int i=0; i<ret.size(); i++) {
            for(int j=0; j<ret.get(0).size(); j++) {
                System.out.print(ret.get(i).get(j) + " ");
            }
            System.out.println();
        }

        return;
    }

    /**
     * Input variable initializations
     *
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
                case "numDenomPairs":
                    numDenomPairs = FileIO.loadIntMatrixFromFile(value);
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
                    List<List<BigInteger>> xList = FileIO.loadMatrixFromFile(value, bigIntPrime);
                    int row = xList.size();
                    int col = xList.get(0).size();
                    xBigIntArr = new BigInteger[row][col];
                    for (int i = 0; i < row; i++) {
                        for (int j = 0; j < col; j++) {
                            xBigIntArr[i][j] = xList.get(i).get(j);
                        }
                    }
                    break;

                case "xBigInt":
                    xBigInt = FileIO.loadListFromFile(value);
                    break;
                case "yBigInt":
                    yBigInt = FileIO.loadListFromFile(value);
                    break;
                case "output":
                    outputPath = value;
                    break;

            }

        }
    }

    private void checkPrimeValidity() {
        if(decPrime == -1) {
            throw new IllegalArgumentException("Please add a valid prime to the config file");
        }
    }

    /**
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void callEquality() throws InterruptedException, ExecutionException {
        checkPrimeValidity();

        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {
            Equality equalityModule = new Equality(x.get(i).get(0), y.get(i).get(0),
                    equalityTiShares.get(i), decimalTiShares.get(i), asymmetricBit, pidMapper,
                    commonSender, clientId, decPrime, i, new LinkedList<>(protocolIdQueue), partyCount, threadID);

            taskList.add(es.submit(equalityModule));
        }

        es.shutdown();

        for(int i=0;i<totalCases;i++) {
            Future<Integer> task = taskList.get(i);
            Integer result = task.get();
            //System.out.println("result:" + result + ", #:" + i);
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }

    private void callMatrixInversion() {
        ExecutorService es = Executors.newFixedThreadPool(1);

        long startTime = System.currentTimeMillis();
        MatrixInversion matrixInversion = new MatrixInversion(xBigIntArr, realTiShares,
                tiTruncationPair,
                1, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, asymmetricBit, partyCount, bigIntPrime, threadID);

        Future<BigInteger[][]> matrixInversionTask = es.submit(matrixInversion);

        es.shutdown();

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

        int n = xBigIntArr.length;
        int l = xBigIntArr[0].length;

        BigInteger xT[][] = LocalMath.transposeMatrix(xBigIntArr);
        int m = xT[0].length;

        long startTime = System.currentTimeMillis();

        //TODO fix ti share count
        MatrixMultiplication matrixMultiplication = new MatrixMultiplication(
                xT, xBigIntArr, realTiShares,
                tiTruncationPair,
                clientId, bigIntPrime, 1, asymmetricBit, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue),
                partyCount, threadID);

        Future<BigInteger[][]> matrixMultiplicationTask = es.submit(matrixMultiplication);

        es.shutdown();
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
        int rows = xBigIntArr.length;
        int cols = xBigIntArr[0].length;
        BigInteger fac = BigInteger.valueOf(2).pow(Constants.DECIMAL_PRECISION);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                xBigIntArr[i][j] = xBigIntArr[i][j].multiply(fac).mod(bigIntPrime);

            }
        }
        BigInteger[][] truncationOutput = new BigInteger[rows][cols];

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<BigInteger[]>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = xBigIntArr.length;
        int tiTruncationStartIndex = 0;

        System.out.println("Total testcases:" + totalCases);
        for (int i = 0; i < totalCases; i++) {

            BatchTruncation truncationPair = new BatchTruncation(xBigIntArr[i],
                    tiTruncationPair.subList(tiTruncationStartIndex,
                            tiTruncationStartIndex + xBigIntArr[i].length),
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, bigIntPrime, i, asymmetricBit, partyCount, threadID);

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

    /**
     * Call multiplication for n test cases in parallel
     *
     */
    public void callMultiplication() {

        checkPrimeValidity();

        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {
            MultiplicationInteger multiplicationModule = new MultiplicationInteger(
                    x.get(i).get(0), y.get(i).get(0),
                    decimalTiShares.get(i), pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, decPrime, i, asymmetricBit, 0, partyCount, threadID);

            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            try {
                Integer result = dWorkerResponse.get();
                //System.out.println("result:" + result + ", #:" + i);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }

    /**
     * Test Unicast feature for n parties
     *
     */
    public void callUnicast() {

        Random random = new Random();

        int value = random.nextInt();
        Message senderMessage = new Message(value,
                clientId, protocolIdQueue, true);
        senderMessage.setThreadID(threadID);
        Logger.getLogger(TestModel.class.getName())
                .log(Level.INFO, "value sent:{0}, client Id:{1}", new Object[]{value, clientId});

        try {
            commonSender.put(senderMessage);
        } catch (InterruptedException ex) {
            Logger.getLogger(MultiplicationInteger.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        if (asymmetricBit == 1) {
            for (int i = 0; i < partyCount - 1; i++) {
                try {
                    Message receivedMessage = pidMapper.get(protocolIdQueue).take();
                    value = (Integer) receivedMessage.getValue();
                    Logger.getLogger(TestModel.class.getName())
                            .log(Level.INFO, "value recieved:{0}, client Id:{1}", new Object[]{value, clientId});
                } catch (InterruptedException ex) {
                    Logger.getLogger(TestModel.class.getName())
                            .log(Level.SEVERE, null, ex);
                }
            }
        }

    }

    /**
     * Call dot product protocol for n test cases in parallel
     *
     */
    public void callDotProduct() {

        checkPrimeValidity();

        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {
            DotProductInteger DPModule = new DotProductInteger(x.get(i),
                    y.get(i), decimalTiShares, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, decPrime, i, asymmetricBit, partyCount, threadID);

            Future<Integer> DPTask = es.submit(DPModule);
            taskList.add(DPTask);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            try {
                Integer result = dWorkerResponse.get();
                //System.out.println("result:" + result + ", #:" + i);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }

    public void callBigIntMultiplication() {

        System.out.print("xShares: ");
        for(int i=0; i<xBigInt.size(); i++) {
            System.out.print(xBigInt.get(i) + " ");
        } System.out.println();

        System.out.print("yShares: ");
        for(int i=0; i<yBigInt.size(); i++) {
            System.out.print(yBigInt.get(i) + " ");
        } System.out.println();


        checkBigIntPrimeValidity();

        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<BigInteger>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = xBigInt.size();
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {
            MultiplicationBigInteger multiplicationModule = new MultiplicationBigInteger(
                    xBigInt.get(i), yBigInt.get(i),
                    bigIntTiShares.get(i), pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId,
                    Constants.BIG_INT_PRIME, i, asymmetricBit, partyCount, threadID);

            Future<BigInteger> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);
        }

        es.shutdown();

        List<BigInteger> results = new ArrayList<>();
        for (int i = 0; i < totalCases; i++) {
            Future<BigInteger> dWorkerResponse = taskList.get(i);
            try {
                BigInteger result = dWorkerResponse.get();
                results.add(result);
                System.out.println("result:" + result.toString() + ", #:" + i);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        FileIO.writeToCSV(results, outputPath, "bigIntMultResult", clientId);

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);

    }

   private void checkBigIntPrimeValidity() {

        if(Constants.BIG_INT_PRIME.equals( new BigInteger("-1"))) {
            throw new IllegalArgumentException(
                "Please add a valid big int prime to the config file");
        }
   }

       /**
     * Call dot product protocol for n test cases in parallel
     *
     */
    public void callBigIntDotProduct() {

        checkBigIntPrimeValidity();

        long startTime = System.currentTimeMillis();

        DotProductBigInteger DPModule = new DotProductBigInteger(xBigInt,
                yBigInt, bigIntTiShares, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId, Constants.BIG_INT_PRIME,
                0, asymmetricBit, partyCount, threadID);

        try {
            BigInteger result = DPModule.call();
            System.out.println("result:" + result.toString());

            List<BigInteger> results = new ArrayList<>();
            results.add(result);
            FileIO.writeToCSV(results, outputPath, "bigIntDotProductResult", clientId);


        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Avg time duration:" + elapsedTime);
    }

        /**
     * Call bitD bigint protocol for testing 1 test case
     */
    public void callBitDecompositionBigInt() {

        ExecutorService es = Executors.newFixedThreadPool(1);

        BitDecompositionBigInteger bitTest = new BitDecompositionBigInteger(
            xBigInt.get(0), binaryTiShares,
            asymmetricBit, Constants.BIT_LENGTH, pidMapper, commonSender,
            new LinkedList<>(protocolIdQueue), clientId,
            Constants.BINARY_PRIME, 0, partyCount, threadID);

        Future<List<Integer>> bitdecompositionTask = es.submit(bitTest);

        try {
            List<Integer> result = bitdecompositionTask.get();
            System.out.print("Big Int Decomposition: ");
            for(int i=0; i<result.size(); i++) {
                System.out.print(result.get(i));
            } System.out.println();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(TestModel.class.getName()).log(Level.SEVERE, null, ex);
        }

        es.shutdown();
    }

    public void callOR_XOR_BigInteger() throws InterruptedException, ExecutionException {

        checkBigIntPrimeValidity();


        OR_XOR_BigInteger or_bigInteger = new OR_XOR_BigInteger(
            xBigInt, yBigInt, bigIntTiShares,
                asymmetricBit, 1, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId, Constants.BIG_INT_PRIME, 0, partyCount, threadID);

        try{
            BigInteger[] resultOR = or_bigInteger.call();
            System.out.println("X OR Y:");
            for(BigInteger bi : resultOR) {
                System.out.println(bi);
            }
        } catch(InterruptedException ie) {System.out.println(ie);}

        OR_XOR_BigInteger xor_bigInteger = new OR_XOR_BigInteger(
            xBigInt, yBigInt, bigIntTiShares,
                asymmetricBit, 2, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue), clientId, Constants.BIG_INT_PRIME, 1, partyCount, threadID);
        try{
            BigInteger[] resultXOR = xor_bigInteger.call();
            System.out.println("\nX XOR Y:");
            for(BigInteger bi : resultXOR) {
                System.out.println(bi);
            }
        } catch(InterruptedException ie) {System.out.println(ie); }

    }

    public void callChangeBinaryToBigIntegerField() {


        checkBigIntPrimeValidity();
        // TODO Add test procedure
    }
}
