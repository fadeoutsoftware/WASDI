package wasdi.filebuffer;

import org.apache.log4j.Logger;

import wasdi.shared.business.ProcessWorkspace;

public class PROBAVDownloadFile extends DownloadFile {
	
	
    public PROBAVDownloadFile() {
		super();
	}
    
    public PROBAVDownloadFile(Logger logger) {
		super(logger);
	}
    

	@Override
	public long GetDownloadFileSize(String sFileURL) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String GetFileName(String sFileURL) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
