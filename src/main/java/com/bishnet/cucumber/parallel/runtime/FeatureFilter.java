package com.bishnet.cucumber.parallel.runtime;

import cucumber.runtime.model.CucumberFeature;

import java.io.IOException;
import java.util.*;

import static com.bishnet.cucumber.parallel.util.RerunUtils.splitRerunFileIntoPathStringsList;
import static cucumber.runtime.model.PathWithLines.stripLineFilters;

public class FeatureFilter {
    private static final String CLASSPATH = "classpath:";
    private String pathPrefix;
    private RuntimeConfiguration runtimeConfiguration;
    private ClassLoader cucumberClassLoader;
    private List<String> failedLineFilteredFeaturesList;
    private FeatureParser featureParser;

    public FeatureFilter(RuntimeConfiguration runtimeConfiguration, ClassLoader cucumberClassLoader) {
        this.runtimeConfiguration = runtimeConfiguration;
        this.cucumberClassLoader = cucumberClassLoader;
    }

    public List<CucumberFeature> getFilteredFeatures() throws IOException {
        List<CucumberFeature> features = getFilteredByFlakyTagFeatures();
        features = getFilteredByLineFeatures(features);
        return features;
    }

    private List<CucumberFeature> getFilteredByFlakyTagFeatures() throws IOException {
        RuntimeConfigurationCloneBuilder configurationBuilder = new RuntimeConfigurationCloneBuilder(runtimeConfiguration);
        List<String> featureParsingArguments = new ArrayList<>();
        Iterator<String> iterator = runtimeConfiguration.featureParsingArguments.iterator();
        String next;
        while (iterator.hasNext()) {
            next = iterator.next();
            featureParsingArguments.add(next);
            if (next.equals("--tags")) break;
        }
        featureParsingArguments.add(runtimeConfiguration.flakyRerunConfig.flakyTag);
        failedLineFilteredFeaturesList = splitRerunFileIntoPathStringsList(runtimeConfiguration.rerunReportReportPath);
        iterator = failedLineFilteredFeaturesList.iterator();
        String strippedPath;
        pathPrefix = getPathPrefix();
        while (iterator.hasNext()) {
            strippedPath = stripLineFilters(iterator.next());
            featureParsingArguments.add(pathPrefix + strippedPath);
        }
        runtimeConfiguration = configurationBuilder
                .numberOfThreads(1)
                .featureParsingArguments(featureParsingArguments).build();
        return parseFeatures();
    }

    private List<CucumberFeature> getFilteredByLineFeatures(List<CucumberFeature> features) {
        RuntimeConfigurationCloneBuilder configurationBuilder = new RuntimeConfigurationCloneBuilder(runtimeConfiguration);
        List<String> featureParsingArguments = new ArrayList<>();
        Iterator<String> iterator = runtimeConfiguration.featureParsingArguments.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            if (next.equals("--tags")) break;
            featureParsingArguments.add(next);
        }
        Map<String, String> featurePathsLineFilters = new HashMap<>();
        String keyPrefix = getKeyPrefix();
        for (String f : failedLineFilteredFeaturesList) {
            featurePathsLineFilters.put(stripLineFilters(keyPrefix + f), pathPrefix + f);
        }
        for (CucumberFeature feature : features) {
            if (featurePathsLineFilters.containsKey(feature.getPath())) {
                featureParsingArguments.add(featurePathsLineFilters.get(feature.getPath()));
            }
        }
        runtimeConfiguration = configurationBuilder
                .numberOfThreads(1)
                .featureParsingArguments(featureParsingArguments).build();
        return parseFeatures();
    }

    private String getKeyPrefix() {
        pathPrefix = getPathPrefix();
        String keyPrefix = "";
        if (!pathPrefix.startsWith(CLASSPATH)){
            keyPrefix = pathPrefix;
        }
        return keyPrefix;
    }

    private String getPathPrefix() {
        String pathPrefix;
        String mainFeaturePath = runtimeConfiguration.featurePaths.get(0);
        if (mainFeaturePath.startsWith(CLASSPATH)) {
            pathPrefix = CLASSPATH;
        } else {
            pathPrefix = mainFeaturePath + "/";
        }
        return pathPrefix;
    }

    private List<CucumberFeature> parseFeatures() {
        featureParser = new FeatureParser(runtimeConfiguration, cucumberClassLoader);
        return featureParser.parseFeatures();
    }
}
