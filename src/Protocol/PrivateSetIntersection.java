package Protocol;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import Communication.Message;
import TrustedInitializer.TripleByte;
import Utility.Constants;

public class PrivateSetIntersection extends Protocol {

	private List<TripleByte> tiShares;
	List<List<Integer>> xShares, yShares;

	public PrivateSetIntersection(List<List<Integer>> x, List<List<Integer>> y, ConcurrentHashMap<Queue<Integer>, BlockingQueue<Message>> pidMapper,
	BlockingQueue<Message> senderQueue, int clientId, int asymmetricBit, int partyCount, Queue<Integer> protocolIdQueue, int protocolID, List<TripleByte> tiShares) throws InterruptedException, ExecutionException {
		
                super(protocolID, pidMapper, senderQueue, protocolIdQueue, clientId, asymmetricBit, partyCount);            
		this.tiShares = tiShares;
                this.xShares = x;
                this.yShares = y;
	}
   
    /**
     * @return 
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
     public List<Integer> call() throws InterruptedException, ExecutionException {
                int pid=0;

		ExecutorService es = Executors.newFixedThreadPool(Constants.THREAD_COUNT);
		List<Future<Integer>> taskList = new ArrayList<>();
		for (List<Integer> share : xShares) {
			for (int i = 0; i < yShares.size(); i++) {
				EqualityByte task = new EqualityByte(yShares.get(i), share, this.tiShares, this.pidMapper, this.senderQueue, 
                                        new LinkedList<>(this.protocolIdQueue), this.clientID, Constants.BINARY_PRIME, pid++, this.asymmetricBit, this.partyCount);
				Future<Integer> equality = es.submit(task);			
				taskList.add(equality);
			}
		}
                                      
		Integer[] result = new Integer[yShares.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = 0;
		
		es.shutdown();
                
		int size = taskList.size();
                                
                List<Integer> xyPsi = new ArrayList<>();      
                
		for (int i = 0; i < size; i++) {
			Future<Integer> resp = taskList.get(i);
			Integer res = resp.get();
			result[(i % yShares.size())] = result[(i % yShares.size())] + res;
		}
		
		for (int i = 0; i < yShares.size(); i++)   
                        xyPsi.add(result[i]%2);
                
            return xyPsi;
    }
}