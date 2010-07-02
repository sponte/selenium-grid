package com.thoughtworks.selenium.grid.configuration;

import junit.framework.Assert;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import org.junit.Test;

import java.io.StringReader;
import java.util.Arrays;

public class GridConfigurationTest {

    @Test
    public void hubConfigurationHasADefaultValue() {
        assertNotNull(new GridConfiguration().getHub());
    }

    @Test
    public void portCanBeSetToANonDefaultValue() {
        final GridConfiguration configuration = new GridConfiguration();
        final HubConfiguration hubConfiguration = new HubConfiguration();
        configuration.setHub(hubConfiguration);
        assertEquals(hubConfiguration, configuration.getHub());
    }

    @Test
    public void toYamlReturnsConfigurationSerializedAsYAML() {
        final EnvironmentConfiguration[] environments;
        final GridConfiguration configuration;
        final String expected;

        configuration = new GridConfiguration();
        configuration.getHub().setPort(3333);
        environments = new EnvironmentConfiguration[]{new EnvironmentConfiguration("*firefox", "*firefox"), new EnvironmentConfiguration("*googlechrome", "*googlchrome")};
        configuration.getHub().setEnvironments(environments);
        expected = "--- \n"
                + "hub: environments: \n"
                + "    - \n"
                + "      browser: \"*firefox\"\n"
                + "      name: \"*firefox\"\n"
                + "  port: 3333\n";
        System.out.print(configuration.toYAML());
        assertEquals(expected.replaceAll("\\s+", " "), configuration.toYAML().replaceAll("\\s+", " "));
    }

    @Test
    public void parseHubPortAsDefinedInYaml() {
        final GridConfiguration configuration;

        configuration = GridConfiguration.parse(new StringReader(
                "hub: \n"
              + "  port: 6666"));
        assertEquals(6666, configuration.getHub().getPort());
    }

    @Test
    public void parseRemoteControlPollingIntervalInSecondsAsDefinedInYaml() {
        final GridConfiguration configuration;

        configuration = GridConfiguration.parse(new StringReader(
                "hub: \n"
              + "  remoteControlPollingIntervalInSeconds: 24"));
        assertEquals(24.0, configuration.getHub().getRemoteControlPollingIntervalInSeconds());
    }

    @Test
    public void parseSessionMaxIdleTimeInSecondsAsDefinedInYaml() {
        final GridConfiguration configuration;

        configuration = GridConfiguration.parse(new StringReader(
                "hub: \n"
              + "  sessionMaxIdleTimeInSeconds: 64"));
        assertEquals(64.0, configuration.getHub().getSessionMaxIdleTimeInSeconds());
    }

    @Test
    public void parseNewSessionMaxWaitTimeInSecondsAsDefinedInYaml() {
        final GridConfiguration configuration;

        configuration = GridConfiguration.parse(new StringReader(
                "hub: \n"
              + "  newSessionMaxWaitTimeInSeconds: 120"));
        assertEquals(120.0, configuration.getHub().getNewSessionMaxWaitTimeInSeconds());
    }

    @Test
    public void parseHubEnvironmentsAsDefinedInYaml() {
        final EnvironmentConfiguration[] environments;
        final GridConfiguration configuration;

        configuration = GridConfiguration.parse(
                "hub: \n"
              + "  environments:\n"
              + "    - name: \"Firefox on Windows\"\n"
              + "      browser: \"*firefox\"\n"
              + "    - name: \"IE 6 on Windows\"\n"
              + "      browser: \"*iehta\"\n");
        environments = configuration.getHub().getEnvironments();

        Assert.assertEquals(2, environments.length);
        Assert.assertTrue(Arrays.asList(environments).contains(new EnvironmentConfiguration("Firefox on Windows", "*firefox")));
        Assert.assertTrue(Arrays.asList(environments).contains(new EnvironmentConfiguration("IE 6 on Windows", "*iehta")));
    }

}
