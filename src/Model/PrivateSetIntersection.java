package Model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import Communication.Message;
import Protocol.Utility.BatchMultiplicationByte;
import TrustedInitializer.TripleByte;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;

public class PrivateSetIntersection extends Model {

	private int hashLength;
	private List<List<Integer>> privateDocumentShares;
	private List<List<Integer>> featureShares;
	
	private List<List<List<Integer>>> polyShares;
	
	private int pid;
	
	private List<TripleByte> tiShares;
	
	public PrivateSetIntersection(ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
	BlockingQueue<Message> senderQueue, int clientId, int asymmetricBit, int partyCount,
	Queue<Integer> protocolIdQueue, int protocolID, String[] args, List<TripleByte> tiShares) throws IOException, InterruptedException, ExecutionException {
		
		super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID);
		
		this.initializeModelVariables(args);
		
		ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
		List<Future<Integer>> taskList = new ArrayList<>();
		
		for (List<Integer> share : privateDocumentShares) {
			for (int i = 0; i < featureShares.size(); i++) {
				// replace task for equality test of featureShares.get(i) and share
				BatchMultiplicationByte task = new BatchMultiplicationByte(
	                    featureShares.get(i), share,
	                    this.tiShares, this.pidMapper, this.commonSender,
	                    this.protocolIdQueue, this.clientId, Constants.BINARY_PRIME, this.modelProtocolId, this.asymmetricBit, this.pid, partyCount);
				
				// Future<Integer> equality = es.submit(task);
			
				// taskList.add(equality);
			}
		}
		
		Integer[] result = new Integer[featureShares.size()];
		
		int size = taskList.size();
		
		for (int i = 0; i < size; i++) {
			result[(i % featureShares.size())] += taskList.get(i).get() % Constants.BINARY_PRIME;
		}
		
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
	                	case "privateDocumentShares":
	                		privateDocumentShares = new ArrayList<List<Integer>>();
	                		List<List<BigInteger>> pdshares = FileIO.loadMatrixFromFile(value);
	                		for (List<BigInteger> share : pdshares) {
	                			List<Integer> row = new ArrayList<>();
	                			for (BigInteger item : share) {
	                				row.add(item.intValue()); 
	                			}
	                			privateDocumentShares.add(row);
	                		}
	                		break;
	                	case "featureShares":
	                		featureShares = new ArrayList<List<Integer>>();
	                		List<List<BigInteger>> fshares = FileIO.loadMatrixFromFile(value);
	                		for (List<BigInteger> share : fshares) {
	                			List<Integer> row = new ArrayList<>();
	                			for (BigInteger item : share) {
	                				row.add(item.intValue()); 
	                			}
	                			featureShares.add(row);
	                		}
	                		break;
	            }
	    	}
    }

}
