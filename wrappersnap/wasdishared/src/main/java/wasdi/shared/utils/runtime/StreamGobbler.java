package wasdi.shared.utils.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import wasdi.shared.utils.LoggerWrapper;

public class StreamGobbler extends Thread {

	InputStream m_oInputStream;
	String m_sStreamType;
	LoggerWrapper m_oLoggerWrapper;

	public StreamGobbler(InputStream oInputStream, String sType, LoggerWrapper oLoggerWrapper)
	{
		this.m_oInputStream = oInputStream;
		this.m_sStreamType = sType;
		this.m_oLoggerWrapper = oLoggerWrapper;
	}

	public void run()
	{
		try
		{
			InputStreamReader oInputStreamReader = new InputStreamReader(m_oInputStream);
			BufferedReader oBufferedReader = new BufferedReader(oInputStreamReader);
			String sLine=null;
			while ( (sLine = oBufferedReader.readLine()) != null) {
				if (m_oLoggerWrapper != null) {
					m_oLoggerWrapper.info(m_sStreamType + ">" + sLine);
				}
			}
				    
		} catch (IOException ioe)
		{
			ioe.printStackTrace();  
		}
	}	
}
