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

import wasdi.shared.utils.log.WasdiLog;

/**
 * @author c.nattero
 *
 */
public class FileStreamingOutput implements StreamingOutput {

	final File m_oFile;

	public FileStreamingOutput(File oFile){
		WasdiLog.debugLog("FileStreamingOutput.FileStreamingOutput");
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
			WasdiLog.debugLog("FileStreamingOutput.write");
			if(null == oOutputStream) {
				throw new NullPointerException("FileStreamingOutput.write: passed a null OutputStream");
			}
			try {
				try (InputStream oInputStream = new FileInputStream(m_oFile)) {
					long lCopiedBytes = 0;
					WasdiLog.debugLog("FileStreamingOutput.write: using guava ByteStreams.copy to copy file");
					lCopiedBytes = ByteStreams.copy(oInputStream,  oOutputStream);
					
					if( oOutputStream!=null ) {
						oOutputStream.flush();
					}
					
					WasdiLog.debugLog("FileStreamingOutput.write: "+ m_oFile.getName()+": copied "+lCopiedBytes+" B out of " + m_oFile.length() );					
				}
				
			} 
			catch (Exception oE) {
				WasdiLog.debugLog("FileStreamingOutput.write: " + oE);
			} finally {
				
				// Flush output
				if( oOutputStream!=null ) {
					try {
						oOutputStream.close();
						WasdiLog.debugLog("FileStreamingOutput.write: OutputStream closed");						
					}
					catch (Exception oEx) {
						WasdiLog.debugLog("FileStreamingOutput.write: OutputStream close exception: " + oEx.toString());
					}
				}
				
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("FileStreamingOutput.write: uncaught error: " + oE);
		}
	}
}
