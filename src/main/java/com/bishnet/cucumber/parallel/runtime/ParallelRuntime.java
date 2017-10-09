package com.bishnet.cucumber.parallel.runtime;

import com.bishnet.cucumber.parallel.cli.ArgumentsParser;
import com.bishnet.cucumber.parallel.report.HtmlReportMerger;
import com.bishnet.cucumber.parallel.report.JsonReportMerger;
import com.bishnet.cucumber.parallel.report.RerunReportMerger;
import com.bishnet.cucumber.parallel.report.thread.FeatureExecutionTimeReporter;
import com.bishnet.cucumber.parallel.report.thread.ThreadExecutionRecorder;
import com.bishnet.cucumber.parallel.report.thread.ThreadExecutionReporter;
import com.bishnet.cucumber.parallel.util.RerunUtils;
import cucumber.runtime.CucumberException;
import cucumber.runtime.model.CucumberFeature;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

public class ParallelRuntime {

	private static final String FLAKY_REPORT_FILENAME_TEMPLATE = "flaky_%s.json";
	private static final Logger LOG = Logger.getLogger(ParallelRuntime.class.getName());
	private static final String LOG_MSG_NONE_OF_THE_FEATURES_FOUND_IN_PATH =
			"None of the features or scenarios at %s matched the filters, or no scenarios found.";
	private static final String LOG_MSG_TOO_MANY_TESTS_FAILED_FOR_RERUN =
			"%d TESTS FAILED - MORE THEN ALLOWED FOR RERUN (%d)! Aborting rerun flaky.";
	private static final String LOG_MSG_RERUN_FLAKY_TESTS_STARTED = "RERUN FLAKY TESTS STARTED. WILL TRY FOR %d ATTEMPT(S).";
	private static final String LOG_MSG_RERUN_FLAKIES_ATTEMPT_FINISHED = "RERUN FLAKY TESTS ATTEMPT #%d FINISHED.";
	private static final String LOG_MSG_RERUN_FLAKY_TESTS_FINISHED = "RERUN FLAKY TESTS FINISHED. TRIED FOR %d ATTEMPT(S).";
	private RuntimeConfiguration runtimeConfiguration;
	private ClassLoader cucumberClassLoader;
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
			return runWithRerunFailed(features);
		} catch (InterruptedException | IOException e) {
			throw new CucumberException(e);
		}
	}

	private byte runWithRerunFailed(List<CucumberFeature> features) throws IOException, InterruptedException {
		List<Path> rerunFiles = splitFeaturesIntoRerunFiles(features);
		if (rerunFiles.isEmpty()) {
		 	LOG.info(String.format(LOG_MSG_NONE_OF_THE_FEATURES_FOUND_IN_PATH, runtimeConfiguration.featurePaths));
			return 0;
		}
		byte result = runFeatures(rerunFiles);
		if (result != 0 && runtimeConfiguration.flakyRerunConfig.flakyAttemptsCount > 0) {
			result = rerunFlakyTests(rerunFiles, result);
		}
		return result;
	}

	private byte rerunFlakyTests(List<Path> rerunFiles, byte result) throws IOException, InterruptedException {
		int failedCount = RerunUtils.countScenariosInRerunFile(runtimeConfiguration.rerunReportReportPath);
		int flakyMaxCount = runtimeConfiguration.flakyRerunConfig.flakyMaxCount;
		if (failedCount > flakyMaxCount) {
			LOG.info(String.format(LOG_MSG_TOO_MANY_TESTS_FAILED_FOR_RERUN, failedCount, flakyMaxCount));
			return result;
		}
		LOG.info(String.format(LOG_MSG_RERUN_FLAKY_TESTS_STARTED, runtimeConfiguration.flakyRerunConfig.flakyAttemptsCount));
		triedRerun = 1;
		while (result != 0 && triedRerun <= runtimeConfiguration.flakyRerunConfig.flakyAttemptsCount) {
			rerunFiles.clear();
			rerunFiles.add(runtimeConfiguration.rerunReportReportPath);
			result = runFeatures(rerunFiles);
			LOG.info(String.format(LOG_MSG_RERUN_FLAKIES_ATTEMPT_FINISHED, triedRerun++));
		}
		LOG.info(String.format(LOG_MSG_RERUN_FLAKY_TESTS_FINISHED, triedRerun));
		return result;
	}

	private List<CucumberFeature> parseFeatures() {
		FeatureParser featureParser = new FeatureParser(runtimeConfiguration, cucumberClassLoader);
		return featureParser.parseFeatures();
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
        } else {
            RerunReportMerger merger = new RerunReportMerger(executor.getRerunReports());
            merger.merge(runtimeConfiguration.rerunReportReportPath);
            JsonReportMerger jsonMerger = new JsonReportMerger(executor.getJsonReports());
            jsonMerger.mergeRerunFailedReports(runtimeConfiguration.jsonReportPath,
                    Paths.get(runtimeConfiguration.flakyRerunConfig.flakyReportPath.toString(),
							String.format(FLAKY_REPORT_FILENAME_TEMPLATE, triedRerun)));
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

		return result;
	}
}
