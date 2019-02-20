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

import org.apache.commons.io.IOUtils;

import it.fadeout.Wasdi;

/**
 * @author c.nattero
 *
 */
public class FileStreamingOutput implements StreamingOutput {

	final File m_oFile;

	public FileStreamingOutput(File oFile){
		Wasdi.DebugLog("FileStreamingOutput.FileStreamingOutput");
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
		Wasdi.DebugLog("FileStreamingOutput.write");
		if(null == oOutputStream) {
			throw new NullPointerException("FileStreamingOutput.write: passed a null OutputStream");
		}
		InputStream oInputStream = null;
		try {
			oInputStream = new FileInputStream(m_oFile);
			long lCopiedBytes = 0;
			long lThreshold = 2L*1024*1024*1024;
			long lSize = m_oFile.length(); 
			if(lSize > lThreshold) {
				lCopiedBytes = IOUtils.copyLarge(oInputStream, oOutputStream);
			} else {
				lCopiedBytes = IOUtils.copy(oInputStream, oOutputStream);
			}
			Wasdi.DebugLog("FileStreamingOutput.write: "+ m_oFile.getName()+": copied "+lCopiedBytes+" B out of " + m_oFile.length() );
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Flush output
			if( oOutputStream!=null ) {
				oOutputStream.flush();
				oOutputStream.close();
				Wasdi.DebugLog("FileStreamingOutput.write: OutputStream closed");
			}
			// Close input
			if( oInputStream !=null ) {
				oInputStream.close();
				Wasdi.DebugLog("FileStreamingOutput.write: InputStream closed");
			}
		}
	}

}
