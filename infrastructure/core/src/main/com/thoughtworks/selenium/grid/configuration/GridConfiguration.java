package com.thoughtworks.selenium.grid.configuration;

import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.Reader;
import java.io.StringReader;

/**
 * Selenium Grid Configuration.
 */
public class GridConfiguration {

    private HubConfiguration hub;

    public GridConfiguration() {
        this.hub = new HubConfiguration();
    }

    public HubConfiguration getHub() {
        return hub;
    }

    public void setHub(HubConfiguration hub) {
        this.hub = hub;
    }

    public static GridConfiguration parse(String yamlDefinition) {
        return parse(new StringReader(yamlDefinition));
    }

    public String toYAML() {
        final Representer representer = new Representer();
        representer.addClassTag(GridConfiguration.class, Tag.MAP);
        representer.addClassTag(HubConfiguration.class, Tag.MAP);
        representer.addClassTag(EnvironmentConfiguration.class, Tag.MAP);

        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setExplicitStart(true);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);

        return new JavaBeanDumper(representer, dumperOptions).dump(this);
    }

    protected static GridConfiguration parse(Reader yamlDefinition) {
        final JavaBeanLoader loader = new JavaBeanLoader(GridConfiguration.class);

        return (GridConfiguration) loader.load(yamlDefinition);
    }

}
