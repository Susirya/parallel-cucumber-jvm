package com.bishnet.cucumber.parallel.report.thread;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import gherkin.deps.com.google.gson.GsonBuilder;


@RunWith(MockitoJUnitRunner.class)
public class FeatureExecutionTimeReporterTest {

	private static final String EXECUTION_TIME_FILE_NAME = "executionTime.json";
	private static final String FILE_NAME_PREFIX = "prefix";
	private static final String NON_EXISTENT_FILE_NAME_PREFIX = "nonExistentPrefix";
	private static final String FIRST_FEATURE_PATH = "featurePath";
	private static final FeatureExecutionTimeData FEATURE_EXECUTION_TIME_DATA_FIRST =
			new FeatureExecutionTimeData(1000L, FIRST_FEATURE_PATH);
	private static final FeatureExecutionTimeData FEATURE_EXECUTION_TIME_DATA_SECOND =
			new FeatureExecutionTimeData(2000L, "featurePath2");
	private static final FeatureExecutionTimeData FEATURE_EXECUTION_TIME_DATA_THIRD =
			new FeatureExecutionTimeData(3000L, "featurePath3");
	private static final String INVALID_PATH = " ";
	private static final long FIRST_SCENARIO_START_TIME = 10L;
	private static final long SECOND_SCENARIO_START_TIME = 50L;
	private static final long THIRD_SCENARIO_START_TIME = 4020L;
	private static final long FIRST_SCENARIO_END_TIME = 1000L;
	private static final long FIRST_FEATURE_EXECUTION_TIME = FIRST_SCENARIO_END_TIME - FIRST_SCENARIO_START_TIME;
	private static final long SECOND_SCENARIO_END_TIME = 4000L;
	private static final long THIRD_SCENARIO_END_TIME = 6000L;
	private static final long SECOND_FEATURE_EXECUTION_TIME = (SECOND_SCENARIO_END_TIME - SECOND_SCENARIO_START_TIME)
			+ (THIRD_SCENARIO_END_TIME - THIRD_SCENARIO_START_TIME);
	private static final String SECOND_FEATURE_PATH = "featurePathSecond";

	@Spy
	private FeatureExecutionTimeReporter reporter = new FeatureExecutionTimeReporter();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Before
	public void setUp() {
		doAnswer(methodCall -> {
			String fileName = (String) methodCall.getArguments()[0];
			return new File(temporaryFolder.getRoot(), fileName);
		}).when(reporter).getFileFromFileSystem(anyString());
	}

	@Test
	public void shouldWriteReportDataToJsonFileIfReportFileDoesNotExist() throws IOException {
		List<ThreadTimelineData> threadTimelineDataList =
				asList(getThreadTimelineData(FIRST_SCENARIO_START_TIME, FIRST_SCENARIO_END_TIME, FIRST_FEATURE_PATH,
						"scenario_1"),
						getThreadTimelineData(SECOND_SCENARIO_START_TIME, SECOND_SCENARIO_END_TIME, SECOND_FEATURE_PATH,
								"scenario_2"),
						getThreadTimelineData(THIRD_SCENARIO_START_TIME, THIRD_SCENARIO_END_TIME, SECOND_FEATURE_PATH,
								"scenario_3"));

		List<FeatureExecutionTimeData> expectedReportData =
				asList(new FeatureExecutionTimeData(FIRST_FEATURE_EXECUTION_TIME, FIRST_FEATURE_PATH),
						new FeatureExecutionTimeData(SECOND_FEATURE_EXECUTION_TIME, SECOND_FEATURE_PATH));

		reporter.writeReport(threadTimelineDataList, FILE_NAME_PREFIX);

		String actualJson = FileUtils
				.readFileToString(new File(temporaryFolder.getRoot(), FILE_NAME_PREFIX + EXECUTION_TIME_FILE_NAME), UTF_8);
		List<FeatureExecutionTimeData> actualExecutionTimeData =
				asList(new GsonBuilder().create().fromJson(actualJson, FeatureExecutionTimeData[].class));
		verifyReportData(expectedReportData, actualExecutionTimeData);
	}

	@Test
	public void shouldOverwriteReportDataJsonFileIfItAlreadyExists() throws IOException {
		List<ThreadTimelineData> threadTimelineDataList =
				singletonList(
						getThreadTimelineData(FIRST_SCENARIO_START_TIME, FIRST_SCENARIO_END_TIME, FIRST_FEATURE_PATH,
								"scenario_1"));
		reset(reporter);
		File existingReportFile = createFile(FILE_NAME_PREFIX, singletonList("Some content"));
		doReturn(existingReportFile).when(reporter).getFileFromFileSystem(FILE_NAME_PREFIX + EXECUTION_TIME_FILE_NAME);

		List<FeatureExecutionTimeData> expectedReportData =
				singletonList(new FeatureExecutionTimeData(FIRST_FEATURE_EXECUTION_TIME, FIRST_FEATURE_PATH));

		reporter.writeReport(threadTimelineDataList, FILE_NAME_PREFIX);

		String actualJson = FileUtils
				.readFileToString(new File(temporaryFolder.getRoot(), FILE_NAME_PREFIX + EXECUTION_TIME_FILE_NAME), UTF_8);
		List<FeatureExecutionTimeData> actualExecutionTimeData =
				asList(new GsonBuilder().create().fromJson(actualJson, FeatureExecutionTimeData[].class));
		verifyReportData(expectedReportData, actualExecutionTimeData);
	}

	private ThreadTimelineData getThreadTimelineData(long startTime, long endTime, String featurePath, String scenarioId) {
		long threadId = 999L;
		ThreadTimelineData threadTimelineData = new ThreadTimelineData(startTime, threadId, scenarioId);
		threadTimelineData.setEndTime(endTime);
		threadTimelineData.setFeaturePath(featurePath);
		return threadTimelineData;
	}

	@Test
	public void shouldReturnEmptyOptionalWhenReportFileDoesNotExist() {
		assertThat(reporter.getReport(NON_EXISTENT_FILE_NAME_PREFIX)).isEqualTo(Optional.empty());
	}

	@Test
	public void shouldThrowWhenFileIsAbsent() {
		File reportFile = spy(new File(INVALID_PATH));
		doReturn(true).when(reportFile).exists();
		reset(reporter);
		doReturn(reportFile).when(reporter).getFileFromFileSystem(NON_EXISTENT_FILE_NAME_PREFIX + EXECUTION_TIME_FILE_NAME);
		expected.expect(RuntimeException.class);
		expected.expectCause(IsInstanceOf.instanceOf(FileNotFoundException.class));

		reporter.getReport(NON_EXISTENT_FILE_NAME_PREFIX);
	}

	@Test
	public void shouldReturnReportDataParsedFromReportFile() throws IOException {
		List<FeatureExecutionTimeData> expectedReportData =
				asList(FEATURE_EXECUTION_TIME_DATA_FIRST, FEATURE_EXECUTION_TIME_DATA_SECOND, FEATURE_EXECUTION_TIME_DATA_THIRD);
		File reportFile = createFile(FILE_NAME_PREFIX, singletonList(new GsonBuilder().create().toJson(expectedReportData)));
		reset(reporter);
		doReturn(reportFile).when(reporter).getFileFromFileSystem(FILE_NAME_PREFIX + EXECUTION_TIME_FILE_NAME);

		List<FeatureExecutionTimeData> actualReportData = reporter.getReport(FILE_NAME_PREFIX).get();

		verifyReportData(expectedReportData, actualReportData);
	}

	private void verifyReportData(List<FeatureExecutionTimeData> expectedReportDataList,
			List<FeatureExecutionTimeData> actualReportDataList) {
		assertThat(actualReportDataList.size()).isEqualTo(expectedReportDataList.size());
		expectedReportDataList.forEach(expectedReportData -> {
			assertThat(actualReportDataList.stream()
					.filter(actualReportData -> actualReportData.getFeaturePath().equals(expectedReportData.getFeaturePath())
							&& actualReportData.getExecutionTime() == expectedReportData.getExecutionTime())
					.count()).isEqualTo(1);
		});
	}

	private File createFile(String fileNamePrefix, List<String> lines) throws IOException {
		Path correctFile = createEmptyFile(fileNamePrefix).toPath();
		Files.write(correctFile, lines);
		return correctFile.toFile();
	}

	private File createEmptyFile(String fileNamePrefix) throws IOException {
		return temporaryFolder.newFile(fileNamePrefix + EXECUTION_TIME_FILE_NAME);
	}
}