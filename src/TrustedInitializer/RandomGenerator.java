/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrustedInitializer;

import Utility.Constants;
import java.math.BigInteger;
import java.security.SecureRandom;


/**
 *
 * @author anisha
 */
public class RandomGenerator {

    /**
     * Generate Decimal Triples
     *
     * @param decTriples
     * @param clientCount
     * @param tiShare
     */
    public static void generateDecimalTriples(int decTriples, int clientCount,
            TIShare[] tiShare) {
        SecureRandom rand = new SecureRandom();
        for (int i = 0; i < decTriples; i++) {
            int U = rand.nextInt(Constants.PRIME);
            int V = rand.nextInt(Constants.PRIME);
            int W = Math.floorMod(U * V, Constants.PRIME);
            int usum = 0, vsum = 0, wsum = 0;
            for (int j = 0; j < clientCount - 1; j++) {
                TripleInteger t = new TripleInteger();
                t.u = rand.nextInt(Constants.PRIME);
                t.v = rand.nextInt(Constants.PRIME);
                t.w = rand.nextInt(Constants.PRIME);
                usum += t.u;
                vsum += t.v;
                wsum += t.w;
                tiShare[j].addDecimal(t);
            }
            TripleInteger t = new TripleInteger();
            t.u = Math.floorMod(U - usum, Constants.PRIME);
            t.v = Math.floorMod(V - vsum, Constants.PRIME);
            t.w = Math.floorMod(W - wsum, Constants.PRIME);
            tiShare[clientCount - 1].addDecimal(t);
        }
    }

    /**
     * Generate Binary Triples
     *
     * @param binTriples
     * @param clientCount
     * @param tiShare
     */
    public static void generateBinaryTriples(int binTriples, int clientCount,
            TIShare[] tiShare) {
        SecureRandom rand = new SecureRandom();
        for (int i = 0; i < binTriples; i++) {
            int U = rand.nextInt(Constants.BINARY_PRIME);
            int V = rand.nextInt(Constants.BINARY_PRIME);
            int W = U * V;
            int usum = 0, vsum = 0, wsum = 0;
            for (int j = 0; j < clientCount - 1; j++) {
                TripleByte t = new TripleByte();
                t.u = (byte) rand.nextInt(Constants.BINARY_PRIME);
                t.v = (byte) rand.nextInt(Constants.BINARY_PRIME);
                t.w = (byte) rand.nextInt(Constants.BINARY_PRIME);
                usum += t.u;
                vsum += t.v;
                wsum += t.w;
                tiShare[j].addBinary(t);
            }
            TripleByte t = new TripleByte();
            t.u = (byte) Math.floorMod(U - usum, Constants.BINARY_PRIME);
            t.v = (byte) Math.floorMod(V - vsum, Constants.BINARY_PRIME);
            t.w = (byte) Math.floorMod(W - wsum, Constants.BINARY_PRIME);
            tiShare[clientCount - 1].addBinary(t);
        }
    }

    /**
     * Generate Big Integer Triples
     *
     * @param realTriples
     * @param clientCount
     * @param tiShare
     */
    public static void generateRealTriples(int realTriples, int clientCount,
            TIShare[] tiShare) {
        SecureRandom rand = new SecureRandom();
        BigInteger Zq = BigInteger.valueOf(2).pow(Constants.INTEGER_PRECISION
                + 2 * Constants.DECIMAL_PRECISION + 1).nextProbablePrime();

        for (int i = 0; i < realTriples; i++) {
            BigInteger U = new BigInteger(Constants.INTEGER_PRECISION, rand).mod(Zq);
            BigInteger V = new BigInteger(Constants.INTEGER_PRECISION, rand).mod(Zq);
            BigInteger W = U.multiply(V).mod(Zq);
            
            BigInteger usum = BigInteger.ZERO, vsum = BigInteger.ZERO, wsum = BigInteger.ZERO;
            for (int j = 0; j < clientCount - 1; j++) {
                TripleBigInteger t = new TripleBigInteger();
                t.u = new BigInteger(Constants.INTEGER_PRECISION, rand);
                t.v = new BigInteger(Constants.INTEGER_PRECISION, rand);
                t.w = new BigInteger(Constants.INTEGER_PRECISION, rand);
                usum = usum.add(t.u);
                vsum = vsum.add(t.v);
                wsum = wsum.add(t.w);
                tiShare[j].addReal(t);
            }
            TripleBigInteger t = new TripleBigInteger();
            t.u = (U.subtract(usum)).mod(Zq);
            t.v = (V.subtract(vsum)).mod(Zq);
            t.w = (W.subtract(wsum)).mod(Zq);
            tiShare[clientCount - 1].addReal(t);
        }

    }



    /**
     * Generate Truncation Pairs
     *
     * @param truncationPairs
     * @param clientCount
     * @param tiShare
     */
    public static void generateTruncationPairs(int truncationPairs,
            int clientCount, TIShare[] tiShare) {
        System.out.println("Generating " + truncationPairs + " truncation shares");
        SecureRandom rand = new SecureRandom();
        BigInteger Zq = BigInteger.valueOf(2).pow(Constants.INTEGER_PRECISION
                + 2 * Constants.DECIMAL_PRECISION + 1).nextProbablePrime();

        int f = Constants.DECIMAL_PRECISION;
        int k = Constants.INTEGER_PRECISION + Constants.DECIMAL_PRECISION;
        //TODO take lambda as a parameter
        int lambda = 0;

        for (int i = 0; i < truncationPairs; i++) {
            // generate a random Rp of f bit length
            BigInteger Rp = new BigInteger(f, rand);

            // generate a random Rp of lambda+k bit length
            BigInteger R2p = new BigInteger(lambda + k, rand);

            BigInteger f2 = BigInteger.valueOf(2).pow(f);

            BigInteger R = R2p.multiply(f2).add(Rp);

            BigInteger rsum = BigInteger.ZERO, rpsum = BigInteger.ZERO;
            for (int j = 0; j < clientCount - 1; j++) {
                TruncationPair t = new TruncationPair();
                t.r = new BigInteger(Zq.bitLength(), rand).mod(Zq);
                t.rp = new BigInteger(Zq.bitLength(), rand).mod(Zq);
                rsum = rsum.add(t.r);
                rpsum = rpsum.add(t.rp);
                tiShare[j].addTruncationPair(t);
            }
            TruncationPair t = new TruncationPair();
            t.r = (R.subtract(rsum)).mod(Zq);
            t.rp = (Rp.subtract(rpsum)).mod(Zq);
            tiShare[clientCount - 1].addTruncationPair(t);
        }

    }
    
    public static void generateEqualityShares(int equalityCount, int clientCount,
            TIShare[] tiShare) {
        SecureRandom rand = new SecureRandom();
        for(int i=0;i<equalityCount;i++) {
            int R = rand.nextInt(Constants.PRIME-1) + 1;
            int rsum = 0;
            for(int j=0;j<clientCount-1;j++){
                int r = rand.nextInt(Constants.PRIME);
                rsum+=r;
                tiShare[j].addEqualityShare(r);
            }
            int r = Math.floorMod(R-rsum, Constants.PRIME);
            tiShare[clientCount-1].addEqualityShare(r);
        }
        
    }
    public static void generateBigIntegerEqualityShares(int equalityCount, int clientCount,
            TIShare[] tiShare) {
        SecureRandom rand = new SecureRandom();
        for(int i=0;i<equalityCount;i++) {
            BigInteger R = (new BigInteger(Constants.BIT_LENGTH, rand)).add(BigInteger.ONE);
            BigInteger rsum = BigInteger.ZERO;
            for(int j=0; j<clientCount-1; j++){
                BigInteger r = new BigInteger(Constants.BIT_LENGTH, rand);
                rsum = rsum.add(r);
                tiShare[j].addBigIntegerEqualityShare(r);
            }
            BigInteger r = (R.subtract(rsum)).mod(Constants.BIG_INT_PRIME);
            tiShare[clientCount-1].addBigIntegerEqualityShare(r);
        }
        
    }


    /**
     * Generate Big Integer Triples
     *
     * @param bigDecTriples
     * @param clientCount
     * @param tiShare
     */
    public static void generateBigIntTriples(int bigIntTriples, int clientCount,
            TIShare[] tiShare) {
        SecureRandom rand = new SecureRandom();
        for (int i = 0; i < bigIntTriples; i++) {
            BigInteger U = new BigInteger(Constants.BIT_LENGTH, rand);
            BigInteger V = new BigInteger(Constants.BIT_LENGTH, rand);
            BigInteger W = (U.multiply(V)).mod(Constants.BIG_INT_PRIME);
            
            BigInteger usum = BigInteger.ZERO, 
                       vsum = BigInteger.ZERO, 
                       wsum = BigInteger.ZERO;

            for (int j = 0; j < clientCount - 1; j++) {
                TripleBigInteger t = new TripleBigInteger();
                t.u = new BigInteger(Constants.BIT_LENGTH, rand);
                t.v = new BigInteger(Constants.BIT_LENGTH, rand);
                t.w = new BigInteger(Constants.BIT_LENGTH, rand);
                usum = usum.add(t.u);
                vsum = vsum.add(t.v);
                wsum = wsum.add(t.w);
                tiShare[j].addBigInt(t);
            }
            TripleBigInteger t = new TripleBigInteger();
            t.u = (U.subtract(usum)).mod(Constants.BIG_INT_PRIME);
            t.v = (V.subtract(vsum)).mod(Constants.BIG_INT_PRIME);
            t.w = (W.subtract(wsum)).mod(Constants.BIG_INT_PRIME);
            tiShare[clientCount - 1].addBigInt(t);
        }
    }

}
