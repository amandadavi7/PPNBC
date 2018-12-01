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
import Protocol.Truncation;
import Protocol.Utility.BatchMultiplicationInteger;
import Protocol.Utility.JaccardDistance;
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

    List<TruncationPair> tiTruncationPair;
    BigInteger bigIntPrime;
    int decPrime;
    String outputPath;
    List<TripleByte> binaryTiShares;
    List<TripleInteger> decimalTiShares;
    List<TripleReal> realTiShares;
    private static final Logger LOGGER = Logger.getLogger(TestModel.class.getName());

    
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
     * @param protocolIdQueue
     * @param protocolID
     * @throws java.io.IOException
     */
    public TestModel(List<TripleByte> binaryTriples, List<TripleInteger> decimalTriples,
            List<TripleReal> realTiShares, List<TruncationPair> tiTruncationPair, 
            int asymmetricBit, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, 
            BlockingQueue<Message> senderQueue, int clientId, int partyCount, String[] args, 
            Queue<Integer> protocolIdQueue, int protocolID) throws IOException {

        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);
        this.tiTruncationPair = tiTruncationPair;
        bigIntPrime = BigInteger.valueOf(2).pow(Constants.INTEGER_PRECISION
                + 2 * Constants.DECIMAL_PRECISION + 1).nextProbablePrime();
        v = new ArrayList<>();
        this.decPrime = Constants.PRIME;
        this.binaryTiShares = binaryTriples;
        this.decimalTiShares = decimalTriples;
        this.realTiShares = realTiShares;
        initalizeModelVariables(args);
    }
    
    /**
     * Main compute model function for the protocols
     *
     * @param protocolName
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public void compute(String protocolName) throws InterruptedException, ExecutionException {

        switch (protocolName) {
            case "BatchTruncation":
                callBatchTruncation();
                break;
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
            case "OR":
                callOR_XOR(1);
                break;
            case "XOR":
                callOR_XOR(2);
                break;
            case "BitDecomposition":
                callBitDecomposition();
                break;
            case "Multiplication":
                callMultiplication();
                break;
            case "BatchMultiplication":
                callBatchMultiplication();
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
            default:
                break;
        }
    }

    /**
     * Call bitD protocol in parallel for n test cases
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public void callBitDecomposition() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<List<Integer>>> taskList = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        int totalCases = x.size();
        for(int i=0; i<totalCases; i++) {
            BitDecomposition bitDModule = new BitDecomposition(x.get(i).get(0),
                    binaryTiShares, asymmetricBit, 10, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId,
                    Constants.BINARY_PRIME, 1, partyCount);
            
            taskList.add(es.submit(bitDModule));
        }
        
        es.shutdown();
        
        for(int i=0;i<totalCases;i++) {
            Future<List<Integer>> resultFuture = taskList.get(i);
            List<Integer> result = resultFuture.get();
            LOGGER.log(Level.FINE, "result: {0}, #: {1}", new Object[]{result , i});
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.log(Level.INFO, "Avg time duration: {0}", elapsedTime);
    }

    /**
     * Calling ArgMax protocol in parallel for n test cases
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public void callArgMax() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer[]>> taskList = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        int totalCases = v.size();

        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {

            ArgMax argmaxModule = new ArgMax(v.get(i), binaryTiShares, asymmetricBit,
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, Constants.BINARY_PRIME, i, partyCount);

            Future<Integer[]> argmaxTask = es.submit(argmaxModule);
            taskList.add(argmaxTask);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer[]> dWorkerResponse = taskList.get(i);
            Integer[] result = dWorkerResponse.get();
            LOGGER.log(Level.FINE, "result: {0}, #: {1}", new Object[]{result, i});
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.log(Level.INFO, "Avg time duration: {0}", elapsedTime);

    }

    /**
     * Call OR_XOR protocol in parallel for n test cases
     * @param val
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public void callOR_XOR(int val) throws InterruptedException, ExecutionException {
        
        checkPrimeValidity();

        ExecutorService es = Executors.newFixedThreadPool(100);
        List<Future<Integer[]>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();

        for (int i = 0; i < totalCases; i++) {

            OR_XOR or_xor = new OR_XOR(x.get(i), y.get(i), decimalTiShares,
                    asymmetricBit, val, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, decPrime, i, partyCount);

            Future<Integer[]> task = es.submit(or_xor);
            taskList.add(task);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer[]> task = taskList.get(i);
            Integer[] result = task.get();
            LOGGER.log(Level.FINE, "result: {0}, #: {1}", new Object[]{Arrays.toString(result) , i});
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.log(Level.INFO, "Avg time duration: {0}", elapsedTime);

    }

    /**
     * Call Oblivious Input Selection for n test cases in parallel
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public void callOIS() throws InterruptedException, ExecutionException {

        ExecutorService es = Executors.newFixedThreadPool(100);
        int totalCases = 100;
        List<Future<Integer[]>> taskList = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        for(int i=0;i<totalCases;i++) {
            OIS ois;
            if (v.isEmpty()) {
                ois = new OIS(null, binaryTiShares, asymmetricBit, pidMapper, commonSender,
                        new LinkedList<>(protocolIdQueue), clientId,
                        Constants.BINARY_PRIME, 0, 4, 2, 3, partyCount);
            } else {
                ois = new OIS(v.get(i), binaryTiShares, asymmetricBit, pidMapper, commonSender,
                        new LinkedList<>(protocolIdQueue), clientId,
                        Constants.BINARY_PRIME, 0, 4, -1, 3, partyCount);
            }
            
            taskList.add(es.submit(ois));

        }
        
        es.shutdown();
        
        for(int i=0;i<totalCases;i++) {
            Future<Integer[]> resultFuture = taskList.get(i);
            Integer[] result = resultFuture.get();
            LOGGER.log(Level.FINE, "result: {0}, #: {1}", new Object[]{Arrays.toString(result) , 0});
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.log(Level.INFO, "Avg time duration: {0}", elapsedTime);
    }

    /**
     * Call comparison protocol for n test cases in parallel
     *
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public void callComparison() throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {

            Comparison comparisonModule = new Comparison(x.get(i), y.get(i),
                    binaryTiShares, asymmetricBit, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME, i, partyCount);

            Future<Integer> comparisonTask = es.submit(comparisonModule);
            taskList.add(comparisonTask);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            Integer result = dWorkerResponse.get();
            LOGGER.log(Level.FINE, "result: {0}, #: {1}", new Object[]{result , i});
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.log(Level.INFO, "Avg time duration: {0}", elapsedTime);
    }
    
    /**
     * 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    public void callJaccard() throws InterruptedException, ExecutionException{
        checkPrimeValidity();
        
        JaccardDistance jdistance = new JaccardDistance(x, y.get(0), asymmetricBit, 
                                    decimalTiShares, pidMapper, commonSender, 
                                    clientId, decPrime, 0, 
                                    new LinkedList<>(protocolIdQueue),partyCount);
        
        List<List<Integer>> result = jdistance.call();
        LOGGER.log(Level.INFO, "result of jaccard distance comparison: {0}", result);

    }

    /**
     * Input variable initializations
     *
     * @param args
     */
    private void initalizeModelVariables(String[] args) throws FileNotFoundException, IOException{

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
                    break;
                case "xCsv":
                    List<List<BigInteger>> xList = FileIO.loadMatrixFromFile(value, bigIntPrime);
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
                
            }

        }
    }
    
    /**
     * check if the decimal prime is initialized
     */
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
    private void callMatrixInversion() throws InterruptedException, ExecutionException {
        
        long startTime = System.currentTimeMillis();
        MatrixInversion matrixInversion = new MatrixInversion(xBigInt, realTiShares,
                tiTruncationPair,
                1, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, asymmetricBit, partyCount, bigIntPrime);

        BigInteger[][] result = matrixInversion.call();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.log(Level.INFO, "Avg time duration: {0}", elapsedTime);

        FileIO.writeToCSV(result, outputPath, "matrixInversion", clientId);
    }

    /**
     * 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void callMatrixMultiplication() throws InterruptedException, ExecutionException {
        
        int n = xBigInt.length;
        int l = xBigInt[0].length;

        BigInteger xT[][] = LocalMath.transposeMatrix(xBigInt);
        int m = xT[0].length;

        long startTime = System.currentTimeMillis();

        //TODO fix ti share count
        MatrixMultiplication matrixMultiplication = new MatrixMultiplication(
                xT, xBigInt, realTiShares,
                tiTruncationPair,
                clientId, bigIntPrime, 1, asymmetricBit, pidMapper, commonSender,
                new LinkedList<>(protocolIdQueue),
                partyCount);

        BigInteger[][] result = matrixMultiplication.call();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.log(Level.INFO, "Avg time duration: {0}", elapsedTime);

        FileIO.writeToCSV(result, outputPath, "matrixMultiplication", clientId);

    }

    /**
     * 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void callBatchTruncation() throws InterruptedException, ExecutionException {
        LOGGER.log(Level.INFO, "calling truncation");

        //Prepare matrix for truncation. Multiply the elements with 2^f
        int rows = xBigInt.length;
        int cols = xBigInt[0].length;
        BigInteger fac = BigInteger.valueOf(2).pow(Constants.DECIMAL_PRECISION);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                xBigInt[i][j] = xBigInt[i][j].multiply(fac).mod(bigIntPrime);

            }
        }
        BigInteger[][] truncationOutput = new BigInteger[rows][cols];

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<BigInteger[]>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = xBigInt.length;
        int tiTruncationStartIndex = 0;

        LOGGER.log(Level.INFO, "Total testcases: {0}", totalCases);
        for (int i = 0; i < totalCases; i++) {

            BatchTruncation truncationPair = new BatchTruncation(xBigInt[i],
                    tiTruncationPair.subList(tiTruncationStartIndex,
                            tiTruncationStartIndex + xBigInt[i].length),
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, bigIntPrime, i, asymmetricBit, partyCount);

            Future<BigInteger[]> task = es.submit(truncationPair);
            taskList.add(task);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<BigInteger[]> task = taskList.get(i);
            truncationOutput[i] = task.get();
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.log(Level.INFO, "Avg time duration: {0}", elapsedTime);

        FileIO.writeToCSV(truncationOutput, outputPath, "truncation", clientId);

    }
    
    /**
     * 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    private void callTruncation() throws InterruptedException, ExecutionException {
        LOGGER.log(Level.INFO, "calling truncation");

        //Prepare matrix for truncation. Multiply the elements with 2^f
        int rows = xBigInt.length;
        int cols = xBigInt[0].length;
        BigInteger fac = BigInteger.valueOf(2).pow(Constants.DECIMAL_PRECISION);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                xBigInt[i][j] = xBigInt[i][j].multiply(fac).mod(bigIntPrime);

            }
        }
        BigInteger[][] truncationOutput = new BigInteger[rows][cols];

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<BigInteger>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = xBigInt.length;
        int tiTruncationStartIndex = 0;

        LOGGER.log(Level.INFO, "Total testcases: {0}", totalCases);
        for (int i = 0; i < totalCases; i++) {

            Truncation truncationPair = new Truncation(xBigInt[i][0],
                    tiTruncationPair.get(i),
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, bigIntPrime, i, asymmetricBit, partyCount);

            Future<BigInteger> task = es.submit(truncationPair);
            taskList.add(task);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<BigInteger> task = taskList.get(i);
            truncationOutput[i][0] = task.get();
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.log(Level.INFO, "Avg time duration: {0}", elapsedTime);

        FileIO.writeToCSV(truncationOutput, outputPath, "truncation", clientId);

    }

    /**
     * Call multiplication for n test cases in parallel
     *
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public void callMultiplication() throws InterruptedException, ExecutionException {
        
        checkPrimeValidity();
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {
            MultiplicationInteger multiplicationModule = new MultiplicationInteger(
                    x.get(i).get(0), y.get(i).get(0),
                    decimalTiShares.get(i), pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, decPrime, i, asymmetricBit, 0, partyCount);

            Future<Integer> multiplicationTask = es.submit(multiplicationModule);
            taskList.add(multiplicationTask);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            Integer result = dWorkerResponse.get();
            LOGGER.log(Level.FINE, "result: {0}, #: {1}", new Object[]{result, i});
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.log(Level.INFO, "Avg time duration: {0}", elapsedTime);
    }
    
    /**
     * Call multiplication for n test cases in parallel
     *
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public void callBatchMultiplication() throws InterruptedException, ExecutionException {
        
        checkPrimeValidity();
        
        int vectorLength = x.get(0).size();

        ExecutorService mults = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        ExecutorCompletionService<Integer[]> multCompletionService = new ExecutorCompletionService<>(mults);

        int i = 0;
        int startpid = 0;

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();

        do {
            int toIndex = Math.min(i + Constants.BATCH_SIZE, vectorLength);

            multCompletionService.submit(new BatchMultiplicationInteger(x.get(0).subList(i, toIndex),
                    y.get(0).subList(i, toIndex), decimalTiShares.subList(0, Constants.BATCH_SIZE),
                    pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                    clientId, decPrime, startpid, asymmetricBit, 0, partyCount));

            startpid++;
            i = toIndex;

        } while (i < vectorLength);

        mults.shutdown();

        for (i = 0; i < startpid; i++) {
            Future<Integer[]> prod = multCompletionService.take();
            Integer[] products = prod.get();
            LOGGER.log(Level.FINE, "result: {0}, #: {1}", new Object[]{products, i});
        }
        
        LOGGER.log(Level.INFO, "Total batches: {0}", startpid);
        
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.log(Level.INFO, "Avg time duration: {0}", elapsedTime);
    }

    /**
     * Test Unicast feature for n parties
     *
     * @throws java.lang.InterruptedException
     */
    public void callUnicast() throws InterruptedException {

        Random random = new Random();

        int value = random.nextInt();
        Message senderMessage = new Message(value,
                clientId, protocolIdQueue, true);
        LOGGER.log(Level.INFO, "value sent:{0}, client Id:{1}", new Object[]{value, clientId});

        commonSender.put(senderMessage);

        if (asymmetricBit == 1) {
            for (int i = 0; i < partyCount - 1; i++) {
                Message receivedMessage = pidMapper.get(protocolIdQueue).take();
                value = (Integer) receivedMessage.getValue();
                LOGGER.log(Level.INFO, "value recieved:{0}, client Id:{1}", new Object[]{value, clientId});
            }
        }

    }

    /**
     * Call dot product protocol for n test cases in parallel
     *
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public void callDotProduct() throws InterruptedException, ExecutionException {
        
        checkPrimeValidity();
        
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> taskList = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        int totalCases = x.size();
        // totalcases number of protocols are submitted to the executorservice
        for (int i = 0; i < totalCases; i++) {
            DotProductInteger DPModule = new DotProductInteger(x.get(i),
                    y.get(i), decimalTiShares, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, decPrime, i, asymmetricBit, partyCount);

            Future<Integer> DPTask = es.submit(DPModule);
            taskList.add(DPTask);
        }

        es.shutdown();

        for (int i = 0; i < totalCases; i++) {
            Future<Integer> dWorkerResponse = taskList.get(i);
            Integer result = dWorkerResponse.get();
            LOGGER.log(Level.FINE, "result: {0}, #: {1}", new Object[]{result, i});
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.log(Level.INFO, "Avg time duration: {0}", elapsedTime);
    }

}
