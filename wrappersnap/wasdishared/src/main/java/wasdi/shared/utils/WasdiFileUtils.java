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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.common.base.Preconditions;

/**
 * @author c.nattero
 *
 */
public class WasdiFileUtils {

	//courtesy of https://www.baeldung.com/java-compress-and-uncompress
	public void zipFile(File oFileToZip, String sFileName, ZipOutputStream oZipOut) {
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
				oFis.close();				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static JSONObject loadJsonFromFile(String sFileFullPath) {
		Preconditions.checkNotNull(sFileFullPath);

		JSONObject oJson = null;
		try(
				FileReader oReader = new FileReader(sFileFullPath);
				){
			
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
	 * Check zip file entries name and path, according to:
	 * https://wiki.sei.cmu.edu/confluence/display/java/IDS04-J.+Safely+extract+files+from+ZipInputStream
	 * 
	 * @param sFilename name of file in zip entry
	 * @param sIntendedDir directory where entry should be extracted
	 * @return canonical path of file name to extract
	 * @throws java.io.IOException if entry is outside intended directory
	 */
	public static String validateZipEntryFilename(String sFilename, String sIntendedDir) throws java.io.IOException {
		  File oFile = new File(sFilename);
		  String sFileCanonicalPath = oFile.getCanonicalPath();
		 
		  File oDir = new File(sIntendedDir);
		  String sDirCanonicalPath = oDir.getCanonicalPath();
		   
		  if (sFileCanonicalPath.startsWith(sDirCanonicalPath)) {
		    return sFileCanonicalPath;
		  } else {
		    throw new IllegalStateException("File is outside extraction target directory.");
		  }
		}
	
}
