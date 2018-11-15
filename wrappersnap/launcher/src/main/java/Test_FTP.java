import java.io.File;
import java.io.IOException;

//TODO read FTP parameters
//TODO write those parameters into configuration file
//import wasdi.ConfigReader;
import wasdi.shared.utils.FtpClient;


public class Test_FTP {
	//local
	private static String m_sLocalFileName;
	
	//remote
	private static String m_sServer;
    private static int m_iPort;
    private static String m_sUser;
    private static String m_sPassword;
    private static String m_sDirToChangeTo;
    
    //the client
    private static FtpClient m_oFtpClient;
    
    
    private static File makeLocalFile() throws IOException {
    	String sLocalPath = System.getProperty("user.dir");
		System.out.println("local directory: "+ sLocalPath );
		File oFile = new File(m_sLocalFileName);
		Boolean bOk = false;
		if(oFile.exists()) {
			bOk = true;
		} else {
			//oFile.getParentFile().mkdirs();
			bOk = oFile.createNewFile();
		}
		if(bOk) {
			//oFile.deleteOnExit();
			return oFile;
		}
		
		return null;
    }
		
	public static void main(String[] args) {
		//m_sLocalFileName = "pom.xml";
		m_sLocalFileName = "phooffa.txt";
		
		m_sServer = new String("127.0.0.1");
		m_iPort = 21;
		m_sUser = new String("wasdi");
		m_sPassword = "wasdiPassword";
		m_sDirToChangeTo = "wasdiTest";
		m_oFtpClient = new FtpClient(m_sServer, m_iPort, m_sUser, m_sPassword);
		
		
		try {
			File oFile = makeLocalFile();
			
			if(!m_oFtpClient.open()) {
				System.out.println("Connection failed  :-(");
				
			} else {
				System.out.println("Succesfullly connected to server :-)");
				
				String sPWD = m_oFtpClient.pwd();
				System.out.println("Working directory on server: " + sPWD);
		
				Boolean bCD = m_oFtpClient.cd(m_sDirToChangeTo);
				if(bCD) {
					System.out.println("Changed to "+m_sDirToChangeTo+" on server :-)");
				} else {
					System.out.println("Failed to cd to "+m_sDirToChangeTo+" on server  :-(");
				}
				
				String sCWD = m_oFtpClient.pwd();
				System.out.println("Working directory on server: " + sCWD);
				sPWD = sCWD;

				if(m_oFtpClient.putFileToPath(oFile, sCWD )) {
					System.out.println("Successfully completed file transfer :-)");
				} else {
					System.out.println("File transfer failed :-(");
				}
				Boolean bFileExistsOnFTPServer = m_oFtpClient.FileIsNowOnServer(sCWD, oFile.getName());
				if(bFileExistsOnFTPServer) {
					System.out.println("File is on server now :-)");
				} else {
					System.out.println("File is not on server :-(");
				}
				
				m_oFtpClient.close();
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
	}

}
