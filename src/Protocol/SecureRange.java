package Protocol;

import Communication.Message;

import java.math.BigInteger;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class SecureRange extends CompositeProtocol implements Callable<int[]> {
	private int numOfPartition;
	private BigInteger[] dataColumn;

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
	public SecureRange(int protocolId, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper, BlockingQueue<Message> senderQueue, Queue<Integer> protocolIdQueue, int clientId, int asymmetricBit, int partyCount, BigInteger[] dataColumn, int numOfPartition,int threadID) {
		super(protocolId, pidMapper, senderQueue, protocolIdQueue, clientId, asymmetricBit, partyCount,threadID);
		this.dataColumn = dataColumn;
		this.numOfPartition = numOfPartition;
	}

	@Override
	public int[] call() throws Exception {
		return new int[0];
	}
}
