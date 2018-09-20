/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utility;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public static final int prime = 32;
    public static final int binaryPrime = 2;        // Prime for bit calculation

    public static final int bitLength = 5;

    public static final int THREAD_COUNT;
    public static final int BATCH_SIZE;

    public static final int DECIMAL_PRECISION;
    public static final int INTEGER_PRECISION;

    public static final int NEWTON_RAPHSON_ROUNDS;

    static {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            // LOAD custom properties file first
            String propertyPath = System.getProperty(CONFIG_FILE);
            if (propertyPath != null) {
                final FileInputStream in = new FileInputStream(propertyPath);
                prop.load(in);
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

        LOGGER.info("Properties file parsed:" + DEFAULT_PROPERTIES);
        LOGGER.info("New ThreadCount:" + THREAD_COUNT);
    }

}
