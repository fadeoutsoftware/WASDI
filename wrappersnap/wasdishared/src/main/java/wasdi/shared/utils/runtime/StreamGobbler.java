package wasdi.shared.utils.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import wasdi.shared.utils.log.WasdiLog;

public class StreamGobbler extends Thread {

	InputStream m_oInputStream;
	String m_sStreamType;

	public StreamGobbler(InputStream oInputStream, String sType)
	{
		this.m_oInputStream = oInputStream;
		this.m_sStreamType = sType;
	}

	public void run()
	{
		try
		{
			InputStreamReader oInputStreamReader = new InputStreamReader(m_oInputStream);
			BufferedReader oBufferedReader = new BufferedReader(oInputStreamReader);
			String sLine=null;
			while ( (sLine = oBufferedReader.readLine()) != null) {
				WasdiLog.infoLog(m_sStreamType + ">" + sLine);
			}
				    
		} catch (IOException oIOEx)
		{
			WasdiLog.errorLog("StreamGobbler.run: error", oIOEx);
		}
	}	
}
