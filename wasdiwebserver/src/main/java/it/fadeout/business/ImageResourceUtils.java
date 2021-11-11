package it.fadeout.business;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;

import it.fadeout.rest.resources.ProcessorsMediaResource;
import wasdi.shared.business.Processor;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.utils.ImageFile;
import wasdi.shared.utils.Utils;

/**
 * Static Methods to handle images related to applications: 
 * can be logos or gallery images
 * @author p.campanella
 *
 */
public class ImageResourceUtils {
	
	private ImageResourceUtils() { 
	}
	
	/**
	 * Base Path of the web application
	 */
	public static String s_sWebAppBasePath = "/var/lib/tomcat8/webapps/wasdiwebserver/";
	
	/**
	 * Check if the extension of a image is valid for WASDI
	 * @param sExt extension to check
	 * @param sValidExtensions List of accepted extensions
	 * @return true if valid
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
	 * @param sPath new dir path
	 */
	public static void createDirectory(String sPath){
		File oDirectory = new File(sPath);
		//create directory
	    if (! oDirectory.exists()){
	    	oDirectory.mkdirs();
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
		String sLogoExtension = getExtensionOfImageInFolder(sPathLogoFolder, asEnableExtension);
		if(sLogoExtension.isEmpty() == false){
			oImage = new ImageFile(sPathLogoFolder + "." + sLogoExtension );
		}
		return oImage;
		
	}

	/**
	 * Get the base relative Path of the folder where processor images are stored
	 * @param sProcessorName
	 * @return
	 */
	public static String getProcessorImagesBasePath(String sProcessorName) {
		
		return ImageResourceUtils.getProcessorImagesBasePath(sProcessorName, true);
	}
	
	/**
	 * Get the path, relative or absolute, of the folder where processor images are stored
	 * @param sProcessorName
	 * @param bRelative
	 * @return
	 */
	public static String getProcessorImagesBasePath(String sProcessorName, boolean bRelative) {
		String sPath = "assets/img/processors/" + sProcessorName + "/";
		
		if (!bRelative) sPath = s_sWebAppBasePath + sPath;
		
		return sPath;
	}
	
	/**
	 * Get the relative path of the processor Logo
	 * @param oProcessor
	 * @return
	 */
	public static String getProcessorLogoRelativePath(Processor oProcessor) {
		
		String sProcessorName = oProcessor.getName();
		
		String sRelativeLogoPath = ImageResourceUtils.getProcessorImagesBasePath(sProcessorName);
		String sAbsoluteLogoPath = ImageResourceUtils.getProcessorImagesBasePath(sProcessorName, false);
				
		ImageFile oLogo = ImageResourceUtils.getImageInFolder(sAbsoluteLogoPath + ProcessorsMediaResource.DEFAULT_LOGO_PROCESSOR_NAME, ProcessorsMediaResource.IMAGE_PROCESSORS_EXTENSIONS );
		
		boolean bExistsLogo = false;
		
		if (oLogo!=null) {
			if (oLogo.exists()) bExistsLogo = true;
		}
		
		if (bExistsLogo) {
			return sRelativeLogoPath + oLogo.getName();
		}
		else {
			
			int iPlaceHolderIndex = oProcessor.getNoLogoPlaceholderIndex();
			
			if (iPlaceHolderIndex == -1) {
				// If we do not have a logo, and a random placeholder has not been assigned, assign it.
				iPlaceHolderIndex = Utils.getRandomNumber(1, 8);
				oProcessor.setNoLogoPlaceholderIndex(iPlaceHolderIndex);
				ProcessorRepository oProcessorRepository = new ProcessorRepository();
				oProcessorRepository.updateProcessor(oProcessor);
			}
			
			String sPlaceHolder = "assets/img/placeholder/placeholder_" + iPlaceHolderIndex + ".jpg";
			
			return sPlaceHolder;
		}
	}
	
	
	/**
	 * Search the extension of a image
	 * @param sPathLogoFolder
	 * @param asEnableExtension
	 * @return
	 */
	public static String getExtensionOfImageInFolder (String sPathLogoFolder, String[] asEnableExtension){
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
				if (!oImage.delete()) {
					Utils.debugLog("Image resource Utils - File " + sFileName + " can't be deleted");
					}
				break;
			} 
			
		}
	}
	
	/**
	 * return a free name for the image (if it is available) 
	 * @param sPathFolder
	 * @return
	 */
	public static String getAvaibleProcessorImageFileName(String sPathFolder) {
		File oFolder = new File(sPathFolder);
		File[] aoListOfFiles = oFolder.listFiles();

		String sReturnValueName = "";
		boolean bIsAvaibleName = false; 
		for (String sAvaibleFileName : ProcessorsMediaResource.IMAGE_NAMES){
			bIsAvaibleName = true;
			sReturnValueName = sAvaibleFileName;
			
			for (File oImage : aoListOfFiles){ 
				String sName = oImage.getName();
				String sFileName = FilenameUtils.removeExtension(sName);	
				
				if(sAvaibleFileName.equalsIgnoreCase(sFileName)){
					bIsAvaibleName = false;
					break;
				} 
				
			}
			
			if(bIsAvaibleName == true){
				break;
			}
			sReturnValueName = "";
		 }

		return sReturnValueName;
	}	
	
	/**
	 * Get the list of relative path of images available for the processor
	 * @param oProcessor
	 * @return
	 */
	public static ArrayList<String> getProcessorImagesList(Processor oProcessor) {
		
		ArrayList<String> asImages = new ArrayList<String>();
		
		String sAbsoluteImagesPath = ImageResourceUtils.getProcessorImagesBasePath(oProcessor.getName(), false);
		String sRelativeImagesPath = ImageResourceUtils.getProcessorImagesBasePath(oProcessor.getName());
						
		File oFolder = new File(sAbsoluteImagesPath);
		
		File[] aoListOfFiles = oFolder.listFiles();
		
		ArrayList<String> asValidNames = new ArrayList<String>(Arrays.asList(ProcessorsMediaResource.IMAGE_NAMES));
		
		if ( aoListOfFiles != null) {
			for (File oImage : aoListOfFiles){
				
				String sImageFileName = oImage.getName();
				String sFileNameWithoutExtension = FilenameUtils.removeExtension(sImageFileName);
				
				if (asValidNames.contains(sFileNameWithoutExtension)) {
					asImages.add(sRelativeImagesPath+sImageFileName);
				}
			}			
		}
		
		
		return asImages;
	}
}
