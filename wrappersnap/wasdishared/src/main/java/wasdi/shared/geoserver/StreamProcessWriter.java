package wasdi.shared.geoserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import wasdi.shared.utils.log.WasdiLog;

/**
 * Created by s.adamo on 22/03/2017.
 */
public class StreamProcessWriter extends Thread {

    InputStream m_oInputStream;
    String m_sType;

    StreamProcessWriter(InputStream oIs, String sType)
    {
        this.m_oInputStream = oIs;
        this.m_sType = sType;
    }
    @Override
    public void run()
    {
        try
        {
            InputStreamReader oIsr = new InputStreamReader(m_oInputStream);
            BufferedReader oBr = new BufferedReader(oIsr);

            String sLine=null;
            while ( (sLine = oBr.readLine()) != null) {
                WasdiLog.debugLog(m_sType + ">" + sLine);
            }

            oBr.close();
        } catch (IOException oEx)
        {
            WasdiLog.debugLog("StreamProcessWriter.run: " +  oEx.getMessage());
        }

        WasdiLog.debugLog("Stream Process Writer " + m_sType + " END");
    }
}
