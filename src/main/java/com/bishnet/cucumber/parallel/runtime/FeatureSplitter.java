package com.bishnet.cucumber.parallel.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bishnet.cucumber.parallel.report.thread.FeatureExecutionTimeData;
import com.bishnet.cucumber.parallel.report.thread.FeatureExecutionTimeReporter;
import com.bishnet.cucumber.parallel.util.ListUtils;

import cucumber.runtime.formatter.PluginFactory;
import cucumber.runtime.model.CucumberFeature;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;

public class FeatureSplitter {

	private RuntimeConfiguration runtimeConfiguration;
	private List<CucumberFeature> features;

	public FeatureSplitter(RuntimeConfiguration runtimeConfiguration, List<CucumberFeature> features) {
		this.runtimeConfiguration = runtimeConfiguration;
		this.features = features;
	}

	public List<Path> splitFeaturesIntoRerunFiles() throws IOException {
		List<Path> rerunPaths = new ArrayList<Path>();
		List<CucumberFeature> filteredFeatures = filterEmptyFeatures(features);
		if (runtimeConfiguration.featureExecutionTimeReportconfig.reportRequired)
			filteredFeatures = sortFeaturesByExecutionTime(filteredFeatures);
		List<List<CucumberFeature>> partitionedFeatures = ListUtils.partition(filteredFeatures, getNumberOfSegments(filteredFeatures));
		for (List<CucumberFeature> threadFeatures : partitionedFeatures) {
			rerunPaths.add(createSingleRerunFile(threadFeatures));
		}
		return rerunPaths;
	}

	private List<CucumberFeature> sortFeaturesByExecutionTime(List<CucumberFeature> filteredFeatures) {
		Optional<List<FeatureExecutionTimeData>> optionalFeaturesExecutionTime = getFeatureExecutionTimeReporter()
				.getReport(runtimeConfiguration.featureExecutionTimeReportconfig.reportFileNamePrefix);
		return optionalFeaturesExecutionTime.map(
				featuresExecutionTime -> getFeatureSorter().sortFeaturesByExecutionTime(filteredFeatures, featuresExecutionTime))
				.orElse(filteredFeatures);
	}

	protected ExecutionTimeFeatureSorter getFeatureSorter() {
		return new ExecutionTimeFeatureSorter();
	}

	protected FeatureExecutionTimeReporter getFeatureExecutionTimeReporter() {
		return new FeatureExecutionTimeReporter();
	}

	private Path createSingleRerunFile(List<CucumberFeature> rerunFeatures) throws IOException {
		Path rerunPath = Files.createTempFile("parallelCukes", ".rerun");
		rerunPath.toFile().deleteOnExit();
		PluginFactory pluginFactory = new PluginFactory();
		Object rerunFormatter = pluginFactory.create("rerun:" + rerunPath);
		RerunFileBuilder rerunFileBuilder = new RerunFileBuilder((Formatter) rerunFormatter, (Reporter) rerunFormatter);
		for (CucumberFeature feature : rerunFeatures)
			rerunFileBuilder.addFeature(feature);
		rerunFileBuilder.close();
		return rerunPath;
	}

	protected List<CucumberFeature> filterEmptyFeatures(List<CucumberFeature> featureList) {
		List<CucumberFeature> emptyFeatures = new ArrayList<>();
		List<CucumberFeature> cleanFeatureList = new ArrayList<>(featureList);
		for (CucumberFeature feature : featureList) {
			if (feature.getFeatureElements().isEmpty()) {
				emptyFeatures.add(feature);
			}
		}
		cleanFeatureList.removeAll(emptyFeatures);
		return cleanFeatureList;
	}

	private int getNumberOfSegments(List<CucumberFeature> features) {
		if (runtimeConfiguration.dynamicFeatureDistribution) {
			return features.size();
		}
		return runtimeConfiguration.numberOfThreads;
	}
}
