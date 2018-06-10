/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utility;

import java.math.BigInteger;

/**
 * Local computations
 * @author anisha
 */
public class LocalMath {
    /**
     * Local matrix Multiplication
     *
     * @param a input matrix a
     * @param b input matrix b
     * @param prime
     * @return 
     */
    public static BigInteger[][] localMatrixMultiplication(BigInteger[][] a,
            BigInteger[][] b, BigInteger prime) {

        int crows = a.length;
        int ccol = b[0].length;
        BigInteger[][] c = new BigInteger[crows][ccol];
        int m = a[0].length;

        for (int i = 0; i < crows; i++) {
            for (int j = 0; j < ccol; j++) {
                // dot product of ith row of a and jth row of b
                BigInteger sum = BigInteger.ZERO;
                for (int k = 0; k < m; k++) {
                    sum = sum.add(a[i][k].multiply(b[k][j]).mod(prime)).mod(prime);
                }
                //System.out.println("sum:"+sum);
                c[i][j] = localScale(sum, prime);

            }
        }
        return c;
    }
    
    //TODO convert this to matrix scaling
    public static BigInteger localScale(BigInteger value, BigInteger prime) {
        
        BigInteger scaleFactor = BigInteger.valueOf(2).pow(Constants.decimal_precision);
        value = value.divide(scaleFactor).mod(prime);
        return value;
    }
    
    /**
     * Transpose a matrix
     * @param x
     * @return 
     */
    public static BigInteger[][] transposeMatrix(BigInteger[][] x) {
        int rows = x.length;
        int cols = x[0].length;
        BigInteger[][] xT = new BigInteger[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                xT[j][i] = x[i][j];
            }
        }
        return xT;
    }

}
