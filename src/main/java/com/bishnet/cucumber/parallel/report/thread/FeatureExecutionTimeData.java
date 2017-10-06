package com.bishnet.cucumber.parallel.report.thread;

import gherkin.deps.com.google.gson.annotations.SerializedName;

public class FeatureExecutionTimeData {

	@SerializedName("executionTime")
	private long executionTime;
	@SerializedName("featurePath")
	private String featurePath;

	public FeatureExecutionTimeData(long executionTime, String featurePath) {
		this.executionTime = executionTime;
		this.featurePath = featurePath;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public String getFeaturePath() {
		return featurePath;
	}
}
