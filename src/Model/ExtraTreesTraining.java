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
import TrustedInitializer.TripleBigInteger;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import Utility.Logging;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author Chaitali
 */
public class ExtraTreesTraining extends Model {

    private int roundOff, numAttributes, bitLength, treeCount, maxTreeDepth, featureCount;
    private String fileName;
    String[] args;
    List<List<Integer>> trainData;
    List<List<List<Integer>>> trainDataBitShares;
    List<List<List<Integer>>> rowShares;
    List<List<Integer>> colShares;
    List<TripleInteger> decimalTiShares;
    List<TripleByte> binaryTiShares;
    List<TripleBigInteger> bigIntTiShares;
    List<Integer> wholeNumTiShares;
    List<BigInteger> equalityTiShares;
    List<Integer> dotProductResults;
    List<String>[] decisionRules;
    List<ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>>> pidMapperList;
    ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper;
    Integer prime;
    String outputPath;
    List<Integer> pidList, tiBinaryStartIndexList, tiDecimalStartIndexList, tiBigIntStartIndexList, tiEqualityStartIndexList;

    public ExtraTreesTraining(int asymmetricBit, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, BlockingQueue<Message> senderQueue, int clientId, int partyCount, List<List<List<Integer>>> rowShares, List<List<Integer>> colShares, List<TripleInteger> decimalShares, List<TripleByte> binaryShares, List<TripleBigInteger> bigIntTriples, List<Integer> wholeNumShares, List<BigInteger> equalityShares, String[] args, Queue<Integer> protocolIdQueue, int protocolID, List<ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>>> pidMapperList, int threadID) {
        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID, threadID);
        trainDataBitShares = new ArrayList<>();
        trainData = new ArrayList<>();
        //this.decisionRules = new ArrayList[treeCount];
        this.rowShares = rowShares;
        this.colShares = colShares;
        this.decimalTiShares = decimalShares;
        this.binaryTiShares = binaryShares;
        this.wholeNumTiShares = wholeNumShares;
        this.equalityTiShares = equalityShares;
        this.bigIntTiShares = bigIntTriples;
        this.prime = Constants.PRIME;
        this.pidMapperList = pidMapperList;
        this.pidMapper = pidMapper;
        this.pidList = new ArrayList<>();
        this.tiBinaryStartIndexList = new ArrayList<>();
        this.tiDecimalStartIndexList = new ArrayList<>();
        this.tiBigIntStartIndexList = new ArrayList<>();
        this.tiEqualityStartIndexList = new ArrayList<>();
        this.outputPath = "/home/chaitali/";
        this.args = args;
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

                case "maxTreeDepth":
                    maxTreeDepth = Integer.parseInt(value);
                    break;

                case "featureCount":
                    featureCount = Integer.parseInt(value);
                    break;
            }
        }
    }

    public void runModel2() throws InterruptedException, ExecutionException {
        List<List<Integer>> trainDataComparisonOutputs = new ArrayList<>();
        Byte[][][] attrValues = generateAttributeValuesForDTTraining(trainDataComparisonOutputs);
        Byte[][] classValues = generateBinaryClassValuesForDTTraining();

        loadDataFromFile(fileName);
        System.out.println("Loaded Data.");

        int treeNum = 0, t = 0;
        pidList.add(0);
        //int tiBinaryStartIndex = (4000*t < binaryTiShares.size() ? 4000*t : 0);
        tiBinaryStartIndexList.add(4000 * t < binaryTiShares.size() ? 4000 * t : 0);
        //int tiDecimalStartIndex = (4000*t < decimalTiShares.size() ? 4000*t : 0);
        tiDecimalStartIndexList.add(4000 * t < decimalTiShares.size() ? 4000 * t : 0);
        //int tiBigIntStartIndex = (4000*t < bigIntTiShares.size() ? 4000*t : 0);
        tiBigIntStartIndexList.add(4000 * t < bigIntTiShares.size() ? 4000 * t : 0);
        //int tiEqualityStartIndex = (4000*t < equalityTiShares.size() ? 4000*t : 0);
        tiEqualityStartIndexList.add(4000 * t < equalityTiShares.size() ? 4000 * t : 0);
        int tiBinaryEndIndex = tiBinaryStartIndexList.get(treeNum) + 4000;
        int tiDecimalEndIndex = tiDecimalStartIndexList.get(treeNum) + 4000;
        int tiBigIntEndIndex = tiBigIntStartIndexList.get(treeNum) + 4000;
        int tiEqualityEndIndex = tiEqualityStartIndexList.get(treeNum) + 4000;

        List<Integer> selectedFeatureMapping = new ArrayList<>();
        Integer[] selectedMapping = {1, 2, 3, 4, 5, 6, 7};
        for (int i = 0; i < featureCount; i++) {
            selectedFeatureMapping.add(selectedMapping[i]);
        }

        trainADecisionTree(tiBinaryStartIndexList, tiBinaryEndIndex, treeNum, tiBigIntStartIndexList, tiBigIntEndIndex, tiDecimalStartIndexList, tiDecimalEndIndex, tiEqualityStartIndexList, tiEqualityEndIndex, classValues, attrValues, selectedFeatureMapping);
        /*DecisionTreeTraining dtModel = new DecisionTreeTraining(
                        asymmetricBit, pidMapperList.get(0), commonSender, clientId,
                binaryTiShares.subList(0, 0 + 2000),
                            bigIntTiShares.subList(0, 0 + 2000),
                            decimalTiShares.subList(0, 0 + 2000),
                            equalityTiShares.subList(0, 0 + 2000),
                args, partyCount,
                        protocolIdQueue, 0, 0);

                dtModel.trainDecisionTree();*/
    }

    public void runModel() throws InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();

        // Load training data from file
        loadDataFromFile(fileName);
        System.out.println("Loaded Data.");

        this.decisionRules = new ArrayList[treeCount];

        // Convert training data to binary
        trainDataBitShares = new ArrayList<>();
        convertTrainDataToBinary();
        System.out.println("Converted train data to binary.");
        //System.out.println("Time taken for input data conversion is " + (System.currentTimeMillis() - startTime1));

        numAttributes = trainData.get(0).size();

        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        ExecutorCompletionService<AbstractMap.SimpleEntry<Integer, List<String>>> multCompletionService = new ExecutorCompletionService<>(es);

        for (int t = 0; t < treeCount; t++) {
            final int treeNum = t;
            List<List<List<Integer>>> selectedTrainDataBitShares = new ArrayList<>();
            List<Integer> selectedFeatureMapping = new ArrayList<>();
            for (int i = 0; i < trainDataBitShares.size(); i++) {
                List<List<Integer>> currRow = new ArrayList<>();
                for (int j = 0; j < numAttributes - 1; j++) {
                    if (colShares.get(treeNum).get(j) == 1) {
                        List<Integer> currShare = new ArrayList<>();
                        for (int k = 0; k < bitLength; k++) {
                            currShare.add(trainDataBitShares.get(i).get(j).get(k));
                        }
                        currRow.add(currShare);
                    }
                }
                selectedTrainDataBitShares.add(currRow);
            }

            for (int i = 0; i < colShares.get(treeNum).size(); i++) {
                if (colShares.get(treeNum).get(i) == 1) {
                    selectedFeatureMapping.add(i);
                }
            }

            /*Integer [] selectedMapping = {0,1,2,3,4,5,6};
            for(int i=0; i<featureCount; i++) {
                selectedFeatureMapping.add(selectedMapping[i]);
            }*/
            if (asymmetricBit == 1) {
                try {
                    FileWriter writer = new FileWriter(this.outputPath + "/Selected_Feature_Mapping_tree_" + treeNum + ".txt");
                    for (int i = 0; i < selectedFeatureMapping.size(); i++) {
                        writer.write(Integer.toString(selectedFeatureMapping.get(i)));
                        writer.write(" ");
                    }
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            //int pid = 0;
            pidList.add(0);
            //int tiBinaryStartIndex = (4000*t < binaryTiShares.size() ? 4000*t : 0);
            tiBinaryStartIndexList.add(4000 * t < binaryTiShares.size() ? 4000 * t : 0);
            //int tiDecimalStartIndex = (4000*t < decimalTiShares.size() ? 4000*t : 0);
            tiDecimalStartIndexList.add(4000 * t < decimalTiShares.size() ? 4000 * t : 0);
            //int tiBigIntStartIndex = (4000*t < bigIntTiShares.size() ? 4000*t : 0);
            tiBigIntStartIndexList.add(4000 * t < bigIntTiShares.size() ? 4000 * t : 0);
            //int tiEqualityStartIndex = (4000*t < equalityTiShares.size() ? 4000*t : 0);
            tiEqualityStartIndexList.add(4000 * t < equalityTiShares.size() ? 4000 * t : 0);
            multCompletionService.submit(() -> {
                FileWriter writer = new FileWriter(this.outputPath + "/Party_" + clientId + "_log_tree_" + treeNum + ".txt");
                // In extra trees algorithm, we only consider K random attributes for
                // constructing a decision tree. In our case, trusted initializer tells
                // us which K columns we need to choose for constructing a tree. 

                // For each column, replaces the values which are not selected as
                // split point by zeros.
                List<List<List<Integer>>> trainDataSelectedSplitPoints = trainDataSelectedSplitPoints(treeNum,
                        selectedTrainDataBitShares,
                        tiDecimalStartIndexList,
                        tiDecimalStartIndexList.get(treeNum) + 4000,
                        pidList);
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
                List<List<Integer>> dotProductResultsBitShares = computeDotProductBitShares(trainDataSelectedSplitPoints,
                        tiDecimalStartIndexList,
                        tiDecimalStartIndexList.get(treeNum) + 4000,
                        pidList,
                        treeNum);
                System.out.println("Completed calculation of dot product bit shares using OR.");
                //System.out.println("Time taken is " + (System.currentTimeMillis() - startTime));
                //System.out.println("Dot product bit shares calcualted using OR module are");
                for (int i = 0; i < dotProductResultsBitShares.size(); i++) {
                    for (int j = 0; j < dotProductResultsBitShares.get(i).size(); j++) {
                        writer.write(Integer.toString(dotProductResultsBitShares.get(i).get(j)));
                    }
                    writer.write(" ");
                    //System.out.print(" ");
                }
                writer.flush();
                writer.close();
                // For each column, compare the values with split point for that
                // column. Values which are greater than or equal to split point
                // are replaced by 1 and values which are less than split point are
                // replaced by 0.
                List<List<Integer>> trainDataComparisonOutputs = doComparison(selectedTrainDataBitShares,
                        dotProductResultsBitShares,
                        tiBinaryStartIndexList,
                        tiBinaryStartIndexList.get(treeNum) + 4000,
                        pidList,
                        treeNum);
                System.out.println("Completed comparison.");
                //writer = new FileWriter(this.outputPath + "/Party_" + clientId + "_discretized_dataset_tree_" + treeNum + ".txt");
                //System.out.println("Time taken is " + (System.currentTimeMillis() - startTime));
                //System.out.println("Discretized dataset:");
                /*for (int i = 0; i < trainDataComparisonOutputs.size(); i++) {
                    for (int j = 0; j < trainDataComparisonOutputs.get(i).size(); j++) {
                        //System.out.print(trainDataComparisonOutputs.get(i).get(j) + " ");
                        writer.write(trainDataComparisonOutputs.get(i).get(j) + ",");
                    }
                    writer.write(trainData.get(i).get(numAttributes - 1) + ",");
                    //System.out.println("");
                    writer.write("\n");
                }
                writer.flush();
                writer.close();*/

                // Now that we have discretized the dataset, next we need to train decision trees.
                // For that, we need to convert the data into the format reuired by decision tree training modeule in Lynx.
                Byte[][][] attrValues = generateAttributeValuesForDTTraining(trainDataComparisonOutputs);
                System.out.println("Generated attribute values in required form for DT learning.");
                //writer = new FileWriter(this.outputPath + "/Party_" + clientId + "_attr_values_tree_" + treeNum + ".txt");
                //System.out.println("Time taken is " + (System.currentTimeMillis() - startTime));
                //System.out.println("AttrValues for Decision Tree Learning");
                /*for (int i = 0; i < attrValues.length; i++) {
                    for (int j = 0; j < attrValues[i].length; j++) {
                        for (int k = 0; k < attrValues[i][j].length; k++) {
                            //System.out.print(attrValues[i][j][k] + " ");
                            writer.write(attrValues[i][j][k] + ",");
                        }
                        //System.out.println("");
                        writer.write("\n");
                    }
                }
                writer.flush();
                writer.close();*/

                Byte[][] classValues;
                if (wholeNumTiShares.size() == 2) {
                    //System.out.println("Called generateBinaryClassValuesForDTTraining");
                    classValues = generateBinaryClassValuesForDTTraining();
                } else {
                    classValues = generateClassValuesForDTTraining(tiBinaryStartIndexList, tiBinaryStartIndexList.get(treeNum) + 4000, pidList, treeNum);
                }
                System.out.println("Generated class values in required form for DT learning.");
                //writer = new FileWriter(this.outputPath + "/Party_" + clientId + "_class_values_tree_" + treeNum + ".txt");                
                //System.out.println("Time taken is " + (System.currentTimeMillis() - startTime));
                //System.out.println("ClassValues for Decision Tree Learning");
                /*for (int i = 0; i < classValues.length; i++) {
                    for (int j = 0; j < classValues[i].length; j++) {
                        //System.out.print(classValues[i][j] + " ");
                        writer.write(classValues[i][j] + ",");
                    }
                    //System.out.println("");
                    writer.write("\n");
                }
                writer.flush();
                writer.close();*/
                return trainADecisionTree(tiBinaryStartIndexList,
                        tiBinaryStartIndexList.get(treeNum) + 4000,
                        treeNum,
                        tiBigIntStartIndexList,
                        tiBigIntStartIndexList.get(treeNum) + 4000,
                        tiDecimalStartIndexList,
                        tiDecimalStartIndexList.get(treeNum) + 4000,
                        tiEqualityStartIndexList,
                        tiEqualityStartIndexList.get(treeNum) + 4000,
                        classValues, attrValues, selectedFeatureMapping);

            });
        }

        es.shutdown();
        for (int i = 0; i < treeCount; i++) {
            try {
                Future<AbstractMap.SimpleEntry<Integer, List<String>>> prod = multCompletionService.take();
                AbstractMap.SimpleEntry<Integer, List<String>> currDecisionTree = prod.get();
                if (currDecisionTree.getValue() == null) {
                    System.out.println("currDecisionRule is null");
                }
                decisionRules[currDecisionTree.getKey()] = currDecisionTree.getValue();

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        es.shutdown();
        if (asymmetricBit == 1) {
            try {
                FileWriter writer = new FileWriter(this.outputPath + "/eT_result.txt");
                for (List<String> rules : decisionRules) {
                    String collect = rules.stream().collect(Collectors.joining(","));
                    writer.write(collect + "\n");
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        System.out.println("Finished Training");
        long endTime = System.currentTimeMillis();
        System.out.println("Time required " + (endTime - startTime) + "milliseconds");
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

    private List<List<Integer>> doComparison(List<List<List<Integer>>> trainDataSelectedFeatures, List<List<Integer>> dotProductResultsBitShares, List<Integer> tiBinaryStartIndexList, int tiBinaryEndIndex, List<Integer> pidList, int treeNum) throws InterruptedException, ExecutionException {
        List<List<Integer>> trainDataComparisonOutputs = new ArrayList<>();
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<List<Future<Integer>>> taskList = new ArrayList<>();
        int comparisonTiCount = (2 * bitLength) + ((bitLength * (bitLength - 1)) / 2);
        tiBinaryStartIndexList.set(treeNum, (tiBinaryStartIndexList.get(treeNum) + comparisonTiCount < tiBinaryEndIndex ? tiBinaryStartIndexList.get(treeNum) : 0));
        for (int i = 0; i < trainDataSelectedFeatures.size(); i++) {
            List<Future<Integer>> taskListRow = new ArrayList<>();
            for (int j = 0; j < featureCount; j++) {
                Comparison comp = new Comparison(trainDataSelectedFeatures.get(i).get(j), dotProductResultsBitShares.get(j),
                        binaryTiShares.subList(tiBinaryStartIndexList.get(treeNum), tiBinaryStartIndexList.get(treeNum) + comparisonTiCount),
                        asymmetricBit, pidMapperList.get(treeNum + 1), commonSender, new LinkedList<>(protocolIdQueue),
                        clientId, Constants.BINARY_PRIME, pidList.get(treeNum), partyCount, treeNum + 1);
                taskListRow.add(es.submit(comp));
                //tiBinaryStartIndex += comparisonTiCount;
                tiBinaryStartIndexList.set(treeNum, (tiBinaryStartIndexList.get(treeNum) + 2 * comparisonTiCount < tiBinaryEndIndex ? tiBinaryStartIndexList.get(treeNum) + comparisonTiCount : 0));
                pidList.set(treeNum, pidList.get(treeNum) + 1);
            }
            taskList.add(taskListRow);
        }
        for (int i = 0; i < trainDataSelectedFeatures.size(); i++) {
            List<Integer> compShares = new ArrayList<>();
            for (int j = 0; j < featureCount; j++) {
                Future<Integer> compResult = taskList.get(i).get(j);
                compShares.add(compResult.get());
            }
            trainDataComparisonOutputs.add(compShares);
        }
        es.shutdown();
        return trainDataComparisonOutputs;
    }

    private Byte[][][] generateAttributeValuesForDTTraining(List<List<Integer>> trainDataComparisonOutputs) {
        Byte[][][] attrValues = new Byte[featureCount][2][trainData.size()];
        Integer curr;
        for (int i = 0; i < attrValues.length; i++) {
            for (int j = 0; j < attrValues[i].length; j++) {
                for (int k = 0; k < attrValues[i][j].length; k++) {
                    if (j == 0 && asymmetricBit == 1) {
                        curr = 1 - trainDataComparisonOutputs.get(k).get(i);
                    } else {
                        curr = trainDataComparisonOutputs.get(k).get(i);
                    }
                    attrValues[i][j][k] = curr.byteValue();
                }
            }
        }
        /*if (asymmetricBit == 1) {
            Byte[][][] attrValues = {{{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}},
            {{0, 1, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1}, {1, 0, 1, 0, 1, 1, 0, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0}},
            {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}},
            {{0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0}, {1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1}},
            {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}},
            {{1, 1, 0, 1, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 1, 0}, {0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 1}},
            {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}}};
            return attrValues;
        }

        Byte[][][] attrValues = {{{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}},
        {{1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1}, {1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1}},
        {{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}},
        {{1, 0, 1, 0, 0, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 0}, {1, 0, 1, 0, 0, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 0}},
        {{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}},
        {{1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0}, {1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0}},
        {{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}}};*/
        return attrValues;
    }

    private Byte[][] generateBinaryClassValuesForDTTraining() {
        Byte[][] classValues = new Byte[wholeNumTiShares.size()][trainData.size()];
        Integer curr;
        for (int i = 0; i < classValues.length; i++) {
            for (int j = 0; j < classValues[i].length; j++) {
                if (i == 0 && asymmetricBit == 1) {
                    curr = 1 - trainData.get(j).get(numAttributes - 1);
                } else {
                    curr = trainData.get(j).get(numAttributes - 1);
                }
                classValues[i][j] = curr.byteValue();
            }
        }
        /*if (asymmetricBit == 1) {
            Byte[][] classValues = {{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1}};
            return classValues;
        }
        Byte[][] classValues = {{0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};*/
        return classValues;
    }

    private Byte[][] generateClassValuesForDTTraining(List<Integer> tiBinaryStartIndexList, int tiBinaryEndIndex, List<Integer> pidList, int treeNum) throws InterruptedException, ExecutionException {
        Byte[][] classValues = new Byte[wholeNumTiShares.size()][trainData.size()];
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<List<Integer>>> bitDtaskList1 = new ArrayList<>();
        tiBinaryStartIndexList.set(treeNum, (tiBinaryStartIndexList.get(treeNum) + bitLength * bitLength < tiBinaryEndIndex ? tiBinaryStartIndexList.get(treeNum) : 0));
        for (int i = 0; i < classValues.length; i++) {
            BitDecomposition bitDModule = new BitDecomposition(wholeNumTiShares.get(i),
                    binaryTiShares.subList(tiBinaryStartIndexList.get(treeNum), tiBinaryStartIndexList.get(treeNum) + bitLength * bitLength),
                    asymmetricBit, bitLength, pidMapperList.get(treeNum + 1), commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME, pidList.get(treeNum), partyCount, treeNum + 1);
            bitDtaskList1.add(es.submit(bitDModule));
            //tiBinaryStartIndex += bitLength * bitLength;
            tiBinaryStartIndexList.set(treeNum, (tiBinaryStartIndexList.get(treeNum) + 2 * (bitLength * bitLength) < tiBinaryEndIndex ? tiBinaryStartIndexList.get(treeNum) + bitLength * bitLength : 0));
            pidList.set(treeNum, pidList.get(treeNum) + 1);
        }
        List<List<Integer>> classValueBitShares = new ArrayList<>();
        tiBinaryStartIndexList.set(treeNum, (tiBinaryStartIndexList.get(treeNum) < tiBinaryEndIndex ? tiBinaryStartIndexList.get(treeNum) : 0));
        for (int i = 0; i < classValues.length; i++) {
            Future<List<Integer>> bitDResult2 = bitDtaskList1.get(i);
            classValueBitShares.add(bitDResult2.get());
        }

        List<List<Future<Integer>>> taskList = new ArrayList<>();
        for (int i = 0; i < classValues.length; i++) {
            List<Future<Integer>> taskListRow = new ArrayList<>();
            for (int j = 0; j < classValues[i].length; j++) {
                EqualityByte EqModule = new EqualityByte(trainDataBitShares.get(j).get(numAttributes - 1), classValueBitShares.get(i),
                        binaryTiShares.get(tiBinaryStartIndexList.get(treeNum)), pidMapperList.get(treeNum + 1), commonSender,
                        new LinkedList<>(protocolIdQueue), clientId, prime, pidList.get(treeNum), asymmetricBit, partyCount, treeNum + 1);
                taskListRow.add(es.submit(EqModule));
                //tiBinaryStartIndex++;
                tiBinaryStartIndexList.set(treeNum, (tiBinaryStartIndexList.get(treeNum) + 1 < tiBinaryEndIndex ? tiBinaryStartIndexList.get(treeNum) + 1 : 0));
                pidList.set(treeNum, pidList.get(treeNum) + 1);
            }
            taskList.add(taskListRow);
        }
        for (int i = 0; i < classValues.length; i++) {
            for (int j = 0; j < classValues[i].length; j++) {
                Future<Integer> compResult = taskList.get(i).get(j);
                classValues[i][j] = compResult.get().byteValue();
            }
        }
        es.shutdown();
        return classValues;
    }

    private AbstractMap.SimpleEntry<Integer, List<String>> trainADecisionTree(List<Integer> tiBinaryStartIndexList, int tiBinaryEndIndex, int treeNum, List<Integer> tiBigIntStartIndexList, int tiBigIntEndIndex, List<Integer> tiDecimalStartIndexList, int tiDecimalEndIndex, List<Integer> tiEqualityStartIndexList, int tiEqualityEndIndex, Byte[][] classValues, Byte[][][] attrValues, List<Integer> selectedFeatureMapping) {
        // tiBinaryStartIndex += 2000;
        tiBinaryStartIndexList.set(treeNum, (tiBinaryStartIndexList.get(treeNum) + 2000 < tiBinaryEndIndex ? tiBinaryStartIndexList.get(treeNum) : 0));
        // tiDecimalStartIndex += 2000;
        tiDecimalStartIndexList.set(treeNum, (tiDecimalStartIndexList.get(treeNum) + 2000 < tiDecimalEndIndex ? tiDecimalStartIndexList.get(treeNum) : 0));
        //tiEqualityStartIndex += 2000;
        tiEqualityStartIndexList.set(treeNum, (tiEqualityStartIndexList.get(treeNum) + 2000 < tiEqualityEndIndex ? tiEqualityStartIndexList.get(treeNum) : 0));
        // tiBigIntStartIndex += 2000;
        tiBigIntStartIndexList.set(treeNum, (tiBigIntStartIndexList.get(treeNum) + 2000 < tiBigIntEndIndex ? tiBigIntStartIndexList.get(treeNum) : 0));
        DecisionTreeTraining dtModel = null;
        try {
            dtModel = new DecisionTreeTraining(
                    asymmetricBit,
                    pidMapperList.get(treeNum + 1),
                    commonSender,
                    clientId,
                    binaryTiShares.subList(tiBinaryStartIndexList.get(treeNum), tiBinaryStartIndexList.get(treeNum) + 2000),
                    bigIntTiShares.subList(tiBigIntStartIndexList.get(treeNum), tiBigIntStartIndexList.get(treeNum) + 2000),
                    decimalTiShares.subList(tiDecimalStartIndexList.get(treeNum), tiDecimalStartIndexList.get(treeNum) + 2000),
                    equalityTiShares.subList(tiEqualityStartIndexList.get(treeNum), tiEqualityStartIndexList.get(treeNum) + 2000),
                    partyCount,
                    new LinkedList<>(protocolIdQueue),
                    pidList.get(treeNum),
                    wholeNumTiShares.size(),
                    featureCount,
                    2,
                    trainData.size(),
                    attrValues,
                    classValues,
                    true,
                    treeNum + 1,
                    maxTreeDepth,
                    selectedFeatureMapping
            );
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Created dt object");
        try {
            dtModel.trainDecisionTree();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Tree " + treeNum + " finished");
        pidList.set(treeNum, pidList.get(treeNum) + 1);
        AbstractMap.SimpleEntry<Integer, List<String>> entry = new AbstractMap.SimpleEntry<>(treeNum, dtModel.decisionTreeNodes);
        return entry;
    }

    /*private void trainDecisionTrees(Byte[][][][] attrValues, Byte[][][] classValues) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        ExecutorCompletionService<List<String>> multCompletionService = new ExecutorCompletionService<>(es);

        for (int i = 0; i < attrValues.length; i++) {
            int treeNum = i;
            multCompletionService.submit(() -> {
                System.out.println("Creating dt object");
                DecisionTreeTraining dtModel = null;
                try {
                    dtModel = new DecisionTreeTraining(
                            asymmetricBit,
                            pidMapperList.get(treeNum + 1),
                            senderQueue,
                            clientId,
                            binaryTiShares.subList(tiBinaryStartIndex, tiBinaryStartIndex + 2000),
                            bigIntTiShares.subList(tiBigIntStartIndex, tiBigIntStartIndex + 2000),
                            decimalTiShares.subList(tiDecimalStartIndex, tiDecimalStartIndex + 2000),
                            equalityTiShares.subList(tiEqualityStartIndex, tiEqualityStartIndex + 2000),
                            partyCount,
                            new LinkedList<>(protocolIdQueue),
                            protocolID,
                            wholeNumTiShares.size(),
                            numAttributes - 1,
                            2,
                            trainData.size(),
                            attrValues[treeNum],
                            classValues[treeNum],
                            true,
                            treeNum + 1,
                            maxTreeDepth
                    );
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                System.out.println("Created dt object");
                try {
                    dtModel.trainDecisionTree();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                System.out.println("Tree " + treeNum + " finished");
                return dtModel.decisionTreeNodes;
            });
            // tiBinaryStartIndex += 2000;
            tiBinaryStartIndex = (tiBinaryStartIndex + 4000 < binaryTiShares.size() ? tiBinaryStartIndex + 2000 : 0);
            // tiDecimalStartIndex += 2000;
            tiDecimalStartIndex = (tiDecimalStartIndex + 4000 < decimalTiShares.size() ? tiDecimalStartIndex + 2000 : 0);
            //tiEqualityStartIndex += 2000;
            tiEqualityStartIndex = (tiEqualityStartIndex + 4000 < equalityTiShares.size() ? tiEqualityStartIndex + 2000 : 0);
            // tiBigIntStartIndex += 2000;
            tiBigIntStartIndex = (tiBigIntStartIndex + 4000 < bigIntTiShares.size() ? tiBigIntStartIndex + 2000 : 0);
        }
        es.shutdown();
        for (int i = 0; i < attrValues.length; i++) {
            try {
                Future<List<String>> prod = multCompletionService.take();
                List<String> currDecisionRule = prod.get();
                if (currDecisionRule == null) {
                    System.out.println("currDecisionRule is null");
                }
                decisionRules.add(currDecisionRule);

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        es.shutdown();
        if (asymmetricBit == 1) {
            try {
                FileWriter writer = new FileWriter(this.outputPath + "/eT_result.txt");
                for (List<String> rules : decisionRules) {
                    String collect = rules.stream().collect(Collectors.joining(","));
                    writer.write(collect + "\n");
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        System.out.println("Finished Training");
        for (int i = 0; i < attrValues.length; i++) {
            DecisionTreeTraining dtModel = new DecisionTreeTraining(
                    asymmetricBit,
                    pidMapper,
                    commonSender,
                    clientId,
                    binaryTiShares.subList(tiBinaryStartIndex, tiBinaryStartIndex + 2000),
                    bigIntTiShares.subList(tiBigIntStartIndex, tiBigIntStartIndex + 2000),
                    decimalTiShares.subList(tiDecimalStartIndex, tiDecimalStartIndex + 2000),
                    equalityTiShares.subList(tiEqualityStartIndex, tiEqualityStartIndex + 2000),
                    partyCount,
                    new LinkedList<>(protocolIdQueue),
                    pid,
                    wholeNumTiShares.size(),
                    numAttributes - 1,
                    2,
                    trainData.size(),
                    attrValues[i],
                    classValues[i],
                    true,
                    threadID
            );

            dtModel.trainDecisionTree();
            decisionRules.add(dtModel.decisionTreeNodes);
            tiBinaryStartIndex += 2000;
            tiDecimalStartIndex += 2000;
            tiEqualityStartIndex += 2000;
            tiBigIntStartIndex += 2000;
            pid++;
        }
    }*/
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

    private List<List<List<Integer>>> trainDataSelectedSplitPoints(int treeNum, List<List<List<Integer>>> selectedTrainDataBitShares, List<Integer> tiDecimalStartIndexList, int tiDecimalEndIndex, List<Integer> pidList) throws InterruptedException, ExecutionException {
        List<List<List<Integer>>> trainDataSelectedSplitPoints = new ArrayList<>();
        int currPid = pidList.get(treeNum);
        for (int k = 0; k < featureCount; k++) {
            List<List<Integer>> trainDataSelectedSplitPointsRow = new ArrayList<>();
            for (int l = 0; l < selectedTrainDataBitShares.size(); l++) {
                int vectorLength = bitLength;
                ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
                List<Future<Integer[]>> taskList = new ArrayList<>();
                List<Integer> rowSharesList = new ArrayList<>(Collections.nCopies(bitLength, rowShares.get(treeNum).get(k).get(l)));
                int i = 0;

                do {
                    int toIndex = Math.min(i + Constants.BATCH_SIZE, vectorLength);

                    if (tiDecimalStartIndexList.get(treeNum) + i + toIndex >= tiDecimalEndIndex) {
                        tiDecimalStartIndexList.set(treeNum, 0);
                    }
                    BatchMultiplicationInteger mults = new BatchMultiplicationInteger(selectedTrainDataBitShares.get(l).get(k).subList(i, toIndex),
                            rowSharesList.subList(i, toIndex), decimalTiShares.subList(tiDecimalStartIndexList.get(treeNum) + i, tiDecimalStartIndexList.get(treeNum) + toIndex),
                            pidMapperList.get(treeNum + 1), commonSender, new LinkedList<>(protocolIdQueue),
                            clientId, Constants.BINARY_PRIME, pidList.get(treeNum), asymmetricBit, currPid, partyCount, treeNum + 1);
                    taskList.add(es.submit(mults));
                    pidList.set(treeNum, pidList.get(treeNum) + 1);
                    i = toIndex;

                } while (i < vectorLength);
                // tiDecimalStartIndex += vectorLength;
                tiDecimalStartIndexList.set(treeNum, (tiDecimalStartIndexList.get(treeNum) + vectorLength < tiDecimalEndIndex ? tiDecimalStartIndexList.get(treeNum) + vectorLength : 0));
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
        return trainDataSelectedSplitPoints;
    }

    private List<List<Integer>> computeDotProductBitShares(List<List<List<Integer>>> trainDataSelectedSplitPoints, List<Integer> tiDecimalStartIndexList, int tiDecimalEndIndex, List<Integer> pidList, int treeNum) throws InterruptedException, ExecutionException {
        List<List<Integer>> dotProductResultsBitShares = new ArrayList<>();
        for (int i = 0; i < featureCount; i++) {
            List<List<Integer>> currORInput = new ArrayList<>();
            for (int k = 0; k < trainDataSelectedSplitPoints.get(i).size(); k++) {
                currORInput.add(trainDataSelectedSplitPoints.get(i).get(k));
            }

            tiDecimalStartIndexList.set(treeNum, (tiDecimalStartIndexList.get(treeNum) + bitLength < tiDecimalEndIndex ? tiDecimalStartIndexList.get(treeNum) : 0));

            while (currORInput.size() > 1) {
                ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
                List<Future<Integer[]>> orTaskList = new ArrayList<>();

                for (int j = 0; j + 1 < currORInput.size(); j += 2) {
                    OR_XOR orModule = new OR_XOR(currORInput.get(j), currORInput.get(j + 1),
                            decimalTiShares.subList(tiDecimalStartIndexList.get(treeNum), tiDecimalStartIndexList.get(treeNum) + bitLength),
                            asymmetricBit, 1, pidMapperList.get(treeNum + 1), commonSender,
                            new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME, pidList.get(treeNum), partyCount, treeNum + 1);

                    Future<Integer[]> orTask = es.submit(orModule);
                    // tiDecimalStartIndex = tiDecimalStartIndex + bitLength;
                    tiDecimalStartIndexList.set(treeNum, (tiDecimalStartIndexList.get(treeNum) + 2 * bitLength < tiDecimalEndIndex ? tiDecimalStartIndexList.get(treeNum) + bitLength : 0));
                    orTaskList.add(orTask);
                    pidList.set(treeNum, pidList.get(treeNum) + 1);
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
        return dotProductResultsBitShares;
    }
}
