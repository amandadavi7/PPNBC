package Client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import Utility.Constants;
import Utility.FileIO;
import Utility.Logging;

public class GenerateNGrams {

	static String sourceFile;
    static String destDir;
    static int gramLen;
    
    static String[] grams = new String[0];
    
    public static void main(String[] args) {
        if (args.length < 3) {
            Logging.nGramUsage();
            System.exit(0);
        }
        String[] parsedArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            String[] currInput = args[i].split("=");
            if (currInput.length < 2) {
                Logging.nGramUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];
            parsedArgs[i] = value;
        }
        initalizeVariables(parsedArgs);
        String document = FileIO.readFile(sourceFile, Charset.defaultCharset());
        for (int i=1; i <= gramLen; i++) {
        		grams = combineArrays(ngrams(document, i),grams);
        }
        saveToCSV();
    }
    
    /**
     * initialize input variables from command line
     *
     * @param args command line arguments
     */
    private static void initalizeVariables(String[] args) {
    		sourceFile = args[0];
    		destDir = args[1];
    		gramLen = Integer.parseInt(args[2]);
    }
    
    // takes a string s and desired gram length len and returns the array of grams
    // https://stackoverflow.com/questions/3656762/n-gram-generation-from-a-sentence
    private static String[] ngrams(String s, int len) {
        String[] parts = s.split(" ");
        String[] result = new String[parts.length - len + 1];
        for(int i = 0; i < parts.length - len + 1; i++) {
           StringBuilder sb = new StringBuilder();
           for(int k = 0; k < len; k++) {
               if(k > 0) sb.append(' ');
               sb.append(parts[i+k]);
           }
           result[i] = sb.toString();
        }
        return result;
    }
    
    // combines two string arrays
    // https://stackoverflow.com/questions/80476/how-can-i-concatenate-two-arrays-in-java
    private static String[] combineArrays(String[] first, String[] second) {
        List<String> both = new ArrayList<String>(first.length + second.length);
        Collections.addAll(both, first);
        Collections.addAll(both, second);
        return both.toArray(new String[both.size()]);
    }
    
    // takes the grams and saves them to a csv
    private static void saveToCSV() {
        String baseFileName = destDir + "/ngrams";
        System.out.println(Arrays.toString(grams));
        try (BufferedWriter br = new BufferedWriter(new FileWriter(
                baseFileName + ".csv"))) {
            for (int i = 0; i < grams.length - 1; i++) {
                br.append(grams[i] + ",");
            }
            br.append(grams[grams.length - 1]);
            br.flush();
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(GenerateNGrams.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
