package com.bishnet.cucumber.parallel.runtime;

import java.nio.file.Path;

public class FlakyRerunConfiguration {
    public final int flakyAttemptsCount;
    public final Path flakyReportPath;
    public final int flakyMaxCount;
    public final String flakyTag;


    public FlakyRerunConfiguration() {
        flakyAttemptsCount = 0;
        flakyReportPath = null;
        flakyMaxCount = 0;
        flakyTag = "";
    }

    public FlakyRerunConfiguration(int flakyAttemptsCount, Path flakyReportPath, int flakyMaxCount, String flakyTag) {
        this.flakyAttemptsCount = flakyAttemptsCount;
        this.flakyReportPath = flakyReportPath;
        this.flakyMaxCount = flakyMaxCount;
        this.flakyTag = flakyTag;
    }
}
