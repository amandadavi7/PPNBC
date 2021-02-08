/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Model;

import Communication.Message;
import Protocol.BitDecomposition;
import Protocol.Comparison;
import Protocol.MultiplicationInteger;
import Protocol.OR_XOR;
import Protocol.PrivateSetIntersection;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 *
 * @author amanda
 */

public class NaiveBayesScoring extends Model {

    List<List<Integer>> privateDocumentShares, featureShares, probVector, probClass, probWord, probmultiply, bitSharesSet;
    List<TripleByte> binaryTiShares;
    List<TripleInteger> decimalTiShares;
    List<Integer> zeroVector, likelihood, addFinal, resultPSI, bitShares;
    Integer[] zqPsi, vectorMultiply;
    String[] args;
   
    int pid = 0;
    int hashLength;
    int qtdClass;
    int binaryTiIndex, decimalTiIndex;
    int decSharesStartInd, binSharesStartInd;
    int prime; 
    int predictedClassLabel;
        
    /**

     * @param pidMapper
     * @param senderQueue
     * @param clientId
     * @param asymmetricBit
     * @param partyCount
     * @param protocolIdQueue
     * @param protocolID
     * @param args
     * @param tiTriples
     * @param decimalTriples
     * @throws java.io.IOException
     */
	
    public NaiveBayesScoring(ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, BlockingQueue<Message> senderQueue, int clientId, 
            int asymmetricBit, int partyCount, Queue<Integer> protocolIdQueue, int protocolID, String[] args, List<TripleByte> tiTriples, 
            List<TripleInteger> decimalTriples) throws IOException {
        
        super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);
        
        this.args = args;
        privateDocumentShares = new ArrayList();
        featureShares = new ArrayList();    
        initializeModelVariables(args);
        this.binaryTiShares = tiTriples;
        this.decimalTiShares = decimalTriples;
        this.binaryTiIndex = 0;
        this.decimalTiIndex = 0;
        this.decSharesStartInd = 0;
        this.binSharesStartInd = 0;
        this.prime=prime;

    }
    
    /**
     * Initialize the model variables, test vector, and bit length
     * 
     * @param args
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private void initializeModelVariables(String[] args) throws FileNotFoundException, IOException {

	for (String arg : args) {
	    String[] currInput = arg.split("=");
	    if (currInput.length < 2) {
	        Logging.partyUsage();
	        System.exit(0);
	    }
	    
            String command = currInput[0];
	    String value = currInput[1];
                    
            switch (command) {
                case "hashLength":
                    hashLength = Integer.parseInt(value);
                    prime = (int) Math.pow(2, hashLength);
                    break;
                    
                case "numberClasses":
                    qtdClass = Integer.parseInt(value);
                    break;    
                                        
                case "featureShares":
                    featureShares = new ArrayList<>();
                    List<List<BigInteger>> pdshares = FileIO.loadMatrixFromFile(value);
                    for (List<BigInteger> share : pdshares) {
                        List<Integer> row = new ArrayList<>();
                        for (BigInteger item : share) {
                            row.add(item.intValue()); 
                        }
                        featureShares.add(row);
                    }
                    zeroVector = new ArrayList<>(Collections.nCopies(featureShares.size(), 0));

                    break;
                                        
                case "privateDocumentShares":
                    privateDocumentShares = new ArrayList<>();
                    List<List<BigInteger>> pashares = FileIO.loadMatrixFromFile(value);
                    for (List<BigInteger> ashare : pashares) {
                        List<Integer> row = new ArrayList<>();
                        for (BigInteger item : ashare) {
                            row.add(item.intValue()); 
                        }
                        privateDocumentShares.add(row);
                    }                    
                    
                    break;                   
                    
                case "probWord":
                    probWord = new ArrayList<>();
                    List<List<BigInteger>> pshares = FileIO.loadMatrixFromFile(value);
                    for (List<BigInteger> share : pshares) {
                        List<Integer> row = new ArrayList<>();
                        for (BigInteger item : share) {
                            row.add(item.intValue()); 
                        }
                        probWord.add(row);
                    }                    
                    break;  
                                                           
                case "probClass":
                    // both parties have the shares of the test vector
                    probClass = FileIO.loadIntListFromFile(value);
                    break;    
                        
            }
        }   
    }
    
    public void scoreNaiveBayes() throws IOException, InterruptedException, ExecutionException {

        //initializeModelVariables(args);   
   
        runPSI();                     
        runXOR();                 
        runMultiply();
        runAdd();            
        runBitDecomp();
        runCompare();
             
        System.out.print("PredictedClassLabel: " +  predictedClassLabel + "\n");
        
    }
    
    public void runPSI() throws InterruptedException, ExecutionException {
        
        PrivateSetIntersection privateIntersection; 

        privateIntersection = new PrivateSetIntersection(privateDocumentShares, featureShares,  pidMapper, commonSender, clientId, asymmetricBit, partyCount, 
                                new LinkedList<>(protocolIdQueue), pid, binaryTiShares);
        
        resultPSI = privateIntersection.call();
        
    }

    private void runXOR() throws InterruptedException, ExecutionException {
               
        OR_XOR xor;
                
        if (asymmetricBit == 1) {           
            xor = new OR_XOR(zeroVector,resultPSI, 
                    decimalTiShares.subList(decSharesStartInd, decSharesStartInd+resultPSI.size()),
                    asymmetricBit, 2, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
        } else {
            xor = new OR_XOR(resultPSI, zeroVector, 
                    decimalTiShares.subList(decSharesStartInd, decSharesStartInd+resultPSI.size()),
                    asymmetricBit, 2, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, prime, pid, partyCount);
        }
        pid++;
        decSharesStartInd += resultPSI.size();
        zqPsi = xor.call();

    }
    
    public void runMultiply() throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
        List<Future<Integer>> taskList = new ArrayList<>();
        
        int i=0, k=0; 

        for(List<Integer> share : probWord){ 
            for(int j = 0; j < share.size(); j++){
                MultiplicationInteger mult = new MultiplicationInteger(zqPsi[i],
                        share.get(j), decimalTiShares.get(k), 
                        pidMapper, commonSender, new LinkedList<>(protocolIdQueue), 
                        clientId, prime, pid, asymmetricBit, 0, partyCount);
                pid++;     
                Future<Integer> multiplicationTask = es.submit(mult);
                taskList.add(multiplicationTask);    
                k++;
            }
            i++;
        }
               
        int size = taskList.size();
        vectorMultiply = new Integer[size];
                
	for (i = 0; i < size; i++)
            vectorMultiply[i] = 0;
        
	for (i = 0; i < size; i++) {
            Future<Integer> resp = taskList.get(i);
            Integer res = resp.get();
            vectorMultiply[i] = Math.floorMod(res,prime);        
	}
                            
        probmultiply= new ArrayList<>();
        List<Integer> listMultiply = Arrays.asList(vectorMultiply);  
        
        for(i=0; i<probWord.size()*qtdClass-1; i += qtdClass)
            probmultiply.add(listMultiply.subList(i, Math.min(i + qtdClass, (probWord.size()*qtdClass))));
         
    }
        
    public void runAdd() throws InterruptedException, ExecutionException {
        likelihood=new ArrayList();
        addFinal = new ArrayList<>();

        int sum;
        
        for (int i = 0; i < qtdClass; i++) {
            sum=0;
            for (int j = 0; j < probWord.size(); j++) 
                sum = Math.floorMod(sum+probmultiply.get(j).get(i), prime);  
              
            likelihood.add(i, sum);
        }    
        
        for (int i = 0; i < qtdClass; i++)  
            addFinal.add(Math.floorMod(likelihood.get(i) + probClass.get(i).get(0), prime));  
                 
    }  
    
    private void runBitDecomp() throws InterruptedException, ExecutionException {
        
        bitSharesSet = new ArrayList<>();
        
        for(Integer add : addFinal){

            BitDecomposition bitDecomp = new BitDecomposition(add,
                    binaryTiShares.subList(binSharesStartInd, binSharesStartInd + hashLength*hashLength), 
                    asymmetricBit, hashLength, pidMapper, commonSender,
                    new LinkedList<>(protocolIdQueue), clientId, Constants.BINARY_PRIME, pid, partyCount);
            bitShares = bitDecomp.call();
        
            pid++;
            binSharesStartInd += hashLength*hashLength;
            bitSharesSet.add(bitShares);
        
        }
    }
    
    public void runCompare() throws InterruptedException, ExecutionException {
        
        Comparison comp = new Comparison(bitSharesSet.get(0), bitSharesSet.get(1),
                binaryTiShares.subList(binSharesStartInd, binSharesStartInd + hashLength*hashLength), 
                asymmetricBit, pidMapper, commonSender, new LinkedList<>(protocolIdQueue),
                clientId, Constants.BINARY_PRIME, pid, partyCount);
        
        predictedClassLabel = comp.call();
        pid++;
        binSharesStartInd += hashLength*hashLength;           
 
    }
}    