package com.sponte.selenium.grid.integration;

import com.thoughtworks.selenium.grid.hub.HubServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: swozniak
 * Date: Nov 28, 2010
 * Time: 11:55:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class HubServerThread extends Thread {

    private static Log logger = LogFactory.getLog(HubServerThread.class);

    public void run() {
        try {
            String[] args = new String[]{"-Dport=4455"};
            HubServer.main(args);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        logger.info("Running hub server");
    }

}
