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
	
}
