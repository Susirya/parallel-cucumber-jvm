package com.bishnet.cucumber.parallel.runtime;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import com.bishnet.cucumber.parallel.report.thread.FeatureExecutionTimeData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import cucumber.runtime.model.CucumberFeature;


@RunWith(MockitoJUnitRunner.class)
public class ExecutionTimeFeatureSorterTest {

	private static final String FIRST_FEATURE_PATH = "C:/somepath/first.feature";
	private static final String SECOND_FEATURE_PATH = "C:/somepath/second.feature";
	private static final String THIRD_FEATURE_PATH = "C:/somepath/third.feature";
	private static final long FIRST_FEATURE_EXECUTION_TIME = 1000L;
	private static final long SECOND_FEATURE_EXECUTION_TIME = 2000L;

	private ExecutionTimeFeatureSorter sorter = new ExecutionTimeFeatureSorter();

	@Mock
	private CucumberFeature cucumberFeatureFirst;
	@Mock
	private CucumberFeature cucumberFeatureSecond;
	@Mock
	private CucumberFeature cucumberFeatureThird;
	@Mock
	private FeatureExecutionTimeData featureExecutionTimeDataFirst;
	@Mock
	private FeatureExecutionTimeData featureExecutionTimeDataSecond;

	@Before
	public void setup() {
		when(featureExecutionTimeDataFirst.getExecutionTime()).thenReturn(FIRST_FEATURE_EXECUTION_TIME);
		when(featureExecutionTimeDataFirst.getFeaturePath()).thenReturn(FIRST_FEATURE_PATH);
		when(featureExecutionTimeDataSecond.getExecutionTime()).thenReturn(SECOND_FEATURE_EXECUTION_TIME);
		when(featureExecutionTimeDataSecond.getFeaturePath()).thenReturn(SECOND_FEATURE_PATH);
		when(cucumberFeatureFirst.getPath()).thenReturn(FIRST_FEATURE_PATH);
		when(cucumberFeatureSecond.getPath()).thenReturn(SECOND_FEATURE_PATH);
		when(cucumberFeatureThird.getPath()).thenReturn(THIRD_FEATURE_PATH);
	}

	@Test
	public void shouldSortTwoFeatureFilesDescByExecutionTime() {
		List<CucumberFeature> features = asList(cucumberFeatureSecond, cucumberFeatureFirst);

		List<CucumberFeature> actualSortedFeatures = sorter.sortFeaturesByExecutionTime(features,
				asList(featureExecutionTimeDataFirst, featureExecutionTimeDataSecond));

		assertThat(actualSortedFeatures).containsExactly(cucumberFeatureSecond,
				cucumberFeatureFirst);
	}

	@Test
	public void shouldSortThreeFeaturesDescByExecutionTimeFeatureWithoutExecutionTimeOnTheFirstPlace() {
		List<CucumberFeature> features = asList(cucumberFeatureSecond, cucumberFeatureFirst, cucumberFeatureThird);

		List<CucumberFeature> actualSortedFeatures = sorter.sortFeaturesByExecutionTime(features,
				asList(featureExecutionTimeDataFirst, featureExecutionTimeDataSecond));

		assertThat(actualSortedFeatures).containsExactly(cucumberFeatureThird, cucumberFeatureSecond,
				cucumberFeatureFirst);
	}

}