package com.bishnet.cucumber.parallel.report.thread;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import gherkin.deps.com.google.gson.Gson;
import gherkin.deps.com.google.gson.GsonBuilder;


public class FeatureExecutionTimeReporter {

	private static final String EXECUTION_TIME_SUFFIX = "executionTime.json";

	private static final Logger LOG = Logger.getLogger(FeatureExecutionTimeReporter.class.getName());

	public void writeReport(List<ThreadTimelineData> reportData, String reportFileNamePrefix) throws IOException {

		File destinationFile = getReportFile(reportFileNamePrefix);

		String json = convertReportDataToJson(reportData);
		writeJsonToFile(destinationFile, json);
	}

	private void writeJsonToFile(File destinationFile, String json) throws IOException {
		FileUtils.writeStringToFile(destinationFile, json, UTF_8);
	}

	private File getReportFile(String reportFileNamePrefix) {
		return getFileFromFileSystem(reportFileNamePrefix + EXECUTION_TIME_SUFFIX);
	}

	protected File getFileFromFileSystem(String reportFileName) {
		return FileUtils.getFile(reportFileName);
	}

	private String convertReportDataToJson(List<ThreadTimelineData> timelineReportData) {
		List<FeatureExecutionTimeData> executionTimeData = timelineReportData.stream()
				.map(timeLine -> new FeatureExecutionTimeData(timeLine.getEndTime() - timeLine.getStartTime(),
						timeLine.getFeaturePath()))
				.collect(toList());
		Map<String, Long> summarizedExecutionTime = executionTimeData.stream()
				.collect(groupingBy(FeatureExecutionTimeData::getFeaturePath,
						summingLong(FeatureExecutionTimeData::getExecutionTime)));
		executionTimeData = summarizedExecutionTime.entrySet().stream()
				.map(entry -> new FeatureExecutionTimeData(entry.getValue(), entry.getKey()))
				.collect(toList());
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(executionTimeData);
		return json;
	}

	public Optional<List<FeatureExecutionTimeData>> getReport(String reportFileNamePrefix) {

		File reportFile = getReportFile(reportFileNamePrefix);
		if (reportFile.exists()) {
			Reader reader;
			try {
				reader = new FileReader(reportFile);
			} catch (FileNotFoundException e) {
				System.out.println("##### Report file '" + reportFile.getAbsolutePath() + "' not found! #####");
				throw new RuntimeException(e);
			}
			FeatureExecutionTimeData[] featureExecutionTimeData =
					new GsonBuilder().create().fromJson(reader, FeatureExecutionTimeData[].class);
			return Optional.of(Arrays.asList(featureExecutionTimeData));
		} else {
			return Optional.empty();
		}
	}
}
