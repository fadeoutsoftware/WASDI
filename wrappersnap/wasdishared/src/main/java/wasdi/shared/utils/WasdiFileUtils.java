/**
 * Created by Cristiano Nattero on 2019-02-12
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sun.tools.javac.file.ZipArchive;

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
			FileInputStream oFis = new FileInputStream(oFileToZip);
			ZipEntry oZipEntry = new ZipEntry(sFileName);
			oZipOut.putNextEntry(oZipEntry);
			byte[] bytes = new byte[1024];
			int iLength;
			while ((iLength = oFis.read(bytes)) >= 0) {
				oZipOut.write(bytes, 0, iLength);
			}
			oFis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void addFileToZip(File oFileToAdd, File oZipFile) {
		try {
		FileOutputStream oOutStream = new FileOutputStream(oZipFile);
		ZipOutputStream oZipStream = new ZipOutputStream(oOutStream);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
