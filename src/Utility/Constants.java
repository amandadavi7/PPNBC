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
    
    // Property names
    private static final String PROPERTY_THREAD_COUNT = "thread.count";
    private static final String PROPERTY_BATCH_SIZE = "batch.size";
    private static final String PROPERTY_DECIMAL_PRECISION = "decimal.precision";
    private static final String PROPERTY_INTEGER_PRECISION = "integer.precision";
    private static final String PROPERTY_NEWTON_RAPHSON_ROUNDS = "newton.raphson.rounds";
    private static final String PROPERTY_PRIME = "prime";
    
    public static final int PRIME;
    public static final int BINARY_PRIME = 2;        // Prime for bit calculation
    
    public static final int THREAD_COUNT;
    public static final int BATCH_SIZE;

    public static final int DECIMAL_PRECISION;
    public static final int INTEGER_PRECISION;

    public static final int NEWTON_RAPHSON_ROUNDS;
    
    // Constants used for real numbers
    
    public static final BigInteger ROUND_OFF_BIT;
    public static final BigInteger F_POW_2;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", 
            "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
        Properties prop = new Properties();
        Properties defaultProperty = new Properties();
        InputStream input = ClassLoader.getSystemClassLoader().getResourceAsStream(DEFAULT_PROPERTIES);

        try {
            defaultProperty.load(input);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        
        try {
            // LOAD custom properties file first
            String propertyPath = System.getProperty(CONFIG_FILE);
 //           System.out.println(propertyPath);
            if (propertyPath != null) {
                final FileInputStream in = new FileInputStream(propertyPath);
                prop.load(in);
            } 
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Property file parse error:" + DEFAULT_PROPERTIES, ex);
        }
        
        if(prop.getProperty(PROPERTY_THREAD_COUNT) != null) {
            THREAD_COUNT = Integer.parseInt(prop.getProperty(PROPERTY_THREAD_COUNT));
        } else {
            THREAD_COUNT = Integer.parseInt(defaultProperty.getProperty(PROPERTY_THREAD_COUNT));
        }
        
        if(prop.getProperty(PROPERTY_BATCH_SIZE) != null) {
            BATCH_SIZE = Integer.parseInt(prop.getProperty(PROPERTY_BATCH_SIZE));
        } else {
            BATCH_SIZE = Integer.parseInt(defaultProperty.getProperty(PROPERTY_BATCH_SIZE));
        }
        
        if(prop.getProperty(PROPERTY_DECIMAL_PRECISION) != null) {
            DECIMAL_PRECISION = Integer.parseInt(prop.getProperty(PROPERTY_DECIMAL_PRECISION));
        } else {
            DECIMAL_PRECISION = Integer.parseInt(defaultProperty.getProperty(PROPERTY_DECIMAL_PRECISION));
        }
        
        if(prop.getProperty(PROPERTY_INTEGER_PRECISION) != null) {
            INTEGER_PRECISION = Integer.parseInt(prop.getProperty(PROPERTY_INTEGER_PRECISION));
        } else {
            INTEGER_PRECISION = Integer.parseInt(defaultProperty.getProperty(PROPERTY_INTEGER_PRECISION));
        }
        
        if(prop.getProperty(PROPERTY_NEWTON_RAPHSON_ROUNDS) != null) {
            NEWTON_RAPHSON_ROUNDS = Integer.parseInt(prop.getProperty(PROPERTY_NEWTON_RAPHSON_ROUNDS));
        } else {
            NEWTON_RAPHSON_ROUNDS = Integer.parseInt(defaultProperty.getProperty(PROPERTY_NEWTON_RAPHSON_ROUNDS));
        }
        
        if(prop.getProperty(PROPERTY_PRIME) != null) {
            PRIME = Integer.parseInt(prop.getProperty(PROPERTY_PRIME));
        } else {
            PRIME = -1;
        }
        ROUND_OFF_BIT = BigInteger.valueOf(2).pow(INTEGER_PRECISION
                + 2 * DECIMAL_PRECISION - 1);
        
        F_POW_2 = BigInteger.valueOf(2).pow(DECIMAL_PRECISION);

        LOGGER.log(Level.INFO, "Properties file parsed: {0}", DEFAULT_PROPERTIES);
        LOGGER.log(Level.INFO, "Thread Count:{0}", THREAD_COUNT);

    }

}
