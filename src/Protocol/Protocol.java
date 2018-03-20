/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Protocol;

/**
 *
 * @author anisha
 */
public class Protocol {
    /**
     * Teardown all local threads
     */
    private void tearDownHandlers() {
        recvqueueHandler.shutdownNow();
        sendqueueHandler.shutdownNow();
    }
}
