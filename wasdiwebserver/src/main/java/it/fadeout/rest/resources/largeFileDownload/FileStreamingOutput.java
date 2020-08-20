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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import com.google.api.client.util.ByteStreams;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class FileStreamingOutput implements StreamingOutput {

	final File m_oFile;

	public FileStreamingOutput(File oFile){
		Utils.debugLog("FileStreamingOutput.FileStreamingOutput");
		if(null==oFile) {
			throw new NullPointerException("FileStreamingOutput.FileStreamingOutput: passed a null File");
		}
		m_oFile = oFile;
	}

	/* (non-Javadoc)
	 * @see javax.ws.rs.core.StreamingOutput#write(java.io.OutputStream)
	 */
	@Override
	public void write(OutputStream oOutputStream) throws IOException, WebApplicationException {
		try {
			Utils.debugLog("FileStreamingOutput.write");
			if(null == oOutputStream) {
				throw new NullPointerException("FileStreamingOutput.write: passed a null OutputStream");
			}
			InputStream oInputStream = null;
			try {
				oInputStream = new FileInputStream(m_oFile);
				long lCopiedBytes = 0;
//				long lThreshold = 2L*1024*1024*1024;
//				long lSize = m_oFile.length();
				Utils.debugLog("FileStreamingOutput.write: using guava ByteStreams.copy to copy file");
				lCopiedBytes = ByteStreams.copy(oInputStream,  oOutputStream);
				/*
				if(lSize > lThreshold) {
					Utils.debugLog("FileStreamingOutput.write: large file, using IOUtils.copyLarge");
					lCopiedBytes = IOUtils.copyLarge(oInputStream, oOutputStream);
				} else {
					Utils.debugLog("FileStreamingOutput.write: small enough file, using IOUtils.copy");
					lCopiedBytes = IOUtils.copy(oInputStream, oOutputStream);
				}
				*/
				if( oOutputStream!=null ) {
					Utils.debugLog("FileStreamingOutput.write: about to flush output stream");
					oOutputStream.flush();
					Utils.debugLog("FileStreamingOutput.write: output flushed");
				}
				Utils.debugLog("FileStreamingOutput.write: "+ m_oFile.getName()+": copied "+lCopiedBytes+" B out of " + m_oFile.length() );
			} catch (Exception oE) {
				Utils.debugLog("FileStreamingOutput.write: " + oE);
			} finally {
				// Flush output
				if( oOutputStream!=null ) {
					Utils.debugLog("FileStreamingOutput.write: about to close output");
					oOutputStream.close();
					Utils.debugLog("FileStreamingOutput.write: OutputStream closed");
				}
				// Close input
				if( oInputStream !=null ) {
					Utils.debugLog("FileStreamingOutput.write: about to close input");
					oInputStream.close();
					Utils.debugLog("FileStreamingOutput.write: InputStream closed");
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("FileStreamingOutput.write: uncaught error: " + oE);
		}
	}
}
