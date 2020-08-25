package it.fadeout.business;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.ProcessorsMediaResource;
import wasdi.shared.utils.ImageFile;
import wasdi.shared.utils.Utils;

public class ImageResourceUtils {
	
	/**
	 * Check the extension of a image
	 * @param sExt
	 * @param sValidExtensions
	 * @return
	 */
	public static boolean isValidExtension(String sExt,String[] sValidExtensions){
		//Check if the extension is valid
		for (String sValidExtension : sValidExtensions) {
			  if(sValidExtension.equals(sExt.toLowerCase()) ){
				  return true;
			  }
		}
		return false;
	}
	
	/**
	 * Safe create directory in path
	 * @param sPath
	 */
	public static void createDirectory(String sPath){
		File oDirectory = new File(sPath);
		//create directory
	    if (! oDirectory.exists()){
	    	oDirectory.mkdir();
	    }
	} 
	
	/**
	 * Get the image file of the logo
	 * @param sPathLogoFolder
	 * @param asEnableExtension
	 * @return
	 */
	public static ImageFile getImageInFolder(String sPathLogoFolder, String[] asEnableExtension){
		ImageFile oImage = null;
		String sLogoExtension = checkExtensionOfImageInFolder(sPathLogoFolder, asEnableExtension);
		if(sLogoExtension.isEmpty() == false){
			oImage = new ImageFile(sPathLogoFolder + "." + sLogoExtension );
		}
		return oImage;
		
	}
	
	public static String getProcessorLogoBasePath(String sProcessorName) {
		String sPath = "./assets/img/processors/" + sProcessorName + "/";
		return sPath;
	}
	
	public static String getProcessorLogoFullPath(String sProcessorName) {
		
		String sLogoPath = ImageResourceUtils.getProcessorLogoBasePath(sProcessorName);
		
		ImageFile oLogo = ImageResourceUtils.getImageInFolder(sLogoPath, ProcessorsMediaResource.IMAGE_PROCESSORS_EXTENSIONS );
		
		boolean bExistsLogo = false;
		
		if (oLogo!=null) {
			if (oLogo.exists()) bExistsLogo = true;
		}
		
		if (bExistsLogo) {
			return sLogoPath + oLogo.getName();
		}
		else {
			String sPlaceHolder = "assets/img/placeholder/placeholder_" + Utils.getRandomNumber(1, 8) + ".jpg";
			return sPlaceHolder;
		}
	}
	
	
	/**
	 * Search the extension of a image
	 * @param sPathLogoFolder
	 * @param asEnableExtension
	 * @return
	 */
	public static String checkExtensionOfImageInFolder (String sPathLogoFolder, String[] asEnableExtension){
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
	
	/**
	 * Delete a file in a folder
	 * @param sPathFolder
	 * @param sDeleteFileName
	 */
	public static void deleteFileInFolder(String sPathFolder,String sDeleteFileName){
		File oFolder = new File(sPathFolder);
		File[] aoListOfFiles = oFolder.listFiles();
		for (File oImage : aoListOfFiles){ 
			String sName = oImage.getName();
			String sFileName = FilenameUtils.removeExtension(sName);	
			
			if(sDeleteFileName.equalsIgnoreCase(sFileName)){
				oImage.delete();
				break;
			} 
			
		}
	}
}
