package com.bishnet.cucumber.parallel.runtime;

import com.bishnet.cucumber.parallel.cli.ArgumentsParser;
import com.bishnet.cucumber.parallel.report.HtmlReportMerger;
import com.bishnet.cucumber.parallel.report.JsonReportMerger;
import com.bishnet.cucumber.parallel.report.RerunReportMerger;
import com.bishnet.cucumber.parallel.report.thread.FeatureExecutionTimeReporter;
import com.bishnet.cucumber.parallel.report.thread.ThreadExecutionRecorder;
import com.bishnet.cucumber.parallel.report.thread.ThreadExecutionReporter;
import cucumber.runtime.CucumberException;
import cucumber.runtime.model.CucumberFeature;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class ParallelRuntime {

	private static final String FLAKY_REPORT_FILENAME_TEMPLATE = "flaky_%s.json";
	private static final Logger LOG = Logger.getLogger(ParallelRuntime.class.getName());
	private static final String LOG_MSG_RERUN_FLAKY_TESTS_STARTED = "RERUN FLAKY TESTS STARTED. WILL TRY FOR %d ATTEMPT(S).";
	private static final String LOG_MSG_RERUN_FLAKIES_ATTEMPT_FINISHED = "RERUN FLAKY TESTS ATTEMPT #%d FINISHED.";
	private static final String LOG_MSG_RERUN_FLAKY_TESTS_FINISHED = "RERUN FLAKY TESTS FINISHED. TRIED FOR %d ATTEMPT(S).";
	private RuntimeConfiguration runtimeConfiguration;
	private ClassLoader cucumberClassLoader;
	private FeatureFilter featureFilter;
	private CucumberBackendFactory cucumberBackendFactory;
	private int triedRerun;

	public ParallelRuntime(List<String> arguments) {
		this(arguments, Thread.currentThread().getContextClassLoader());
	}

	public ParallelRuntime(List<String> arguments, ClassLoader cucumberClassLoader) {
		this(arguments, cucumberClassLoader, null);
	}

	public ParallelRuntime(List<String> arguments, CucumberBackendFactory cucumberBackendFactory) {
		this(arguments, Thread.currentThread().getContextClassLoader(), cucumberBackendFactory);
	}

	public ParallelRuntime(List<String> arguments, ClassLoader cucumberClassLoader, CucumberBackendFactory cucumberBackendFactory) {
		this.cucumberClassLoader = cucumberClassLoader;
		this.cucumberBackendFactory = cucumberBackendFactory;
		ArgumentsParser argumentsParser = new ArgumentsParser(arguments);
		runtimeConfiguration = argumentsParser.parse();
	}

	public byte run() {
		List<CucumberFeature> features = parseFeatures();
		if (features.isEmpty())
			return 0;
		try {
			List<Path> rerunFiles = splitFeaturesIntoRerunFiles(features);
			byte result = runFeatures(rerunFiles);
			if (result != 0 && isRerunByTagRequied()) {
				List<CucumberFeature> flakyFeatures = getFlakyFilteredFeatures();
				if (flakyFeatures.isEmpty())
					return result;
				rerunFiles = splitFeaturesIntoRerunFiles(flakyFeatures);
				byte rerunResult = rerunFlakyTests(rerunFiles);
				if (features.size() == flakyFeatures.size()) {
					result = rerunResult;
				}
			}
			return result;
		} catch (InterruptedException | IOException e) {
			throw new CucumberException(e);
		}
	}

	private boolean isRerunByTagRequied() {
		return runtimeConfiguration.rerunReportRequired && !runtimeConfiguration.flakyRerunConfig.flakyTag.isEmpty();
	}

	private byte rerunFlakyTests(List<Path> rerunFiles) throws IOException, InterruptedException {
		LOG.info(String.format(LOG_MSG_RERUN_FLAKY_TESTS_STARTED, runtimeConfiguration.flakyRerunConfig.flakyAttemptsCount));
		triedRerun = 1;
		byte result = runFeatures(rerunFiles);
		LOG.info(String.format(LOG_MSG_RERUN_FLAKIES_ATTEMPT_FINISHED, triedRerun));
		while (result != 0 && triedRerun <= runtimeConfiguration.flakyRerunConfig.flakyAttemptsCount) {
			rerunFiles.clear();
			rerunFiles.add(runtimeConfiguration.rerunReportReportPath);
			result = runFeatures(rerunFiles);
			LOG.info(String.format(LOG_MSG_RERUN_FLAKIES_ATTEMPT_FINISHED, ++triedRerun));
		}
		LOG.info(String.format(LOG_MSG_RERUN_FLAKY_TESTS_FINISHED, triedRerun));
		return result;
	}

	private List<CucumberFeature> parseFeatures() {
		FeatureParser featureParser = new FeatureParser(runtimeConfiguration, cucumberClassLoader);
		return featureParser.parseFeatures();
	}

	private List<CucumberFeature> getFlakyFilteredFeatures() throws IOException {
		FeatureFilter featureFilter = new FeatureFilter(runtimeConfiguration, cucumberClassLoader);
		return featureFilter.getFilteredFeatures();
	}

	private List<Path> splitFeaturesIntoRerunFiles(List<CucumberFeature> features) throws IOException {
		FeatureSplitter featureSplitter = new FeatureSplitter(runtimeConfiguration, features);
		return featureSplitter.splitFeaturesIntoRerunFiles();
	}

	private byte runFeatures(List<Path> rerunFiles) throws InterruptedException, IOException {

		CucumberRuntimeFactory runtimeFactory = null;
		ThreadExecutionRecorder threadExecutionRecorder = null;

		if (runtimeConfiguration.threadTimelineReportRequired)
			threadExecutionRecorder = new ThreadExecutionRecorder();

		runtimeFactory = new CucumberRuntimeFactory(runtimeConfiguration, cucumberBackendFactory, cucumberClassLoader, threadExecutionRecorder);

		CucumberRuntimeExecutor executor = new CucumberRuntimeExecutor(runtimeFactory, rerunFiles, runtimeConfiguration);

		byte result = executor.run();

        if (triedRerun == 0) {
            if (runtimeConfiguration.rerunReportRequired) {
                RerunReportMerger merger = new RerunReportMerger(executor.getRerunReports());
                merger.merge(runtimeConfiguration.rerunReportReportPath);
            }
            if (runtimeConfiguration.jsonReportRequired) {
                JsonReportMerger merger = new JsonReportMerger(executor.getJsonReports());
                merger.merge(runtimeConfiguration.jsonReportPath);
            }
            if (runtimeConfiguration.htmlReportRequired) {
                HtmlReportMerger merger = new HtmlReportMerger(executor.getHtmlReports());
                merger.merge(runtimeConfiguration.htmlReportPath);
            }
			if (runtimeConfiguration.threadTimelineReportRequired) {
				ThreadExecutionReporter threadExecutionReporter = new ThreadExecutionReporter();
				threadExecutionReporter.writeReport(threadExecutionRecorder.getRecordedData(), runtimeConfiguration.threadTimelineReportPath);
			}
			if (runtimeConfiguration.featureExecutionTimeReportconfig.reportRequired) {
				FeatureExecutionTimeReporter reporter = new FeatureExecutionTimeReporter();
				reporter.writeReport(threadExecutionRecorder.getRecordedData(),
						runtimeConfiguration.featureExecutionTimeReportconfig.reportFileNamePrefix);
			}
		} else {
			RerunReportMerger merger = new RerunReportMerger(executor.getRerunReports());
			merger.merge(runtimeConfiguration.rerunReportReportPath);
			JsonReportMerger jsonMerger = new JsonReportMerger(executor.getJsonReports());
			jsonMerger.mergeRerunFailedReports(runtimeConfiguration.jsonReportPath,
                    Paths.get(runtimeConfiguration.flakyRerunConfig.flakyReportPath.toString(),
							String.format(FLAKY_REPORT_FILENAME_TEMPLATE, triedRerun)));
		}

		return result;
	}
}
