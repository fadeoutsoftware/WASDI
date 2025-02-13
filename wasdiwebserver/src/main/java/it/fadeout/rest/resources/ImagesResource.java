package it.fadeout.rest.resources;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import wasdi.shared.business.ImagesCollections;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.users.User;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.utils.ImageFile;
import wasdi.shared.utils.ImageResourceUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;

/**
 * Images Resource.
 * Hosts the API to upload and show Images in WASDI. 
 * Images can be associated to different entities (ie User, Processor, Organization..).
 * Each image must belong to a Collection. Collections of Images are declared in ImagesCollections
 * 
 * Given a collection, the image can also be set in a subfolder.
 * 
 * The API are:
 * 	.upload image
 * 	.get image
 * 	.delete Image
 * 	.There are 2 convience methods for Processor Logo and Images (for retro-compatiability)
 * 
 * @author p.campanella
 *
 */
@Path("/images")
public class ImagesResource {
	
	/**
	 * Upload a generic image in WASDI
	 * @param oInputFileStream File Input Stram
	 * @param oFileMetaData Metadata of the file
	 * @param sSessionId Session Id 
	 * @param sCollection Collection where the image must be added. It is a subfolder of the base images path
	 * @param sImageName Name of the image to add
	 * @param obResize Optional flag to resize the image
	 * @param obThumbnail Optional flag to create a thumbnail of the image
	 * @return Http standard response
	 */
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadImage(@FormDataParam("image") InputStream oInputFileStream, @FormDataParam("image") FormDataContentDisposition oFileMetaData,
										@HeaderParam("x-session-token") String sSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder, @QueryParam("name") String sImageName,
										@QueryParam("resize") Boolean obResize, @QueryParam("thumbnail") Boolean obThumbnail) {
		
		try {
			WasdiLog.debugLog("ImagesResource.uploadImage( collection: " + sCollection + " sImageName: " + sImageName +")");
			
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ImagesResource.uploadImage: invalid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!ImageResourceUtils.isValidCollection(sCollection)) {
				WasdiLog.warnLog("ImagesResource.uploadImage: invalid collection");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			if (Utils.isNullOrEmpty(sImageName)) {
				WasdiLog.warnLog("ImagesResource.uploadImage: invalid image name");
				return Response.status(Status.BAD_REQUEST).build();			
			}			
			
			if (!PermissionsUtils.canUserWriteImage(oUser.getUserId(), sCollection, sFolder, sImageName)) {
				WasdiLog.warnLog("ImagesResource.uploadImage: user cannot access image");
				return Response.status(Status.FORBIDDEN).build();				
			}
						
			if (Utils.isNullOrEmpty(sFolder)) {
				sFolder = "";			
			}
			
			//sanity check: is sImageName safe? It must be a file name, not a path
			if(sImageName.contains("/") || sImageName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\") || sFolder.contains("/") || sFolder.contains("\\") ) {
				WasdiLog.warnLog("ImagesResource.uploadImage: Image or Collection name looks like a path" );
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			String sExt;
			String sFileName;
			
			//get filename and extension 
			if(oFileMetaData != null && Utils.isNullOrEmpty(oFileMetaData.getFileName()) == false){
				
				sFileName = oFileMetaData.getFileName();
				sExt = FilenameUtils.getExtension(sFileName);
				
				WasdiLog.debugLog("ImagesResource.uploadImage: FileName " + sFileName + " Extension: " + sExt);
			} 
			else {
				WasdiLog.warnLog("ImagesResource.uploadImage: File metadata not available");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			// Check if this is an accepted file extension
			if(ImageResourceUtils.isValidExtension(sExt) == false ){
				WasdiLog.warnLog("ImagesResource.uploadImage: extension invalid");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Take path
			String sPath = ImageResourceUtils.getImagesSubPath(sCollection, sFolder);
			sPath += sImageName;
			
			WasdiLog.debugLog("ImagesResource.uploadImage: sPath: " + sPath);
			
			String sExtensionOfSavedImage = ImageResourceUtils.getExtensionOfImageInFolder(WasdiFileUtils.getFileNameWithoutLastExtension(sPath));
			
			//if there is a saved logo with a different extension remove it 
			if(!Utils.isNullOrEmpty(sExtensionOfSavedImage)) {
				
				WasdiLog.debugLog("ImagesResource.uploadImage: cleaning old image");
				
			    File oOldImage = new File(sPath.replace(WasdiFileUtils.getFileNameExtension(sPath), sExtensionOfSavedImage));
			    
			    if (oOldImage.exists()) {
			    	WasdiLog.debugLog("ImagesResource.uploadImage: delete old image with same name and different extension");
			    }
			    
			    if (!oOldImage.delete()) {
			    	WasdiLog.debugLog("ImagesResource.uploadImage: can't delete old image");
			   	}
			}
			else {
				WasdiLog.debugLog("ImagesResource.uploadImage: no old logo present");
			}
		    	   
		    File oTouchFile = new File(sPath);
		    
		    try {
				if (!oTouchFile.createNewFile()) {
					WasdiLog.debugLog("ImagesResource.uploadImage: can't create new file");
				}
				else {
					WasdiLog.debugLog("ImagesResource.uploadImage: created new empty file in path");
				}
			} catch (IOException e) {
				WasdiLog.errorLog("ImagesResource.uploadImage: " + e.toString());
			}	    
		    
		    ImageFile oOutputImage = new ImageFile(sPath);
		    WasdiLog.debugLog("ImagesResource.uploadImage: created Image, saving");
		    boolean bIsSaved =  oOutputImage.saveImage(oInputFileStream);
		    
		    if(bIsSaved == false){
		    	WasdiLog.warnLog("ImagesResource.uploadImage:  not saved!");
		    	return Response.status(Status.BAD_REQUEST).build();
		    }
		    else {
		    	WasdiLog.debugLog("ImagesResource.uploadImage: Image saved");
		    }
		    
			double dBytes = (double) oOutputImage.length();
			double dKilobytes = (dBytes / 1024);
			double dMegabytes = (dKilobytes / 1024);
			
			if( dMegabytes > (double) ImageResourceUtils.s_iMAX_IMAGE_MB_SIZE){
				WasdiLog.warnLog("ImagesResource.uploadImage: image too big, delete it");
				oOutputImage.delete();
		    	return Response.status(Status.BAD_REQUEST).build();
			}
			else {
				WasdiLog.debugLog("ImagesResource.uploadImage: Dimension is ok proceed");
			}
		    
//		    if (obResize!=null) {
//		    	if (obResize) {
//		    		
//		    		WasdiLog.debugLog("ImagesResource.uploadImage: start resizing");
//		    		
//		    		try {
//			    	    boolean bIsResized = oOutputImage.resizeImage(ImageResourceUtils.s_iLOGO_SIZE, ImageResourceUtils.s_iLOGO_SIZE);
//			    	    
//			    	    if(bIsResized == false){
//			    	    	WasdiLog.debugLog("ImagesResource.uploadImage: error in resize");
//			    	    }	    				    			
//		    		}
//		    		catch (Throwable oEx) {
//		    			WasdiLog.warnLog("ImagesResource.uploadImage: exception in resize " + oEx.toString());
//		    		}
//		    	}
//		    }
//		    
//		    WasdiLog.debugLog("ImagesResource.uploadImage: check if we need to create a thumb");
//		    
//		    if (obThumbnail!=null) {
//		    	if (obThumbnail) {
//		    		WasdiLog.debugLog("ImagesResource.uploadImage: create thumb");
//		    		try {
//		    			ImageResourceUtils.createThumbOfImage(sPath);
//		    		}
//		    		catch (Throwable oEx) {
//		    			WasdiLog.warnLog("ImagesResource.uploadImage: exception in thumb " + oEx.toString());
//		    		}
//		    	}
//		    }
		    
		    WasdiLog.debugLog("ImagesResource.uploadImage: ok, all done!");
		    
			return Response.status(Status.OK).build();
		}
		catch (Throwable oEx) {
			WasdiLog.errorLog("ImagesResource.uploadImage: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	
	
	
	/**
	 * Gets an image as a Byte Stream
	 * @param sSessionId User Session Id
	 * @param sCollection Processor Id
	 * @return Logo byte stream
	 */
	@GET
	@Path("/get")
	public Response getImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("token") String sTokenSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder, @QueryParam("name") String sImageName) {
		
		try {
			
			// Check session
			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}

			WasdiLog.debugLog("ImagesResource.getImage( collection: " + sCollection + " sImageName: " + sImageName +")");
			
			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ImagesResource.getImage: no valid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!ImageResourceUtils.isValidCollection(sCollection)) {
				WasdiLog.warnLog("ImagesResource.getImage: invalid collection");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			if(Utils.isNullOrEmpty(sImageName)) {
				WasdiLog.warnLog("ImagesResource.getImage: Image name is null" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			if (!PermissionsUtils.canUserAccessImage(oUser.getUserId(), sCollection, sFolder, sImageName)) {
				WasdiLog.warnLog("ImagesResource.getImage: invalid user or session");
				return Response.status(Status.FORBIDDEN).build();				
			}			
			
			if (sFolder==null) sFolder="";
			
			//sanity check: are the inputs safe? It must be a file name, not a path
			if(sImageName.contains("/") || sImageName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\")|| sFolder.contains("/") || sFolder.contains("\\")) {
				WasdiLog.warnLog("ImagesResource.getImage: Image or Collection name looks like a path" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
					
			String sPathLogoFolder = ImageResourceUtils.getImagesSubPath(sCollection, sFolder);
			String sAbsolutePath = sPathLogoFolder + sImageName;
			
			WasdiLog.debugLog("ImagesResource.getImage: sAbsolutePath " + sAbsolutePath);
			
			ImageFile oLogo = new ImageFile(sAbsolutePath);
			
			//Check the logo and extension
			if(oLogo.exists() == false){
				WasdiLog.warnLog("ImagesResource.getImage: unable to find image in " + sAbsolutePath);
				return Response.status(Status.NO_CONTENT).build();
			}
			
			//prepare buffer and send the logo to the client 
			ByteArrayInputStream abImageLogo = oLogo.getByteArrayImage();
			
		    return Response.ok(abImageLogo).build();			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ImagesResource.getImage: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	

	/**
	 * Deletes one of the gallery images of a processor
	 * @param sSessionId User session Id
	 * @param sProcessorId Processor Id
	 * @param sImageName Image Name
	 * @return std http response
	 */
	@DELETE
	@Path("/delete")
	public Response deleteImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder, @QueryParam("name") String sImageName) {
		
		WasdiLog.debugLog("ImagesResource.deleteImage( Collection: " + sCollection + ", Image Name: " + sImageName + " )");
		
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser==null) {
			WasdiLog.warnLog("ImagesResource.deleteImage: user or session invalid");
			return Response.status(Status.UNAUTHORIZED).build();
		}
						
		if(Utils.isNullOrEmpty(sImageName)) {
			WasdiLog.warnLog("ImagesResource.deleteImage: Image name is null" );
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		if (!ImageResourceUtils.isValidCollection(sCollection)) {
			WasdiLog.warnLog("ImagesResource.deleteImage: invalid collection");
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		if (!PermissionsUtils.canUserWriteImage(oUser.getUserId(), sCollection, sFolder, sImageName)) {
			WasdiLog.warnLog("ImagesResource.deleteImage: invalid user or session");
			return Response.status(Status.FORBIDDEN).build();				
		}		
		
		if (sFolder==null) sFolder="";		

		//sanity check: is sImageName safe? It must be a file name, not a path
		if(sImageName.contains("/") || sImageName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\")|| sFolder.contains("/") || sFolder.contains("\\")) {
			WasdiLog.warnLog("ImagesResource.deleteImage: Image or Collection name looks like a path" );
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		String sPath = ImageResourceUtils.getImagesSubPath(sCollection, sFolder);
		String sFilePath = sPath+sImageName;
		
		File oFile = new File(sFilePath);
		
		if (!oFile.exists()) {
			WasdiLog.warnLog("ImagesResource.deleteImage: file " + sImageName +" not found");
			return Response.status(Status.NOT_FOUND).build();			
		}
				
		//delete file
		try {
			if (!oFile.delete()) {
				WasdiLog.warnLog("ImagesResource.deleteImage: error deleting file ");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();				
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("ImagesResource.deleteImage: exception " + oE );
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		return Response.status(Status.OK).build();
	}	
	
	/**
	 * Upload a new processor Logo
	 * Utility method for processors logo. 
	 * If the logo exists it is overwritten.
	 * All logos are resized to support the LOGO_SIZE
	 * 
	 * @param oInputFileStream Stream of the logo image
	 * @param oFileMetaData Info about the file
	 * @param sSessionId User session
	 * @param sProcessorId Processor Id
	 * @return std http response
	 */
	@POST
	@Path("/processors/logo/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadProcessorLogo(@FormDataParam("image") InputStream oInputFileStream, @FormDataParam("image") FormDataContentDisposition oFileMetaData,
										@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {
		
		try {
			WasdiLog.debugLog("ImagesResource.uploadProcessorLogo( ProcId: " + sProcessorId + ")");
			
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ImagesResource.uploadProcessorLogo: invalid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			WasdiLog.debugLog("ImagesResource.uploadProcessorLogo: get Processor " + sProcessorId);	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessor == null) {
				WasdiLog.warnLog("ImagesResource.uploadProcessorLogo: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}
			
			if (!PermissionsUtils.canUserWriteProcessor(oUser.getUserId(), sProcessorId)) {
				WasdiLog.warnLog("ImagesResource.uploadProcessorLogo: processor not accesable by user " + oUser.getUserId());
				return Response.status(Status.FORBIDDEN).build();								
			}
			
			String sFileName = ImageResourceUtils.s_sDEFAULT_LOGO_PROCESSOR_NAME;
			
			//get filename and extension 
			if(oFileMetaData != null && Utils.isNullOrEmpty(oFileMetaData.getFileName()) == false){
				
				String sInputFileName = oFileMetaData.getFileName();
				sFileName += "."+ FilenameUtils.getExtension(sInputFileName);
			} 
			
			Response oResponse = uploadImage(oInputFileStream, oFileMetaData, sSessionId, "processors", oProcessor.getName(), sFileName, true, true);
			
			if (oResponse.getStatus()==200) {
				oProcessor.setLogo(ImageResourceUtils.getImageLink(ImagesCollections.PROCESSORS.getFolder(), oProcessor.getName(), sFileName));
				oProcessorRepository.updateProcessor(oProcessor);
				oProcessorRepository.updateProcessorDate(oProcessor);
			}
			
			return oResponse;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ImagesResource.uploadProcessorLogo: exception " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

	}
	
	
	/**
	 * Upload a new image for the gallery of the processor
	 * 
	 * @param oFileInputStream Stream of the image file
	 * @param oFileMetaData File metadata
	 * @param sSessionId User Session Id
	 * @param sProcessorId Processor Id
	 * @return std http response
	 */
	@POST
	@Path("/processors/gallery/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadProcessorImage(@FormDataParam("image") InputStream oFileInputStream, @FormDataParam("image") FormDataContentDisposition oFileMetaData,
										@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {
		
		WasdiLog.debugLog("ImagesResource.uploadProcessorImage( ProcId: " + sProcessorId + " )");
		
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser==null) {
			WasdiLog.warnLog("ImagesResource.uploadProcessorImage: user or session invalid");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			WasdiLog.warnLog("ImagesResource.uploadProcessorImage: unable to find processor " + sProcessorId);
			return Response.status(Status.NOT_FOUND).build();
		}
		
		if (!PermissionsUtils.canUserWriteProcessor(oUser.getUserId(), sProcessorId)) {
			WasdiLog.warnLog("ImagesResource.uploadProcessorImage: user cannot access " + oUser.getUserId());
			return Response.status(Status.FORBIDDEN).build();								
		}
				
		// Take path
		String sAbsolutePathFolder = ImageResourceUtils.getImagesSubPath(ImagesCollections.PROCESSORS.getFolder(), oProcessor.getName());
		String sAvaibleFileName = ImageResourceUtils.getAvaibleProcessorImageFileName(sAbsolutePathFolder);
		
		//get filename and extension 
		if(oFileMetaData != null && Utils.isNullOrEmpty(oFileMetaData.getFileName()) == false){
			
			String sInputFileName = oFileMetaData.getFileName();
			sAvaibleFileName += "." + FilenameUtils.getExtension(sInputFileName);
		} 		
		
		WasdiLog.debugLog("ImagesResource.uploadProcessorImage: available file name: " + sAvaibleFileName);
		
		if(sAvaibleFileName.isEmpty()){
			WasdiLog.warnLog("ImagesResource.uploadProcessorImage: max images count reached");
			//the user have reach the max number of images 
	    	return Response.status(Status.BAD_REQUEST).build();
		}
		
		Response oResponse = uploadImage(oFileInputStream, oFileMetaData, sSessionId, "processors", oProcessor.getName(), sAvaibleFileName, null, true);
		
		if (oResponse.getStatus() == 200) {
			String sImageLink = ImageResourceUtils.getImageLink(ImagesCollections.PROCESSORS.getFolder(), oProcessor.getName(), sAvaibleFileName);
			
			PrimitiveResult oPrimitiveResult = new PrimitiveResult();
			oPrimitiveResult.setStringValue(sImageLink);
			
			return Response.ok(oPrimitiveResult).build();			
		}
		else {
			return oResponse;
		}
	}
	
	/**
	 * Gets an image as a Byte Stream
	 * @param sSessionId User Session Id
	 * @param sCollection Processor Id
	 * @return Logo byte stream
	 */
	@GET
	@Path("/exists")
	public Response existsImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("token") String sTokenSessionId, @QueryParam("collection") String sCollection, @QueryParam("folder") String sFolder, @QueryParam("name") String sImageName) {
		
		try {
			
			// Check session
			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}

			WasdiLog.debugLog("ImagesResource.existsImage( collection: " + sCollection + " sImageName: " + sImageName +")");
			
			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ImagesResource.existsImage: no valid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!ImageResourceUtils.isValidCollection(sCollection)) {
				WasdiLog.warnLog("ImagesResource.existsImage: invalid collection");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			if(Utils.isNullOrEmpty(sImageName)) {
				WasdiLog.warnLog("ImagesResource.existsImage: Image name is null" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			if (!PermissionsUtils.canUserAccessImage(oUser.getUserId(), sCollection, sFolder, sImageName)) {
				WasdiLog.warnLog("ImagesResource.existsImage: invalid user or session");
				return Response.status(Status.FORBIDDEN).build();				
			}			
			
			if (sFolder==null) sFolder="";
			
			//sanity check: are the inputs safe? It must be a file name, not a path
			if(sImageName.contains("/") || sImageName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\")|| sFolder.contains("/") || sFolder.contains("\\")) {
				WasdiLog.warnLog("ImagesResource.existsImage: Image or Collection name looks like a path" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
					
			String sPathLogoFolder = ImageResourceUtils.getImagesSubPath(sCollection, sFolder);
			String sAbsolutePath = sPathLogoFolder + sImageName;
			
			WasdiLog.debugLog("ImagesResource.existsImage: sAbsolutePath " + sAbsolutePath);
			
			File oImage = new File(sAbsolutePath);
			
			//Check the logo and extension
			if(oImage.exists() == false){
				WasdiLog.debugLog("ImagesResource.existsImage: unable to find image in " + sAbsolutePath);
				return Response.status(Status.NOT_FOUND).build();
			}
			
		    return Response.ok().build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ImagesResource.existsImage: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}		
}
