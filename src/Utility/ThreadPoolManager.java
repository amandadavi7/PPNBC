/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utility;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author anisha
 */
public class ThreadPoolManager {
    private static ExecutorService priorityJobPoolExecutor = 
            Executors.newFixedThreadPool(150);
    
    private ThreadPoolManager() {}
    
    public static ExecutorService getInstance() {
        return priorityJobPoolExecutor;
    }
    
    public static void shutDownThreadService() {
        priorityJobPoolExecutor.shutdown();
    }
}
