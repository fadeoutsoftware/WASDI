package wasdi.shared.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FilenameUtils;

public class WrapperFile  extends File  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3976820120016533898L;

	public WrapperFile(String paramString) {
		super(paramString);

	}
	
	public boolean save(InputStream oInputStream){
		
		int iRead = 0;
		byte[] ayBytes = new byte[1024];

		// Try with resources. Resource declaration auto closes object(even if IOExeption occours)
		try (OutputStream oOutStream = new FileOutputStream(this)){
			while ((iRead = oInputStream.read(ayBytes)) != -1) {
				oOutStream.write(ayBytes, 0, iRead);
			}
			oOutStream.flush();
			oOutStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public String getNameWithouteExtension() {
		String sName = this.getName();
		return FilenameUtils.removeExtension(sName);				
	}
	
	public String getExtension(){
		String sName = this.getName();
		return FilenameUtils.getExtension(sName);	

	}
}
