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
import it.fadeout.business.ImageResourceUtils;
import wasdi.shared.business.Processor;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.utils.ImageFile;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;

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
										@HeaderParam("x-session-token") String sSessionId, @QueryParam("collection") String sCollection, @QueryParam("name") String sImageName,
										@QueryParam("resize") Boolean obResize, @QueryParam("thumbnail") Boolean obThumbnail) {
		
		try {
			WasdiLog.debugLog("ImagesResource.uploadImage( collection: " + sCollection + " sImageName: " + sImageName +")");
			
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.debugLog("ImagesResource.uploadImage: invalid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (Utils.isNullOrEmpty(sCollection)) {
				WasdiLog.debugLog("ImagesResource.uploadImage: invalid collection");
				return Response.status(Status.BAD_REQUEST).build();			
			}

			if (Utils.isNullOrEmpty(sImageName)) {
				WasdiLog.debugLog("ImagesResource.uploadImage: invalid image name");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			//sanity check: is sImageName safe? It must be a file name, not a path
			if(sImageName.contains("/") || sImageName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\")) {
				WasdiLog.debugLog("ImagesResource.uploadImage: Image or Collection name looks like a path" );
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
				WasdiLog.debugLog("ImagesResource.uploadImage: File metadata not available");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			// Check if this is an accepted file extension
			if(ImageResourceUtils.isValidExtension(sExt) == false ){
				WasdiLog.debugLog("ImagesResource.uploadImage: extension invalid");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// Take path
			String sPath = ImageResourceUtils.getImagesSubPath(sCollection);
			sPath += sImageName;
			
			WasdiLog.debugLog("ImagesResource.uploadImage: sPath: " + sPath);
			
			String sExtensionOfSavedImage = ImageResourceUtils.getExtensionOfImageInFolder(Utils.getFileNameWithoutLastExtension(sPath));
			
			//if there is a saved logo with a different extension remove it 
			if(!Utils.isNullOrEmpty(sExtensionOfSavedImage)) {
				
			    File oOldImage = new File(sPath.replace(Utils.GetFileNameExtension(sPath), sExtensionOfSavedImage));
			    
			    if (oOldImage.exists()) {
			    	WasdiLog.debugLog("ImagesResource.uploadImage: delete old image with same name and different extension");
			    }
			    
			    if (!oOldImage.delete()) {
			    	WasdiLog.debugLog("ImagesResource.uploadImage: can't delete old image");
			   	}
			}
		    	    
		    File oTouchFile = new File(sPath);
		    
		    try {
				if (!oTouchFile.createNewFile()) {
					WasdiLog.debugLog("ImagesResource.uploadImage: can't create new file");
				}
			} catch (IOException e) {
				WasdiLog.debugLog("ImagesResource.uploadImage: " + e.toString());
				e.printStackTrace();
			}	    
		    
		    ImageFile oOutputImage = new ImageFile(sPath);
		    boolean bIsSaved =  oOutputImage.saveImage(oInputFileStream);
		    
		    if(bIsSaved == false){
		    	WasdiLog.debugLog("ImagesResource.uploadImage:  not saved!");
		    	return Response.status(Status.BAD_REQUEST).build();
		    }
		    
			double dBytes = (double) oOutputImage.length();
			double dKilobytes = (dBytes / 1024);
			double dMegabytes = (dKilobytes / 1024);
			
			if( dMegabytes > (double) ImageResourceUtils.s_iMAX_IMAGE_MB_SIZE){
				WasdiLog.debugLog("ImagesResource.uploadImage: image too big, delete it");
				oOutputImage.delete();
		    	return Response.status(Status.BAD_REQUEST).build();
			}
		    
		    if (obResize!=null) {
		    	if (obResize) {
		    	    boolean bIsResized = oOutputImage.resizeImage(ImageResourceUtils.s_iLOGO_SIZE, ImageResourceUtils.s_iLOGO_SIZE);
		    	    
		    	    if(bIsResized == false){
		    	    	WasdiLog.debugLog("ImagesResource.uploadImage: error in resize");
		    	    }	    		
		    	}
		    }
		    
		    if (obThumbnail!=null) {
		    	if (obThumbnail) {
		    		ImageResourceUtils.createThumbOfImage(sPath);
		    	}
		    }
		    
			return Response.status(Status.OK).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ImagesResource.uploadImage: exception " + oEx.toString());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}	
	
	
	/**
	 * Gets the logo of a processor as a Byte Stream
	 * @param sSessionId User Session Id
	 * @param sCollection Processor Id
	 * @return Logo byte stream
	 */
	@GET
	@Path("/get")
	public Response getImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("collection") String sCollection, @QueryParam("name") String sImageName) {
		
		try {
			WasdiLog.debugLog("ImagesResource.getImage( collection: " + sCollection + " sImageName: " + sImageName +")");
			
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.debugLog("ImagesResource.getProcessorLogo: no valid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			//sanity check: is sImageName safe? It must be a file name, not a path
			if(sImageName.contains("/") || sImageName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\")) {
				WasdiLog.debugLog("ImagesResource.getImage: Image or Collection name looks like a path" );
				return Response.status(Status.BAD_REQUEST).build();
			}			
					
			String sPathLogoFolder = ImageResourceUtils.getImagesSubPath(sCollection);
			String sAbsolutePath = sPathLogoFolder + sImageName;
			
			WasdiLog.debugLog("ImagesResource.getImage: sAbsolutePath " + sAbsolutePath);
			
			ImageFile oLogo = new ImageFile(sAbsolutePath);
			
			//Check the logo and extension
			if(oLogo.exists() == false){
				WasdiLog.debugLog("ImagesResource.getImage: unable to find image in " + sAbsolutePath);
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
	public Response deleteImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("collection") String sCollection, @QueryParam("name") String sImageName) {
		
		WasdiLog.debugLog("ImagesResource.deleteImage( Collection: " + sCollection + ", Image Name: " + sImageName + " )");
		
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser==null) {
			WasdiLog.debugLog("ImagesResource.deleteImage: user or session invalid");
			return Response.status(Status.UNAUTHORIZED).build();
		}
						
		if(Utils.isNullOrEmpty(sImageName)) {
			WasdiLog.debugLog("ImagesResource.deleteImage: Image name is null" );
			return Response.status(Status.BAD_REQUEST).build();
		}

		//sanity check: is sImageName safe? It must be a file name, not a path
		if(sImageName.contains("/") || sImageName.contains("\\") || sCollection.contains("/") || sCollection.contains("\\")) {
			WasdiLog.debugLog("ImagesResource.deleteImage: Image or Collection name looks like a path" );
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		String sPath = ImageResourceUtils.getImagesSubPath(sCollection);
		String sFilePath = sPath+sImageName;
		
		File oFile = new File(sFilePath);
		
		if (!oFile.exists()) {
			WasdiLog.debugLog("ImagesResource.deleteImage: file " + sImageName +" not found");
			return Response.status(Status.NOT_FOUND).build();			
		}
				
		//delete file
		try {
			if (!oFile.delete()) {
				WasdiLog.debugLog("ImagesResource.deleteImage: error deleting file ");
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
				WasdiLog.debugLog("ImagesResource.uploadProcessorLogo: invalid user or session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			WasdiLog.debugLog("ImagesResource.uploadProcessorLogo: get Processor " + sProcessorId);	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessor == null) {
				WasdiLog.debugLog("ImagesResource.uploadProcessorLogo: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}
			
			if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), sProcessorId)) {
				WasdiLog.debugLog("ImagesResource.uploadProcessorLogo: processor not accesable by user " + oUser.getUserId());
				return Response.status(Status.UNAUTHORIZED).build();								
			}
			
			String sFileName = ImageResourceUtils.s_sDEFAULT_LOGO_PROCESSOR_NAME;
			
			//get filename and extension 
			if(oFileMetaData != null && Utils.isNullOrEmpty(oFileMetaData.getFileName()) == false){
				
				String sInputFileName = oFileMetaData.getFileName();
				sFileName += FilenameUtils.getExtension(sInputFileName);
			} 		
			
			
			
			Response oResponse = uploadImage(oInputFileStream, oFileMetaData, sSessionId, oProcessor.getName(), sFileName, true, true);
			
			if (oResponse.getStatus()==200) {
				oProcessor.setLogo(ImageResourceUtils.getImageLink(oProcessor.getName(), sFileName));
				oProcessorRepository.updateProcessor(oProcessor);
				oProcessorRepository.updateProcessorDate(oProcessor);
			}
			
			return oResponse;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ImagesResource.deleteImage: exception " + oEx);
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
			WasdiLog.debugLog("ImagesResource.uploadProcessorImage: user or session invalid");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			WasdiLog.debugLog("ImagesResource.uploadProcessorImage: unable to find processor " + sProcessorId);
			return Response.status(Status.NOT_FOUND).build();
		}
		
		if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), sProcessorId)) {
			WasdiLog.debugLog("ImagesResource.uploadProcessorImage: user cannot access " + oUser.getUserId());
			return Response.status(Status.UNAUTHORIZED).build();								
		}
				
		// Take path
		String sAbsolutePathFolder = ImageResourceUtils.getImagesSubPath(oProcessor.getName());
		String sAvaibleFileName = ImageResourceUtils.getAvaibleProcessorImageFileName(sAbsolutePathFolder);
		
		//get filename and extension 
		if(oFileMetaData != null && Utils.isNullOrEmpty(oFileMetaData.getFileName()) == false){
			
			String sInputFileName = oFileMetaData.getFileName();
			sAvaibleFileName += FilenameUtils.getExtension(sInputFileName);
		} 		
		
		WasdiLog.debugLog("ImagesResource.uploadProcessorImage: available file name: " + sAvaibleFileName);
		
		if(sAvaibleFileName.isEmpty()){
			WasdiLog.debugLog("ImagesResource.uploadProcessorImage: max images count reached");
			//the user have reach the max number of images 
	    	return Response.status(Status.BAD_REQUEST).build();
		}
		
		Response oResponse = uploadImage(oFileInputStream, oFileMetaData, sSessionId, oProcessor.getName(), sAvaibleFileName, null, null);
		
		if (oResponse.getStatus() == 200) {
			String sImageLink = ImageResourceUtils.getImageLink(oProcessor.getName(), sAvaibleFileName);
			
			PrimitiveResult oPrimitiveResult = new PrimitiveResult();
			oPrimitiveResult.setStringValue(sImageLink);
			
			return Response.ok(oPrimitiveResult).build();			
		}
		else {
			return oResponse;
		}
	}
}
