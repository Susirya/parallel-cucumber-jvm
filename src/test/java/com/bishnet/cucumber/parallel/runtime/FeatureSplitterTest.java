package com.bishnet.cucumber.parallel.runtime;

import com.bishnet.cucumber.parallel.report.thread.FeatureExecutionTimeData;
import com.bishnet.cucumber.parallel.report.thread.FeatureExecutionTimeReporter;
import cucumber.runtime.model.CucumberFeature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.Silent.class)
public class FeatureSplitterTest {

	private static final String FILE_NAME_PREFIX = "prefix";

	@Mock
	private FeatureExecutionTimeReporter executionTimeReporter;

	@Mock
	private ExecutionTimeFeatureSorter executionTimeFeatureSorter;

	@Mock
	private CucumberFeature cucumberFeatureFirst;
	@Mock
	private CucumberFeature cucumberFeatureSecond;

	@Test
	public void whenPassedARequestForOneThreadShouldReturnOneRerunFile() throws IOException {
		List<String> arguments = new ArrayList<String>();
		arguments.add("classpath:com/bishnet/cucumber/parallel/runtime/samplefeatures/directory");
		RuntimeConfiguration runtimeConfiguration = getRuntimeConfiguration(arguments, 1);
		FeatureParser featureParser = new FeatureParser(runtimeConfiguration, Thread.currentThread().getContextClassLoader());
		FeatureSplitter featureSplitter = getFeatureSplitter(runtimeConfiguration, featureParser.parseFeatures());
		List<Path> rerunFiles = featureSplitter.splitFeaturesIntoRerunFiles();
		assertThat(rerunFiles.size()).isEqualTo(1);
	}

	@Test
	public void whenPassedARequestForTwoThreadsShouldReturnTwoRerunFiles() throws IOException {
		List<String> arguments = new ArrayList<String>();
		arguments.add("classpath:com/bishnet/cucumber/parallel/runtime/samplefeatures/directory");
		RuntimeConfiguration runtimeConfiguration = getRuntimeConfiguration(arguments, 2);
		FeatureParser featureParser = new FeatureParser(runtimeConfiguration, Thread.currentThread().getContextClassLoader());
		FeatureSplitter featureSplitter = new FeatureSplitter(runtimeConfiguration, featureParser.parseFeatures());
		List<Path> rerunFiles = featureSplitter.splitFeaturesIntoRerunFiles();
		assertThat(rerunFiles.size()).isEqualTo(2);
	}

	@Test
	public void whenPassedARequestForMoreThreadsThanFeaturesShouldReturnRerunFilesEqualToFeatureCount() throws IOException {
		List<String> arguments = new ArrayList<String>();
		arguments.add("classpath:com/bishnet/cucumber/parallel/runtime/samplefeatures/directory");
		RuntimeConfiguration runtimeConfiguration = getRuntimeConfiguration(arguments, 10);
		FeatureParser featureParser = new FeatureParser(runtimeConfiguration, Thread.currentThread().getContextClassLoader());
		List<CucumberFeature> features = featureParser.parseFeatures();
		FeatureSplitter featureSplitter = new FeatureSplitter(runtimeConfiguration, features);
		List<Path> rerunFiles = featureSplitter.splitFeaturesIntoRerunFiles();
		assertThat(rerunFiles.size()).isEqualTo(features.size());
	}

	@Test
	public void whenFeatureIsEmptyShouldExcludeEntireFeature() throws IOException {
		List<String> arguments = new ArrayList<String>();
        arguments.add("classpath:com/bishnet/cucumber/parallel/runtime/samplefeatures/individual/ValidFeatureWithNoScenarios.feature");
        RuntimeConfiguration runtimeConfiguration = getRuntimeConfiguration(arguments, 1);
        FeatureParser featureParser = new FeatureParser(runtimeConfiguration, Thread.currentThread().getContextClassLoader());
        FeatureSplitter featureSplitter = getFeatureSplitter(runtimeConfiguration, featureParser.parseFeatures());
        List<Path> rerunFiles = featureSplitter.splitFeaturesIntoRerunFiles();
        assertThat(rerunFiles.size()).isEqualTo(0);
    }

	@Test
	public void whenFeatureContainsNoScenariosMatchedTheFiltersShouldExcludeEntireFeature() throws IOException {
		List<String> arguments = new ArrayList<String>();
        arguments.add("classpath:com/bishnet/cucumber/parallel/runtime/samplefeatures/individual/ValidFeatureWithExcludingTag.feature");
        arguments.add("--tags");
        arguments.add("@IncludedTag");
        arguments.add("--tags");
        arguments.add("~@ExcludedTag");
        RuntimeConfiguration runtimeConfiguration = getRuntimeConfiguration(arguments, 1);
        FeatureParser featureParser = new FeatureParser(runtimeConfiguration, Thread.currentThread().getContextClassLoader());
        FeatureSplitter featureSplitter = getFeatureSplitter(runtimeConfiguration, featureParser.parseFeatures());
        List<Path> rerunFiles = featureSplitter.splitFeaturesIntoRerunFiles();
        assertThat(rerunFiles.size()).isEqualTo(0);
    }

	@Test
	public void whenRunTimeConfigurationReportIsNotRequiredThenSortFeaturesNotApplied() throws IOException {
		RuntimeConfiguration runtimeConfiguration =
				getRuntimeConfiguration(new FeatureExecutionTimeReportConfiguration(false, FILE_NAME_PREFIX));
		FeatureSplitter featureSplitter = getFeatureSplitter(runtimeConfiguration, singletonList(mock(CucumberFeature.class)));

		featureSplitter.splitFeaturesIntoRerunFiles();

		verifyZeroInteractions(executionTimeReporter, executionTimeFeatureSorter);
	}

	@Test
	public void whenRunTimeConfigurationReportIsRequiredThenSortFeaturesIsApplied() throws IOException {
		RuntimeConfiguration runtimeConfiguration =
				getRuntimeConfiguration(new FeatureExecutionTimeReportConfiguration(true, FILE_NAME_PREFIX));
		List<CucumberFeature> filteredFeatureList = asList(cucumberFeatureFirst, cucumberFeatureSecond);
		FeatureSplitter featureSplitter = getFeatureSplitter(runtimeConfiguration, filteredFeatureList);
		doReturn(filteredFeatureList).when(featureSplitter).filterEmptyFeatures(filteredFeatureList);
		doReturn(executionTimeFeatureSorter).when(featureSplitter).getFeatureSorter();
		List<FeatureExecutionTimeData> featureExecutionTimeData = singletonList(new FeatureExecutionTimeData(1L, "path"));
		when(executionTimeReporter.getReport(any())).thenReturn(of(featureExecutionTimeData));

		featureSplitter.splitFeaturesIntoRerunFiles();

		verify(executionTimeFeatureSorter).sortFeaturesByExecutionTime(eq(filteredFeatureList), eq(featureExecutionTimeData));
	}

	@Test
	public void whenRunTimeConfigurationReportIsRequiredAndReportFileAbsentThenSortFeaturesIsNotApplied() throws IOException {
		RuntimeConfiguration runtimeConfiguration =
				getRuntimeConfiguration(new FeatureExecutionTimeReportConfiguration(true, FILE_NAME_PREFIX));
		List<CucumberFeature> filteredFeatureList = asList(cucumberFeatureFirst, cucumberFeatureSecond);
		FeatureSplitter featureSplitter = getFeatureSplitter(runtimeConfiguration, filteredFeatureList);
		doReturn(filteredFeatureList).when(featureSplitter).filterEmptyFeatures(filteredFeatureList);
		doReturn(executionTimeFeatureSorter).when(featureSplitter).getFeatureSorter();
		when(executionTimeReporter.getReport(any())).thenReturn(Optional.empty());

		featureSplitter.splitFeaturesIntoRerunFiles();

		verify(executionTimeFeatureSorter, never()).sortFeaturesByExecutionTime(anyList(), anyList());
	}

	private RuntimeConfiguration getRuntimeConfiguration(List<String> featureParsingArguments, int numberOfThreads) {
		return new RuntimeConfiguration(numberOfThreads, null, featureParsingArguments, null, null, false, null,
				false, null, false, null, false, 0, null, 0, false, new FeatureExecutionTimeReportConfiguration());
    }

    private RuntimeConfiguration
    getRuntimeConfiguration(FeatureExecutionTimeReportConfiguration featureExecutionTimeReportConfiguration) {
        return new RuntimeConfiguration(1, null, emptyList(), null, null, false, null,
				false, null, false, null, false, 0, null, 0, false, featureExecutionTimeReportConfiguration);
    }

    private FeatureSplitter getFeatureSplitter(RuntimeConfiguration runtimeConfiguration, List<CucumberFeature> features) {
        FeatureSplitter featureSplitter = new FeatureSplitter(runtimeConfiguration, features);
        featureSplitter = spy(featureSplitter);
        doReturn(executionTimeReporter).when(featureSplitter).getFeatureExecutionTimeReporter();
        return featureSplitter;
    }
}