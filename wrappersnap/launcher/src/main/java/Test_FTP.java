import java.io.IOException;
import java.util.Collection;

//TODO read FTP parameters
//TODO write those parameters into configuration file
//import wasdi.ConfigReader;
import wasdi.shared.utils.FtpClient;


public class Test_FTP {
	private String m_sServer;
    private int m_iPort;
    private String m_sUser;
    private String m_sPassword;
    private FtpClient m_oFtpClient;

		
	public void main(String[] args) {
		m_sServer = new String("127.0.0.1");
		m_iPort = 21;
		m_sUser = new String("wasdi");
		m_sPassword = "wasdiPassword";
		m_oFtpClient = new FtpClient(m_sServer, m_iPort, m_sUser, m_sPassword);
		
		try {
			m_oFtpClient.open();
			Collection<String> asFiles = m_oFtpClient.listFiles(".");			
			
			m_oFtpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
	}

}
