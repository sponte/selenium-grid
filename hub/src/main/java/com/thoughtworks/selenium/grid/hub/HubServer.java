package com.thoughtworks.selenium.grid.hub;

import com.thoughtworks.selenium.grid.configuration.HubConfiguration;
import com.thoughtworks.selenium.grid.hub.management.LifecycleManagerServlet;
import com.thoughtworks.selenium.grid.hub.management.RegistrationServlet;
import com.thoughtworks.selenium.grid.hub.management.UnregistrationServlet;
import com.thoughtworks.selenium.grid.hub.management.console.ConsoleServlet;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * Self contained Selenium Grid Hub. Uses Jetty to as a standalone web application.
 */
public class HubServer {

    public static void main(String[] args) throws Exception {
        final ContextHandlerCollection contexts;
        final HubConfiguration configuration;
        final Server server;
        final Context root;

        configuration = HubRegistry.registry().gridConfiguration().getHub();
        server = new Server(configuration.getPort());

        contexts = new ContextHandlerCollection();
        server.setHandler(contexts);

        root = new Context(contexts, "/", Context.SESSIONS);
        root.addServlet(new ServletHolder(new WebDriverServlet()), "/wd/hub/*");
        root.addServlet(new ServletHolder(new HubServlet()), "/selenium-server/driver/*");
        root.addServlet(new ServletHolder(new ConsoleServlet()), "/console");
        root.addServlet(new ServletHolder(new RegistrationServlet()), "/registration-manager/register");
        root.addServlet(new ServletHolder(new UnregistrationServlet()), "/registration-manager/unregister");
        root.addServlet(new ServletHolder(new LifecycleManagerServlet()), "/lifecycle-manager");
        root.addServlet(new ServletHolder(new HeartbeatServlet()), "/heartbeat");

        startRemoteControlPoller();
        ensureRemoteControlPollerStopOnShutdown();

        server.start();
        server.join();
    }

    protected static void startRemoteControlPoller() {
        final Thread pollerThread;

        pollerThread = new Thread(
                HubRegistry.registry().remoteControlPoller(),
                "RC Poller Heartbeat");
        pollerThread.start();
    }

    protected static void ensureRemoteControlPollerStopOnShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                HubRegistry.registry().remoteControlPoller().stop();
            }
        });
    }

}