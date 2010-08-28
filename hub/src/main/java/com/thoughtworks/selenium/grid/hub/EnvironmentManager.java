package com.thoughtworks.selenium.grid.hub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Keep track of the environments offered by the Selenium farm.
 */
public class EnvironmentManager {

    private final ConcurrentMap<String, Environment> environmentMap = new ConcurrentHashMap<String, Environment>();

    public List<Environment> environments() {
        return new ArrayList<Environment>(environmentMap.values());
    }

    public void addEnvironment(Environment newEnvironment) {
        environmentMap.put(newEnvironment.name(), newEnvironment);
    }

    public Environment environment(String environmentName) {
        return environmentMap.get(environmentName);
    }

}
