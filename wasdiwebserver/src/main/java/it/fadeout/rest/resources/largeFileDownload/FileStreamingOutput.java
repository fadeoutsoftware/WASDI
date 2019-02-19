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
		if(null==oFile) {
			throw new NullPointerException("FileStreamingOutput.FileStreamingOutput: passed a null OutputStream");
		}
		m_oFile = oFile;
	}

	/* (non-Javadoc)
	 * @see javax.ws.rs.core.StreamingOutput#write(java.io.OutputStream)
	 */
	@Override
	public void write(OutputStream oOutputStream) throws IOException, WebApplicationException {
		if(null == oOutputStream) {
			throw new NullPointerException("FileStreamingOutput.write: passed a null OutputStream");
		}
		InputStream oInputStream = null;
		try {
			oInputStream = new FileInputStream(m_oFile);
			long lCopiedBytes = 0;
			if(m_oFile.length() > 2*1024*1024*1024) {
				lCopiedBytes = IOUtils.copyLarge(oInputStream, oOutputStream);
			} else {
				lCopiedBytes = IOUtils.copy(oInputStream, oOutputStream);
			}
			Wasdi.DebugLog("ZipStreamingOutput.write: "+ m_oFile.getName()+": copied "+lCopiedBytes+" B out of " + m_oFile.length() );
		} catch (Exception e) {
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
