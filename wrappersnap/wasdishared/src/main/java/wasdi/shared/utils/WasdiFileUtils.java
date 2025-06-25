/**
 * Created by Cristiano Nattero on 2019-02-12
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import wasdi.shared.config.PathsConfig;
import wasdi.shared.utils.log.WasdiLog;

/**
 * @author c.nattero
 *
 */
public class WasdiFileUtils {
	
	/**
	 * Static list of extensions associtated to a shape file
	 */
	static List<String> asShapeFileExtensions;
	/**
	 * Static list of extesions considered a valid doc
	 */
	static List<String> asDocumentFileExtensions;
	
	static {
		asShapeFileExtensions = new ArrayList<>(
				Arrays.asList(
						"shp",
						"shx",
						"dbf",
						"sbn",
						"sbx",
						"fbn",
						"fbx",
						"ain",
						"aih",
						"atx",
						"ixs",
						"mxs",
						"prj",
						"cpg"
				)
		);
		
		asDocumentFileExtensions = new ArrayList<>(
				Arrays.asList(
						"doc",
						"docx",
						"pdf",
						"txt",
						"log",
						"dot",
						"dotx",
						"rtf",
						"odt",
						"csv",
						"htm",
						"html",
						"md"
				)
		);
		
	}

	/**
	 * Get the file name without the extension and the trailing dots
	 * @param sInputFile the initial name of the file
	 * @return the cleaned name of the file
	 */
	public static String getFileNameWithoutExtensionsAndTrailingDots(String sInputFile) {
		if(Utils.isNullOrEmpty(sInputFile)) {
			WasdiLog.debugLog("Utils.GetFileNameExtension: input null or empty");
			return sInputFile;
		}
		
		String sReturn = sInputFile;
			
		//remove trailing dots: filename....
		while(sReturn.endsWith(".")) {
			sReturn = sReturn.replaceAll("\\.$", "");
		}
		//remove two-letters, e.g., .gz, .7z
		sReturn = sReturn.replaceAll("\\...$", "");			
		//remove three-letters, e.g., .zip, .tar, .rar....
		sReturn = sReturn.replaceAll("\\....", "");
			
		//again, remove trailing dots: filename...zip
		while(sReturn.endsWith(".")) {
			sReturn = sReturn.replaceAll("\\.$", "");
		}
		return sReturn;
	}


	/**
	 * Utilities method that fix non homogeneous path separators in a 
	 * String representing a PATH. Using different file separators 
	 * could lead to errors in path specifications. In particular 
	 * Windows-based systems use the char '\' as file separator and
	 * Unix Based systems use the char '/'.
	 * With this method the system file separator is used and its 
	 * Initialisation is done by the JVM
	 * @param sPathString
	 * @return
	 */
	public static String fixPathSeparator(String sPathString) {
		return sPathString.replace("/",File.separator).replace("\\",File.separator);
	}

	/**
	 * Get the name of the zip file without the .zip extension.
	 * @param sProductName the name of the zip file
	 * @return the name without the zip extension
	 */
	public static String removeZipExtension(String sProductName) {
		if (sProductName == null || !sProductName.endsWith(".zip")) {
			return sProductName;
		} else {
			return sProductName.replace(".zip", "");
		}
	}

	/**
	 * Check if the file exists.
	 * @param oFile the file
	 * @return true if the file exists, false otherwise.
	 */
	public static boolean fileExists(File oFile) {
		if (oFile == null) {
			WasdiLog.errorLog("WasdiFileUtils.doesFileExist: file is null");
			return false;
		}

		return oFile != null && oFile.exists();
	}

	/**
	 * Check if the filePath corresponds to an existing file.
	 * @param sFileFullPath the path of the file
	 * @return true if the file exists, false otherwise
	 */
	public static boolean fileExists(String sFileFullPath) {
		if (sFileFullPath == null) {
			WasdiLog.errorLog("WasdiFileUtils.doesFileExist: filePath is null");
			return false;
		}

		File oFile = new File(sFileFullPath);

		return fileExists(oFile);
	}

	/**
	 * Compare two files to see if they are the same.
	 * @param oFile1 the first file
	 * @param oFile2 the second file
	 * @return true if the files are the same, false otherwise
	 */
	public static boolean filesAreTheSame(File oFile1, File oFile2) {
		if (!fileExists(oFile1)) {
			WasdiLog.debugLog("WasdiLog.debugLog | WasdiFileUtils.filesAreTheSame: file1 does not exist");
			return false;
		}

		if (!fileExists(oFile2)) {
			WasdiLog.debugLog("WasdiLog.debugLog | WasdiFileUtils.filesAreTheSame: file2 does not exist");
			return false;
		}

		try {
			long lFile1Checksum = FileUtils.checksumCRC32(oFile1);
			long lFile2Checksum = FileUtils.checksumCRC32(oFile2);
			return lFile1Checksum == lFile2Checksum;
			
		} catch (IOException e) {
			WasdiLog.errorLog("WasdiLog.errorLog | WasdiFileUtils.fileToText: cannot compare files: " + e.getMessage());

			return false;
		}
	}

	/**
	 * Compare two files to see if they are the same.
	 * @param sFile1FullPath the path of the first file
	 * @param sFile2FullPath the path of the second file
	 * @return true if the files are the same, false otherwise
	 */
	public static boolean filesAreTheSame(String sFile1FullPath, String sFile2FullPath) {
		if (Utils.isNullOrEmpty(sFile1FullPath)) {
			return false;
		}

		if (Utils.isNullOrEmpty(sFile2FullPath)) {
			return false;
		}

		File oFile1 = new File(sFile1FullPath);
		File oFile2 = new File(sFile2FullPath);

		return filesAreTheSame(oFile1, oFile2);
	}

	/**
	 * Create the directory structure in case it does not already exit.
	 * @param sPathname the full path of the directory
	 */
	public static void createDirectoryIfDoesNotExist(String sPathname) {
		File oPath = new File(sPathname);

		if (!oPath.exists()) {
			oPath.mkdirs();
		}
	}

	/**
	 * Write input-stream to file
	 * @param oInputStream the input-stream to be written
	 * @param oFile the file to be written
	 * @throws FileNotFoundException in case of any issues with the file
	 * @throws IOException if an I/O error occurs
	 */
	public static void writeFile(InputStream oInputStream, File oFile) throws FileNotFoundException, IOException {
		int iRead = 0;
		byte[] ayBytes = new byte[1024];

		try (OutputStream oOutStream = new FileOutputStream(oFile)) {
			while ((iRead = oInputStream.read(ayBytes)) != -1) {
				oOutStream.write(ayBytes, 0, iRead);
			}
			oOutStream.flush();
			oOutStream.close();
		}
		
	}
	
	public static boolean writeFile(String sContent, File oFile) throws FileNotFoundException, IOException {
		return writeFile(sContent, oFile, false);
	}

	public static boolean writeFile(String sContent, File oFile, boolean bAppend) throws FileNotFoundException, IOException {

		if (sContent == null) {
			WasdiLog.errorLog("WasdiFileUtils.writeFile: sContent is null");
			return false;
		}

		File oParentDirectory = oFile.getParentFile();

		if (fileExists(oFile)) {
			deleteFile(oFile.getAbsolutePath());
		} else if (!oParentDirectory.exists()) {
			oParentDirectory.mkdirs();
		}

		try (OutputStream oOutStream = new FileOutputStream(oFile, bAppend)) {
			byte[] ayBytes = sContent.getBytes();
			oOutStream.write(ayBytes);
		}

		if (fileExists(oFile)) {
			return true;
		} 
		else {
			return false;
		}
	}

	public static boolean writeFile(String sContent, String sFileFullPath) throws FileNotFoundException, IOException {

		if (sContent == null) {
			WasdiLog.errorLog("WasdiFileUtils.writeFile: sContent is null");

			return false;
		}

		if (Utils.isNullOrEmpty(sFileFullPath)) {
			WasdiLog.errorLog("WasdiFileUtils.writeFile: sFileFullPath is null");

			return false;
		}

		File oFile = new File(sFileFullPath);

		return writeFile(sContent, oFile);
	}
	
	/**
	 * Move a file to a destination directory.
	 * @param sSourcePath the path of the file to be moved
	 * @param sDestinationDirectoryPath the path of the destination directory
	 * @return true if the operation was successful, false otherwise
	 */
	public static boolean moveFile(String sSourcePath, String sDestinationDirectoryPath) {
		if (sSourcePath == null) {
			WasdiLog.errorLog("WasdiFileUtils.moveFile: sourcePath is null");
			return false;
		}

		if (sDestinationDirectoryPath == null) {
			WasdiLog.errorLog("WasdiFileUtils.moveFile: destinationDirectoryPath is null");
			return false;
		}

		if (!sDestinationDirectoryPath.endsWith(File.separator)) {
			sDestinationDirectoryPath += File.separator;
		}

		File oSourceFile = new File(sSourcePath);
		if (!fileExists(oSourceFile)) {
			WasdiLog.errorLog("WasdiFileUtils.moveFile: sourceFile does not exist");
			return false;
		}

		File oDestinationDirectory = new File(sDestinationDirectoryPath);
		if (!oDestinationDirectory.exists()) {
			oDestinationDirectory.mkdirs();
		}

		boolean bOutcome = true;

		if (oSourceFile.isDirectory()) {
			for (File oFile : oSourceFile.listFiles()) {
				bOutcome = bOutcome & moveFile(oFile.getAbsolutePath(), sDestinationDirectoryPath+ oSourceFile.getName());
			}

			if (oSourceFile.listFiles().length == 0) {
				deleteFile(sSourcePath);
			}
		} else {
			File oDestinationFile = new File(sDestinationDirectoryPath + oSourceFile.getName());
			bOutcome = oSourceFile.renameTo(oDestinationFile);
		}

		return bOutcome;
	}
	
	
	/**
	 * Move a file to a destination directory.
	 * @param sSourcePath the path of the file to be moved
	 * @param sDestinationDirectoryPath the path of the destination directory
	 * @return true if the operation was successful, false otherwise
	 */
	public static boolean moveFile2(String sSourcePath, String sDestinationDirectoryPath) {
		if (sSourcePath == null) {
			WasdiLog.errorLog("WasdiFileUtils.moveFile: sourcePath is null");
			return false;
		}

		if (sDestinationDirectoryPath == null) {
			WasdiLog.errorLog("WasdiFileUtils.moveFile: destinationDirectoryPath is null");
			return false;
		}

		if (!sDestinationDirectoryPath.endsWith(File.separator)) {
			sDestinationDirectoryPath += File.separator;
		}

		File oSourceFile = new File(sSourcePath);
		if (!fileExists(oSourceFile)) {
			WasdiLog.errorLog("WasdiFileUtils.moveFile: sourceFile does not exist");
			return false;
		}

		File oDestinationDirectory = new File(sDestinationDirectoryPath);
		if (!oDestinationDirectory.exists()) {
			oDestinationDirectory.mkdirs();
		}

		boolean bOutcome = true;

		if (oSourceFile.isDirectory()) {
			for (File oFile : oSourceFile.listFiles()) {
				bOutcome = bOutcome & moveFile2(oFile.getAbsolutePath(), sDestinationDirectoryPath);
			}

			if (oSourceFile.listFiles().length == 0) {
				deleteFile(sSourcePath);
			}
		} else {
			File oDestinationFile = new File(sDestinationDirectoryPath + oSourceFile.getName());
			bOutcome = oSourceFile.renameTo(oDestinationFile);
		}

		return bOutcome;
	}

	public static String renameFile(String sOldFileFullName, String sNewFileSimpleName) {
		if (sOldFileFullName == null) {
			WasdiLog.errorLog("WasdiFileUtils.renameFile: sSourceAbsoluteFullName is null");
			return null;
		}

		if (sNewFileSimpleName == null) {
			WasdiLog.errorLog("WasdiFileUtils.renameFile: sNewFileName is null");
			return null;
		}

		File oSourceFile = new File(sOldFileFullName);
		if (!fileExists(oSourceFile)) {
			WasdiLog.errorLog("WasdiFileUtils.renameFile: sourceFile does not exist");
			return null;
		}

		File oNewFile = new File(oSourceFile.getParent(), sNewFileSimpleName);
		boolean bIsFileRenamed = oSourceFile.renameTo(oNewFile);
		
		if (!bIsFileRenamed)
			WasdiLog.warnLog("WasdiFileUtils.renameFile: the file was not renamed");

		return oNewFile.getAbsolutePath();
	}
	
	/**
	 * Delete a file from the filesystem. If the file is a directory, also delete the child directories and files.
	 * @param sFileFullPath the absolute path of the file
	 * @return true if the file was deleted, false otherwise
	 */
	public static boolean deleteFile(String sFileFullPath) {
		try {
			
			if (sFileFullPath == null) {
				WasdiLog.errorLog("WasdiFileUtils.deleteFile: filePath is null");
				return false;
			} else if (!fileExists(sFileFullPath)) {
				WasdiLog.errorLog("WasdiFileUtils.deleteFile: file does not exist: " + sFileFullPath);
				return false;
			}

			File oFile = new File(sFileFullPath);

			if (oFile.isDirectory()) {
				for (File child : oFile.listFiles()) {
					deleteFile(child.getPath());
				}
			}

			return oFile.delete();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WasdiFileUtils.deleteFile: error: ", oEx);
			return false;
		}
	}

	/**
	 * Get the complete directory path. Basically, add a trailing slash if it is missing.
	 * 
	 * @param sDirPath the directory path
	 * @return the complete path of the directory, or null if the dirPath is null
	 */
	public static String completeDirPath(String sDirPath) {
		if (sDirPath == null || sDirPath.endsWith("/")) {
			return sDirPath;
		}

		return sDirPath + "/";
	}

	/**
	 * Read the content of a text file.
	 * @param sFilePath the file to be read
	 * @return the text content of the file
	 */
	public static String fileToText(String sFilePath) {
		if (sFilePath == null) {
			WasdiLog.errorLog("WasdiFileUtils.fileToText: filePath is null");
			return null;
		}

		File file = new File(sFilePath);
		if (!fileExists(file)) {
			WasdiLog.errorLog("WasdiFileUtils.fileToText: file does not exist");
			return null;
		}

		try {
			return FileUtils.readFileToString(file,StandardCharsets.UTF_8.name());
		} catch (IOException e) {
			WasdiLog.errorLog("WasdiFileUtils.fileToText: cannot read file");
			return null;
		}
	}

	/**
	 * Check if a file is a help-file.
	 * More exactly, checks if the file-name is "readme" or "help" and if the extension is "md" or "txt".
	 * @param oFile the file
	 * @return true if the file is a help file, false otherwise
	 */
	public static boolean isHelpFile(File oFile) {
		if (!fileExists(oFile)) {
			WasdiLog.errorLog("WasdiFileUtils.isHelpFile: file is null");
			return false;
		}

		return isHelpFile(oFile.getName());
	}

	/**
	 * Check if a file is a help-file.
	 * More exactly, checks if the file-name is "readme" or "help" and if the extension is "md" or "txt".
	 * @param sFileName the name of the file
	 * @return true if the file is a help file, false otherwise
	 */
	public static boolean isHelpFile(String sFileName) {
		if (sFileName == null) {
			WasdiLog.errorLog("WasdiFileUtils.isHelpFile: fileName is null");
			return false;
		}

		String[] asTokens = sFileName.split("\\.(?=[^\\.]+$)");
		if (asTokens.length != 2) {
			WasdiLog.debugLog("WasdiFileUtils.isHelpFile: " + sFileName + " is not a help file-name");
			return false;
		}

		String sName = asTokens[0];
		String sExtension = asTokens[1];

		return (sName.equalsIgnoreCase("readme") || sName.equalsIgnoreCase("help"))
				&& (sExtension.equalsIgnoreCase("md") || sExtension.equalsIgnoreCase("txt"));
	}

	/**
	 * Check if a file is a PackagesInfo-file.
	 * More exactly, checks if the file-name is "packagesInfo" and if the extension is "json".
	 * @param oFile the file
	 * @return true if the file is a PackagesInfo file, false otherwise
	 */
	public static boolean isPackagesInfoFile(File oFile) {
		if (!fileExists(oFile)) {
			WasdiLog.errorLog("WasdiFileUtils.isPackagesInfoFile: file is null");
			return false;
		}

		return isPackagesInfoFile(oFile.getName());
	}

	/**
	 * Check if a file is a PackagesInfo-file.
	 * More exactly, checks if the file-name is "packagesInfo" and if the extension is "json".
	 * @param sFileName the name of the file
	 * @return true if the file is a PackagesInfo file, false otherwise
	 */
	public static boolean isPackagesInfoFile(String sFileName) {
		if (sFileName == null) {
			WasdiLog.errorLog("WasdiFileUtils.isPackagesInfoFile: fileName is null");
			return false;
		}

		String[] asTokens = sFileName.split("\\.(?=[^\\.]+$)");
		if (asTokens.length != 2) {
			WasdiLog.debugLog("WasdiFileUtils.isPackagesInfoFile: " + sFileName + " is not a packagesInfo file-name");
			return false;
		}

		String sName = asTokens[0];
		String sExtension = asTokens[1];

		return (sName.equalsIgnoreCase("packagesInfo"))
				&& (sExtension.equalsIgnoreCase("json"));
	}
		
	public static boolean isShapeFile(String sFileName) {
		try {
			if(Utils.isNullOrEmpty(sFileName)) {
				return false;
			}
			String sLo = sFileName.toLowerCase(); 
			for (String sExtension : asShapeFileExtensions) {
				if(sLo.endsWith("."+sExtension)) {
					return true;
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isShapeFile( String ): " + oE);
		}
		return false;
	}
	
	public static boolean isShapeFile(File oFile) {
		try {
			if(null==oFile) {
				return false;
			}
			return isShapeFile(oFile.getName());
		} 
		catch (Exception oE) {
			WasdiLog.errorLog("WasdiFileUtils.isShapeFile( File ): ", oE);
		}
		return false;
	}
	
	
	/**
	 * Check if a file is a (presumed) Shape File: it checks if it contains a .shp file
	 * It also checks that the total number of files inside the zip itself is 
	 * limited, to avoid processor cycle waste. 
	 * The limit imposed it 
	 * @param sZipFile Full path of the zip file
	 * @param iMaxFileInZipFile the maximum number of file allowed to be considered inside the zip file
	 * @return True if the zip contains a .shp file, False if it's not contained and the value iMaxFileInZipFile is exceeded
	 */
	public static boolean isShapeFileZipped(String sZipFile, int iMaxFileInZipFile) {
		int iFileCounter = 0;
		
		if (Utils.isNullOrEmpty(sZipFile)) return false;
		
		if (!sZipFile.toLowerCase().endsWith(".zip")) return false;
		
		Path oZipPath = Paths.get(sZipFile).toAbsolutePath().normalize();
		if(!oZipPath.toFile().exists()) {
			return false;
		}
		try (ZipFile oZipFile = new ZipFile(oZipPath.toString())){
		
			Enumeration<? extends ZipEntry> aoEntries = oZipFile.entries();
			
			while(aoEntries.hasMoreElements()) {
				ZipEntry oZipEntry = aoEntries.nextElement();
				
				if (iFileCounter > iMaxFileInZipFile) {
					WasdiLog.warnLog("WasdiFileUtils.isShapeFileZipped: too many files inside the zip. The limit is " + iMaxFileInZipFile);
					return false;
				}
				
				if (WasdiFileUtils.isShapeFile(oZipEntry.getName())) {
					return true;
				}
				iFileCounter++;
			}			
			
		}
		catch (ZipException oZipException) {
			WasdiLog.debugLog("WasdiFileUtils.isShapeFileZipped: Zip Error, this is not a zip file likely " + oZipException.toString());
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WasdiFileUtils.isShapeFileZipped: error", oEx);
		}
		
		return false;
	}

	/**
	 * Check if a file is a (presumed) Shape File: it checks if it contains a .shp file
	 * @param sZipFile Full path of the zip file
	 * @param iMaxFileInZipFile TODO
	 * @return True if the zip contains a .shp file, False otherwise
	 */
	public static String getShpFileNameFromZipFile(String sZipFile, int iMaxFileInZipFile) {
		Path oZipPath = Paths.get(sZipFile).toAbsolutePath().normalize(); 
		if(!oZipPath.toFile().exists()) {
			return "";
		}
		
		int iFileCounter = 0;
		try(ZipFile oZipFile = new ZipFile(oZipPath.toString())) {
			Enumeration<? extends ZipEntry> aoEntries = oZipFile.entries();
			
			while(aoEntries.hasMoreElements()) {
				ZipEntry oZipEntry = aoEntries.nextElement();
				if (iFileCounter > iMaxFileInZipFile) {
					WasdiLog.errorLog("WasdiFileUtils.isShapeFileZipped: too many files inside the zip. The limit is " + iMaxFileInZipFile);
					return "";
				}
				
				if (oZipEntry.getName().toLowerCase().endsWith(".shp")) {
					return oZipEntry.getName();
				}
				iFileCounter++;
			}			
			
		} catch (Exception e) {
			WasdiLog.errorLog("WasdiFileUtils.getShpFileNameFromZipFile: error", e);
		}
		
		return "";
	}

	
	/***
	 * Check if a file is a "classic" image type.
	 * The images accepted are "jpg", "png", "svg" at the moment.
	 * 
	 * @param oFile
	 * @return
	 */
	public static boolean isImageFile(File oFile) {
		try {
			String sFileName = oFile.getName();
			String sExt = FilenameUtils.getExtension(sFileName);

			if(ImageResourceUtils.isValidExtension(sExt)){
				return true;
			}					
		}
		catch (Exception oE) {
			WasdiLog.errorLog("WasdiFileUtils.isImageFile exception: ", oE);
		}

		return false;
	}
	
	/**
	 * Check if the file is some form of document.
	 * We support at the moment doc, docx, pdf, txt, log
	 * @param oFile
	 * @return
	 */
	public static boolean isDocumentFormatFile(File oFile) {
		try {
			String sFileName = oFile.getName();
			String sExt = FilenameUtils.getExtension(sFileName);
			sExt = sExt.toLowerCase();
			
			for (String sExtension : asDocumentFileExtensions) {
				if(sExt.equals(sExtension)) {
					return true;
				}
			}
		}
		catch (Exception oE) {
			WasdiLog.errorLog("WasdiFileUtils.isDocumentFormatFile exception: ", oE);
		}

		return false;
	}	
	
	/**
	 * Load the log4j2 configuration file for the logger, looking for it first in the parameters passed to the JVM. If the parameter
	 * was not set, then it looks for file in the folder of the current jar being executed
	 * @param sCurrentJarPath the path of the current jar being executed
	 */
	public static void loadLogConfigFile(String sCurrentJarDirectory) {
        String sLogConfigFilePath = null;
        String sPropertyValue = System.getProperty("log4j2.configurationFile");

        if (Utils.isNullOrEmpty(sPropertyValue) || !fileExists(sPropertyValue)) {
        	
        	sLogConfigFilePath = sCurrentJarDirectory + "/log4j2.xml";
        	
        	LoggerContext oContext = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false); 
            File oLogConfigFile = new File(sLogConfigFilePath);
            oContext.setConfigLocation(oLogConfigFile.toURI());
	    }
	}
	
	/**
	 * This method removes the last extension from a filename 
	 * @param sInputFile the name of the input file
	 * @return
	 */
	public static String getFileNameWithoutLastExtension(String sInputFile) {
		File oFile = new File(sInputFile);
		String sInputFileNameOnly = oFile.getName();
		String sReturn = sInputFileNameOnly;
		
		if(sInputFileNameOnly.contains(".")) {
			sReturn = sInputFileNameOnly.substring(0, sInputFileNameOnly.lastIndexOf('.'));
		}

		return sReturn;
	}

	/**
	 * Extracts the extension fro a file name
	 * @param sInputFile Input File Name
	 * @return Extension or empty string
	 */
	public static String getFileNameExtension(String sInputFile) {
		String sReturn = "";
		File oFile = new File(sInputFile);
		String sInputFileNameOnly = oFile.getName();

		if (sInputFileNameOnly.contains(".")) {
			// Create a clean layer id: the file name without any extension
			String[] asLayerIdSplit = sInputFileNameOnly.split("\\.");
			if (asLayerIdSplit != null && asLayerIdSplit.length > 0) {
				sReturn = asLayerIdSplit[asLayerIdSplit.length - 1];
			}			
		}

		return sReturn;
	}

	public static void fixUpPermissions(Path destPath) throws IOException {
		Stream<Path> files = Files.list(destPath);
		files.forEach(path -> {
			if (Files.isDirectory(path)) {
				try {
					fixUpPermissions(path);
				} catch (IOException oEx) {
					WasdiLog.errorLog("Utils.fixUpPermissions: error", oEx);

				}
			} else {
				setExecutablePermissions(path);
			}
		});
		files.close();
	}

	private static void setExecutablePermissions(Path executablePathName) {
		if (SystemUtils.IS_OS_UNIX) {
			Set<PosixFilePermission> permissions = new HashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ,
					PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ,
					PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ,
					PosixFilePermission.OTHERS_EXECUTE));
			try {
				Files.setPosixFilePermissions(executablePathName, permissions);
			} catch (IOException oEx) {
				WasdiLog.errorLog("Utils.setExecutablePermissions: error", oEx);
			}
		}
	}
	
    
    /**
     * Compute the size of the folder representing a workspace on the file system
     * @return the size of the folder in bytes
     */
    public static long getWorkspaceFolderSize(String sUserId, String sWorkspaceId) {
    	WasdiLog.infoLog("WasdiFileUtils.getWorkspaceFolderSize. (userId: " + sUserId + ", workspaceId: " + sWorkspaceId + ")");

    	long lWorkspaceSize = 0L;
    	
    	if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sWorkspaceId)) {
    		return lWorkspaceSize;
    	}
		
    	try {
	    	String sWorkspacePath = PathsConfig.getWorkspacePath(sUserId, sWorkspaceId);
	    	WasdiLog.infoLog("WasdiFileUtils.getWorkspaceFolderSize. Path to workspace: " + sWorkspacePath);
	        File oWorkspaceDir = new File(sWorkspacePath);
	        
	        if (oWorkspaceDir.exists()) {
	        	lWorkspaceSize = FileUtils.sizeOfDirectory(oWorkspaceDir);
	        	WasdiLog.infoLog("WasdiFileUtils.getWorkspaceFolderSize. Workspace exists and has size: " + lWorkspaceSize);
	        	return lWorkspaceSize;
	        }
	        else {
	        	WasdiLog.infoLog("WasdiFileUtils.getWorkspaceFolderSize. Workspace does NOT exist");
	        }
	        
    	} catch (Exception oEx) {
    		WasdiLog.errorLog("WasdiFileUtils.getWorkspaceFolderSize. Error computing workspace size", oEx);
    	}
    	
        return lWorkspaceSize;
    	
    }
	
}
