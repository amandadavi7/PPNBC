/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ThreadManagement;

import Utility.Constants;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author anisha
 */
public class ThreadPoolManager extends ThreadPoolExecutor {
    
    private static final PriorityBlockingQueue<Runnable> THREAD_POOL_QUEUE = 
            new PriorityBlockingQueue<Runnable>(Constants.THREAD_COUNT);
    
    private static final ThreadPoolManager THREAD_POOL_MANAGER = new 
        ThreadPoolManager(Constants.THREAD_COUNT, Constants.THREAD_COUNT, 1000L, 
                TimeUnit.MILLISECONDS, THREAD_POOL_QUEUE);
    
    private ThreadPoolManager(int corePoolSize, int maximumPoolSize,
            long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public static ThreadPoolManager getInstance() {
        return THREAD_POOL_MANAGER;
    }
    
    public <T> Future<T> submit(Runnable task, T result, int priority) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<T> ftask = newTaskFor(task, result, priority);
        execute(ftask);
        return ftask;
    }

    public <T> Future<T> submit(Callable<T> task, int priority) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<T> ftask = newTaskFor(task, priority);
        execute(ftask);
        return ftask;
    }

    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value,
            int priority) {
        return new ComparableFutureTask<>(runnable, value, priority);
    }

    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable,
            int priority) {
        return new ComparableFutureTask<>(callable, priority);
    }
    
    public static void shutDownThreadService() {
        THREAD_POOL_MANAGER.shutdown();
    }
}
