package com.bishnet.cucumber.parallel.runtime;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RuntimeConfigurationCloneBuilder {
    private int numberOfThreads;
    private List<String> cucumberPassthroughArguments;
    private List<String> featureParsingArguments;
    private List<String> featurePaths;
    private Path htmlReportPath;
    private Path jsonReportPath;
    private Path threadTimelineReportPath;
    private Path rerunReportReportPath;
    private boolean htmlReportRequired;
    private boolean jsonReportRequired;
    private boolean threadTimelineReportRequired;
    private boolean rerunReportRequired;
    private FlakyRerunConfiguration flakyRerunConfig;
    private boolean dynamicFeatureDistribution;
    private FeatureExecutionTimeReportConfiguration featureExecutionTimeReportconfig;

    public RuntimeConfigurationCloneBuilder(RuntimeConfiguration originalRuntimeConfiguration) {
        this.numberOfThreads = originalRuntimeConfiguration.numberOfThreads;
        this.cucumberPassthroughArguments = originalRuntimeConfiguration.cucumberPassthroughArguments;
        this.featureParsingArguments = originalRuntimeConfiguration.featureParsingArguments;
        this.featurePaths = originalRuntimeConfiguration.featurePaths;
        this.htmlReportPath = originalRuntimeConfiguration.htmlReportPath;
        this.htmlReportRequired = originalRuntimeConfiguration.htmlReportRequired;
        this.jsonReportPath = originalRuntimeConfiguration.jsonReportPath;
        this.jsonReportRequired = originalRuntimeConfiguration.jsonReportRequired;
        this.threadTimelineReportPath = originalRuntimeConfiguration.threadTimelineReportPath;
        this.threadTimelineReportRequired = originalRuntimeConfiguration.threadTimelineReportRequired;
        this.rerunReportReportPath = originalRuntimeConfiguration.rerunReportReportPath;
        this.rerunReportRequired = originalRuntimeConfiguration.rerunReportRequired;
        this.flakyRerunConfig = originalRuntimeConfiguration.flakyRerunConfig;
        this.dynamicFeatureDistribution = originalRuntimeConfiguration.dynamicFeatureDistribution;
        this.featureExecutionTimeReportconfig = originalRuntimeConfiguration.featureExecutionTimeReportconfig;
    }

    public RuntimeConfigurationCloneBuilder numberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        return this;
    }

    public RuntimeConfigurationCloneBuilder cucumberPassthroughArguments(List<String> cucumberPassthroughArguments) {
        this.cucumberPassthroughArguments = new ArrayList<>(cucumberPassthroughArguments);
        return this;
    }

    public RuntimeConfigurationCloneBuilder featureParsingArguments(List<String> featureParsingArguments) {
        this.featureParsingArguments = new ArrayList<>(featureParsingArguments);
        return this;
    }

    public RuntimeConfigurationCloneBuilder featurePaths(List<String> featurePaths) {
        this.featurePaths = new ArrayList<>(featurePaths);
        return this;
    }

    public RuntimeConfigurationCloneBuilder htmlReportPath(Path htmlReportPath) {
        this.htmlReportPath = htmlReportPath;
        return this;
    }

    public RuntimeConfigurationCloneBuilder jsonReportPath(Path jsonReportPath) {
        this.jsonReportPath = jsonReportPath;
        return this;
    }

    public RuntimeConfigurationCloneBuilder threadTimelineReportPath(Path threadTimelineReportPath) {
        this.threadTimelineReportPath = threadTimelineReportPath;
        return this;
    }

    public RuntimeConfigurationCloneBuilder rerunReportReportPath(Path rerunReportReportPath) {
        this.rerunReportReportPath = rerunReportReportPath;
        return this;
    }

    public RuntimeConfigurationCloneBuilder htmlReportRequired(boolean htmlReportRequired) {
        this.htmlReportRequired = htmlReportRequired;
        return this;
    }

    public RuntimeConfigurationCloneBuilder jsonReportRequired(boolean jsonReportRequired) {
        this.jsonReportRequired = jsonReportRequired;
        return this;
    }

    public RuntimeConfigurationCloneBuilder threadTimelineReportRequired(boolean threadTimelineReportRequired) {
        this.threadTimelineReportRequired = threadTimelineReportRequired;
        return this;
    }

    public RuntimeConfigurationCloneBuilder rerunReportRequired(boolean rerunReportRequired) {
        this.rerunReportRequired = rerunReportRequired;
        return this;
    }

    public RuntimeConfigurationCloneBuilder flakyRerunConfig(FlakyRerunConfiguration flakyRerunConfig) {
        this.flakyRerunConfig = flakyRerunConfig;
        return this;
    }

    public RuntimeConfigurationCloneBuilder dynamicFeatureDistribution(boolean dynamicFeatureDistribution) {
        this.dynamicFeatureDistribution = dynamicFeatureDistribution;
        return this;
    }

    public RuntimeConfigurationCloneBuilder featureExecutionTimeReportconfig(FeatureExecutionTimeReportConfiguration featureExecutionTimeReportconfig) {
        this.featureExecutionTimeReportconfig = featureExecutionTimeReportconfig;
        return this;
    }

    public RuntimeConfiguration build() {
        return new RuntimeConfiguration(numberOfThreads, Collections.unmodifiableList(cucumberPassthroughArguments),
                Collections.unmodifiableList(featureParsingArguments), Collections.unmodifiableList(featurePaths),
                htmlReportPath, htmlReportRequired, jsonReportPath, jsonReportRequired, threadTimelineReportPath,
                threadTimelineReportRequired, rerunReportReportPath, rerunReportRequired, flakyRerunConfig,
                dynamicFeatureDistribution, featureExecutionTimeReportconfig);
    }
}
