package com.bishnet.cucumber.parallel.runtime;


public class FeatureExecutionTimeReportConfiguration {

	public final boolean reportRequired;
	public final String reportFileNamePrefix;

	public FeatureExecutionTimeReportConfiguration() {
		this(false, "");
	}

	public FeatureExecutionTimeReportConfiguration(boolean featureExecutionTimeReportRequired, String reportFileNamePrefix) {
		this.reportRequired = featureExecutionTimeReportRequired;
		this.reportFileNamePrefix = reportFileNamePrefix;
	}

}
