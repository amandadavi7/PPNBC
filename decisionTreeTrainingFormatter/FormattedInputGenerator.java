/*
 * To convert raw data to decision tree training model compatible format
 */

import java.io.*;
import java.util.*;
/*
  * This class converts categorical data to binary by performing one hot encoding
  * @author himani
  */

public class FormattedInputGenerator {
    static int numberOfTestCases;
    static int numberOfAttributes;
    static int maxAttrCount = Integer.MIN_VALUE;
    static int classValueCount;

    public static void main(String[] args) {
        if(args.length<2){
            System.out.println("Incorrect number of arguments");
            System.out.println("java FormattedInputGenerator <path to raw data file> <output data file name>");
            System.exit(0);
         }
        File data = new File(args[0]);
        String line = null;
        int[][] dataset = null;
        int[] counts;
        List<Map<String, Integer>> values = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(data));
            String[] attributes = br.readLine().split(",");
            numberOfAttributes = attributes.length;
            for (int i = 0; i < attributes.length; i++) {
                values.add(new HashMap<>());
            }
            counts = new int[attributes.length];
            while ((line = br.readLine()) != null) {
                String[] dataPoints = line.split(",");
                for (int i = 0; i < dataPoints.length; i++) {
                    if (values.get(i).containsKey(dataPoints[i])) {
                        continue;
                    } else {
                        values.get(i).put(dataPoints[i], counts[i]);
                        counts[i]++;
                    }
                }

            }
            writeToIntermediateFile(values,args);
            List<Integer> features = allAttributes(values);
            dataset = createTwoDMatrixFromInputData(values);
            List<String> res = convertToFormattedData(values,dataset);
            writeFormattedOutput(res,args,features);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void writeToIntermediateFile(List<Map<String, Integer>> values, String[] args) {
        File data = new File(args[0]);
        String line = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(data));
            FileWriter fileWriter = new FileWriter("../output");
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.write(br.readLine());
            printWriter.write("\n");
            while ((line = br.readLine()) != null) {
                String[] dataPoints = line.split(",");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < dataPoints.length; i++) {
                    sb.append(values.get(i).get(dataPoints[i]));
                    if (i != dataPoints.length - 1) {
                        sb.append(",");
                    }
                }
                sb.append("\n");
                printWriter.write(sb.toString());
                numberOfTestCases++;
            }
            printWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int[][] createTwoDMatrixFromInputData(List<Map<String, Integer>> values) throws Exception{
        int[][] dataset = new int[numberOfTestCases][numberOfAttributes];
        File input = new File("../output");
        String line = null;
        BufferedReader br = new BufferedReader(new FileReader(input));
        String[] headings = br.readLine().split(",");
        for(int i=0;i<numberOfTestCases;i++){
            line = br.readLine();
            String[] inputs = line.split(",");
            int[] numbers =new int[inputs.length];
            for(int j = 0;j < inputs.length;j++)
            {
                numbers[j] = Integer.parseInt(inputs[j]);
            }
            dataset[i]=numbers;

        }

        return dataset;
    }

    public static List<String>  convertToFormattedData(List<Map<String, Integer>> values, int[][] dataset){
        List<String> res = new ArrayList<>();
        for(int i=0;i<values.size();i++){
            Map<String,Integer> mp = values.get(i);
            List<Integer> list = new ArrayList<Integer>( mp.values() );
            Collections.sort(list);
            for(int num =0; num < maxAttrCount; num++){
                StringBuilder sb = new StringBuilder();
                for(int k=0;k<dataset.length;k++){
                    if(dataset[k][i]==num){
                        sb.append("1");
                        if(k<dataset.length-1) {
                            sb.append(",");
                        }
                    }else{
                        sb.append("0");
                        if(k<dataset.length-1) sb.append(",");
                    }
                }
                res.add(sb.toString());
            }
        }
        return res;

    }


    public static void writeFormattedOutput(List<String> res,String[] args, List<Integer> features) throws Exception{
        FileWriter fileWriter = new FileWriter(args[1]);
        for(Integer num : features){
            fileWriter.write(String.valueOf(num));
            fileWriter.write("\n");
        }
        for(String str: res) {
            fileWriter.write(str);
            fileWriter.write("\n");
        }
        fileWriter.close();
    }

    public static void getAllAttributesCount(List<Map<String, Integer>> values){
         for(int i=0;i<values.size();i++){
             getMax(values.get(i));
             if(i==values.size()-1){
                 classValueCount = values.get(i).size();
             }
         }
    }

    public static void getMax(Map<String, Integer> values){
        if(values.size()>maxAttrCount){
            maxAttrCount=values.size();
        }
    }

    public static List<Integer> allAttributes(List<Map<String, Integer>> values){
        getAllAttributesCount(values);
        List<Integer> features = new ArrayList<>();
        features.add(classValueCount);
        features.add(numberOfAttributes-1);
        features.add(maxAttrCount);
        features.add(numberOfTestCases);
       return features;
    }



}

