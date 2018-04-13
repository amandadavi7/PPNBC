/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

import java.util.*;

/**
 *
 * @author bhagatsanchya
 */
public class DecimalToBinaryTest {
    public static void main(String[] args) {
        int x = 4;
        List<Integer> bits = decimalToBinary(x);
        System.out.println("List is:");
        for(int i = 0; i < bits.size();i++){
            System.out.println("index = " + i + " value: " + bits.get(i) );
        }
       
    }
    public static List<Integer> decimalToBinary(int decimal_val){
         List<Integer> bits = new ArrayList<>();
        while(decimal_val > 0){
            
            bits.add(decimal_val % 2);
            
            decimal_val = decimal_val / 2;
          
        }
       return bits;
    }
    
}
