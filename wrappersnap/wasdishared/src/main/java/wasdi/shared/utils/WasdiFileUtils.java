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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.base.Preconditions;

import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.log.WasdiLog;

/**
 * @author c.nattero
 *
 */
public class WasdiFileUtils {
	
	static List<String> asShapeFileExtensions;
	static{
		
		//populate shapefiles extensions as found here:
		// https://desktop.arcgis.com/en/arcmap/latest/manage-data/shapefiles/shapefile-file-extensions.htm
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
//						"xml", //commented out since it is a too common extension
						"cpg"
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
	 * Load the JSON content of a file.
	 * @param sFileFullPath the full path of the file
	 * @return the JSONObject that contains the payload
	 */
	public static JSONObject loadJsonFromFile(String sFileFullPath) {
		Preconditions.checkNotNull(sFileFullPath);

		JSONObject oJson = null;
		try(FileReader oReader = new FileReader(sFileFullPath);){
			
			JSONTokener oTokener = new JSONTokener(oReader);
			oJson = new JSONObject(oTokener);
		} catch (FileNotFoundException oFnf) {
			WasdiLog.errorLog("WasdiFileUtils.loadJsonFromFile: file " + sFileFullPath + " was not found: " + oFnf);
		} catch (Exception oE) {
			WasdiLog.errorLog("WasdiFileUtils.loadJsonFromFile: " + oE);
		}
		return oJson;
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
	 * @param fileInputStream the input-stream to be written
	 * @param oFile the file to be written
	 * @throws FileNotFoundException in case of any issues with the file
	 * @throws IOException if an I/O error occurs
	 */
	public static void writeFile(InputStream fileInputStream, File oFile) throws FileNotFoundException, IOException {
		int iRead = 0;
		byte[] ayBytes = new byte[1024];

		try (OutputStream oOutStream = new FileOutputStream(oFile)) {
			while ((iRead = fileInputStream.read(ayBytes)) != -1) {
				oOutStream.write(ayBytes, 0, iRead);
			}
			oOutStream.flush();
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

	public static boolean writeMapAsJsonFile(Map<String, Object> aoJSONMap, String sFileFullPath) throws FileNotFoundException, IOException {

		if (aoJSONMap == null) {
			WasdiLog.errorLog("WasdiFileUtils.writeMapAsJsonFile: aoJSONMap is null");

			return false;
		}

		if (Utils.isNullOrEmpty(sFileFullPath)) {
			WasdiLog.errorLog("WasdiFileUtils.writeMapAsJsonFile: sFileFullPath is null");

			return false;
		}

		String sJson = JsonUtils.stringify(aoJSONMap);

		return writeFile(sJson, sFileFullPath);
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
			WasdiLog.errorLog("WasdiFileUtils.isHelpFile: " + sFileName + " is not a help file-name");
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
	
	public static List<String> getShapefileExtensions(){
		return null;
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
			WasdiLog.errorLog("WasdiFileUtils.isImageFile( File ): ", oE);
		}

		return false;
	}	
	
	public static boolean isSentinel5PFile(File oFile) {
		try {
			if(null==oFile) {
				return false;
			}
			if (oFile.getName().toLowerCase().startsWith("s5p") && ! (oFile.getName().toLowerCase().endsWith(".tif")|| oFile.getName().toLowerCase().endsWith(".tiff"))) {
				return true;
			}
			else {
				return false;
			}
			
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel5PFile( File ): " + oE);
		}
		return false;
	}

	public static boolean isGpmZipFile(File oFile) {
		try {
			if (null == oFile) {
				return false;
			}

			if ((oFile.getName().toUpperCase().startsWith("3B-") || oFile.getName().toUpperCase().contains("IMERG"))
					&& oFile.getName().toLowerCase().endsWith(".zip")) {
				return true;
			} else {
				return false;
			}
			
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isGpmZipFile( File ): " + oE);
		}

		return false;
	}

	private static boolean isSentinel3ZippedFile(String sName) {
		try {
			if(Utils.isNullOrEmpty(sName)) {
				return false;
			}
			if(sName.toLowerCase().startsWith("s3") && sName.toLowerCase().endsWith(".zip")){
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel3File( String): " + oE);
		}
		return false;
	}
	
	public static boolean isSentinel3ZippedFile(File oFile) {
		try {
			if(null==oFile) {
				return false;
			}
			return isSentinel3ZippedFile(oFile.getName());
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel3File( File ): " + oE);
		}
		return false;
	}

	private static boolean isSentinel3Name(String sName) {
		try {
			if(Utils.isNullOrEmpty(sName)) {
				return false;
			}
			if(sName.toLowerCase().startsWith("s3") && ! (sName.toLowerCase().endsWith(".tif") || sName.toLowerCase().endsWith(".tiff") || sName.toLowerCase().endsWith(".shp"))  ){
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel3File( String): " + oE);
		}
		return false;
	}
	
	public static boolean isSentinel3Name(File oFile) {
		try {
			if(null==oFile) {
				return false;
			}
			return isSentinel3Name(oFile.getName());
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel3File( File ): " + oE);
		}
		return false;
	}
	
	private static boolean isSentinel3Directory(String sName) {
		try {
			if(Utils.isNullOrEmpty(sName)) {
				return false;
			}
			if(sName.toLowerCase().startsWith("s3") && sName.toLowerCase().endsWith(".sen3")){
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel3File( String): " + oE);
		}
		return false;
	}
	
	public static boolean isSentinel3Directory(File oFile) {
		try {
			if(null==oFile) {
				return false;
			}
			return isSentinel3Directory(oFile.getName());
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel3File( File ): " + oE);
		}
		return false;
	}
	
	/**
	 * Get the Platform code of the mission starting from the file Name
	 * @param sFileName File Name to investigate
	 * @return Code of the Platform as definied in the Platforms class. Null if not recognized
	 */
	public static String getPlatformFromSatelliteImageFileName(String sFileName) {
		try {
			if (Utils.isNullOrEmpty(sFileName)) return null;

			if (sFileName.toUpperCase().startsWith("S1A_") || sFileName.toUpperCase().startsWith("S1B_")) {
				return Platforms.SENTINEL1;
			}
			else if (sFileName.toUpperCase().startsWith("S2A_") || sFileName.toUpperCase().startsWith("S2B_")) {
				return Platforms.SENTINEL2;
			}
			else if (sFileName.toUpperCase().startsWith("S3A_") || sFileName.toUpperCase().startsWith("S3B_") || sFileName.toUpperCase().startsWith("S3__")) {
				return Platforms.SENTINEL3;
			}
			else if (sFileName.toUpperCase().startsWith("S5P_")) {
				return Platforms.SENTINEL5P;
			}
			else if (sFileName.toUpperCase().startsWith("LC08_")) {
				return Platforms.LANDSAT8;
			}
			else if (sFileName.toUpperCase().startsWith("MER_") || sFileName.toUpperCase().startsWith("ASA_")) {
				return Platforms.ENVISAT;
			}
			else if (sFileName.toUpperCase().startsWith("RIVER-FLD")) {
				return Platforms.VIIRS;
			}
			else if (sFileName.toUpperCase().startsWith("PROBAV_")) {
				return Platforms.PROBAV;
			}
			else if (sFileName.toUpperCase().startsWith("ERA5_")) {
				return Platforms.ERA5;
			}
			else if (sFileName.toUpperCase().startsWith("CAMS_")) {
				return Platforms.CAMS;
			}
			else if (sFileName.toUpperCase().startsWith("PLANET_")) {
				return Platforms.PLANET;
			}
			else if (sFileName.toUpperCase().startsWith("COPERNICUS_DSM_COG_")) {
				return Platforms.DEM;
			}
			else if (sFileName.toUpperCase().startsWith("ESA_WORLDCOVER")) {
				return Platforms.WORLD_COVER;
			}
			else if (sFileName.toUpperCase().startsWith("WASDI_STATIC_")) {
				return Platforms.STATICS;
			}
			else if (sFileName.toUpperCase().startsWith("3B-") || sFileName.toUpperCase().contains("IMERG")) {
				return Platforms.IMERG;
			}
			else if (sFileName.toUpperCase().startsWith("EEHCM")
					|| sFileName.toUpperCase().startsWith("EEHSEBS")
					|| sFileName.toUpperCase().startsWith("EEHSTIC")
					|| sFileName.toUpperCase().startsWith("EEHSW")
					|| sFileName.toUpperCase().startsWith("EEHTES")
					|| sFileName.toUpperCase().startsWith("EEHTSEB")
					|| sFileName.toUpperCase().startsWith("ECOSTRESS")) {
				return Platforms.ECOSTRESS;
			}
			else if (sFileName.toLowerCase().endsWith(".nc")) {
				return Platforms.CM;
			}
			else if (sFileName.toUpperCase().startsWith("SKYWATCH_")) {
				return Platforms.EARTHCACHE;
			} 
			else if (sFileName.toUpperCase().startsWith("MOD11A2")) {
				return Platforms.TERRA;
			}
			else if (sFileName.toUpperCase().startsWith("GHS_BUILT_S_E2018_GLOBE_R2023A_54009_10_V1_0_")) {
				return Platforms.JRC_GHSL;
			}
			
			return null;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("WasdiFileUtils.getPlatformFromFileName: exception " + oEx.toString());
		}
		
		return null;
	}
	
	/**
	 * Get the reference date of a Satellite Image from the file Name
	 * If not available, not relevant or in case of error returns "now".
	 * @param sFileName Name of the Satellite Image File
	 * @return Reference Date 
	 */
	public static Date getDateFromSatelliteImageFileName(String sFileName) {
		
		try {
			String sPlatform = getPlatformFromSatelliteImageFileName(sFileName);
			if (Utils.isNullOrEmpty(sPlatform)) return new Date();
			
			if (sPlatform.equals(Platforms.SENTINEL1)) {
				sFileName = sFileName.replace("__", "_");
				String [] asS1Parts = sFileName.split("_");
				String sDate = asS1Parts[4];
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd'T'HHmmss");
				return new Date(lTime);
			}
			else if (sPlatform.equals(Platforms.SENTINEL2)) {
				String [] asS2Parts = sFileName.split("_");
				String sDate = asS2Parts[2];
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd'T'HHmmss");
				return new Date(lTime);				
			}
			else if (sPlatform.equals(Platforms.SENTINEL3)) {
				String sDate = sFileName.substring(16,31);
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd'T'HHmmss");
				return new Date(lTime);
			}
			else if (sPlatform.equals(Platforms.SENTINEL5P)) {
				String sDate = sFileName.substring(20, 20+15);
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd'T'HHmmss");
				return new Date(lTime);
			}
			else if (sPlatform.equals(Platforms.ENVISAT)) {
				String sDate = sFileName.substring(14, 14+8);
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd");
				return new Date(lTime);
			}
			else if (sPlatform.equals(Platforms.LANDSAT8)) {
				String [] asL8Parts = sFileName.split("_");
				String sDate = asL8Parts[3];
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd");
				return new Date(lTime);				
			}
			else if (sPlatform.equals(Platforms.VIIRS)) {
				String [] asViirsParts = sFileName.split("_");
				String sDate = asViirsParts[1];
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd");
				return new Date(lTime);				
			}			
			
			// For CMEMS, ERA5 are Not relevant 
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("WasdiFileUtils.getDateFromFileName: exception " + oEx.toString());
		}
		
		return new Date();
	}
	
	
	
	/**
	 * Get the Product Type of a Satellite Image from the file Name
	 * If not available, not relevant or in case of error returns "".
	 * @param sFileName Name of the Satellite Image File
	 * @return Product Type, or ""  
	 */
	public static String getProductTypeSatelliteImageFileName(String sFileName) {
		
		try {
			String sPlatform = getPlatformFromSatelliteImageFileName(sFileName);
			if (Utils.isNullOrEmpty(sPlatform)) return "";
			
			if (sPlatform.equals(Platforms.SENTINEL1)) {
				String [] asS1Parts = sFileName.split("_");
				String sType = asS1Parts[2];
				return sType.substring(0,3);
			}
			else if (sPlatform.equals(Platforms.SENTINEL2)) {
				String [] asS2Parts = sFileName.split("_");
				String sType = asS2Parts[1];
				return sType;				
			}
			else if (sPlatform.equals(Platforms.SENTINEL3)) {
				String sType = sFileName.substring(9,9+6);
				return sType;
			}
			else if (sPlatform.equals(Platforms.SENTINEL5P)) {
				String sType = sFileName.substring(9, 9+10);
				return sType;
			}

			// For Others are Not relevant 
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("WasdiFileUtils.getDateFromFileName: exception " + oEx.toString());
		}
		
		return "";
	}
		
	public static void testImageDecoders() {
		
		testImageDecode("S1A_IW_GRDH_1SDV_20211227T052748_20211227T052813_041190_04E503_D2FB");
		testImageDecode("S1B_IW_RAW__0SDV_20211222T171436_20211222T171508_030141_039960_631F");
		testImageDecode("S1B_IW_SLC__1SDV_20211222T171528_20211222T171556_030141_039960_73C3");
		testImageDecode("S2A_MSIL1C_20211222T070311_N0301_R063_T40SBA_20211222T080206");
		testImageDecode("S2A_MSIL2A_20211222T070311_N0301_R063_T39RXN_20211222T091646");
		testImageDecode("S3B_SY_2_VG1____20211222T000000_20211222T235959_20211228T125214_EUROPE____________LN2_O_NT_002");
		testImageDecode("S3B_SY_2_VGP____20211222T061412_20211222T065817_20211223T184339_2645_060_305______LN2_O_NT_002");
		testImageDecode("S3B_OL_2_WRR____20211222T061412_20211222T065817_20211222T084230_2645_060_305______MAR_O_NR_003");
		testImageDecode("S3B_OL_1_ERR____20211222T061412_20211222T065817_20211222T084221_2645_060_305______MAR_O_NR_002");
		testImageDecode("S3B_OL_2_LRR____20211222T061412_20211222T065817_20211223T112207_2645_060_305______LN1_O_NT_002");
		testImageDecode("S3B_OL_2_WRR____20211222T061412_20211222T065817_20211223T114743_2645_060_305______MAR_O_NT_003");
		testImageDecode("S5P_OFFL_L2__NP_BD6_20211222T090116_20211222T104246_21721_02_010300_20211223T224704");
		testImageDecode("S5P_OFFL_L1B_RA_BD4_20211222T090116_20211222T104246_21721_02_020000_20211222T122628");
		testImageDecode("S5P_NRTI_L2__AER_AI_20211222T100245_20211222T100745_21721_02_020301_20211222T103531");
		testImageDecode("S5P_NRTI_L2__HCHO___20211222T100245_20211222T100745_21721_02_020201_20211222T105201");
		testImageDecode("S5P_NRTI_L2__O3_____20211222T100245_20211222T100745_21721_02_020201_20211222T105159");
		testImageDecode("LC08_L1TP_200030_20211223_20211223_01_RT");
		testImageDecode("LC08_L1GT_196028_20211227_20211227_01_RT");
		testImageDecode("MER_FRS_1PPEPA20041222_110737_000003212033_00123_14706_0540");		
		testImageDecode("ASA_IMS_1PNESA20041224_100709_000000152033_00151_14734_0000");
		testImageDecode("ASA_IMP_1PNESA20041224_100724_000000152033_00151_14734_0000");
		testImageDecode("RIVER-FLDglobal-composite1_20211222_000000.part057.tif");
		testImageDecode("RIVER-FLDglobal-composite1_20211222_000000.part071");
	}
	
	public static void testImageDecode(String sImage) {
		String sResult = "";
		Date oDate;
		
		sResult = WasdiFileUtils.getPlatformFromSatelliteImageFileName(sImage);
		System.out.println(sResult);
		
		sResult = WasdiFileUtils.getProductTypeSatelliteImageFileName(sImage);
		System.out.println(sResult);
		
		oDate = WasdiFileUtils.getDateFromSatelliteImageFileName(sImage);
		System.out.println(oDate.toString());		
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
}
