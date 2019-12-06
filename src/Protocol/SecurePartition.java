package Protocol;

import Communication.Message;
import Protocol.Utility.CompareAndConvertField;
import Utility.Constants;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

public class SecurePartition extends CompositeProtocol implements Callable<int[]> {
	private int numOfPartition;
	private BigInteger[] dataColumn;
	public static final String PARTITION_BY_RANGE = "range";
	public static final String PARTITION_BY_QUANTILE = "quantile";
	private String mode = PARTITION_BY_RANGE;

	/**
	 * Constructor
	 *
	 * @param protocolId
	 * @param pidMapper
	 * @param senderQueue
	 * @param protocolIdQueue
	 * @param clientId
	 * @param asymmetricBit
	 * @param partyCount
	 */
	public SecurePartition(int protocolId, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, BlockingQueue<Message> senderQueue, Queue<Integer> protocolIdQueue, int clientId, int asymmetricBit, int partyCount, BigInteger[] dataColumn, int numOfPartition, String mode, int threadID) {
		super(protocolId, pidMapper, senderQueue, protocolIdQueue, clientId, asymmetricBit, partyCount, threadID);
		this.dataColumn = dataColumn;
		this.numOfPartition = numOfPartition;
		this.mode = mode;
		for (int j = 0; j < dataColumn.length; j++) {


//			attributeValueTransactionVectorInteger.get(i).add(Arrays.asList(
//					CompareAndConvertField.changeBinaryToDecimalField(
//							convertToIntList(null, attributeValueTransactionVector.get(i).get(j)),
//							decimalTiShares, pid, pidMapper, commonSender,
//							protocolIdQueue, asymmetricBit, clientId, datasetSizePrime,
//							partyCount)));
//			pid++;
//
//			attributeValueTransactionVectorBigInteger.get(i).add(Arrays.asList(
//					CompareAndConvertField.changeBinaryToBigIntegerField(
//							attributeValueTransactionVector.get(i).get(j),
//							bigIntTiShares, pid, pidMapper, commonSender,
//							protocolIdQueue, asymmetricBit, clientId, prime,
//							partyCount)));
//			pid++;
		}
	}

	@Override
	public int[] call() throws Exception {
		int[] result = new int[this.dataColumn.length];
		if (this.mode.equals(PARTITION_BY_QUANTILE)) {
			// TODO: 7/30/19 implement Secure partition by quantile
		} else {

			SecureRange secureRange = new SecureRange(super.protocolId, super.pidMapper, super.senderQueue, super.protocolIdQueue,
					super.clientID, super.asymmetricBit, super.partyCount, this.dataColumn, this.numOfPartition,threadID);
			int[] buckets = secureRange.call();
			ExecutorService es =
					Executors.newFixedThreadPool(Constants.THREAD_COUNT);
			List<Future<Integer>> futures = new LinkedList<>();
			for (int i = 0; i < this.dataColumn.length; i++) {
//				futures.add(es.submit(() -> {
//
//					return ;
//				}));
			}
		}
		return result;
	}
}
