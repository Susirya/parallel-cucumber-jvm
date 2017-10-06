
package com.bishnet.cucumber.parallel.runtime;

import java.util.List;
import java.util.stream.Collectors;

import com.bishnet.cucumber.parallel.report.thread.FeatureExecutionTimeData;

import cucumber.runtime.model.CucumberFeature;


public class ExecutionTimeFeatureSorter {

	public List<CucumberFeature> sortFeaturesByExecutionTime(List<CucumberFeature> filteredFeatures,
			List<FeatureExecutionTimeData> featuresExecutionTime) {
		return filteredFeatures.stream().map(feature -> {
			long executionTime = featuresExecutionTime.stream()
					.filter(featureExecutionTime -> feature.getPath().equals(featureExecutionTime.getFeaturePath()))
					.findFirst()
					.map(FeatureExecutionTimeData::getExecutionTime)
					.orElse(Long.MAX_VALUE);
			return new WeighedCucumberFeature(feature, executionTime);
		}).sorted()
				.map(weighedCucumberFeature -> weighedCucumberFeature.feature)
				.collect(Collectors.toList());
	}

	private class WeighedCucumberFeature implements Comparable<WeighedCucumberFeature> {

		final CucumberFeature feature;
		final long weight;

		public WeighedCucumberFeature(CucumberFeature feature, long weight) {
			this.feature = feature;
			this.weight = weight;
		}

		@Override
		public int compareTo(WeighedCucumberFeature other) {
			return Long.compare(other.weight, weight);
		}
	}
}
