/**
 * Created by Cristiano Nattero on 2019-02-19
 * 
 * Fadeout software
 *
 */
package it.fadeout.rest.resources.largeFileDownload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;

import wasdi.shared.utils.log.WasdiLog;

/**
 * @author c.nattero
 *
 */
public class ZipStreamingOutput implements StreamingOutput {

	final Map<String,File> m_aoFileEntries;

	public ZipStreamingOutput(Map<String,File> aoInitMap) {
		WasdiLog.debugLog("ZipStreamingOutput.ZipStreamingOutput");
		if(null==aoInitMap) {
			throw new NullPointerException("ZipStreamingOutput.ZipStreamingOutput: passed a null Map");
		}

		m_aoFileEntries = aoInitMap;
	}

	/* (non-Javadoc)
	 * @see javax.ws.rs.core.StreamingOutput#write(java.io.OutputStream)
	 */
	@Override
	public void write(OutputStream oOutputStream) throws IOException, WebApplicationException {
		WasdiLog.debugLog("ZipStreamingOutput.write");
		if(null == oOutputStream) {
			throw new NullPointerException("ZipStreamingOutput.write: passed a null OutputStream");
		}

		//BufferedOutputStream oBufferedOutputStream = new BufferedOutputStream(oOutputStream);
		//ZipOutputStream oZipOutputStream = new ZipOutputStream(oBufferedOutputStream);
		ZipOutputStream oZipOutputStream = new ZipOutputStream(oOutputStream);
		//TODO try reducing the compression to increase speed
		//oZipOutputStream.setLevel(level);
		
		try {
			Set<String> oZippedFileNames = m_aoFileEntries.keySet();
			int iTotalFiles = m_aoFileEntries.size();
			int iDone = 0;
			WasdiLog.debugLog("ZipStreamingOutput.write: begin");
			for (String oZippedName : oZippedFileNames) {
				iDone++;
				WasdiLog.debugLog("ZipStreamingOutput.write: File: "+oZippedName);
				File oFileToZip = m_aoFileEntries.get(oZippedName);
				if(oFileToZip.isDirectory()) {
					oZipOutputStream.putNextEntry(new ZipEntry(oZippedName));
					oZipOutputStream.closeEntry();
				} else {
					oZipOutputStream.putNextEntry(new ZipEntry(oZippedName));
					
					try(InputStream oInputStream = new FileInputStream(oFileToZip)){
						long lCopiedBytes = 0;
						long lThreshold = 2L*1024*1024*1024;
						long lSize = oFileToZip.length(); 
						if(lSize > lThreshold) {
							lCopiedBytes = IOUtils.copyLarge(oInputStream, oZipOutputStream);
						} else {
							lCopiedBytes = IOUtils.copy(oInputStream, oZipOutputStream);
						}
						WasdiLog.debugLog("ZipStreamingOutput.write: file " + oZippedName + "copied " + lCopiedBytes + " B out of " + oFileToZip.length());

						oZipOutputStream.closeEntry();
						oInputStream.close();
					}
				}
				WasdiLog.debugLog("ZipStreamingOutput.write: done file: "+oZippedName+" -> "+ iDone +" / " +iTotalFiles);
			}
			WasdiLog.debugLog("ZipStreamingOutput.write: done writing to zipstream");
			//oZipOutputStream.flush();
			oZipOutputStream.close();
			WasdiLog.debugLog("ZipStreamingOutput.write: ZipOutputStream closed");
		} 
		catch (Exception e) {
			WasdiLog.errorLog("ZipStreamingOutput.write:  error ", e);
		} finally {
			// Flush output
			if( oOutputStream!=null ) {
				
				try {
					oOutputStream.flush();
					oOutputStream.close();
					WasdiLog.debugLog("ZipStreamingOutput.write: OutputStream closed");						
				}
				catch (Exception oEx) {
					WasdiLog.debugLog("ZipStreamingOutput.write: OutputStream close exception: " + oEx.toString());
				}				
			}
		}
	}
}


