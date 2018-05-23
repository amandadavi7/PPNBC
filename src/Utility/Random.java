/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utility;

import TrustedInitializer.TIShare;
import TrustedInitializer.TripleByte;
import TrustedInitializer.TripleInteger;
import TrustedInitializer.TripleReal;
import TrustedInitializer.TruncationPair;
import java.math.BigInteger;

/**
 *
 * @author anisha
 */
public class Random {

    public static void generateDecimalTriples(int decTriples, int clientCount,
            TIShare[] tiShare) {
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < decTriples; i++) {
            int U = rand.nextInt(Constants.prime);
            int V = rand.nextInt(Constants.prime);
            int W = Math.floorMod(U * V, Constants.prime);
            int usum = 0, vsum = 0, wsum = 0;
            for (int j = 0; j < clientCount - 1; j++) {
                TripleInteger t = new TripleInteger();
                t.u = rand.nextInt(Constants.prime);
                t.v = rand.nextInt(Constants.prime);
                t.w = rand.nextInt(Constants.prime);
                usum += t.u;
                vsum += t.v;
                wsum += t.w;
                tiShare[j].addDecimal(t);
            }
            TripleInteger t = new TripleInteger();
            t.u = Math.floorMod(U - usum, Constants.prime);
            t.v = Math.floorMod(V - vsum, Constants.prime);
            t.w = Math.floorMod(W - wsum, Constants.prime);
            tiShare[clientCount - 1].addDecimal(t);
        }
    }

    public static void generateBinaryTriples(int binTriples, int clientCount,
            TIShare[] tiShare) {
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < binTriples; i++) {
            int U = rand.nextInt(Constants.binaryPrime);
            int V = rand.nextInt(Constants.binaryPrime);
            int W = U * V;
            int usum = 0, vsum = 0, wsum = 0;
            for (int j = 0; j < clientCount - 1; j++) {
                TripleByte t = new TripleByte();
                t.u = (byte) rand.nextInt(Constants.binaryPrime);
                t.v = (byte) rand.nextInt(Constants.binaryPrime);
                t.w = (byte) rand.nextInt(Constants.binaryPrime);
                usum += t.u;
                vsum += t.v;
                wsum += t.w;
                tiShare[j].addBinary(t);
            }
            TripleByte t = new TripleByte();
            t.u = (byte) Math.floorMod(U - usum, Constants.binaryPrime);
            t.v = (byte) Math.floorMod(V - vsum, Constants.binaryPrime);
            t.w = (byte) Math.floorMod(W - wsum, Constants.binaryPrime);
            tiShare[clientCount - 1].addBinary(t);
        }
    }

    public static void generateBigIntTriples(int bigIntTriples, int clientCount,
            TIShare[] tiShare) {
        java.util.Random rand = new java.util.Random();
        BigInteger Zq = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1).nextProbablePrime();

        for (int i = 0; i < bigIntTriples; i++) {
            BigInteger U = new BigInteger(Constants.integer_precision, rand);
            BigInteger V = new BigInteger(Constants.integer_precision, rand);
            BigInteger W = U.multiply(V);
            W = W.mod(Zq);

            BigInteger usum = BigInteger.ZERO, vsum = BigInteger.ZERO, wsum = BigInteger.ZERO;
            for (int j = 0; j < clientCount - 1; j++) {
                TripleReal t = new TripleReal();
                t.u = new BigInteger(Constants.integer_precision, rand);
                t.v = new BigInteger(Constants.integer_precision, rand);
                t.w = new BigInteger(Constants.integer_precision, rand);
                usum = usum.add(t.u);
                vsum = vsum.add(t.v);
                wsum = wsum.add(t.w);
                tiShare[j].addBigInt(t);
            }
            TripleReal t = new TripleReal();
            t.u = (U.subtract(usum)).mod(Zq);
            t.v = (V.subtract(vsum)).mod(Zq);
            t.w = (W.subtract(wsum)).mod(Zq);
            tiShare[clientCount - 1].addBigInt(t);
        }

    }

    public static void generateTruncationPairs(int truncationPairs,
            int clientCount, TIShare[] tiShare) {
        java.util.Random rand = new java.util.Random();
        BigInteger Zq = BigInteger.valueOf(2).pow(Constants.integer_precision
                + 2 * Constants.decimal_precision + 1).nextProbablePrime();

        int f = Constants.decimal_precision;
        int k = Constants.integer_precision + Constants.decimal_precision;

        for (int i = 0; i < truncationPairs; i++) {
            BigInteger Rp = new BigInteger(f, rand);
            BigInteger R2p = new BigInteger(k, rand);

            BigInteger f2 = BigInteger.valueOf(2).pow(f);

            BigInteger R = R2p.multiply(f2).add(Rp);

            BigInteger rsum = BigInteger.ZERO, rpsum = BigInteger.ZERO;
            for (int j = 0; j < clientCount - 1; j++) {
                TruncationPair t = new TruncationPair();
                t.r = new BigInteger(Zq.bitLength(), rand).mod(Zq);
                t.rp = new BigInteger(Zq.bitLength(), rand).mod(Zq);
                rsum = rsum.add(t.r);
                rpsum = rpsum.add(t.rp);
                tiShare[j].addBigInt(t);
            }
            TruncationPair t = new TruncationPair();
            t.r = (R.subtract(rsum)).mod(Zq);
            t.rp = (Rp.subtract(rpsum)).mod(Zq);
            tiShare[clientCount - 1].addBigInt(t);
        }

    }
}
