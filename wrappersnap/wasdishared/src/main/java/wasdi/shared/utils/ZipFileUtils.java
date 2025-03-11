/*
 * Copyright (c) Fadeout Software 2020. Marco Menapace
 *
 */

package wasdi.shared.utils;

import static wasdi.shared.utils.WasdiFileUtils.completeDirPath;
import static wasdi.shared.utils.WasdiFileUtils.deleteFile;
import static wasdi.shared.utils.WasdiFileUtils.fileExists;
import static wasdi.shared.utils.WasdiFileUtils.moveFile;
import static wasdi.shared.utils.WasdiFileUtils.removeZipExtension;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import wasdi.shared.utils.log.WasdiLog;

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
	
	private long m_lTotal;
	private int m_iEntries;
	private long m_lSingle;


	/**
	 * Look into a zip file and extract the list of file names.
	 * @param sZipFileAbsolutePath the path of the zip file
	 * @return the list of file names
	 * @throws Exception in case of any issue with reading the zip file
	 */
	public static List<String> peepZipArchiveContent(String sZipFileAbsolutePath) throws Exception {
		List<String> asFileNames = new ArrayList<>();

		try {
			File oInputFile = new File(sZipFileAbsolutePath);

			try (ZipFile oZipFile = new ZipFile(oInputFile)) {
				Enumeration<? extends ZipArchiveEntry> aoZipArchiveEntries = oZipFile.getEntries();
				while(aoZipArchiveEntries.hasMoreElements()) {
					ZipArchiveEntry oEntry = aoZipArchiveEntries.nextElement();
					asFileNames.add(oEntry.getName());
				}
			}
		} 
		catch (Exception oE) {
			WasdiLog.errorLog("ZipFileUtils.pokeZipArchiveContent: Error during creation of zip archive " );
		}

		return asFileNames;
	}

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
				WasdiLog.infoLog(m_sLoggerPrefix + "unzip: Temporary directory created: "  + sTempAbsolutePath);
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
		} 
		catch (Exception oE) {
			WasdiLog.errorLog(m_sLoggerPrefix + ".unzip: " + oE);
			throw oE;
		} 
		finally {
			// make sure temporary directory gets deleted
			WasdiLog.infoLog(m_sLoggerPrefix + "Copy and clean tmp dir.");
			if (!cleanTempDir(sTempAbsolutePath, sTempRelativeDirectory)) {
				WasdiLog.errorLog(m_sLoggerPrefix + " cleanTempDir( " + sTempAbsolutePath + ", " + sTempRelativeDirectory + " returned false...");
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
			//WasdiLog.infoLog(m_sLoggerPrefix + "unzip: extracting: " + oEntry);

			String sName = validateFilename(sTempAbsolutePath + oEntry.getName(), sTempAbsolutePath); // throws exception in case

			if(oEntry.isDirectory()) {
				new File(sName).mkdirs();
				return;
			}

			//it's a file: create required directories
			File oFile = new File(sName);
			File oParent = oFile.getParentFile(); 
			if(null!=oParent && !oParent.exists()) {
				WasdiLog.infoLog(m_sLoggerPrefix + "unzip: creating parent directory " + oParent);
				if(!oParent.mkdirs()) {
					String sMessage = "failed creating required directories of " + oFile;
					WasdiLog.errorLog(m_sLoggerPrefix + "unzip: " + sMessage);
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
			WasdiLog.errorLog(m_sLoggerPrefix + "unzip: error extracting entry: "+ oE);
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
			WasdiLog.errorLog(m_sLoggerPrefix + "nameRandomTempLocalDirectory: " + oE);
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
			WasdiLog.errorLog(m_sLoggerPrefix + "checkUnzipStatus: File being unzipped is too big. The limit is " + humanReadableByteCountSI(m_lToobigsingle));
			throw new IllegalStateException("File being unzipped is too big. The limit is " + humanReadableByteCountSI(m_lToobigsingle));
		}
		if ( (lTotal + BUFFER > m_lToobigtotal) && (m_lToobigtotal>0)) {
			cleanTempDir(sTempFullPath, sTempDirectory);
			WasdiLog.errorLog(m_sLoggerPrefix + "checkUnzipStatus: File extraction interrupted because total dimension is over extraction limits. The limit is " + humanReadableByteCountSI(m_lToobigtotal));
			throw new IllegalStateException("File extraction interrupted because total dimension is over extraction limits. The limit is " + humanReadableByteCountSI(m_lToobigtotal));
		}
		if ( (iEntries > m_lToomany) && (m_lToomany>0)) {
			cleanTempDir(sTempFullPath, sTempDirectory);
			WasdiLog.errorLog(m_sLoggerPrefix + "checkUnzipStatus: Too many files inside the archive. The limit is "+m_lToomany);
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
			WasdiLog.errorLog(m_sLoggerPrefix + "buildTempFullPath: " + oE);
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
			WasdiLog.errorLog(m_sLoggerPrefix + "validateFilename: File is outside extraction target directory." );
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
							WasdiLog.errorLog(m_sLoggerPrefix +".cleanTempDir: set posix file permissions failed because: " + oE);
						}
	
					} catch (Exception oE) {
						WasdiLog.errorLog(m_sLoggerPrefix +".cleanTempDir: map for each file failed because: " + oE);
					}
				});
			} catch (IOException oE) {
				WasdiLog.errorLog(m_sLoggerPrefix +".cleanTempDir: files walk failed because: " + oE);
				return false;
			}
	
			try {
				deleteDirectory(oDir.toPath());
			} catch (IOException oE) {
				WasdiLog.errorLog(m_sLoggerPrefix +".cleanTempDir: delete directory failed because: " + oE);
				return false;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog(m_sLoggerPrefix +".cleanTempDir (outmost): " + oE);
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
			
			try (Stream<Path> oPathStream = Files.walk(oPath)) {
				oPathStream.filter(path -> !Files.isDirectory(path))
				.forEach(path -> {
					ZipEntry zipEntry = new ZipEntry(oPath.relativize(path).toString());
					try {
						oZipOutputStream.putNextEntry(zipEntry);
						Files.copy(path, oZipOutputStream);
						oZipOutputStream.closeEntry();
					} catch (IOException e) {
						WasdiLog.errorLog(m_sLoggerPrefix + "zip: Error during creation of zip archive " );
					}
				});
			}		
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
			boolean bIsFileDeleted = oCheckExists.delete();
			if (!bIsFileDeleted)
				WasdiLog.warnLog("ZipFileUtils.zipFiles: " + sZipFilePath + " already existed and it was not deleted");
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
					WasdiLog.errorLog("ZipFileUtils.zipFiles: Error during creation of zip archive " );
				}				
			}
		}
	}
	
	public static void zipFile(File oFileToZip, String sFileName, ZipOutputStream oZipOut) {
		zipFile(oFileToZip, sFileName, oZipOut, 1024);
	}

	//courtesy of https://www.baeldung.com/java-compress-and-uncompress
	public static void zipFile(File oFileToZip, String sFileName, ZipOutputStream oZipOut, int iBufferSize) {
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
				byte[] bytes = new byte[iBufferSize];
				int iLength;
				while ((iLength = oFis.read(bytes)) >= 0) {
					oZipOut.write(bytes, 0, iLength);
				}
				//				oFis.close();				
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("ZipFileUtils.zipFile: ", oE);
		}
	}

	/**
	 * Extract the content of a zip file, removing the initial file.
	 * @param oZipFile the zip file to be extracted
	 * @param sDestDir the destination directory where the content should be moved
	 * @throws Exception 
	 */
	public static void cleanUnzipFile(File oZipFile, File sDestDir) throws Exception {
		if (oZipFile == null) {
			WasdiLog.errorLog("ZipFileUtils.cleanUnzipFile: zipFile is null");
			return;
		} else if (!oZipFile.exists()) {
			WasdiLog.errorLog("ZipFileUtils.cleanUnzipFile: zipFile does not exist: " + oZipFile.getAbsolutePath());
		}

		if (sDestDir == null) {
			WasdiLog.errorLog("ZipFileUtils.cleanUnzipFile: destDir is null");
			return;
		} else if (!sDestDir.exists()) {
			WasdiLog.errorLog("ZipFileUtils.cleanUnzipFile: destDir does not exist: " + sDestDir.getAbsolutePath());
		}

		ZipFileUtils oZipExtractor = new ZipFileUtils();

		String sFilename = oZipFile.getAbsolutePath();
		String sPath = sDestDir.getAbsolutePath();
		oZipExtractor.unzip(sFilename, sPath);

		String sDirPath = completeDirPath(sDestDir.getAbsolutePath());
		String sFileZipPath = sDirPath + oZipFile.getName();

		String sUnzippedDirectoryPath = sDirPath + removeZipExtension(oZipFile.getName());

		if (fileExists(sUnzippedDirectoryPath)) {
			boolean filesMovedFlag = WasdiFileUtils.moveFile2(sUnzippedDirectoryPath, sDirPath);

			if (filesMovedFlag) {
				deleteFile(sUnzippedDirectoryPath);
				deleteFile(sFileZipPath);
			}
		}
	}

	public static void extractInnerZipFileAndCleanZipFile(File oZipFile, File oDestDir) throws Exception {
		if (oZipFile == null) {
			WasdiLog.errorLog("ZipFileUtils.extractInnerZipFileAndCleanZipFile: zipFile is null");
			return;
		} else if (!oZipFile.exists()) {
			WasdiLog.errorLog("ZipFileUtils.extractInnerZipFileAndCleanZipFile: zipFile does not exist: " + oZipFile.getAbsolutePath());
		}

		if (oDestDir == null) {
			WasdiLog.errorLog("ZipFileUtils.extractInnerZipFileAndCleanZipFile: destDir is null");
			return;
		} else if (!oDestDir.exists()) {
			WasdiLog.errorLog("ZipFileUtils.extractInnerZipFileAndCleanZipFile: destDir does not exist: " + oDestDir.getAbsolutePath());
		}

		ZipFileUtils oZipExtractor = new ZipFileUtils();

		String sFilename = oZipFile.getAbsolutePath();
		String sPath = oDestDir.getAbsolutePath();

		String sDirPath = completeDirPath(sPath);
		String sSimpleFilename = removeZipExtension(oZipFile.getName());
		String unzippedDirectoryPath = completeDirPath(sDirPath + sSimpleFilename);

		oZipExtractor.unzip(sFilename, unzippedDirectoryPath);

		String fileZipPath = sDirPath + oZipFile.getName();

		if (fileExists(unzippedDirectoryPath)) {
			boolean fileMovedFlag = moveFile(unzippedDirectoryPath + sSimpleFilename + ".tif", sDirPath);

			if (fileMovedFlag) {
				deleteFile(unzippedDirectoryPath);
				deleteFile(fileZipPath);
			}
		}
	}

	public static void fixZipFileInnerSafePath(String zipFilePath) throws Exception {
		if (zipFilePath == null) {
			WasdiLog.errorLog("ZipFileUtils.fixZipFileInnerSafePath: zipFilePath is null");
			return;
		}

		File zipFile = new File(zipFilePath);
		if (!zipFile.exists()) {
			WasdiLog.errorLog("ZipFileUtils.fixZipFileInnerSafePath: zipFile does not exist: " + zipFile.getAbsolutePath());
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
	
	/**
	 * Cheks if it is a Valid Zip File
	 * @param oFile
	 * @return
	 */
	 public static boolean isValidZipFile(final File oFile) {
		    ZipFile oZipfile = null;
		    try {
		        oZipfile = new ZipFile(oFile);
		        return true;
		    } catch (IOException e) {
		        return false;
		    } finally {
		        try {
		            if (oZipfile != null) {
		                oZipfile.close();
		                oZipfile = null;
		            }
		        } catch (IOException e) {
		        }
		    }
		}	

}
