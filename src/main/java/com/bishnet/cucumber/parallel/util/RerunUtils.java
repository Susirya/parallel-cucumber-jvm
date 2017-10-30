package com.bishnet.cucumber.parallel.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;


public class RerunUtils {
	public static int countScenariosInRerunFile (Path path) throws IOException {
		return getStringsFromRerunFile(path).length;
	}

	public static List<String> splitRerunFileIntoPathStringsList(Path path) throws IOException {
		return Arrays.asList(getStringsFromRerunFile(path));
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

	private static String[] getStringsFromRerunFile(Path path) throws IOException {
		byte[] encoded = Files.readAllBytes(path);
		return new String(encoded, Charset.defaultCharset()).split("[ ]+");
	}
}
