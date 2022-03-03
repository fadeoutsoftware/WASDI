/*
 * Copyright (c) Fadeout Software 2020. Marco Menapace
 *
 */

package wasdi.shared.utils;

import static wasdi.shared.utils.WasdiFileUtils.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.log4j.Logger;

/**
 * Utility class for zip extraction operation with some security considerations.
 *
 * @see <a href="http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-1263">CVE-2018-1263</a>
 * @see <a href="http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-16131">CVE-2018-16131</a>
 */
public class ZipFileUtils {

	/**
	 * These parameters must be instantiated using configurations values
	 */
	static final int BUFFER = 512;
	/**
	 * Max size of unzipped total Data, 1 GB
	 */
	//long m_lToobigtotal = 1024L * 1024L * 1024L;
	long m_lToobigtotal = 0L;
	/**
	 * Max size of single unzipped Data, 512 MB
	 */
	//long m_lToobigsingle = 1024L * 1024L * 512L;
	long m_lToobigsingle = 0L;
	/**
	 * Maximum number of files that can be extracted
	 */
	//int m_lToomany = 1024;
	int m_lToomany = 0;
	/**
	 * Logger prefix
	 */
	String m_sLoggerPrefix = "ZipExtractor.";
	/**
	 * Static logger reference
	 */
	static Logger s_oLogger = Logger.getLogger(ZipFileUtils.class);


	/**
	 * Safer version of the unzip method.
	 * This method take in account the total dimension extracted and the dimension for each file.
	 * If the extraction fails a clean method should be invoked, removing all the files extracted till
	 * that point.
	 * Try-with-resources approach for the ZipInputStream.
	 * The extraction creates a temporary directory which is programmatically deleted if
	 * - errors occurs (I.E. a file is bigger than the limit imposed)
	 * - the unzip is completed without errors
	 *
	 * @param sFilename the filename of the Zip file to be extracted
	 * @param sPath     The intended path in which the file should be extracted
	 * @throws java.io.IOException Throws IO exception in case the zip file is not founded
	 */
	public String unzip(String sFilename, String sPath) throws java.io.IOException {

		FileInputStream oFis = new FileInputStream(WasdiFileUtils.fixPathSeparator(sFilename));

		int iEntries = 0;
		long lTotal = 0;
		long lSingle = 0;

		int iRandom = new SecureRandom().nextInt() & Integer.MAX_VALUE;

		String sTemp = "tmp-" + iRandom + File.separator;
		String sTempPath = WasdiFileUtils.fixPathSeparator(sPath);

		if(!sTempPath.endsWith(File.separator)) {
			sTempPath += File.separator;
		}
		sTempPath += sTemp;

		Path oPath = Paths.get(sTempPath).toAbsolutePath().normalize();
		if (oPath.toFile().mkdir()) {
			s_oLogger.info(m_sLoggerPrefix + "unzip: Temporary directory created: "  + sTempPath);
		} else {
			throw new IOException("Can't create temporary dir " + sTempPath);
		}

		try (ArchiveInputStream oZis = new ZipArchiveInputStream(oFis, "UTF-8", false, true)){

			ArchiveEntry oEntry;
			while ((oEntry = oZis.getNextEntry()) != null) {
				try {
					if (!oZis.canReadEntryData(oEntry)) {
						s_oLogger.warn(m_sLoggerPrefix + ": can't read entry: " + oEntry + ", skipping");
						continue;
					}
					
					s_oLogger.info(m_sLoggerPrefix + "Extracting: " + oEntry);
					int iCount;

					byte[] ayData = new byte[BUFFER];
					// Write the files to the disk, but ensure that the filename is valid,
					// and that the file is not insanely big
					String sName = validateFilename(sTempPath + oEntry.getName(), sTempPath); // throws exception in case

					// Random used to mitigate attacks
					if (!oEntry.isDirectory()) {
						File oFile = new File(sName);
						if (!oFile.getParentFile().exists()) {
							s_oLogger.info(m_sLoggerPrefix + "unzip: Creating parent directory " + oFile.getParent());
							oFile.getParentFile().mkdirs();
						}
					}
					else {
						new File(sName).mkdirs();
						continue;
					}

					FileOutputStream oFos = new FileOutputStream(sName);

					try (BufferedOutputStream oDest = new BufferedOutputStream(oFos, BUFFER)){

						while ( (lTotal + BUFFER <= m_lToobigtotal || m_lToobigtotal==0) && (lSingle + BUFFER <= m_lToobigsingle || m_lToobigsingle == 0) && (iCount = oZis.read(ayData, 0, BUFFER)) != -1) {
							oDest.write(ayData, 0, iCount);
							lTotal += iCount;
							lSingle += iCount;
						}

						oDest.flush();
						//oZis.closeEntry();
						iEntries++;

						if ( (lSingle + BUFFER > m_lToobigsingle) && (m_lToobigsingle>0)) {
							cleanTempDir(sTempPath, sTemp);
							s_oLogger.error(m_sLoggerPrefix + "unzip: File being unzipped is too big. The limit is " + humanReadableByteCountSI(m_lToobigsingle));
							throw new IllegalStateException("File being unzipped is too big. The limit is " + humanReadableByteCountSI(m_lToobigsingle));
						}
						if ( (lTotal + BUFFER > m_lToobigtotal) && (m_lToobigtotal>0)) {
							cleanTempDir(sTempPath, sTemp);
							s_oLogger.error(m_sLoggerPrefix + "unzip: File extraction interrupted because total dimension is over extraction limits. The limit is " + humanReadableByteCountSI(m_lToobigtotal));
							throw new IllegalStateException("File extraction interrupted because total dimension is over extraction limits. The limit is " + humanReadableByteCountSI(m_lToobigtotal));
						}
						if ( (iEntries > m_lToomany) && (m_lToomany>0)) {
							cleanTempDir(sTempPath, sTemp);
							s_oLogger.error(m_sLoggerPrefix + "unzip: Too many files inside the archive. The limit is "+m_lToomany);
							throw new IllegalStateException("Too many files inside the archive. The limit is "+m_lToomany);
						}

						// resets single file byte-counter
						lSingle = 0; 
					}					
				}
				catch (Exception e) {
					s_oLogger.error(m_sLoggerPrefix + "unzip: error extracting entry: "+ e.toString());
					throw e;
				}

			}

			// IF everything went well cp temp content to original folder (overwrite it's fine) and delete temp dir
			s_oLogger.info(m_sLoggerPrefix + "Copy and clean tmp dir.");
			if (!cleanTempDir(sTempPath, sTemp)) {
				s_oLogger.error(m_sLoggerPrefix + " cleanTempDir( " + sTempPath + ", " + sTemp + " returned false...");
			}
		}
		return sTempPath;
	}

	/**
	 * Instantiates a ZipExtractor with default parameters
	 */
	public ZipFileUtils() {
	}

	/**
	 * Instantiates a ZipExtractor with default parameters and initialize the logger
	 * prefix
	 * @param sLoggerPrefix a string that must be passed in order to identify the process from a logging perspective
	 */
	public ZipFileUtils(String sLoggerPrefix) {
		if(!Utils.isNullOrEmpty(sLoggerPrefix)) {
			this.m_sLoggerPrefix = sLoggerPrefix + " - [" + this.m_sLoggerPrefix + "]";
		}
	}

	/**
	 * Instantiates a ZipExtractor passing 3 critical parameters (in bytes)
	 *
	 * @param lToobigtotal  the total maximum size allowed for extraction
	 * @param lToobigsingle the maximum single size for each file
	 * @param lToomany      the maximum number of files allowed to be extracted
	 * @param sLoggerPrefix a string that must be passed in order to identify the process from a logging perspective
	 */
	public ZipFileUtils(long lToobigtotal, long lToobigsingle, int lToomany, String sLoggerPrefix) {
		this.m_lToobigtotal = lToobigtotal;
		this.m_lToobigsingle = lToobigsingle;
		this.m_lToomany = lToomany;
		this.m_sLoggerPrefix = sLoggerPrefix + " - " + this.m_sLoggerPrefix; 
	}

	/**
	 * Checks that the the output dir is coherent with the current dir.
	 * This methods filters out directory traversal attempts.
	 *
	 * @param sFilename    the file name of the zip file to be extracted
	 * @param sIntendedDir the intended directory where the extraction must be done
	 * @return the canonical path of the file
	 * @throws java.io.IOException in case of the file is outside target extraction directory
	 */
	private String validateFilename(String sFilename, String sIntendedDir)
			throws java.io.IOException {
		File oF = new File(sFilename);
		String sCanonicalPath = oF.getCanonicalPath();

		File oIDir = new File(sIntendedDir);
		String sCanonicalID = oIDir.getCanonicalPath();

		if (sCanonicalPath.startsWith(sCanonicalID)) {
			return sCanonicalPath;
		} else {
			s_oLogger.error(m_sLoggerPrefix + "validateFilename: File is outside extraction target directory." );
			throw new IllegalStateException("File is outside extraction target directory.");
		}
	}

	/**
	 * Util methods to obtain a human readable byte count(GB,MB,KB) from
	 * a byte count.
	 *
	 * @param lBytes the number of the bytes that should be considered
	 * @return String with the human readable string (e.g. kB, MB, GB)
	 */
	private String humanReadableByteCountSI(long lBytes) {
		if (-1000 < lBytes && lBytes < 1000) {
			return lBytes + " B";
		}
		CharacterIterator oCi = new StringCharacterIterator("kMGTPE");
		while (lBytes <= -999_950 || lBytes >= 999_950) {
			lBytes /= 1000;
			oCi.next();
		}
		return String.format("%.1f %cB", lBytes / 1000.0, oCi.current());
	}

	/**
	 * After a successful extraction this method is invoked in order to move the
	 * temporary folder content and cleanup the directory.
	 * @param sTempPath the temp path where to clean up
	 * @param sTemp the folder name of the temp path [temp-{random-generated-id}]
	 * @return True if the operation is done without errors nor exceptions. False instead
	 */
	private boolean cleanTempDir(String sTempPath, String sTemp) {

		// point the temp dir
		File oDir = new File(sTempPath); 

		try (Stream<Path> aoPathStream = Files.walk(oDir.toPath())){

			// this make the dir before other files
			aoPathStream.sorted(Comparator.naturalOrder()).map(Path::toFile).forEach(oFile -> {
				try {
					// removes the tmp-part from the destination files
					File oDest = new File(oFile.getCanonicalPath().replace(sTemp, ""));
					// checks the existence of the dir
					if (oFile.isDirectory()) {
						oDest.mkdir();
						return;
					}

					if  (!oDest.getParentFile().exists()) {
						oDest.mkdirs();
					}

					Files.copy(oFile.getCanonicalFile().toPath(), oDest.getCanonicalFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
					try {
						Files.setPosixFilePermissions(oDest.getCanonicalFile().toPath(), PosixFilePermissions.fromString("rw-rw-r--"));
					}
					catch (Exception e) {
					}

				} catch (Exception oE) {
					oE.printStackTrace();
				}
			});
		} catch (IOException oE) {
			oE.printStackTrace();
			return false;
		}

		try {
			deleteDirectory(oDir.toPath());
		} catch (IOException oE) {
			oE.printStackTrace();
			return false;
		}

		return true;

	}

	/**
	 * Util method that recursively delete the path passed as input
	 * On unix systems is equivalent to "rm -rf [path]"
	 * @param oToBeDeleted path to be deleted
	 * @throws IOException
	 */
	protected void deleteDirectory(Path oToBeDeleted) throws IOException {
		try (Stream<Path> aoPathStream = Files.walk(oToBeDeleted)){
			aoPathStream.sorted(Comparator.reverseOrder())
			.map(Path::toFile)
			.forEach(File::delete);
		}
	}

	/**
	 * Setter of the parameter that checks the total size of the extracted files
	 *
	 * @param lTooBigTotal size, in bytes, of the limit
	 */
	public void setTOOBIGTOTAL(long lTooBigTotal) {
		m_lToobigtotal = lTooBigTotal;
	}

	/**
	 * Setter of the parameter that checks the size of each single file
	 *
	 * @param lTooBigSingle size, in bytes, of the limit
	 */
	public void setTOOBIGSINGLE(long lTooBigSingle) {
		m_lToobigsingle = lTooBigSingle;
	}

	/**
	 * Setter of the parameter that checks the number of files being extracted
	 *
	 * @param iTooMany count of the files that must not be exceed
	 */
	public void setTOOMANY(int iTooMany) {
		m_lToomany = iTooMany;
	}


	/**
	 * Util method to zip a complete Path, traversing all the subdirectories, using java stream support
	 * @param sSourceDirPath The path to the directory that should be added
	 * @param sZipFilePath the destination Zip File path
	 * @throws IOException
	 */
	public void zipFolder(String sSourceDirPath, String sZipFilePath) throws IOException {
		
		Path oZipFilePath = Files.createFile(Paths.get(sZipFilePath));
		
		try (ZipOutputStream oZipOutputStream = new ZipOutputStream(Files.newOutputStream(oZipFilePath))) {
			Path oPath = Paths.get(sSourceDirPath);
			Files.walk(oPath)
			.filter(path -> !Files.isDirectory(path))
			.forEach(path -> {
				ZipEntry zipEntry = new ZipEntry(oPath.relativize(path).toString());
				try {
					oZipOutputStream.putNextEntry(zipEntry);
					Files.copy(path, oZipOutputStream);
					oZipOutputStream.closeEntry();
				} catch (IOException e) {
					s_oLogger.error(m_sLoggerPrefix + "zip: Error during creation of zip archive " );
				}
			});
		}
	}
	
	/**
	 * Zip a list of files. They are supposed to be all on the same folder
	 * @param asSourceFiles
	 * @param sZipFilePath
	 * @throws IOException
	 */
	public static void zipFiles(ArrayList<String> asSourceFiles, String sZipFilePath) throws IOException {
		
		File oCheckExists = new File(sZipFilePath);
		
		if (oCheckExists.exists()) {
			oCheckExists.delete();
		}
		
		Path oZipFilePath = Files.createFile(Paths.get(sZipFilePath));
		
		try (ZipOutputStream oZipOutputStream = new ZipOutputStream(Files.newOutputStream(oZipFilePath))) {
			
			for (String sFileToZip: asSourceFiles) {
				
				File oFileToZip = new File(sFileToZip);
				
				Path oPath = Paths.get(oFileToZip.getPath());
				
				ZipEntry zipEntry = new ZipEntry(oFileToZip.getName());
				try {
					oZipOutputStream.putNextEntry(zipEntry);
					Files.copy(oPath, oZipOutputStream);
					oZipOutputStream.closeEntry();
				} catch (IOException e) {
					s_oLogger.error("ZipFileUtils.zipFiles: Error during creation of zip archive " );
				}				
			}
		}
	}

	//courtesy of https://www.baeldung.com/java-compress-and-uncompress
	public static void zipFile(File oFileToZip, String sFileName, ZipOutputStream oZipOut) {
		try {
			if(oFileToZip.getName().equals(".") || oFileToZip.getName().equals("..")) {
				return;
			}
			if (oFileToZip.isDirectory()) {
				if (sFileName.endsWith("/")) {
					oZipOut.putNextEntry(new ZipEntry(sFileName));
					oZipOut.closeEntry();
				} else {
					oZipOut.putNextEntry(new ZipEntry(sFileName + "/"));
					oZipOut.closeEntry();
				}
				File[] oChildren = oFileToZip.listFiles();
				for (File oChildFile : oChildren) {
					zipFile(oChildFile, sFileName + "/" + oChildFile.getName(), oZipOut);
				}
				return;
			}
			try (FileInputStream oFis = new FileInputStream(oFileToZip)) {
				ZipEntry oZipEntry = new ZipEntry(sFileName);
				oZipOut.putNextEntry(oZipEntry);
				byte[] bytes = new byte[1024];
				int iLength;
				while ((iLength = oFis.read(bytes)) >= 0) {
					oZipOut.write(bytes, 0, iLength);
				}
//				oFis.close();				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Extract the content of a zip file, removing the initial file.
	 * @param zipFile the zip file to be extracted
	 * @param destDir the destination directory where the content should be moved
	 * @throws IOException in case of any issue
	 */
	public static void cleanUnzipFile(File zipFile, File destDir) throws IOException {
		if (zipFile == null) {
			Utils.log("ERROR", "ZipFileUtils.cleanUnzipFile: zipFile is null");
			return;
		} else if (!zipFile.exists()) {
			Utils.log("ERROR", "ZipFileUtils.cleanUnzipFile: zipFile does not exist: " + zipFile.getAbsolutePath());
		}

		if (destDir == null) {
			Utils.log("ERROR", "ZipFileUtils.cleanUnzipFile: destDir is null");
			return;
		} else if (!destDir.exists()) {
			Utils.log("ERROR", "ZipFileUtils.cleanUnzipFile: destDir does not exist: " + destDir.getAbsolutePath());
		}

		ZipFileUtils oZipExtractor = new ZipFileUtils();

		String sFilename = zipFile.getAbsolutePath();
		String sPath = destDir.getAbsolutePath();
		oZipExtractor.unzip(sFilename, sPath);

		String dirPath = completeDirPath(destDir.getAbsolutePath());
		String fileZipPath = dirPath + zipFile.getName();

		String unzippedDirectoryPath = dirPath + removeZipExtension(zipFile.getName());

		if (fileExists(unzippedDirectoryPath)) {
			boolean filesMovedFlag = moveFile(unzippedDirectoryPath, dirPath);

			if (filesMovedFlag) {
				deleteFile(unzippedDirectoryPath);
				deleteFile(fileZipPath);
			}
		}
	}

	public static void fixZipFileInnerSafePath(String zipFilePath) throws IOException {
		if (zipFilePath == null) {
			Utils.log("ERROR", "ZipFileUtils.fixZipFileInnerSafePath: zipFilePath is null");
			return;
		}
		
		File zipFile = new File(zipFilePath);
		if (!zipFile.exists()) {
			Utils.log("ERROR", "ZipFileUtils.fixZipFileInnerSafePath: zipFile does not exist: " + zipFile.getAbsolutePath());
			return;
		}

		String dirPath = completeDirPath(zipFile.getParentFile().getAbsolutePath());
		String simpleName = removeZipExtension(zipFile.getName());

		String unzippedDirectoryPath = dirPath + simpleName;
		File unzippedDirectory = new File(unzippedDirectoryPath);

		if  (!unzippedDirectory.exists()) {
			unzippedDirectory.mkdirs();
		}

		ZipFileUtils oZipExtractor = new ZipFileUtils();
		oZipExtractor.unzip(zipFilePath, unzippedDirectoryPath);

		File[] files = unzippedDirectory.listFiles();
		while (files != null && files.length > 0) {
			for (File file : files) {
				if (file.isDirectory()) {
					if ((simpleName).equalsIgnoreCase(file.getName())) {
						oZipExtractor.zipFolder(file.getAbsolutePath(), dirPath + simpleName + "_temp" + ".zip");
						files = null;
						break;
					} else {
						files = file.listFiles();
						continue;
					}
				}
			}
		}

		deleteFile(unzippedDirectoryPath);
		deleteFile(zipFilePath);
		WasdiFileUtils.renameFile(dirPath + simpleName + "_temp" + ".zip", simpleName + ".zip");
	}

}
