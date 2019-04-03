package Model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import Utility.Constants;

import Communication.Message;
import Protocol.Utility.BatchMultiplicationByte;
import Protocol.Utility.BatchMultiplicationInteger;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.FileIO;
import Utility.Logging;

public class PrivateSetIntersectionOld extends Model {
	
	private int hashLength;
	private List<Integer> cShares;
	private List<List<Integer>> zShares;
	
	private List<List<List<Integer>>> polyShares;
	
	private int pid;
	
	private List<TripleByte> tiShares;
	

	public PrivateSetIntersectionOld(ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
			BlockingQueue<Message> senderQueue, int clientId, int asymmetricBit, int partyCount,
			Queue<Integer> protocolIdQueue, int protocolID, String[] args, List<TripleByte> tiShares) throws IOException, InterruptedException, ExecutionException {
		super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);
		
		this.tiShares = tiShares;
		initializeModelVariables(args);
		polyShares = new ArrayList<List<List<Integer>>>();
		System.out.println("cShares:");
		for (int i = 0; i <cShares.size(); i++) {
			System.out.println(cShares.get(i));
		}
		for (List<Integer> share : zShares) {
			List<List<Integer>> matrix = new ArrayList<List<Integer>>();
			for (int i = 0; i < share.size(); i++) {
				matrix.add(new ArrayList<>());
			}
			for (int j = 0; j < cShares.size(); j++) {
				for (int i = 0; i < share.size(); i++) {
					// asymmetric bit x-or's the message with 1 to get the z not's
					matrix.get(i).add(new Integer(
							(share.get(i).intValue() + 
									(this.asymmetricBit == 1? 
											(j / (cShares.size() / (int) (Math.pow(2, i+1)))) + 1 % 2
											: 0)) % Constants.BINARY_PRIME
					));
				}
			}
			for (int i = 0; i < share.size(); i++) {
				System.out.print(share.get(i) + " ");
			}
			System.out.println();
			matrix.add(cShares);
			polyShares.add(matrix);
		}	
		System.out.println("POLY SHARES:");
		System.out.println(polyShares);
		
        
		List<List<Integer>> results = new ArrayList<>();
		for (List<List<Integer>> matrix : polyShares) {
			
			int size = matrix.size();
			List<Integer> first = matrix.remove(0);
			for (int i = 0; i < size - 1; i++) {
				ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
				BatchMultiplicationByte multiplicationModule = new BatchMultiplicationByte(
	                    first, matrix.remove(0),
	                    this.tiShares, this.pidMapper, this.commonSender,
	                    this.protocolIdQueue, this.clientId, Constants.BINARY_PRIME, this.modelProtocolId, this.asymmetricBit, this.pid, partyCount);
				
	            Future<Integer[]> multiplicationTask = es.submit(multiplicationModule);
	            es.shutdown();
	            Integer[] res = multiplicationTask.get();
	            
	            first = new ArrayList<Integer>();
	            for (Integer inte : res) {
	            		first.add(inte);
	            }
	            this.pid++;
			}
			results.add(first);
		}
		ArrayList<Integer> fin = new ArrayList<>();
		for (int j = 0; j < cShares.size(); j++) {
			int sum = 0;
			for (int i = 0; i < results.size(); i++) {
				sum += results.get(i).get(j).intValue();
			}
			fin.add(sum % Constants.BINARY_PRIME);
		}
		System.out.println(fin);
	}

	/**
     * Initialize variables
     *
     * @param args
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
	                		break;
	                	case "cShares":
	                		cShares = new ArrayList<Integer>();
	                		List<BigInteger> shares = FileIO.loadListFromFile(value);
	                		for (BigInteger share : shares) {
	                			cShares.add(share.intValue());
	                		}
	                		break;
	                	case "zShares":
	                		zShares = new ArrayList<List<Integer>>();
	                		List<List<BigInteger>> zshares = FileIO.loadMatrixFromFile(value);
	                		for (List<BigInteger> share : zshares) {
	                			List<Integer> row = new ArrayList<>();
	                			for (BigInteger item : share) {
	                				row.add(item.intValue()); 
	                			}
	                			zShares.add(row);
	                		}
	                		break;
	            }
	    	}
    }
}
