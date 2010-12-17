package com.sponte.selenium.grid.integration;

import com.thoughtworks.selenium.grid.remotecontrol.SelfRegisteringRemoteControlLauncher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: swozniak
 * Date: Nov 28, 2010
 * Time: 11:59:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteClientThread extends Thread {

    private static Log logger = LogFactory.getLog(RemoteClientThread.class);

    public void run() {
        try {
            String[] args = new String[]{"-port", "4433", "-hubPollerIntervalInSeconds", "180", "-env", "firefox on any"};
            SelfRegisteringRemoteControlLauncher.main(args);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        logger.info("Starting remote client test");
    }

}
