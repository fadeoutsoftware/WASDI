package it.fadeout.business;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;

import com.google.common.io.Files;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.ImagesResource;
import wasdi.shared.business.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.utils.ImageFile;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Static Methods to handle images related to applications: 
 * can be logos or gallery images
 * @author p.campanella
 *
 */
public class ImageResourceUtils {
	
	/**
	 * List of extensions enabled to upload a user image
	 */
	public static String[] USER_IMAGE_ENABLED_EXTENSIONS = {"jpg", "png", "svg"};
	
	/**
	 * Width of Thumbail images
	 */
	public static int s_iTHUMB_WIDTH = 44;
	/**
	 * Heigth of Thumbail images
	 */
	public static int s_iTHUMB_HEIGHT = 50;
	
	/**
	 * Max image size in Mb
	 */
	public static int s_iMAX_IMAGE_MB_SIZE = 2;
	/**
	 * Default base name of the processor logo
	 */
	public static String s_sDEFAULT_LOGO_PROCESSOR_NAME = "logo";
	/**
	 * Size of the logo in pixels
	 */
	public static Integer s_iLOGO_SIZE = 540;
	/**
	 * Pre-defined names of images in the processor gallery
	 */
	public static String[] s_asIMAGE_NAMES = { "1", "2", "3", "4", "5", "6" };	

	
	private ImageResourceUtils() { 
	}
	
	/**
	 * Base Path of the web application
	 */
	public static String s_sWebAppBasePath = "/var/lib/tomcat8/webapps/wasdiwebserver/";
	
	/**
	 * Check if the extension of an image is valid for WASDI
	 * @param sExt extension to check
	 * @param sValidExtensions List of accepted extensions
	 * @return true if valid
	 */
	public static boolean isValidExtension(String sExt){
		//Check if the extension is valid
		for (String sValidExtension : USER_IMAGE_ENABLED_EXTENSIONS) {
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
	public static ImageFile getImageInFolder(String sPathLogoFolder){
		ImageFile oImage = null;
		String sLogoExtension = getExtensionOfImageInFolder(sPathLogoFolder);
		
		WasdiLog.debugLog("ImageResourceUtils.getImageInFolder " + sLogoExtension);
		
		if(sLogoExtension.isEmpty() == false){
			String sPath = sPathLogoFolder;
			
			WasdiLog.debugLog("ImageResourceUtils.getImageInFolder: sPath "+ sPath);
			
			if (sPath.endsWith("."+sLogoExtension) == false) {
				sPath = sPathLogoFolder + "." + sLogoExtension ;
			}
			
			oImage = new ImageFile(sPath);
		}
		else {
			WasdiLog.debugLog("ImageResourceUtils.getImageInFolder: logo empty ");
		}
		return oImage;
		
	}
	
	/**
	 * Get the base path of images in WASDI
	 * @return
	 */
	public static String getImagesBasePath() {
		String sWasdiBasePath = Wasdi.getDownloadPath();
		String sImagesBasePath = sWasdiBasePath + "images/";
		return sImagesBasePath;
	}
	
	/**
	 * Get the path of a subofolder of images in WASDI
	 * @return
	 */
	public static String getImagesSubPath(String sCollection, String sFolder) {
		String sPath = getImagesBasePath();
		sPath += sCollection;
		if (!sPath.endsWith("/")) sPath += "/";
		
		if (Utils.isNullOrEmpty(sFolder)==false) {
			sPath += sFolder;
			if (!sPath.endsWith("/")) sPath += "/";
		}
		
		ImageResourceUtils.createDirectory(sPath);
		
		return sPath;
	}
	
	/**
	 * Get the relative path of the processor Logo
	 * @param oProcessor
	 * @return
	 */
	public static String getProcessorLogoPlaceholderPath(Processor oProcessor) {
		
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
	
	
	/**
	 * Search the extension of a image
	 * @param sPathLogoFolder
	 * @param asEnableExtension
	 * @return
	 */
	public static String getExtensionOfImageInFolder (String sPathLogoFolder){
		File oLogo = null;
		String sExtensionReturnValue = "";
		for (String sValidExtension : USER_IMAGE_ENABLED_EXTENSIONS) {
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
					WasdiLog.debugLog("Image resource Utils - File " + sFileName + " can't be deleted");
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
		for (String sAvaibleFileName : ImageResourceUtils.s_asIMAGE_NAMES){
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
	 * Get the list of links of images available for the processor
	 * @param oProcessor
	 * @return
	 */
	public static ArrayList<String> getProcessorImagesList(Processor oProcessor) {
		
		ArrayList<String> asImages = new ArrayList<String>();
		
		String sAbsoluteImagesPath = ImageResourceUtils.getImagesSubPath(ImagesCollections.PROCESSORS.getFolder(), oProcessor.getName());
						
		File oFolder = new File(sAbsoluteImagesPath);
		
		File[] aoListOfFiles = oFolder.listFiles();
		
		ArrayList<String> asValidNames = new ArrayList<String>(Arrays.asList(ImageResourceUtils.s_asIMAGE_NAMES));
		
		if ( aoListOfFiles != null) {
			for (File oImage : aoListOfFiles){
				
				String sImageFileName = oImage.getName();
				String sFileNameWithoutExtension = FilenameUtils.removeExtension(sImageFileName);
				
				if (asValidNames.contains(sFileNameWithoutExtension)) {
					asImages.add(ImageResourceUtils.getImageLink(ImagesCollections.PROCESSORS.getFolder(), oProcessor.getName(), sImageFileName));
				}
			}			
		}
		
		
		return asImages;
	}
	
	/**
	 * Create a thumb of the image in sAbsoluteImageFilePath
	 * @param sAbsoluteImageFilePath path of the input image
	 * @return Path of the thumbail or null in case of problems
	 */
	public static String createThumbOfImage(String sAbsoluteImageFilePath) {
	    try {
	    	
	    	if (Utils.isNullOrEmpty(sAbsoluteImageFilePath)) {
	    		WasdiLog.infoLog("ImageResourceUtils.createThumbOfImage: sAbsoluteImageFilePath null ");
	    		return null;
	    	}
	    	
	    	ImageFile oNewImage = new ImageFile(sAbsoluteImageFilePath);
	    	
	    	if (oNewImage.exists() == false) {
	    		WasdiLog.infoLog("ImageResourceUtils.createThumbOfImage: oNewImage not found at " + sAbsoluteImageFilePath);
	    		return null;
	    	}
	    	
		    // Create the thumb:	    	
	    	WasdiLog.debugLog("ImageResourceUtils.createThumbOfImage: creating thumb");
	    	
	    	String [] asSplit = sAbsoluteImageFilePath.split("\\.");
	    	
	    	String sThumbPath = asSplit[0] + "_thumb." + asSplit[1];	    	
	    	
	    	File oThumb = new File(sThumbPath);
	    	
	    	Files.copy(oNewImage, oThumb);
	    	
	    	WasdiLog.debugLog("ImageResourceUtils.createThumbOfImage: thumb file created " + sThumbPath);
	    	
	    	ImageFile oImageThumb = new ImageFile(sThumbPath);
	    	
	    	if (!oImageThumb.resizeImage(ImageResourceUtils.s_iTHUMB_HEIGHT, ImageResourceUtils.s_iTHUMB_WIDTH)) {
	    		WasdiLog.debugLog("ImageResourceUtils.createThumbOfImage: error resizing the thumb");
	    	}
	    	
	    	return sThumbPath;
	    	
	    }
	    catch (Exception oEx) {
	    	WasdiLog.debugLog("ImageResourceUtils.createThumbOfImage:  error creating the thumb " + oEx.toString());
		}
	    
	    return null;
	}
	
	/**
	 * Get the http link to access an image
	 * @param sCollection Image Collection 
	 * @param sImage Image Name
	 * @return Url to get the image or empty string
	 */
	public static String getImageLink(String sCollection, String sFolder, String sImage) {
		try {
			String sAddress = WasdiConfig.Current.baseUrl;
			
			if (!sAddress.endsWith("/")) sAddress += "/";
			
			sAddress +="images/get?collection="+sCollection;
			if (!Utils.isNullOrEmpty(sFolder)) {
				sAddress += "&folder=" + sFolder;
			}
			
			sAddress += "&name="+sImage;
			
			return sAddress;
		}
	    catch (Exception oEx) {
	    	WasdiLog.debugLog("ImageResourceUtils.getImageLink:  error " + oEx.toString());
		}		
		
		return "";
	}
}
