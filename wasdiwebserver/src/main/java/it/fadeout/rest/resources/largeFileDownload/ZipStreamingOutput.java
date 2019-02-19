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

import it.fadeout.Wasdi;

/**
 * @author c.nattero
 *
 */
public class ZipStreamingOutput implements StreamingOutput {

	final Map<String,File> m_aoFileEntries;

	public ZipStreamingOutput(Map<String,File> aoInitMap) {
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
		Wasdi.DebugLog("ZipStreamingOutput.write");
		if(null == oOutputStream) {
			throw new NullPointerException("ZipStreamingOutput.write: passed a null OutputStream");
		}

		//BufferedOutputStream oBufferedOutputStream = new BufferedOutputStream(oOutputStream);
		//ZipOutputStream oZipOutputStream = new ZipOutputStream(oBufferedOutputStream);
		ZipOutputStream oZipOutputStream = new ZipOutputStream(oOutputStream);
		//TODO try reducing the compression to increase speed
		//oZipOutputStream.setLevel(level);
		InputStream oInputStream = null;
		try {
			Set<String> oZippedFileNames = m_aoFileEntries.keySet();
			int iTotalFiles = m_aoFileEntries.size();
			int iDone = 0;
			Wasdi.DebugLog("ZipStreamingOutput.write: begin");
			for (String oZippedName : oZippedFileNames) {
				iDone++;
				Wasdi.DebugLog("ZipStreamingOutput.write: File: "+oZippedName);
				File oFileToZip = m_aoFileEntries.get(oZippedName);
				if(oFileToZip.isDirectory()) {
					oZipOutputStream.putNextEntry(new ZipEntry(oZippedName));
					oZipOutputStream.closeEntry();
				} else {
					oZipOutputStream.putNextEntry(new ZipEntry(oZippedName));
					oInputStream = new FileInputStream(oFileToZip);
					long lCopiedBytes = 0;
					if(oFileToZip.length()>2*1024*1024*1024) {
						lCopiedBytes = IOUtils.copyLarge(oInputStream, oZipOutputStream);
					} else {
						//IOUtils.copy(oInputStream, oZipOutputStream, 16384);
						lCopiedBytes = IOUtils.copy(oInputStream, oZipOutputStream);
					}
					Wasdi.DebugLog("ZipStreamingOutput.write: file " + oZippedName + "copied " + lCopiedBytes + " B out of " + oFileToZip.length());
					//
					//TODO try different buffer sizes
					//					byte[] bytes = new byte[16384];
					//					//byte[] bytes = new byte[1024];
					//					int iLength = -1;
					//					long lTotalLength = oFileToZip.length();
					//					long lCumulativeLength = 0;
					//					iLength = oInputStream.read(bytes);
					//					//while ((iLength = oInputStream.read(bytes)) >= 0) {
					//					while(iLength>=0) {
					//						lCumulativeLength += iLength;
					//						oZipOutputStream.write(bytes, 0, iLength);
					//						oZipOutputStream.flush();
					//						oBufferedOutputStream.flush();
					//						oOutputStream.flush();
					//						double dPerc = (double)lCumulativeLength / (double)lTotalLength;
					//						dPerc = Math.floor(dPerc * 100.0) / 100.0;
					//						//Wasdi.DebugLog("ZipStreamingOutput.write: File: "+oZippedName+": "+lCumulativeLength + " / " + lTotalLength + " = " + dPerc + "%");
					//						iLength = oInputStream.read(bytes);
					//					}
					//

					oZipOutputStream.closeEntry();
					oInputStream.close();
				}
				Wasdi.DebugLog("ZipStreamingOutput.write: done file: "+oZippedName+" -> "+ iDone +" / " +iTotalFiles);
			}
			Wasdi.DebugLog("ZipStreamingOutput.write: done writing to zipstream");
			//oZipOutputStream.flush();
			oZipOutputStream.close();
			Wasdi.DebugLog("ZipStreamingOutput.write: ZipOutputStream closed");
		} catch (Exception e) {
			Wasdi.DebugLog("ZipStreamingOutput.write: exception caught:");
			Wasdi.DebugLog("ZipStreamingOutput.write: "+e.getMessage() );
			e.printStackTrace();
		} finally {
			// Flush output
			if( oOutputStream!=null ) {
				oOutputStream.flush();
				oOutputStream.close();
				Wasdi.DebugLog("ZipStreamingOutput.write: OutputStream closed");
			}
			// Close input
			if( oInputStream !=null ) {
				oInputStream.close();
				Wasdi.DebugLog("ZipStreamingOutput.write: InputStream closed");
			}
		}
	}
}


