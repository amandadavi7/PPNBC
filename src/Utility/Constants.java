/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utility;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This contains all the global variables and their values.
 *
 * @author anisha
 */
public class Constants {

    private static final Logger LOGGER = Logger.getLogger(Constants.class.getName());
    private static final String CONFIG_FILE = "config.properties";
    private static final String DEFAULT_PROPERTIES = "resources/" + CONFIG_FILE;

    public static final int prime;
    public static final int binaryPrime = 2;        // Prime for bit calculation

    public static final int bitLength = 5;

    public static final int THREAD_COUNT;
    public static final int BATCH_SIZE;

    public static final int DECIMAL_PRECISION;
    public static final int INTEGER_PRECISION;

    public static final int NEWTON_RAPHSON_ROUNDS;
    
    // Constants used for real numbers
    
    public static final BigInteger ROUND_OFF_BIT;
    public static final BigInteger F_POW_2;

    static {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            // LOAD custom properties file first
            String propertyPath = System.getProperty(CONFIG_FILE);
            if (propertyPath != null) {
                final FileInputStream in = new FileInputStream(propertyPath);
                prop.load(in);
                LOGGER.log(Level.INFO, "Properties file parsed:{0}", propertyPath);
            } else {
                input = ClassLoader.getSystemClassLoader().getResourceAsStream(DEFAULT_PROPERTIES);
                prop.load(input);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Property file parse error:" + DEFAULT_PROPERTIES, ex);
        }
        THREAD_COUNT = Integer.parseInt(prop.getProperty("thread.count"));
        BATCH_SIZE = Integer.parseInt(prop.getProperty("batch.size"));
        DECIMAL_PRECISION = Integer.parseInt(prop.getProperty("decimal.precision"));
        INTEGER_PRECISION = Integer.parseInt(prop.getProperty("integer.precision"));
        NEWTON_RAPHSON_ROUNDS = Integer.parseInt(prop.getProperty("newton.raphson.rounds"));
        
        ROUND_OFF_BIT = BigInteger.valueOf(2).pow(INTEGER_PRECISION
                + 2 * DECIMAL_PRECISION - 1);
        
        F_POW_2 = BigInteger.valueOf(2).pow(DECIMAL_PRECISION);
        
        prime = Integer.parseInt(prop.getProperty("prime"));

        LOGGER.info("Properties file parsed:" + DEFAULT_PROPERTIES);
        LOGGER.log(Level.INFO, "Thread Count:{0}", THREAD_COUNT);
    }

}
