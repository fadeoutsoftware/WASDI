package wasdi.filebuffer;

public class LocalFileDescriptor {
	public String Code;
	public String Folder;
	public boolean SingleFile;
	
	public LocalFileDescriptor() {
		
	}
	
	public LocalFileDescriptor(String sCode, String sFolder, boolean bSingleFile) {
		Code = sCode;
		Folder = sFolder;
		SingleFile = bSingleFile;
	}
	
}
