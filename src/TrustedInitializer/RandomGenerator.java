/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TrustedInitializer;

import Utility.Constants;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
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
            TIShare[] tiShare, int ensembleIndex) {
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
                if (ensembleIndex == -1) {
                    tiShare[j].addDecimal(t);
                } else {
                    tiShare[j].ensembleShares.get(ensembleIndex).addDecimal(t);
                }
            }
            TripleInteger t = new TripleInteger();
            t.u = Math.floorMod(U - usum, Constants.PRIME);
            t.v = Math.floorMod(V - vsum, Constants.PRIME);
            t.w = Math.floorMod(W - wsum, Constants.PRIME);
            if (ensembleIndex == -1) {
                tiShare[clientCount - 1].addDecimal(t);
            } else {
                tiShare[clientCount - 1].ensembleShares.get(ensembleIndex).addDecimal(t);
            }
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
            TIShare[] tiShare, int ensembleIndex) {
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
                if (ensembleIndex == -1) {
                    tiShare[j].addBinary(t);
                } else {
                    tiShare[j].ensembleShares.get(ensembleIndex).addBinary(t);
                }
            }
            TripleByte t = new TripleByte();
            t.u = (byte) Math.floorMod(U - usum, Constants.BINARY_PRIME);
            t.v = (byte) Math.floorMod(V - vsum, Constants.BINARY_PRIME);
            t.w = (byte) Math.floorMod(W - wsum, Constants.BINARY_PRIME);
            if (ensembleIndex == -1) {
                tiShare[clientCount - 1].addBinary(t);
            } else {
                tiShare[clientCount - 1].ensembleShares.get(ensembleIndex).addBinary(t);
            }
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
            TIShare[] tiShare, int ensembleIndex) {
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
                if (ensembleIndex == -1) {
                    tiShare[j].addReal(t);
                } else {
                    tiShare[j].ensembleShares.get(ensembleIndex).addReal(t);
                }
            }
            TripleBigInteger t = new TripleBigInteger();
            t.u = (U.subtract(usum)).mod(Zq);
            t.v = (V.subtract(vsum)).mod(Zq);
            t.w = (W.subtract(wsum)).mod(Zq);
            if (ensembleIndex == -1) {
                tiShare[clientCount - 1].addReal(t);
            } else {
                tiShare[clientCount - 1].ensembleShares.get(ensembleIndex).addReal(t);
            }
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
            int clientCount, TIShare[] tiShare, int ensembleIndex) {
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
                if (ensembleIndex == -1) {
                    tiShare[j].addTruncationPair(t);
                } else {
                    tiShare[j].ensembleShares.get(ensembleIndex).addTruncationPair(t);
                }
            }
            TruncationPair t = new TruncationPair();
            t.r = (R.subtract(rsum)).mod(Zq);
            t.rp = (Rp.subtract(rpsum)).mod(Zq);
            if (ensembleIndex == -1) {
                tiShare[clientCount - 1].addTruncationPair(t);
            } else {
                tiShare[clientCount - 1].ensembleShares.get(ensembleIndex).addTruncationPair(t);
            }
        }

    }

    public static void generateEqualityShares(int equalityCount, int clientCount,
            TIShare[] tiShare, int ensembleIndex) {
        SecureRandom rand = new SecureRandom();
        for (int i = 0; i < equalityCount; i++) {
            int R = rand.nextInt(Constants.PRIME - 1) + 1;
            int rsum = 0;
            for (int j = 0; j < clientCount - 1; j++) {
                int r = rand.nextInt(Constants.PRIME);
                rsum += r;
                if (ensembleIndex == -1) {
                    tiShare[j].addEqualityShare(r);
                } else {
                    tiShare[j].ensembleShares.get(ensembleIndex).addEqualityShare(r);
                }
            }
            int r = Math.floorMod(R - rsum, Constants.PRIME);
            if (ensembleIndex == -1) {
                tiShare[clientCount - 1].addEqualityShare(r);
            } else {
                tiShare[clientCount - 1].ensembleShares.get(ensembleIndex).addEqualityShare(r);
            }
        }

    }

    public static void generateBigIntegerEqualityShares(int equalityCount, int clientCount,
            TIShare[] tiShare, int ensembleIndex) {
        SecureRandom rand = new SecureRandom();
        for (int i = 0; i < equalityCount; i++) {
            BigInteger R = (new BigInteger(Constants.BIT_LENGTH, rand)).add(BigInteger.ONE);
            BigInteger rsum = BigInteger.ZERO;
            for (int j = 0; j < clientCount - 1; j++) {
                BigInteger r = new BigInteger(Constants.BIT_LENGTH, rand);
                rsum = rsum.add(r);
                if (ensembleIndex == -1) {
                    tiShare[j].addBigIntegerEqualityShare(r);
                } else {
                    tiShare[j].ensembleShares.get(ensembleIndex).addBigIntegerEqualityShare(r);
                }
            }
            BigInteger r = (R.subtract(rsum)).mod(Constants.BIG_INT_PRIME);
            if (ensembleIndex == -1) {
                tiShare[clientCount - 1].addBigIntegerEqualityShare(r);
            } else {
                tiShare[clientCount - 1].ensembleShares.get(ensembleIndex).addBigIntegerEqualityShare(r);
            }
        }

    }

    /**
     * Generate Big Integer Triples
     *
     * @param clientCount
     * @param tiShare
     */
    public static void generateBigIntTriples(int bigIntTriples, int clientCount,
            TIShare[] tiShare, int ensembleIndex) {
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

                if (ensembleIndex == -1) {
                    tiShare[j].addBigInt(t);
                } else {
                    tiShare[j].ensembleShares.get(ensembleIndex).addBigInt(t);
                }
            }
            TripleBigInteger t = new TripleBigInteger();
            t.u = (U.subtract(usum)).mod(Constants.BIG_INT_PRIME);
            t.v = (V.subtract(vsum)).mod(Constants.BIG_INT_PRIME);
            t.w = (W.subtract(wsum)).mod(Constants.BIG_INT_PRIME);

            if (ensembleIndex == -1) {
                tiShare[clientCount - 1].addBigInt(t);
            } else {
                tiShare[clientCount - 1].ensembleShares.get(ensembleIndex).addBigInt(t);
            }
        }
    }

    public static void generateRowFeatureSampling(int max, int amountPerRound, boolean isRow, int clientCount, TIShare[] tiShares, int ensembleIndex) {
        SecureRandom rand = new SecureRandom();
        int count = 0;
        List<Integer> result = new LinkedList<>();
        for (int j = 0; j < max; j++) {
            result.add(0);
        }
        while (count < amountPerRound) {
            int item = rand.nextInt(max);
            if (isRow || result.get(item) == 0) {
                result.set(item, result.get(item) + 1);
                count++;
            }
        }
        for (int k = 0; k < clientCount; k++) {
            if (isRow) {
                if (ensembleIndex == -1) {
                    tiShares[k].setRowSelections(result);
                } else {
                    tiShares[k].ensembleShares.get(ensembleIndex).setRowSelections(result);
                }
            } else {
                if (ensembleIndex == -1) {
                    tiShares[k].setColumnSelections(result);
                } else {
                    tiShares[k].ensembleShares.get(ensembleIndex).setColumnSelections(result);
                }
            }
        }

    }

    public static void generateRowShares(int rowCount, int colCount, int treeCount, int clientCount, TIShare[] tiShare) {
        System.out.println("Generating " + rowCount + " row shares");
        java.util.Random rand = new java.util.Random();
        for (int treeNum = 0; treeNum < treeCount; treeNum++) {
            for (int i = 0; i < colCount; i++) {
                int r = rand.nextInt(rowCount);
                for (int j = 0; j < rowCount; j++) {
                    int valueToBeShared = (j == r) ? 1 : 0;
                    int sum = 0;
                    for (int k = 0; k < clientCount - 1; k++) {
                        int currValue = rand.nextInt(Constants.PRIME);
                        sum = Math.floorMod(sum + currValue, Constants.PRIME);
                        tiShare[k].addRowShare(i, currValue, treeNum);
                    }
                    int currValue = Math.floorMod(valueToBeShared - sum, Constants.PRIME);
                    tiShare[clientCount - 1].addRowShare(i, currValue, treeNum);
                }
            }
        }
    }

    public static void generateColShares(int featureCount, int treeCount, int colCount, int clientCount, TIShare[] tiShare) {
        System.out.println("Generating" + treeCount + " col shares");
        ArrayList<Integer> helperList, finalList;
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < treeCount; i++) {
            helperList = new ArrayList<>();
            finalList = new ArrayList<>(Collections.nCopies(colCount, 0));
            for (int k = 0; k < colCount; k++) {
                helperList.add(k);
            }
            int boundary = colCount;
            for (int l = 0; l < featureCount; l++) {
                int r = rand.nextInt(boundary);
                Collections.swap(helperList, r, boundary - 1);
                boundary--;
            }
            for (int l = boundary; l < colCount; l++) {
                finalList.set(helperList.get(l), 1);
            }
            for (int j = 0; j < colCount; j++) {
                for (int k = 0; k < clientCount; k++) {
                    tiShare[k].addColShare(i, finalList.get(j));
                }
            }
        }
    }

    static void generateWholeNumShares(int classValueCount, int clientCount, TIShare[] tiShare) {
        System.out.println("Generating whole num shares");
        java.util.Random rand = new java.util.Random();
        for (int classValue = 0; classValue < classValueCount; classValue++) {
            int valueToBeShared = classValue;
            int sum = 0;
            for (int k = 0; k < clientCount - 1; k++) {
                int currValue = rand.nextInt(Constants.PRIME);
                sum = Math.floorMod(sum + currValue, Constants.PRIME);
                tiShare[k].addWholeNumShare(currValue);
            }
            int currValue = Math.floorMod(valueToBeShared - sum, Constants.PRIME);
            tiShare[clientCount - 1].addWholeNumShare(currValue);
        }
    }

}
