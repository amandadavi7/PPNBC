/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utility;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.logging.Level;

/**
 * Local computations
 * @author anisha
 */
public class LocalMath {
    
    /**
     * convert a decimal value to over Zq
     * @param x value in reals
     * @param f bit resolution of decimal component
     * @param q
     * @return BigInteger of x in Z_q
     */
    public static BigInteger realToZq(double x, int f, BigInteger q) {
        // Our integer space must be at least 2^k
        BigDecimal X = BigDecimal.valueOf(x);
        BigDecimal fac = BigDecimal.valueOf(2).pow(f);
        X = X.multiply(fac);

        return X.toBigInteger().mod(q);
    }

    /**
     * convert a Zq value to decimal
     * @param x value in reals
     * @param f bit resolution of decimal component
     * @param q prime field
     * @return BigDecimal of x 
     */
    public static BigDecimal ZqToReal(BigInteger x, int f, BigInteger q) {
        BigInteger partition = q.subtract(BigInteger.ONE)
                .divide(BigInteger.valueOf(2));
        BigInteger inverse = BigInteger.valueOf(2).pow(f);
        BigDecimal Zk;
        
        // If x is more than half of the field size, it is negative.
        if (x.compareTo(partition) > 0) {
            Zk = new BigDecimal(x.subtract(q));
        } else {
            Zk = new BigDecimal(x);
        }

        BigDecimal Q = Zk.divide(new BigDecimal(inverse), f, 
                RoundingMode.CEILING);
        return Q;
    }
    
    
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
    
    /**
     * Scale down the value from 2*f to f over Zq
     * TODO convert this to matrix scaling
     * @param value
     * @param prime
     * @return 
     */
    public static BigInteger localScale(BigInteger value, BigInteger prime) {
        
        BigInteger scaleFactor = BigInteger.valueOf(2).pow(Constants.DECIMAL_PRECISION);
        
        BigDecimal realValue = ZqToReal(value, Constants.DECIMAL_PRECISION, prime);
        
        realValue = realValue.divide(BigDecimal.valueOf(2).pow(Constants.DECIMAL_PRECISION));
        value = realToZq(realValue.doubleValue(), Constants.DECIMAL_PRECISION, prime);
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

    /**
     * Compute RMSE for the predicted shares
     * @param predictedYList
     * @param actualYList
     * @return 
     */
    public static double computeRMSE(List<Double> predictedYList,
            List<Double> actualYList) {

        double error_sum = 0.0;

        int totalPredictions = predictedYList.size();
        for (int i = 0; i < totalPredictions; i++) {
            double err = predictedYList.get(i) - actualYList.get(i);
            error_sum+= Math.pow(err, 2);
        }

        return error_sum/totalPredictions;
    }
}
