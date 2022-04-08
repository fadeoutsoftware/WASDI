/*
 * Copyright (c) Fadeout Software 2020. Marco Menapace
 *
 */
package wasdi.shared.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Test class for safe extraction using it.fadeout.utils.ZipFileUtils
 * Checks different condition. Before every single test the directory of the extraction is
 * cleaned. Tests also several settings of zip extractor itself.
 */
class TestZipFileUtils {

	private ZipFileUtils oZipExtractor;

	// final string pointing the path for testing
	private final static String s_sStartingPath = //"." + File.separator +
			"src" + File.separator + "test" + File.separator +
			"java" + File.separator + "wasdi" + File.separator +
			"shared" + File.separator + "utils" + File.separator +
			"extractionFolder" + File.separator;
	private static String s_sExtractionPath = ""; 
	private final static String s_sExtractionFileName = "test.zip";


	/**
	 * This methods cleans the out directory before and after each test.
	 */
	@BeforeEach
	@AfterEach
	private void deleteExtractedFolder() {

		oZipExtractor = new ZipFileUtils();
		oZipExtractor.setTOOBIGTOTAL(1024L * 1024L * 1024L * 1L); // Max size of unzipped total Data, 1 GB
		oZipExtractor.setTOOBIGSINGLE(1024L * 1024L * 512L); // Max size of single file, 512 MB
		oZipExtractor.setTOOMANY(30); // Max count of unzipped files

		Path oWorkingDir = Paths.get(System.getProperty("user.dir"));
		Path oExtractionPath = oWorkingDir.resolve(s_sStartingPath);
		s_sExtractionPath = oExtractionPath.toString() + File.separator;
		// Directory clean
		// for each file
		for (File oFile : new File(s_sExtractionPath).listFiles()) {
			try {
				if (!oFile.getCanonicalFile().toString().contains(s_sExtractionFileName)) { // exclude test.zip
					if (oFile.isDirectory()) oZipExtractor.deleteDirectory(oFile.toPath());
					else {
						if (!oFile.delete()) {
							System.err.println("Before Test extraction - can't delete file " + oFile.getCanonicalFile());
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * Test that the extraction is done by checking that the files list contained
	 * in the original zip file are on the file system.
	 */
	@Test
	void testExtractionDone() {
		// first unzip
		String sTempDir = "";
		try {
			sTempDir = oZipExtractor.unzip(s_sExtractionPath + s_sExtractionFileName, s_sExtractionPath);
		} catch (Exception oE) {
			oE.printStackTrace();
		}
		// then check files in temp dir
		File oHere = new File(sTempDir);
		ArrayList<String> path = new ArrayList<>();
		try {
			Files.walk(oHere.toPath().getParent()) // get the parent of the temp dir
			.forEach(oFileInStream -> path.add(oFileInStream.toString()));
		} catch (IOException oE) {
			oE.printStackTrace();
		}
		String[] asFileListInTestZip = getFileListInTestZip();
		// this checks is done on temp dir but at this point the temp dir is deleted
		for (String sZipEntryName : asFileListInTestZip) {
			Assertions.assertTrue(path.contains(s_sExtractionPath + sZipEntryName));
		}
	}

	/**
	 * Test that the extraction is done by checking that the files list contained
	 * in the original zip file are on the file system.
	 */
	@Test
	void testExtractionDoneBackSlash() {
		// first unzip
		String sTempDir = "";
		try {
			sTempDir = oZipExtractor.unzip(s_sExtractionPath + s_sExtractionFileName, s_sExtractionPath);
		} catch (Exception oE) {
			oE.printStackTrace();
		}
		// then check files in temp dir
		File oHere = new File(sTempDir);
		ArrayList<String> path = new ArrayList<>();
		try {
			Files.walk(oHere.toPath().getParent()) // get the parent of the temp dir
			.forEach(oFileInStream -> path.add(oFileInStream.toString()));
		} catch (IOException oE) {
			oE.printStackTrace();
		}
		String[] asFileListInTestZip = getFileListInTestZip();
		// this checks is done on temp dir but at this point the temp dir is deleted
		for (String sZipEntryName : asFileListInTestZip) {
			Assertions.assertTrue(path.contains(s_sExtractionPath + sZipEntryName));
		}
	}

	/**
	 * Test that the extraction is done by checking that the files list contained
	 * in the original zip file are on the file system.
	 */
	@Test
	void testExtractionDoneFileSeparator() {
		// first unzip
		String sTempDir = "";
		try {
			sTempDir = oZipExtractor.unzip(s_sExtractionPath + s_sExtractionFileName, s_sExtractionPath);
		} catch (Exception oE) {
			oE.printStackTrace();
		}
		// then check files in temp dir
		File oHere = new File(sTempDir);
		ArrayList<String> path = new ArrayList<>();
		try {
			Files.walk(oHere.toPath().getParent()) // get the parent of the temp dir
			.forEach(oFileInStream -> path.add(oFileInStream.toString()));
		} catch (IOException oE) {
			oE.printStackTrace();
		}
		String[] asFileListInTestZip = getFileListInTestZip();
		// this checks is done on temp dir but at this point the temp dir is deleted
		for (String sZipEntryName : asFileListInTestZip) {
			Assertions.assertTrue(path.contains(s_sExtractionPath + sZipEntryName));
		}
	}

	/**
	 * Test that the extraction is done by checking that the files list contained
	 * in the original zip file are on the file system.
	 */
	@Test
	void testExtractionDoneNoSep() {
		// first unzip
		String sTempDir = "";
		try {
			String sPath = new String(s_sExtractionPath.substring(0, s_sExtractionPath.length()-1));
			sTempDir = oZipExtractor.unzip(sPath + File.separator + s_sExtractionFileName, sPath);
		} catch (Exception oE) {
			oE.printStackTrace();
		}
		// then check files in temp dir
		File oHere = new File(sTempDir);
		ArrayList<String> path = new ArrayList<>();
		try {
			Files.walk(oHere.toPath().getParent()) // get the parent of the temp dir
			.forEach(oFileInStream -> path.add(oFileInStream.toString()));
		} catch (IOException oE) {
			oE.printStackTrace();
		}
		String[] asFileListInTestZip = getFileListInTestZip();
		// this checks is done on temp dir but at this point the temp dir is deleted
		for (String sZipEntryName : asFileListInTestZip) {
			Assertions.assertTrue(path.contains(s_sExtractionPath + sZipEntryName));
		}
	}


	/**
	 * Test that the extraction is done by checking that the files list contained
	 * in the original zip file are on the file system.
	 */
	@Test
	void testExtractionDoneGiveMeAName() {
		// first unzip
		String sTempDir = "";
		try {
			String sPath = new String(s_sExtractionPath.substring(0, s_sExtractionPath.length()-1));
			sPath += "\\\\\\\\/////";
			sTempDir = oZipExtractor.unzip(sPath + s_sExtractionFileName, sPath);
		} catch (Exception oE) {
			oE.printStackTrace();
		}
		// then check files in temp dir
		File oHere = new File(sTempDir);
		ArrayList<String> path = new ArrayList<>();
		try {
			Files.walk(oHere.toPath().getParent()) // get the parent of the temp dir
			.forEach(oFileInStream -> path.add(oFileInStream.toString()));
		} catch (IOException oE) {
			oE.printStackTrace();
		}
		String[] asFileListInTestZip = getFileListInTestZip();
		// this checks is done on temp dir but at this point the temp dir is deleted
		for (String sZipEntryName : asFileListInTestZip) {
			Assertions.assertTrue(path.contains(s_sExtractionPath + sZipEntryName));
		}
	}

	/**
	 * Checks that extraction fails for a single file being too big
	 */
	@Test
	void testExtractionSingleFileTooBig() {
		//oZipExtractor.setTOOBIGSINGLE(1024 * 20); // aka 20 KB
		oZipExtractor.setTOOBIGSINGLE(500); // aka 20 B
		// with the current test must be checked that Big L script shouldn't be extracted
		Exception oException = Assertions.assertThrows(IllegalStateException.class, () -> {
			oZipExtractor.unzip(s_sExtractionPath + s_sExtractionFileName, s_sExtractionPath);
		});
		// first unzip
		Assertions.assertTrue(oException.getMessage().contains("File being unzipped is too big"));
	}

	/**
	 * Checks that extraction fails for total size exceeding the imposed limits
	 */
	@Test
	void testExtractionAllFilesTooBig() {
		oZipExtractor.setTOOBIGTOTAL(1); // aka 1 Byte
		// with the current test must be checked that Big L script shouldn't be extracted
		Exception oException = Assertions.assertThrows(IllegalStateException.class, () -> {
			oZipExtractor.unzip(s_sExtractionPath + s_sExtractionFileName, s_sExtractionPath);
		});
		// first unzip
		Assertions.assertTrue(oException.getMessage().contains("File extraction interrupted because total dimension is over extraction limits"));


	}

	/**
	 * Checks that extraction fails for total number of files exceed the imposed limit
	 */
	@Test
	void testExtractionTooMany() {
		oZipExtractor.setTOOMANY(5); // aka 5 files
		// with the current test must be checked that Big L script shouldn't be extracted
		Exception oException = Assertions.assertThrows(IllegalStateException.class, () -> {
			oZipExtractor.unzip(s_sExtractionPath + s_sExtractionFileName, s_sExtractionPath);
		});
		// first unzip
		Assertions.assertTrue(oException.getMessage().contains("Too many files inside the archive."));
	}


	/**
	 * Utils methods used that returns a String array containing all the
	 * entries inside the zip file.
	 *
	 * @return String array with the entries of the zip file used in these test
	 */
	static String[] getFileListInTestZip() {
		try {
			FileInputStream oFis = new FileInputStream(s_sExtractionPath + s_sExtractionFileName);
			ZipInputStream oZis = new ZipInputStream(new BufferedInputStream(oFis));
			ArrayList<String> asReturnArray = new ArrayList<>();
			ZipEntry oZe = oZis.getNextEntry();
			while (oZe != null) {
				String sName = oZe.getName();
				sName = sName.replace("/", File.separator);
				sName = sName.replace("\\", File.separator);
				if (sName.endsWith(File.separator)) {
					asReturnArray.add(sName.substring(0, oZe.getName().length() - 1));
				} else {
					asReturnArray.add(sName);
				}
				oZe = oZis.getNextEntry();
			}

			oZis.close();
			return asReturnArray.toArray(new String[0]);

		} catch (FileNotFoundException oE) {
			oE.printStackTrace();
		} catch (IOException oE) {
			oE.printStackTrace();
		}
		return null;

	}
}
