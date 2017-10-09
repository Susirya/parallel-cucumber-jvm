package com.bishnet.cucumber.parallel.runtime;

import cucumber.runtime.CucumberException;
import cucumber.runtime.model.CucumberFeature;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FeatureParserTest {

	@Test
	public void singleFeatureFileWithValidScenariosShouldReturnOneFeature() {
		List<String> arguments = new ArrayList<String>();
		arguments.add("classpath:com/bishnet/cucumber/parallel/runtime/samplefeatures/individual/ValidFeature.feature");
		FeatureParser featureParser = new FeatureParser(getRuntimeConfiguration(arguments), Thread.currentThread()
				.getContextClassLoader());
		List<CucumberFeature> features = featureParser.parseFeatures();
		assertThat(features.size()).isEqualTo(1);
	}

	@Test
	public void featureDirectoryWithTwoValidFeaturesShouldReturnTwoFeatures() {
		List<String> arguments = new ArrayList<String>();
		arguments.add("classpath:com/bishnet/cucumber/parallel/runtime/samplefeatures/directory");
		FeatureParser featureParser = new FeatureParser(getRuntimeConfiguration(arguments), Thread.currentThread()
				.getContextClassLoader());
		List<CucumberFeature> features = featureParser.parseFeatures();
		assertThat(features.size()).isEqualTo(2);
	}

	@Test(expected = CucumberException.class)
	public void singleFeatureFileWithAnInvalidScenarioShouldThrowACucumberException() {
		List<String> arguments = new ArrayList<String>();
		arguments.add("classpath:com/bishnet/cucumber/parallel/runtime/samplefeatures/individual/InvalidFeature.feature");
		FeatureParser featureParser = new FeatureParser(getRuntimeConfiguration(arguments), Thread.currentThread()
				.getContextClassLoader());
		featureParser.parseFeatures();
	}

	private RuntimeConfiguration getRuntimeConfiguration(List<String> featureParsingArguments) {
		return new RuntimeConfiguration(0, null, featureParsingArguments, null, null, false, null, false,
				null, false, null, false, new FlakyRerunConfiguration(), false, new FeatureExecutionTimeReportConfiguration());
	}
}
