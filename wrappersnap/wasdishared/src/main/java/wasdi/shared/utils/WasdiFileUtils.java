/**
 * Created by Cristiano Nattero on 2019-02-12
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.base.Preconditions;

/**
 * @author c.nattero
 *
 */
public class WasdiFileUtils {

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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the file name without the extension and the trailing dots
	 * @param sInputFile the initial name of the file
	 * @return the cleaned name of the file
	 */
	public static String getFileNameWithoutExtensionsAndTrailingDots(String sInputFile) {
			if(Utils.isNullOrEmpty(sInputFile)) {
				Utils.debugLog("Utils.GetFileNameExtension: input null or empty");
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
			Utils.log("ERROR", "WasdiFileUtils.loadJsonFromFile: file " + sFileFullPath + " was not found: " + oFnf);
		} catch (Exception oE) {
			Utils.log("ERROR", "WasdiFileUtils.loadJsonFromFile: " + oE);
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
	 * Extract the zip-file into the declared directory.
	 * 
	 * <pre>
	 * The processing in streaming mode of zip-files that are not compressed/deflated 
	 * (see CREODIAS downloads) might end up in an error:
	 * java.util.zip.ZipException: only DEFLATED entries can have EXT descriptor
	 * 
	 * Due to this fact, an alternative solution based on Apache's commons-compress was used.
	 * For more details see the following page:
	 * https://stackoverflow.com/questions/47208272/android-zipinputstream-only-deflated-entries-can-have-ext-descriptor
	 * </pre>
	 * @param zipFile the zip-file to be extracted
	 * @param destDir the destination directory
	 * @throws IOException if an error occurs
	 */
	public static void unzipFile(File zipFile, File destDir) throws IOException {
		if (zipFile == null) {
			Utils.log("ERROR", "WasdiFileUtils.cleanUnzipFile: zipFile is null");
			return;
		} else if (!zipFile.exists()) {
			Utils.log("ERROR", "WasdiFileUtils.cleanUnzipFile: zipFile does not exist: " + zipFile.getAbsolutePath());
		}

		if (destDir == null) {
			Utils.log("ERROR", "WasdiFileUtils.cleanUnzipFile: destDir is null");
			return;
		} else if (!destDir.exists()) {
			Utils.log("ERROR", "WasdiFileUtils.cleanUnzipFile: destDir does not exist: " + destDir.getAbsolutePath());
		}

		String destDirectory = destDir.getAbsolutePath();

		try (ArchiveInputStream i = new ZipArchiveInputStream(new FileInputStream(zipFile), "UTF-8", false, true)) {
			ArchiveEntry entry = null;
			while ((entry = i.getNextEntry()) != null) {
				if (!i.canReadEntryData(entry)) {
					Utils.debugLog("Utils.GetFileNameExtension: Can't read entry: " + entry);
					continue;
				}
				String name = destDirectory + File.separator + entry.getName();
				File f = new File(name);
				if (entry.isDirectory()) {
					if (!f.isDirectory() && !f.mkdirs()) {
						throw new IOException("failed to create directory " + f);
					}
				} else {
					File parent = f.getParentFile();
					if (!parent.isDirectory() && !parent.mkdirs()) {
						throw new IOException("failed to create directory " + parent);
					}
					try (OutputStream o = Files.newOutputStream(f.toPath())) {
						IOUtils.copy(i, o);
					}
				}
			}
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
			Utils.log("ERROR", "WasdiFileUtils.cleanUnzipFile: zipFile is null");
			return;
		} else if (!zipFile.exists()) {
			Utils.log("ERROR", "WasdiFileUtils.cleanUnzipFile: zipFile does not exist: " + zipFile.getAbsolutePath());
		}

		if (destDir == null) {
			Utils.log("ERROR", "WasdiFileUtils.cleanUnzipFile: destDir is null");
			return;
		} else if (!destDir.exists()) {
			Utils.log("ERROR", "WasdiFileUtils.cleanUnzipFile: destDir does not exist: " + destDir.getAbsolutePath());
		}

		unzipFile(zipFile, destDir);

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
	 * @param file the file
	 * @return true if the file exists, false otherwise.
	 */
	public static boolean fileExists(File file) {
		if (file == null) {
			Utils.log("ERROR", "WasdiFileUtils.doesFileExist: file is null");
			return false;
		}

		return file != null && file.exists();
	}

	/**
	 * Check if the filePath corresponds to an existing file.
	 * @param filePath the path of the file
	 * @return true if the file exists, false otherwise
	 */
	public static boolean fileExists(String filePath) {
		if (filePath == null) {
			Utils.log("ERROR", "WasdiFileUtils.doesFileExist: filePath is null");
			return false;
		}

		File file = new File(filePath);

		return fileExists(file);
	}


	/**
	 * Move a file to a destination directory.
	 * @param sourcePath the path of the file to be moved
	 * @param destinationDirectoryPath the path of the destination directory
	 * @return true if the operation was successful, false otherwise
	 */
	private static boolean moveFile(String sourcePath, String destinationDirectoryPath) {
		if (sourcePath == null) {
			Utils.log("ERROR", "WasdiFileUtils.moveFile: sourcePath is null");
			return false;
		}

		if (destinationDirectoryPath == null) {
			Utils.log("ERROR", "WasdiFileUtils.moveFile: destinationDirectoryPath is null");
			return false;
		}

		File sourceFile = new File(sourcePath);
		if (!fileExists(sourceFile)) {
			Utils.log("ERROR", "WasdiFileUtils.moveFile: sourceFile does not exist");
			return false;
		}

		File destinationDirectory = new File(destinationDirectoryPath);
		if (!destinationDirectory.exists()) {
			destinationDirectory.mkdirs();
		}

		boolean outcome = true;

		if (sourceFile.isDirectory()) {
			for (File file : sourceFile.listFiles()) {
				outcome = outcome & moveFile(file.getAbsolutePath(), destinationDirectoryPath);
			}
		} else {
			File destinationFile = new File(destinationDirectoryPath + sourceFile.getName());
			outcome = sourceFile.renameTo(destinationFile);
		}

		return outcome;
	}

	/**
	 * Delete a file from the filesystem. If the file is a directory, also delete the child directories and files.
	 * @param filePath the absolute path of the file
	 * @return true if the file was deleted, false otherwise
	 */
	private static boolean deleteFile(String filePath) {
		if (filePath == null) {
			Utils.log("ERROR", "WasdiFileUtils.deleteFile: filePath is null");
			return false;
		} else if (!fileExists(filePath)) {
			Utils.log("ERROR", "WasdiFileUtils.deleteFile: file does not exist: " + filePath);
			return false;
		}

		File file = new File(filePath);

		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				deleteFile(child.getPath());
			}
		}

		return file.delete();
	}

	/**
	 * Get the complete directory path. Basically, add a trailing slash if it is missing.
	 * 
	 * @param dirPath the directory path
	 * @return the complete path of the directory, or null if the dirPath is null
	 */
	public static String completeDirPath(String dirPath) {
		if (dirPath == null || dirPath.endsWith("/")) {
			return dirPath;
		}

		return dirPath + "/";
	}

	/**
	 * Read the content of a text file.
	 * @param filePath the file to be read
	 * @return the text content of the file
	 */
	public static String fileToText(String filePath) {
		if (filePath == null) {
			Utils.log("ERROR", "WasdiFileUtils.fileToText: filePath is null");
			return null;
		}

		File file = new File(filePath);
		if (!fileExists(file)) {
			Utils.log("ERROR", "WasdiFileUtils.fileToText: file does not exist");
			return null;
		}

		try {
			return new String(Files.readAllBytes(Paths.get(filePath)));
		} catch (IOException e) {
			Utils.log("ERROR", "WasdiFileUtils.fileToText: cannot read file");
			return null;
		}
	}

	/**
	 * Check if a file is a help-file.
	 * More exactly, checks if the file-name is "readme" or "help" and if the extension is "md" or "txt".
	 * @param file the file
	 * @return true if the file is a help file, false otherwise
	 */
	public static boolean isHelpFile(File file) {
		if (!fileExists(file)) {
			Utils.log("ERROR", "WasdiFileUtils.isHelpFile: file is null");
			return false;
		}

		return isHelpFile(file.getName());
	}

	/**
	 * Check if a file is a help-file.
	 * More exactly, checks if the file-name is "readme" or "help" and if the extension is "md" or "txt".
	 * @param fileName the name of the file
	 * @return true if the file is a help file, false otherwise
	 */
	public static boolean isHelpFile(String fileName) {
		if (fileName == null) {
			Utils.log("ERROR", "WasdiFileUtils.isHelpFile: fileName is null");
			return false;
		}

		String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
		if (tokens.length != 2) {
			Utils.log("ERROR", "WasdiFileUtils.isHelpFile: " + fileName + " is not a help file-name");
			return false;
		}

		String name = tokens[0];
		String extension = tokens[1];

		return (name.equalsIgnoreCase("readme") || name.equalsIgnoreCase("help"))
				&& (extension.equalsIgnoreCase("md") || extension.equalsIgnoreCase("txt"));
	}

}
