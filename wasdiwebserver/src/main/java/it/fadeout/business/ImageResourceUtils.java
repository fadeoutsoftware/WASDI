package it.fadeout.business;

import java.io.File;

public class ImageResourceUtils {
	
	public boolean isValidExtension(String sExt,String[] sValidExtensions){
		//Check if the extension is valid
		for (String sValidExtension : sValidExtensions) {
			  if(sValidExtension.equals(sExt.toLowerCase()) ){
				  return true;
			  }
		}
		return false;
	}
	
	public void createDirectory(String sPath){
		File oDirectory = new File(sPath);
		//create directory
	    if (! oDirectory.exists()){
	    	oDirectory.mkdir();
	    }
	} 
}
