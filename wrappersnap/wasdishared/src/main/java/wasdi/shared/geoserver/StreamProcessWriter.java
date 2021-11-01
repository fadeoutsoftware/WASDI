package wasdi.shared.geoserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
                Publisher.s_oLogger.debug(m_sType + ">" + sLine);
                System.out.println(m_sType + ">" + sLine);
            }

            oBr.close();
        } catch (IOException oEx)
        {
            Publisher.s_oLogger.debug("StreamProcessWriter.run: " +  oEx.getMessage());
        }

        Publisher.s_oLogger.debug("Stream Process Writer " + m_sType + " END");
    }
}
