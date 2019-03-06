package Client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import Utility.FileIO;
import Utility.Logging;

public class ClearSetIntersection {
	static String destDir;
	static String featureFile;
	static String privateDoc;
	
	static char[] intersection;
	
	public static void main(String[] args) {
        if (args.length < 3) {
            Logging.clearSetIntersection();
            System.exit(0);
        }
        String[] parsedArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            String[] currInput = args[i].split("=");
            if (currInput.length < 2) {
                Logging.clearSetIntersection();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];
            parsedArgs[i] = value;
        }
        initalizeVariables(parsedArgs);
        String featString = FileIO.readFile(featureFile, Charset.defaultCharset());
        String docString = FileIO.readFile(privateDoc, Charset.defaultCharset());
        
        List<String> features = Arrays.asList(featString.split("\\s*,\\s*"));
        List<String> document = Arrays.asList(docString.split("\\s*,\\s*"));
        intersection = new char[features.size()];
        for (int i =0; i < intersection.length; i++) {
        		intersection[i] = document.contains(features.get(i)) ? '1':'0';
        }
        saveToCSV();
    }
	
	/**
     * initialize input variables from command line
     *
     * @param args command line arguments
     */
    private static void initalizeVariables(String[] args) {
    		destDir = args[0];
    		featureFile = args[1];
    		privateDoc = args[2];
    }
    
    // takes the grams and saves them to a csv
    private static void saveToCSV() {
        String baseFileName = destDir + "/intersection";
        try (BufferedWriter br = new BufferedWriter(new FileWriter(
                baseFileName + ".csv"))) {
            for (int i = 0; i < intersection.length - 1; i++) {
                br.append(intersection[i] + ",");
            }
            br.append(intersection[intersection.length - 1]);
            br.flush();
            br.close();
        } catch (IOException ex) {
            Logger.getLogger(GenerateNGrams.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
