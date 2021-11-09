package wasdi.jwasdilib.utils;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

public final class FileUtils {

	private final static Logger LOGGER = Logger.getLogger(FileUtils.class);


	private FileUtils() {
		throw new AssertionError("Utility class should not be instantiated.");
	}

	public static String getAbsoluteFilePath(String fileName) {
		LOGGER.info("getAbsoluteFilePath");

		ClassLoader loader = FileUtils.class.getClassLoader();
		File file = new File(loader.getResource(fileName).getFile());

		String absolutePath = file.getAbsolutePath();

		return absolutePath;
	}

	public static void copyResourceFileToLocalBasePath(String fileName, String destinationPath) throws IOException {
		LOGGER.info("copyResourceFileToLocalBasePath");

		String sFileUrl = FileUtils.getAbsoluteFilePath("./images/" + fileName);

		org.apache.commons.io.FileUtils.copyFile(new File(sFileUrl), new File(destinationPath));
	}

}
