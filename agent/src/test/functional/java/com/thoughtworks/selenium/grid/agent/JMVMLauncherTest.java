package com.thoughtworks.selenium.grid.agent;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JMVMLauncherTest {
    public static final int SECOND = 1000;

    @Test
    public void canLaunchAnAgentAndStopIt() throws IOException, InterruptedException {
        final Classpath classpath;
        final JVMLauncher launcher;
        final JVMHandle handle;

        classpath = new Classpath();
        classpath.add("d:\\SeleniumGrid_Sponte\\selenium-grid\\agent\\target\\dist\\lib\\selenium-grid-agent-standalone-1.0.2.jar");
        launcher = new JVMLauncher(classpath, "com.thoughtworks.selenium.grid.agent.AgentServer");
        handle = launcher.launchNewJVM();
        assertTrue(handle.alive());
        Thread.sleep(4 * SECOND);
        handle.kill();
        Thread.sleep(1 * SECOND);
        assertFalse(handle.alive());
    }


    @Test
    public void canCaptureProcessOutut() throws IOException, InterruptedException {
        final Classpath classpath;
        final JVMLauncher launcher;
        final JVMHandle handle;

        classpath = new Classpath();
        classpath.add(String.format("/Users/swozniak/Documents/git-projects/selenium-grid/vendor/selenium-server-standalone-%s.jar", "2.0b1"));
        classpath.add(String.format("/Users/swozniak/Documents/git-projects/selenium-grid/remote-control/target/dist/lib/selenium-grid-remote-control-standalone-%s.jar", "1.1.0.2-SPONTE-SNAPSHOT"));

        launcher = new JVMLauncher(classpath, "com.thoughtworks.selenium.grid.remotecontrol.SelfRegisteringRemoteControlLauncher");
        handle = launcher.launchNewJVM();
        assertTrue(handle.alive());
        Thread.sleep(1 * SECOND);
        handle.waitForProg(System.out);
    }


}
