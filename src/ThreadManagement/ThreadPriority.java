/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ThreadManagement;

/**
 *
 * @author anisha
 */
public enum ThreadPriority {
    
    HIGHEST(0),
    HIGH(1),
    MEDIUM(2),
    LOW(3),
    LOWEST(4);

    int value;

    ThreadPriority(int val) {
        this.value = val;
    }

    public int getValue(){
        return value;
    }
}
