/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ThreadManagement;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 *
 * @author anisha
 */
public class ComparableFutureTask<V> extends FutureTask<V> implements Runnable,
        Comparable<ComparableFutureTask<V>> {

    private int priority;

    public ComparableFutureTask(Callable<V> callable, int priority) {
        super(callable);
        this.priority = priority;
    }

    public ComparableFutureTask(Runnable runnable, V result, int priority) {
        super(runnable, result);
        this.priority = priority;
    }

    @Override
    public int compareTo(ComparableFutureTask<V> o) {
        return this.priority - o.priority;
    }

}
