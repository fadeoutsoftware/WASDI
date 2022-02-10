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
import java.util.Comparator;
import java.util.Enumeration;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
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
	private long m_lTotal;
	private int m_iEntries;
	private long m_lSingle;


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
	 * @param sZipFileAbsolutePath the filename of the Zip file to be extracted
	 * @param sDestinationPath     The intended path in which the file should be extracted
	 * @throws Exception 
	 */
	public String unzip(String sZipFileAbsolutePath, String sDestinationPath) throws Exception {
		String sTempRelativeDirectory = null;
		String sTempAbsolutePath = null;
		try {
			m_iEntries = 0;
			m_lTotal = 0;
	
			sTempRelativeDirectory = nameRandomTempLocalDirectory();
			sTempAbsolutePath = buildTempFullPath(sDestinationPath, sTempRelativeDirectory);
	
			Path oPath = Paths.get(sTempAbsolutePath).toAbsolutePath().normalize();
			if (oPath.toFile().mkdirs()) {
				s_oLogger.info(m_sLoggerPrefix + "unzip: Temporary directory created: "  + sTempAbsolutePath);
			} else {
				throw new IOException("Can't create temporary dir " + sTempAbsolutePath);
			}
	
			File oInputFile = new File(sZipFileAbsolutePath);
			try(ZipFile oZipFile = new ZipFile(oInputFile)){
				Enumeration<? extends ZipArchiveEntry> aoZipArchiveEntries = oZipFile.getEntries();
				while(aoZipArchiveEntries.hasMoreElements()) {
					extractOneEntry(sTempRelativeDirectory, sTempAbsolutePath, oZipFile, aoZipArchiveEntries);
	
				}
			}
		} catch (Exception oE) {
			s_oLogger.error(m_sLoggerPrefix + ".unzip: " + oE);
			throw oE;
		} finally {
			// make sure temporary directory gets deleted
			s_oLogger.info(m_sLoggerPrefix + "Copy and clean tmp dir.");
			if (!cleanTempDir(sTempAbsolutePath, sTempRelativeDirectory)) {
				s_oLogger.error(m_sLoggerPrefix + " cleanTempDir( " + sTempAbsolutePath + ", " + sTempRelativeDirectory + " returned false...");
			}
		}
		return sTempAbsolutePath;
	}

	/**
	 * @param sTempRelativeDirectory
	 * @param sTempAbsolutePath
	 * @param oZipFile
	 * @param aoZipArchiveEntries
	 * @throws Exception
	 */
	private void extractOneEntry(String sTempRelativeDirectory, String sTempAbsolutePath, ZipFile oZipFile,
			Enumeration<? extends ZipArchiveEntry> aoZipArchiveEntries) throws Exception {
		try {
			ZipArchiveEntry oEntry = aoZipArchiveEntries.nextElement();
			s_oLogger.info(m_sLoggerPrefix + "unzip: extracting: " + oEntry);

			String sName = validateFilename(sTempAbsolutePath + oEntry.getName(), sTempAbsolutePath); // throws exception in case

			if(oEntry.isDirectory()) {
				new File(sName).mkdirs();
				return;
			}

			//it's a file: create required directories
			File oFile = new File(sName);
			File oParent = oFile.getParentFile(); 
			if(null!=oParent && !oParent.exists()) {
				s_oLogger.info(m_sLoggerPrefix + "unzip: creating parent directory " + oParent);
				if(!oParent.mkdirs()) {
					String sMessage = "failed creating required directories of " + oFile;
					s_oLogger.error(m_sLoggerPrefix + "unzip: " + sMessage);
					throw new RuntimeException(sMessage);
				}
			}
			
			byte[] ayData = new byte[BUFFER];
			try (FileOutputStream oFos = new FileOutputStream(sName); BufferedOutputStream oDest = new BufferedOutputStream(oFos, BUFFER)){
				InputStream oZis = oZipFile.getInputStream(oEntry);
				copyOneEntryOutOfArchive(oZis, ayData, oDest);
				m_iEntries++;
			}
			checkUnzipStatus(m_iEntries, m_lTotal, sTempRelativeDirectory, sTempAbsolutePath, m_lSingle);
		} catch (Exception oE) {
			s_oLogger.error(m_sLoggerPrefix + "unzip: error extracting entry: "+ oE);
			throw oE;
		}
	}

	/**
	 * @return
	 */
	private String nameRandomTempLocalDirectory() {
		try {
			int iRandom = new SecureRandom().nextInt() & Integer.MAX_VALUE;
			String sTempDirectory = "tmp-" + iRandom + File.separator;
			return sTempDirectory;
		}catch (Exception oE) {
			s_oLogger.error(m_sLoggerPrefix + "nameRandomTempLocalDirectory: " + oE);
			return null;
		}
	}

	/**
	 * @param oZis
	 * @param ayData
	 * @param oDest
	 * @throws IOException
	 */
	private void copyOneEntryOutOfArchive(InputStream oZipInputStream, byte[] ayData, BufferedOutputStream oDest) throws IOException {
		m_lSingle = 0;
		int iCount = 0;
		while (
				(m_lTotal + BUFFER <= m_lToobigtotal || m_lToobigtotal<=0) &&
				(m_lSingle + BUFFER <= m_lToobigsingle || m_lToobigsingle <= 0) &&
				(iCount = oZipInputStream.read(ayData, 0, BUFFER)) != -1) {
			oDest.write(ayData, 0, iCount);
			m_lTotal += iCount;
			m_lSingle += iCount;
		}
		oDest.flush();
	}

	/**
	 * @param iEntries
	 * @param lTotal
	 * @param sTempDirectory
	 * @param sTempFullPath
	 * @param lSingle
	 * @throws IllegalStateException
	 */
	private void checkUnzipStatus(int iEntries, long lTotal, String sTempDirectory, String sTempFullPath, long lSingle)
			throws IllegalStateException {
		if ( (lSingle + BUFFER > m_lToobigsingle) && (m_lToobigsingle>0)) {
			cleanTempDir(sTempFullPath, sTempDirectory);
			s_oLogger.error(m_sLoggerPrefix + "checkUnzipStatus: File being unzipped is too big. The limit is " + humanReadableByteCountSI(m_lToobigsingle));
			throw new IllegalStateException("File being unzipped is too big. The limit is " + humanReadableByteCountSI(m_lToobigsingle));
		}
		if ( (lTotal + BUFFER > m_lToobigtotal) && (m_lToobigtotal>0)) {
			cleanTempDir(sTempFullPath, sTempDirectory);
			s_oLogger.error(m_sLoggerPrefix + "checkUnzipStatus: File extraction interrupted because total dimension is over extraction limits. The limit is " + humanReadableByteCountSI(m_lToobigtotal));
			throw new IllegalStateException("File extraction interrupted because total dimension is over extraction limits. The limit is " + humanReadableByteCountSI(m_lToobigtotal));
		}
		if ( (iEntries > m_lToomany) && (m_lToomany>0)) {
			cleanTempDir(sTempFullPath, sTempDirectory);
			s_oLogger.error(m_sLoggerPrefix + "checkUnzipStatus: Too many files inside the archive. The limit is "+m_lToomany);
			throw new IllegalStateException("Too many files inside the archive. The limit is "+m_lToomany);
		}
	}

	private String buildTempFullPath(String sPath, String sTemp) {
		try {
			String sTempPath = WasdiFileUtils.fixPathSeparator(sPath);
	
			if(!sTempPath.endsWith(File.separator)) {
				sTempPath += File.separator;
			}
			sTempPath += sTemp;
			return sTempPath;
		} catch (Exception oE) {
			s_oLogger.error(m_sLoggerPrefix + "buildTempFullPath: " + oE);
			return null;
		}
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
		try {
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
						catch (Exception oE) {
							s_oLogger.error(m_sLoggerPrefix +".cleanTempDir: set posix file permissions failed because: " + oE);
						}
	
					} catch (Exception oE) {
						s_oLogger.error(m_sLoggerPrefix +".cleanTempDir: map for each file failed because: " + oE);
					}
				});
			} catch (IOException oE) {
				s_oLogger.error(m_sLoggerPrefix +".cleanTempDir: files walk failed because: " + oE);
				return false;
			}
	
			try {
				deleteDirectory(oDir.toPath());
			} catch (IOException oE) {
				s_oLogger.error(m_sLoggerPrefix +".cleanTempDir: delete directory failed because: " + oE);
				return false;
			}
		} catch (Exception oE) {
			s_oLogger.error(m_sLoggerPrefix +".cleanTempDir (outmost): " + oE);
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
	 * @param sourceDirPath The path to the directory that should be added
	 * @param zipFilePath the destination Zip File path
	 * @throws IOException
	 */
	public void zip(String sourceDirPath, String zipFilePath) throws IOException {
		Path p = Files.createFile(Paths.get(zipFilePath));
		try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p))) {
			Path pp = Paths.get(sourceDirPath);
			Files.walk(pp)
			.filter(path -> !Files.isDirectory(path))
			.forEach(path -> {
				ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
				try {
					zs.putNextEntry(zipEntry);
					Files.copy(path, zs);
					zs.closeEntry();
				} catch (IOException e) {
					s_oLogger.error(m_sLoggerPrefix + "zip: Error during creation of zip archive " );
				}
			});
		}
	}

	//courtesy of https://www.baeldung.com/java-compress-and-uncompress
	public static void zipFile(File oFileToZip, String sFileName, ZipOutputStream oZipOut) {
		try {
			//			if (oFileToZip.isHidden()) {
			//				return;
			//			}
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
		} catch (Exception oE) {
			System.out.println("ZipFileUtils.zipFile: " + oE);
		}
	}

	/**
	 * Extract the content of a zip file, removing the initial file.
	 * @param zipFile the zip file to be extracted
	 * @param destDir the destination directory where the content should be moved
	 * @throws Exception 
	 */
	public static void cleanUnzipFile(File zipFile, File destDir) throws Exception {
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

	public static void fixZipFileInnerSafePath(String zipFilePath) throws Exception {
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
						oZipExtractor.zip(file.getAbsolutePath(), dirPath + simpleName + "_temp" + ".zip");
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
