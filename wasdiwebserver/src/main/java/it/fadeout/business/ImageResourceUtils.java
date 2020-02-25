package it.fadeout.business;

import java.io.File;

import wasdi.shared.business.ImageFile;

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
	
	// get image with unknow  extension
	public ImageFile getImageInFolder(String sPathLogoFolder, String[] asEnableExtension){
		ImageFile oImage = null;
		String sLogoExtension = checkExtensionOfImageInFolder(sPathLogoFolder, asEnableExtension);
		if(sLogoExtension.isEmpty() == false){
			oImage = new ImageFile(sPathLogoFolder + "." + sLogoExtension );
		}
		return oImage;
		
	}
	
	
	// get extension of the image if it's in the list of enable extension
	public String checkExtensionOfImageInFolder (String sPathLogoFolder, String[] asEnableExtension){
		File oLogo = null;
		String sExtensionReturnValue = "";
		for (String sValidExtension : asEnableExtension) {
			oLogo = new File(sPathLogoFolder + "." + sValidExtension );
		    if (oLogo.exists()){
		    	sExtensionReturnValue = sValidExtension;
		    	break;
		    }

		}
		return sExtensionReturnValue;
	}
}
