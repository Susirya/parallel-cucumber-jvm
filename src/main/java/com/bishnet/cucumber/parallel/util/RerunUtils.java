package com.bishnet.cucumber.parallel.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class RerunUtils {
	public static int countScenariosInRerunFile (Path path) throws IOException {
		byte[] encoded = Files.readAllBytes(path);
		String[] strings = new String(encoded, Charset.defaultCharset()).split("[ ]+");
		return strings.length;
	}

	public static Path getTempFilePathWithExtention(String fileExtention) {
		Path rerunReportReportPath = Paths.get("");
		try {
			rerunReportReportPath = Files.createTempFile("parallelCukesTmp", fileExtention);
			rerunReportReportPath.toFile().deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rerunReportReportPath;
	}
}
