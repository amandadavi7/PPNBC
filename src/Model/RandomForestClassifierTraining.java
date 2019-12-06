package Model;

import Communication.Message;
import TrustedInitializer.TIShare;
import TrustedInitializer.TripleBigInteger;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import Utility.Constants;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class RandomForestClassifierTraining extends Model {
	int ensembleRounds;
	List<List<String>> decisionRules;
	BlockingQueue<Message> senderQueue;
	TIShare tiShare;
	String[] args;
	int protocolID;
	ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper;
	List<ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>>> pidMapperList;
	Queue<Integer> protocolIdQueue;
	String outputPath = null;

	private void initializeVariables(String[] args) {
		for (String arg : args) {
			String[] currInput = arg.split("=");
			String command = currInput[0];
			String value = currInput[1];
			switch (command) {
				case "ensembleRounds":
					this.ensembleRounds = Integer.parseInt(value);
					break;
				case "output":
					this.outputPath = value;
					break;
			}
		}
	}

	/**
	 * @param pidMapper
	 * @param senderQueue
	 * @param clientId
	 * @param asymmetricBit
	 * @param partyCount
	 * @param protocolIdQueue
	 * @param protocolID
	 */
	public RandomForestClassifierTraining(int asymmetricBit,
	                                      ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
	                                      BlockingQueue<Message> senderQueue, int clientId, TIShare tiShare, String[] args, int partyCount,
	                                      Queue<Integer> protocolIdQueue, int protocolID,List<ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>>> pidMapperList, int threadID) {
		super(pidMapper, senderQueue, clientId, asymmetricBit, partyCount, protocolIdQueue, protocolID, threadID);

		this.initializeVariables(args);
		this.decisionRules = new LinkedList<>();
		this.tiShare = tiShare;
		this.args = args;
		this.senderQueue = senderQueue;
		this.protocolID = protocolID;
		this.pidMapper = pidMapper;
		this.pidMapperList = pidMapperList;
		this.protocolIdQueue = protocolIdQueue;
	}

	public void trainRandomForestClassifier() {
		ExecutorService es =
				Executors.newFixedThreadPool(Constants.THREAD_COUNT);
		ExecutorCompletionService<List<String>> multCompletionService = new ExecutorCompletionService<>(es);

//		List<Future<List<String>>> futures = new LinkedList<>();

		for (int i = 0; i < this.ensembleRounds; i++) {
			int finalI = i;
			multCompletionService.submit(() -> {
				TIShare share = tiShare.ensembleShares.get(finalI);

				DecisionTreeTraining decisionTreeTraining = new DecisionTreeTraining(
						asymmetricBit, pidMapperList.get(finalI+1), senderQueue, clientId,
						share.binaryShares, share.bigIntShares,
						share.decimalShares,
						share.bigIntEqualityShares, args, partyCount,
						new LinkedList<>(protocolIdQueue), protocolID, share.columnSelections, share.rowSelections,true,finalI+1);
				decisionTreeTraining.trainDecisionTree();
				System.out.println("Tree " + finalI + " finished");
				return decisionTreeTraining.decisionTreeNodes;
			});

		}
		es.shutdown();
		for (int i = 0; i < this.ensembleRounds; i++) {
			try {
				Future<List<String>> prod = multCompletionService.take();
				decisionRules.add(prod.get());

			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		if (asymmetricBit == 1) {
			try {
				FileWriter writer = new FileWriter(this.outputPath + "/rf_result.txt");
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
	}

}
