package wasdi.dataproviders;

public class LocalFileDescriptor {
	public String m_sCode;
	public String m_sFolder;
	public boolean m_bSingleFile;
	
	public LocalFileDescriptor() {
		
	}
	
	public LocalFileDescriptor(String sCode, String sFolder, boolean bSingleFile) {
		m_sCode = sCode;
		m_sFolder = sFolder;
		m_bSingleFile = bSingleFile;
	}
	
}
