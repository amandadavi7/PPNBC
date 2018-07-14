/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utility;

/**
 * This contains all the global variables and their values.
 * @author anisha
 */
public class Constants {


    public static final int prime = 32;
    public static final int binaryPrime = 2;        // Prime for bit calculation

    public static final int bitLength = 5;
    
    public static final int threadCount = 10; 
    public static final int batchSize = 10;
    
    public static final int decimal_precision = 64;
    public static final int integer_precision = 64;
    
    public static final int comparisonTICount = (2*Constants.bitLength) + ((Constants.bitLength*(Constants.bitLength-1))/2);
    public static final int bitDTiCount = Constants.bitLength*3 - 2;

}
