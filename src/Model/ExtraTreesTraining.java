/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.BitDecomposition;
import Protocol.Comparison;
import Protocol.EqualityByte;
import Protocol.OR_XOR;
import Protocol.Utility.BatchMultiplicationInteger;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import Utility.Logging;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chaitali
 */
public class ExtraTreesTraining extends Model {

    private int roundOff, numAttributes, pid, bitLength, tiBinaryStartIndex, treeCount, startpid, tiDecimalStartIndex, tiEqualityStartIndex, protocolID;
    private String fileName;
    List<List<Integer>> trainData;
    List<List<List<Integer>>> trainDataBitShares;
    List<List<List<Integer>>> trainDataSelectedFeatures;
    List<List<List<Integer>>> trainDataSelectedSplitPoints;
    List<List<List<Integer>>> rowShares;
    List<List<Integer>> colShares;
    List<TripleInteger> decimalTiShares;
    List<TripleByte> binaryTiShares;
    List<Integer> wholeNumTiShares;
    List<Integer> equalityTiShares;
    List<Integer> dotProductResults;
    List<List<Integer>> dotProductResultsBitShares;
    List<List<Integer>> trainDataComparisonOutputs;
    Integer prime;

    public ExtraTreesTraining(int asymmetricBit, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, BlockingQueue<Message> senderQueue, int clientId, int partyCount, List<List<List<Integer>>> rowShares, List<List<Integer>> colShares, List<TripleInteger> decimalShares, List<TripleByte> binaryShares, List<Integer> wholeNumShares, List<Integer> equalityShares, String[] args, Queue<Integer> protocolIdQueue, int protocolID) {
        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);
        trainDataBitShares = new ArrayList<>();
        trainData = new ArrayList<>();
        this.rowShares = rowShares;
        this.colShares = colShares;
        this.decimalTiShares = decimalShares;
        this.binaryTiShares = binaryShares;
        this.wholeNumTiShares = wholeNumShares;
        this.equalityTiShares = equalityShares;
        this.prime = Constants.PRIME;
        this.protocolID = protocolID;
        pid = 0;
        startpid = 0;
        tiBinaryStartIndex = 0;
        tiDecimalStartIndex = 0;
        tiEqualityStartIndex = 0;
        if (prime == -1) {
            throw new IllegalArgumentException("Please add a valid prime to the config file");
        }
        initializeVariables(args);
    }

    private void initializeVariables(String[] args) {
        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.partyUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];

            switch (command) {
                case "trainCsv":
                    fileName = value;
                    break;

                case "roundOff":
                    roundOff = Integer.parseInt(value);
                    break;

                case "bitLength": //bitLength should be such that 2^bitLength <= prime
                    bitLength = Integer.parseInt(value);
                    break;

                case "treeCount":
                    treeCount = Integer.parseInt(value);
                    break;
            }
        }
    }

    public void runModel() throws InterruptedException, ExecutionException {
        //long startTime1 = System.currentTimeMillis();

        // Load training data from file
        loadDataFromFile(fileName);
        System.out.println("Loaded Data.");

        // Convert training data to binary
        trainDataBitShares = new ArrayList<>();
        convertTrainDataToBinary();
        System.out.println("Converted train data to binary.");
        //System.out.println("Time taken for input data conversion is " + (System.currentTimeMillis() - startTime1));

        long totalTime = 0;
        numAttributes = trainData.get(0).size();

        for (int treeNum = 0; treeNum < treeCount; treeNum++) {
            final long startTime = System.currentTimeMillis();

            // In extra trees algorithm, we only consider K random attributes for
            // constructing a decision tree. In our case, trusted initializer tells
            // us which K columns we need to choose for constructing a tree. To
            // exclude unwanted unselected columns from the data, we replace the
            // values in those columns with zeros.
            trainDataSelectedFeatures = new ArrayList<>();
            findSelectedFeatures(treeNum);
            System.out.println("Feature Selection completed.");
            //System.out.println("Time taken is " + (System.currentTimeMillis() - startTime));
            /*System.out.println("For Tree number: " + treeNum);

            System.out.println("Column shares:");
            for (int j = 0; j < colShares.get(treeNum).size(); j++) {
                System.out.print(colShares.get(treeNum).get(j) + " ");
            }
            System.out.println("");

            System.out.println("Selected Features are:");
            for (int j = 0; j < trainDataSelectedFeatures.size(); j++) {
                for (int k = 0; k < trainDataSelectedFeatures.get(j).size(); k++) {
                    for (int l = 0; l < trainDataSelectedFeatures.get(j).get(k).size(); l++) {
                        System.out.print(trainDataSelectedFeatures.get(j).get(k).get(l));
                    }
                    System.out.print(" ");
                }
                System.out.println("");
            }*/

            // For each column, replaces the values which are not selected as
            // split point by zeros.
            trainDataSelectedSplitPoints = new ArrayList<>();
            trainDataSelectedSplitPoints(treeNum);
            System.out.println("Batch multiplication for selecting split points is done.");
            //System.out.println("Time taken is " + (System.currentTimeMillis() - startTime));
            /*System.out.println("Batch Multiplication results are: ");
            for (int i = 0; i < trainDataSelectedSplitPoints.size(); i++) {
                for (int j = 0; j < trainDataSelectedSplitPoints.get(i).size(); j++) {
                    for (int k = 0; k < trainDataSelectedSplitPoints.get(i).get(j).size(); k++) {
                        System.out.print(trainDataSelectedSplitPoints.get(i).get(j).get(k));
                    }
                    System.out.print(" ");
                }
                System.out.println("");
            }*/

            // For each column, find the split point.
            dotProductResultsBitShares = new ArrayList<>();
            computeDotProductBitShares();
            System.out.println("Completed calculation of dot product bit shares using OR.");
            //System.out.println("Time taken is " + (System.currentTimeMillis() - startTime));
            /*System.out.println("Dot product bit shares calcualted using OR module are");
            for(int i=0; i<dotProductResultsBitShares2.size(); i++) {
                for(int j=0; j<dotProductResultsBitShares2.get(i).size(); j++) {
                    System.out.print(dotProductResultsBitShares.get(i).get(j));
                }
                System.out.print(" ");
            }*/

            // For each column, compare the values with split point for that
            // column. Values which are greater than or equal to split point
            // are replaced by 1 and values which are less than split point are
            // replaced by 0.
            trainDataComparisonOutputs = new ArrayList<>();
            doComparison();
            System.out.println("Completed comparison.");
            //System.out.println("Time taken is " + (System.currentTimeMillis() - startTime));
            System.out.println("Discretized dataset:");
            for (int i = 0; i < trainDataComparisonOutputs.size(); i++) {
                for (int j = 0; j < trainDataComparisonOutputs.get(i).size(); j++) {
                    System.out.print(trainDataComparisonOutputs.get(i).get(j) + " ");
                }
                System.out.println("");
            }

            // Now that we have discretized the dataset, next we need to train decision trees.
            // For that, we need to convert the data into the format reuired by decision tree training modeule in Lynx.
            Integer[][][] attrValues = new Integer[numAttributes - 1][2][trainData.size()];
            generateAttributeValuesForDTTraining(attrValues);
            System.out.println("Generated attribute values in required form for DT learning.");
            //System.out.println("Time taken is " + (System.currentTimeMillis() - startTime));
            System.out.println("AttrValues for Decision Tree Learning");
            for (int i = 0; i < attrValues.length; i++) {
                for (int j = 0; j < attrValues[i].length; j++) {
                    for (int k = 0; k < attrValues[i][j].length; k++) {
                        System.out.print(attrValues[i][j][k] + " ");
                    }
                    System.out.println("");
                }
            }

            Integer[][] classValues = new Integer[wholeNumTiShares.size()][trainData.size()];
            if (wholeNumTiShares.size() == 2) {
                //System.out.println("Called generateBinaryClassValuesForDTTraining");
                generateBinaryClassValuesForDTTraining(classValues);
            } else {
                generateClassValuesForDTTraining(classValues);
            }
            System.out.println("Generated class values in required form for DT learning.");
            //System.out.println("Time taken is " + (System.currentTimeMillis() - startTime));
            System.out.println("ClassValues for Decision Tree Learning");
            for (int i = 0; i < classValues.length; i++) {
                for (int j = 0; j < classValues[i].length; j++) {
                    System.out.print(classValues[i][j] + " ");
                }
                System.out.println("");
            }

            final long endTime = System.currentTimeMillis();
            System.out.println("Time for training this decision tree is " + String.valueOf(endTime - startTime) + "ms");
            totalTime += endTime - startTime;
        }
        System.out.println("Average time for one decision tree is " + String.valueOf(totalTime / treeCount) + "ms");
    }

    private void loadDataFromFile(String fileName) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                ArrayList<Integer> row = new ArrayList<>();
                String[] columns = line.split(",");
                for (int i = 0; i < columns.length - 1; i++) {
                    row.add(Math.round(Float.parseFloat(columns[i]) * roundOff));
                }
                row.add(Integer.parseInt(columns[columns.length - 1]));
                trainData.add(row);
            }
            br.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ExtraTreesTraining.class.getName()).log(Level.SEVERE, "Could not find " + fileName + ".", ex);
        } catch (IOException ex) {
            Logger.getLogger(ExtraTreesTraining.class.getName()).log(Level.SEVERE, "Failed to read the file " + fileName + ".", ex);
        }
    }

    private void doComparison() throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<List<Future<Integer>>> taskList = new ArrayList<>();
        int comparisonTiCount = (2 * bitLength) + ((bitLength * (bitLength - 1)) / 2);
        for (int i = 0; i < trainDataSelectedFeatures.size(); i++) {
            List<Future<Integer>> taskListRow = new ArrayList<>();
            for (int j = 0; j < numAttributes - 1; j++) {
                Comparison comp = new Comparison(trainDataSelectedFeatures.get(i).get(j), dotProductResultsBitShares.get(j),
                        binaryTiShares.subList(tiBinaryStartIndex, tiBinaryStartIndex + comparisonTiCount),
                        asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                        clientId, Constants.BINARY_PRIME, pid, partyCount);
                taskListRow.add(es.submit(comp));
                tiBinaryStartIndex += comparisonTiCount;
                pid++;
            }
            taskList.add(taskListRow);
        }
        for (int i = 0; i < trainDataSelectedFeatures.size(); i++) {
            List<Integer> compShares = new ArrayList<>();
            for (int j = 0; j < numAttributes - 1; j++) {
                Future<Integer> compResult = taskList.get(i).get(j);
                compShares.add(compResult.get());
            }
            trainDataComparisonOutputs.add(compShares);
        }
        es.shutdown();
    }

    private void generateAttributeValuesForDTTraining(Integer[][][] attrValues) {
        for (int i = 0; i < attrValues.length; i++) {
            for (int j = 0; j < attrValues[i].length; j++) {
                for (int k = 0; k < attrValues[i][j].length; k++) {
                    if (j == 0 && asymmetricBit == 1) {
                        attrValues[i][j][k] = 1 - trainDataComparisonOutputs.get(k).get(i);
                    } else {
                        attrValues[i][j][k] = trainDataComparisonOutputs.get(k).get(i);
                    }
                }
            }
        }
    }

    private void generateBinaryClassValuesForDTTraining(Integer[][] classValues) {
        for (int i = 0; i < classValues.length; i++) {
            for (int j = 0; j < classValues[i].length; j++) {
                if (i == 0 && asymmetricBit == 1) {
                    classValues[i][j] = 1 - trainData.get(j).get(numAttributes - 1);
                } else {
                    classValues[i][j] = trainData.get(j).get(numAttributes - 1);
                }
            }
        }
    }

    private void generateClassValuesForDTTraining(Integer[][] classValues) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<List<Integer>>> bitDtaskList1 = new ArrayList<>();
        for (int i = 0; i < classValues.length; i++) {
            BitDecomposition bitDModule = new BitDecomposition(wholeNumTiShares.get(i),
                    binaryTiShares.subList(tiBinaryStartIndex, tiBinaryStartIndex + bitLength * bitLength),
                    asymmetricBit, bitLength, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME, pid, partyCount);
            bitDtaskList1.add(es.submit(bitDModule));
            tiBinaryStartIndex += bitLength * bitLength;
            pid++;
        }
        List<List<Integer>> classValueBitShares = new ArrayList<>();
        for (int i = 0; i < classValues.length; i++) {
            Future<List<Integer>> bitDResult2 = bitDtaskList1.get(i);
            classValueBitShares.add(bitDResult2.get());
        }

        List<List<Future<Integer>>> taskList = new ArrayList<>();
        for (int i = 0; i < classValues.length; i++) {
            List<Future<Integer>> taskListRow = new ArrayList<>();
            for (int j = 0; j < classValues[i].length; j++) {
                EqualityByte EqModule = new EqualityByte(trainDataBitShares.get(j).get(numAttributes - 1), classValueBitShares.get(i),
                        binaryTiShares.get(tiBinaryStartIndex), pidMapper, commonSender,
                        new LinkedList<>(protocolIdQueue), clientId, prime, pid, asymmetricBit, partyCount);
                taskListRow.add(es.submit(EqModule));
                tiBinaryStartIndex++;
                pid++;
            }
            taskList.add(taskListRow);
        }
        for (int i = 0; i < classValues.length; i++) {
            for (int j = 0; j < classValues[i].length; j++) {
                Future<Integer> compResult = taskList.get(i).get(j);
                classValues[i][j] = compResult.get();
            }
        }
        es.shutdown();
    }

    private void trainDecisionTree(Integer[][][] attrValues, Integer[][] classValues) throws InterruptedException, ExecutionException {
        DecisionTreeTraining dtModel = new DecisionTreeTraining(
                asymmetricBit,
                pidMapper,
                commonSender,
                clientId,
                binaryTiShares.subList(tiBinaryStartIndex, tiBinaryStartIndex + 2000),
                decimalTiShares.subList(tiDecimalStartIndex, tiDecimalStartIndex + 2000),
                equalityTiShares.subList(tiEqualityStartIndex, tiEqualityStartIndex + 2000),
                partyCount,
                protocolIdQueue,
                protocolID,
                wholeNumTiShares.size(),
                numAttributes - 1,
                2,
                trainData.size(),
                attrValues,
                classValues
        );

        dtModel.trainDecisionTree();
        tiBinaryStartIndex += 2000;
        tiDecimalStartIndex += 2000;
        tiEqualityStartIndex += 2000;
    }

    private void convertTrainDataToBinary() {
        for (int i = 0; i < trainData.size(); i++) {
            List<List<Integer>> currRow = new ArrayList<>();
            for (int j = 0; j < trainData.get(i).size(); j++) {
                int num = trainData.get(i).get(j);
                List<Integer> currNum = new ArrayList<>();
                for (int k = 0; k < bitLength; k++) {
                    int n = num % 2;
                    currNum.add(n);
                    num /= 2;
                }
                currRow.add(currNum);
            }
            trainDataBitShares.add(currRow);
        }
    }

    private void findSelectedFeatures(int treeNum) throws InterruptedException, ExecutionException {
        for (int k = 0; k < trainDataBitShares.size(); k++) {
            List<List<Integer>> trainDataSelectedFeaturesRow = new ArrayList<>();
            for (int l = 0; l < trainDataBitShares.get(k).size() - 1; l++) {
                int vectorLength = bitLength;
                ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
                List<Future<Integer[]>> taskList = new ArrayList<>();
                List<Integer> colSharesList = new ArrayList<>(Collections.nCopies(bitLength, colShares.get(treeNum).get(l)));
                int i = 0;

                do {
                    int toIndex = Math.min(i + Constants.BATCH_SIZE, vectorLength);

                    BatchMultiplicationInteger mults = new BatchMultiplicationInteger(trainDataBitShares.get(k).get(l).subList(i, toIndex),
                            colSharesList.subList(i, toIndex), decimalTiShares.subList(tiDecimalStartIndex + i, tiDecimalStartIndex + toIndex),
                            pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                            clientId, Constants.BINARY_PRIME, startpid, asymmetricBit, pid, partyCount);
                    taskList.add(es.submit(mults));
                    startpid++;
                    i = toIndex;

                } while (i < vectorLength);
                tiDecimalStartIndex += vectorLength;
                List<Integer> trainDataSelectedFeaturesElement = new ArrayList<>();
                for (int j = 0; j < taskList.size(); j++) {
                    Integer[] multBatchResults = taskList.get(j).get();
                    trainDataSelectedFeaturesElement.addAll(Arrays.asList(multBatchResults));
                }
                trainDataSelectedFeaturesRow.add(trainDataSelectedFeaturesElement);
                es.shutdown();
            }
            trainDataSelectedFeatures.add(trainDataSelectedFeaturesRow);
        }
    }

    private void trainDataSelectedSplitPoints(int treeNum) throws InterruptedException, ExecutionException {
        for (int k = 0; k < numAttributes - 1; k++) {
            List<List<Integer>> trainDataSelectedSplitPointsRow = new ArrayList<>();
            for (int l = 0; l < trainDataBitShares.size(); l++) {
                int vectorLength = bitLength;
                ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
                List<Future<Integer[]>> taskList = new ArrayList<>();
                List<Integer> rowSharesList = new ArrayList<>(Collections.nCopies(bitLength, rowShares.get(treeNum).get(k).get(l)));
                int i = 0;

                do {
                    int toIndex = Math.min(i + Constants.BATCH_SIZE, vectorLength);

                    BatchMultiplicationInteger mults = new BatchMultiplicationInteger(trainDataSelectedFeatures.get(l).get(k).subList(i, toIndex),
                            rowSharesList.subList(i, toIndex), decimalTiShares.subList(tiDecimalStartIndex + i, tiDecimalStartIndex + toIndex),
                            pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                            clientId, Constants.BINARY_PRIME, startpid, asymmetricBit, pid, partyCount);
                    taskList.add(es.submit(mults));
                    startpid++;
                    i = toIndex;

                } while (i < vectorLength);
                tiDecimalStartIndex += vectorLength;
                List<Integer> trainDataSelectedSplitPointsElement = new ArrayList<>();
                for (int j = 0; j < taskList.size(); j++) {
                    Integer[] multBatchResults = taskList.get(j).get();
                    trainDataSelectedSplitPointsElement.addAll(Arrays.asList(multBatchResults));
                }
                trainDataSelectedSplitPointsRow.add(trainDataSelectedSplitPointsElement);
                es.shutdown();
            }
            trainDataSelectedSplitPoints.add(trainDataSelectedSplitPointsRow);
        }
    }

    private void computeDotProductBitShares() throws InterruptedException, ExecutionException {
        for (int i = 0; i < numAttributes - 1; i++) {
            List<List<Integer>> currORInput = new ArrayList<>();
            for (int k = 0; k < trainDataSelectedSplitPoints.get(i).size(); k++) {
                currORInput.add(trainDataSelectedSplitPoints.get(i).get(k));
            }

            while (currORInput.size() > 1) {
                ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
                List<Future<Integer[]>> orTaskList = new ArrayList<>();

                for (int j = 0; j + 1 < currORInput.size(); j += 2) {
                    OR_XOR orModule = new OR_XOR(currORInput.get(j), currORInput.get(j + 1),
                            decimalTiShares.subList(tiDecimalStartIndex, tiDecimalStartIndex + bitLength),
                            asymmetricBit, 1, pidMapper, commonSender,
                            new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME, startpid, partyCount);

                    Future<Integer[]> orTask = es.submit(orModule);
                    tiDecimalStartIndex = tiDecimalStartIndex + bitLength;
                    orTaskList.add(orTask);
                    startpid++;
                }

                int n = currORInput.size();
                List<Integer> lastElement = new ArrayList<>();
                for (int m = 0; m < currORInput.get(n - 1).size(); m++) {
                    lastElement.add(currORInput.get(n - 1).get(m));
                }
                currORInput = new ArrayList<>();
                for (int l = 0; l < orTaskList.size(); l++) {
                    List<Integer> currORResult = new ArrayList<>();
                    Integer[] orResults = orTaskList.get(l).get();
                    currORResult.addAll(Arrays.asList(orResults));
                    currORInput.add(currORResult);
                }
                if (n % 2 != 0) {
                    currORInput.add(lastElement);
                }
                es.shutdown();
            }

            dotProductResultsBitShares.add(currORInput.get(0));
        }
    }
}
