/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BroadcastAgent;

import Utility.Logging;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author anisha
 */
public class BA {
    
    static int baPort, partyCount;
    static List<String[]> partyAddress;
    
    public static void main(String[] args) {
        if (args.length < 3) {
            Logging.baUsage();
            System.exit(0);
        }

        initializeVariables(args);

        
    }

    private static void initializeVariables(String[] args) {
        partyAddress = new ArrayList<>();
        
        for (String arg : args) {
            String[] currInput = arg.split("=");
            if (currInput.length < 2) {
                Logging.baUsage();
                System.exit(0);
            }
            String command = currInput[0];
            String value = currInput[1];
            switch(command) {
                case "port":
                    baPort = Integer.parseInt(value);
                    break;
                case "partyCount":
                    partyCount = Integer.valueOf(value);
                    break;
                case "partyIP":
                    String[] address = value.split(";");
                    for(String curr:address) {
                        String[] currAddress = new String[2];
                        currAddress[0] = curr.split(":")[0];
                        currAddress[1] = curr.split(":")[1];
                    }
            }
        }
    }
}
