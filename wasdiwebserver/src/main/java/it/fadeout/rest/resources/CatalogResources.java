package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Stream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import it.fadeout.rest.resources.largeFileDownload.ZipStreamingOutput;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.users.User;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.parameters.FtpUploadParameters;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.products.FtpTransferViewModel;
import wasdi.shared.viewmodels.products.ProductPropertiesViewModel;

/**
 * Catalog Resource
 * 
 * Hosts APIs for:
 * 	.download files from wasdi
 * 	.send files to internal or external sftp
 *  .check if a file is available on node
 *  .ingest (add) a file uploaded in a workspace 
 *   
 * @author p.campanella
 *
 */
@Path("/catalog")
public class CatalogResources {
		
	/**
	 * Download a File from the name
	 * @param sSessionId User session id, as available in headers
	 * @param sTokenSessionId Same user session id that can be also in this case passed as a query param
	 * @param sFileName name of the file to download 
	 * @param sWorkspaceId workspace where the file is
	 * @return Stream of the file
	 */
	@GET
	@Path("downloadbyname")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadEntryByName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("token") String sTokenSessionId,
			@QueryParam("filename") String sFileName,
			@QueryParam("workspace") String sWorkspaceId,
			@QueryParam("procws") String sProcessObjId
			)
	{			

		WasdiLog.debugLog("CatalogResources.downloadEntryByName( FileName: " + sFileName + ", Ws: " + sWorkspaceId);
		
		try {
			
			// Check session
			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}

			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser == null) {
				WasdiLog.warnLog("CatalogResources.downloadEntryByName: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// Check if the user can access the workspace
			if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				WasdiLog.warnLog("CatalogResources.downloadEntryByName: user cannot access workspace");
				return Response.status(Status.FORBIDDEN).build();				
			}
			
			// Get the File object
			File oFile = this.getEntryFile(sFileName, sWorkspaceId);
			
			ResponseBuilder oResponseBuilder = null;
			
			if (oFile == null) {
				oFile = PermissionsUtils.getFileFromS3Volume(oUser.getUserId(), sFileName, sWorkspaceId, sProcessObjId);
			}
			
			if(oFile == null) {				
				// File invalid
				WasdiLog.debugLog("CatalogResources.downloadEntryByName: file not readable");
				oResponseBuilder = Response.serverError();	
			} 
			else {
				
				// Ok send the file to the user
				FileStreamingOutput oStream;
				
				// Some files, like .shp or .dim, are not sigle files but folders: see if we need to make a zip
				boolean bMustZip = mustBeZipped(oFile); 
				
				if(bMustZip) {
					// Yes, zip and stream on the fly
					WasdiLog.debugLog("CatalogResources.downloadEntryByName: file " + oFile.getName() + " must be zipped");
					return prepareAndReturnZip(oFile);
				} else {
					// No, just return the stream
					WasdiLog.debugLog("CatalogResources.downloadEntryByName: no need to zip file " + oFile.getName());
					oStream = new FileStreamingOutput(oFile);
					WasdiLog.debugLog("CatalogResources.downloadEntryByName: file ok return content");
					oResponseBuilder = Response.ok(oStream);
					oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oFile.getName());
					//oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
					oResponseBuilder.header("Access-Control-Expose-Headers", "Content-Disposition");
				}
			}
			
			return oResponseBuilder.build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("CatalogResources.downloadEntryByName: " + oEx);
		}
		return null;
	}
	
	/**
	 * Check if a file is present on the actual node
	 * @param sSessionId User Session
	 * @param sFileName File Name
	 * @param sWorkspaceId Workspace where the file is
	 * @return Primitive Result with boolValue = true if the file exist or false if not
	 */
	@GET
	@Path("fileOnNode")
	@Produces({"application/xml", "application/json", "text/xml"})
	@Consumes({"application/xml", "application/json", "text/xml"})
	public Response checkFileByNode(@QueryParam("token") String sSessionId, @QueryParam("filename") String sFileName, @QueryParam("workspace") String sWorkspaceId)
	{	
		WasdiLog.debugLog("CatalogResources.checkFileByNode");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("CatalogResources.checkFileByNode: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
			WasdiLog.warnLog("CatalogResources.checkFileByNode: user cannot access workspace");
			return Response.status(Status.FORBIDDEN).build();			
		}
		
		try {
			String sTargetFilePath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId) + sFileName;

			File oFile = new File(sTargetFilePath);
			
			WasdiLog.debugLog("CatalogResources.checkFileByNode: path " + sTargetFilePath);
			
			boolean bExists = oFile.exists();
			
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setBoolValue(bExists);
			
			WasdiLog.debugLog("CatalogResources.checkFileByNode " + sFileName + ": " + bExists);
			
			return Response.ok(oResult).build();					
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CatalogResources.checkFileByNode: exception " + oEx.toString());
			return Response.serverError().build();
		}		
	}
	
	/**
	 * Check if a file exists on WASDI (this node for instance) or not
	 * @param sSessionId User Session
	 * @param sFileName File Name
	 * @param sWorkspaceId Workspace Id
	 * @return Primitive Result with boolValue = true if the file exists, false if not exists (is not added to WASDI and/or is not on this node)
	 */
	@GET
	@Path("checkdownloadavaialibitybyname")
	@Produces({"application/xml", "application/json", "text/xml"})
 	public Response checkDownloadEntryAvailabilityByName(@QueryParam("token") String sSessionId, @QueryParam("filename") String sFileName, @QueryParam("workspace") String sWorkspaceId, @QueryParam("procws") String sProcessObjId, @QueryParam("volumepath") String sVolumePath)
	{
		try {
			WasdiLog.debugLog("CatalogResources.checkDownloadEntryAvailabilityByName " + sFileName);

			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				WasdiLog.warnLog("CatalogResources.checkDownloadEntryAvailabilityByName: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				WasdiLog.warnLog("CatalogResources.checkDownloadEntryAvailabilityByName: user cannot access workspace");
				return Response.status(Status.FORBIDDEN).build();			
			}
						
			// Try to see if the file is in the wasdi db and present
			File oFile = this.getEntryFile(sFileName,sWorkspaceId);
			
			if(oFile == null) {
				
				// Often we do not have the extension for S1 and S2 files. Try to help
				String sExtension = WasdiFileUtils.getFileNameExtension(sFileName);
				
				boolean bRetry = false;
				
				if (Utils.isNullOrEmpty(sExtension)) {
					// We are without extension, is it a S1 or S2 ?
					if (sFileName.startsWith("S1") || sFileName.startsWith("S2")) {
						WasdiLog.debugLog("CatalogResources.checkDownloadEntryAvailabilityByName: file starts with S1 or S2, try to add .zip" );
						// Yes! Retry with extension
						sFileName += ".zip";
						bRetry = true;
					}
				}
				
				if (bRetry) {
					WasdiLog.warnLog("CatalogResources.checkDownloadEntryAvailabilityByName: retry adding extension new file name = " + sFileName);
					oFile = this.getEntryFile(sFileName,sWorkspaceId);
				}
				
				// Here we check again maybe something changed due to the retry
				if (oFile == null) {
					WasdiLog.debugLog("CatalogResources.checkDownloadEntryAvailabilityByName: file not found, check if it is in a Volume");
					
					if (!Utils.isNullOrEmpty(sVolumePath)) {
						
						WasdiLog.debugLog("CatalogResources.checkDownloadEntryAvailabilityByName: adding volume path " + sVolumePath);
						
						if (!sVolumePath.endsWith("/")) {
							sVolumePath += "/";
						}
						
						sFileName = sVolumePath+sFileName;
					}
					
					// The file is not in the WASDI db. Can be a file on an S3 Volume?
					File oFileInVolume = PermissionsUtils.getFileFromS3Volume(oUser.getUserId(), sFileName, sWorkspaceId, sProcessObjId);
					
					if (oFileInVolume!=null) {
						if (oFileInVolume.exists()) {
							PrimitiveResult oResult = new PrimitiveResult();
							oResult.setBoolValue(true);
							return Response.ok(oResult).build();									
						}
						else {
							WasdiLog.debugLog("CatalogResources.checkDownloadEntryAvailabilityByName: no, we cannot read it");
						}					
					}
					else {
						WasdiLog.debugLog("CatalogResources.checkDownloadEntryAvailabilityByName: file not found in any volume");
					}				

					//return Response.serverError().build();					
				}
			}

			PrimitiveResult oResult = new PrimitiveResult();
			
			if (oFile!=null) {
				WasdiLog.debugLog("CatalogResources.checkDownloadEntryAvailabilityByName: file found!");
				oResult.setBoolValue(true);
				return Response.ok(oResult).build();
			}
			else {
				WasdiLog.debugLog("CatalogResources.checkDownloadEntryAvailabilityByName: file not found!");
				oResult.setBoolValue(false);
				return Response.status(Status.NOT_FOUND).entity(oResult).build();
			}
			
						
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CatalogResources.checkDownloadEntryAvailabilityByName: exception " + oEx.toString());
			return Response.serverError().build();
		}
	}

	/**
	 * Ingest a new file from sftp in to a target Workspace
	 * @param sSessionId User session token header
	 * @param sFile name of the file to ingest
	 * @param sWorkspaceId target workspace
	 * @param sParentProcessWorkspaceId Proc Id of the parent process
	 * @param sStyle Default style to use for the file
	 * @return Http Response
	 */
	@PUT
	@Path("/upload/ingest")
	@Produces({"application/json", "text/xml"})
	public Response ingestFile(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspaceId, @QueryParam("parent") String sParentProcessWorkspaceId, @QueryParam("style") String sStyle, @QueryParam("platform") String sPlatform) {
		
		if (Utils.isNullOrEmpty(sParentProcessWorkspaceId)) sParentProcessWorkspaceId = "";
		if (Utils.isNullOrEmpty(sStyle)) sStyle = "";
		
		// If we do not have a platform, we try to get it from the file name
		if (Utils.isNullOrEmpty(sPlatform)) {
			sPlatform = MissionUtils.getPlatformFromSatelliteImageFileName(sFile);
		}

		WasdiLog.debugLog("CatalogResource.ingestFile File: " + sFile + " Ws: " + sWorkspaceId + " ParentId " + sParentProcessWorkspaceId + " Style " + sStyle);

		// Check user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			WasdiLog.warnLog("CatalogResource.ingestFile: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();		
		}
		
		if (!PermissionsUtils.canUserWriteWorkspace(oUser.getUserId(), sWorkspaceId)) {
			WasdiLog.warnLog("CatalogResources.checkDownloadEntryAvailabilityByName: user cannot write in the workspace");
			return Response.status(Status.FORBIDDEN).build();			
		}		
		
		if (!Utils.isNullOrEmpty(sStyle)) {
			if (!PermissionsUtils.canUserAccessStyle(oUser.getUserId(), sStyle)) {
				WasdiLog.warnLog("CatalogResources.checkDownloadEntryAvailabilityByName: user cannot access workspace");
				return Response.status(Status.UNAUTHORIZED).build();			
			}			
		}
		
		String sUserId = oUser.getUserId();		

		// Find the sftp folder
		String sUserBaseDir = WasdiConfig.Current.paths.sftpRootPath;

		File oUserBaseDir = new File(sUserBaseDir);
		File oFilePath = new File(new File(new File(oUserBaseDir, sUserId), "uploads"), sFile);

		// Is the file available?
		if (!oFilePath.canRead()) {
			WasdiLog.debugLog("CatalogResource.ingestFile: ERROR: unable to access uploaded file " + oFilePath.getAbsolutePath());
			return Response.serverError().build();
		}
		try {						
			// Generate the unique process id
			String sProcessObjId = Utils.getRandomName();
			
			// Ingest file parameter
			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			oParameter.setStyle(sStyle);
			oParameter.setProcessObjId(sProcessObjId);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			oParameter.setPlatform(sPlatform);

			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.INGEST.name(), oFilePath.getName(), oParameter, sParentProcessWorkspaceId);
			
			if (oRes.getBoolValue()) {
				return Response.ok().build();
			}
			else {
				return Response.serverError().build();
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("CatalogResource.ingestFile: " + oEx);
		}

		return Response.serverError().build();

	}

	/**
	 * Ingest a file already existing in a Workspace 
	 * @param sSessionId User Session token
	 * @param sFile Name of the file to ingest
	 * @param sWorkspaceId Id of the target workspace 
	 * @param sParentProcessWorkspaceId Proc Id of the parent process
	 * @param sStyle Default style to use for the file
	 * @return Primitive Result with boolValue true or false and Http Code in intValue
	 */
	@GET
	@Path("/upload/ingestinws")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult ingestFileInWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspaceId, @QueryParam("parent") String sParentProcessWorkspaceId, @QueryParam("style") String sStyle, @QueryParam("platform") String sPlatform) {
		
		// Create the result object
		PrimitiveResult oResult = new PrimitiveResult();

		WasdiLog.debugLog("CatalogResource.ingestFileInWorkspace: file " + sFile + " workspace: " + sWorkspaceId);

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			WasdiLog.warnLog("CatalogResource.ingestFileInWorkspace: invalid session");
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;		
		}
		
		if (!PermissionsUtils.canUserWriteWorkspace(oUser.getUserId(), sWorkspaceId)) {
			WasdiLog.warnLog("CatalogResource.ingestFileInWorkspace: user cannot write in the workspace");
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;			
		}
		
		// If we do not have a platform, we try to get it from the file name
		if (Utils.isNullOrEmpty(sPlatform)) {
			sPlatform = MissionUtils.getPlatformFromSatelliteImageFileName(sFile);
		}		
		
		// Get the user account
		String sUserId = oUser.getUserId();
		
		// Get the file path		
		String sFilePath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId) + sFile;
		
		WasdiLog.debugLog("CatalogResource.ingestFileInWorkspace: computed file path: " + sFilePath);
		
		File oFilePath = new File(sFilePath);
		
		// Check if the file exists 
		if (!oFilePath.canRead()) {
			WasdiLog.debugLog("CatalogResource.ingestFileInWorkspace: file not found. Check if it is an extension problem");

			String [] asSplittedFileName = sFile.split("\\.");

			if (asSplittedFileName.length == 1) {

				WasdiLog.debugLog("CatalogResource.ingestFileInWorkspace: file without exension, try .dim");

				sFile = sFile + ".dim";
				sFilePath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId) + sFile;
				oFilePath = new File(sFilePath);

				if (!oFilePath.canRead()) {
					WasdiLog.debugLog("CatalogResource.ingestFileInWorkspace: file not availalbe. Can be a developer process. Return 500 [file: " + sFile + "]");
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;							
				}
			}
			else {
				WasdiLog.debugLog("CatalogResource.ingestFileInWorkspace: file not availalbe. Can be a developer process. Return 500 [file: " + sFile + "]");
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				return oResult;											
			}

		}
		
		try {
			String sProcessObjId = Utils.getRandomName();

			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			oParameter.setStyle(sStyle);
			//set the process object Id to params
			oParameter.setProcessObjId(sProcessObjId);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			oParameter.setPlatform(sPlatform);

			return Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.INGEST.name(), oFilePath.getName(), oParameter, sParentProcessWorkspaceId);

		} catch (Exception oEx) {
			WasdiLog.errorLog("CatalogueResource.ingestFileInWorkspace: " + oEx);
		}

		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		return oResult;
	}

	/**
	 * Copy a file from a workspace to the user sftp folder 
	 * @param sSessionId User Session token
	 * @param sFile Name of the file to copy
	 * @param sWorkspaceId Id of the workspace
	 * @param sParentProcessWorkspaceId Proc Id of the parent Process 
	 * @param sRelativePath relative path to use from the sftp home
	 * @return Primitive Result with boolValue true or false and Http Code in intValue
	 */
	@GET
	@Path("/copytosfpt")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult copyFileToSftp(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspaceId, @QueryParam("parent") String sParentProcessWorkspaceId, @QueryParam("path") String sRelativePath) {
		
		// Create the result object
		PrimitiveResult oResult = new PrimitiveResult();

		WasdiLog.debugLog("CatalogResource.copyFileToSftp: file " + sFile + " from workspace: " + sWorkspaceId);

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			WasdiLog.warnLog("CatalogResource.copyFileToSftp: computed file path: invalid session");
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;		
		}
		
		if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
			WasdiLog.warnLog("CatalogResource.copyFileToSftp: user cannot access the workspace");
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;			
		}		
		
		// Get the user account
		String sUserId = oUser.getUserId();
		
		// Get the file path		
		String sFilePath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId) + sFile;
		
		WasdiLog.debugLog("CatalogResource.copyFileToSftp: computed file path: " + sFilePath);
		
		File oFilePath = new File(sFilePath);
		
		// Check if the file exists 
		if (!oFilePath.canRead()) {
			WasdiLog.debugLog("CatalogResource.copyFileToSftp: file not found. Check if it is an extension problem");

			String [] asSplittedFileName = sFile.split("\\.");

			if (asSplittedFileName.length == 1) {

				WasdiLog.debugLog("CatalogResource.copyFileToSftp: file without exension, try .dim");

				sFile = sFile + ".dim";
				sFilePath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId) + sFile;
				oFilePath = new File(sFilePath);

				if (!oFilePath.canRead()) {
					WasdiLog.debugLog("CatalogResource.copyFileToSftp: file not availalbe. Can be a developer process. Return 500 [file: " + sFile + "]");
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;							
				}
			}
			else {
				WasdiLog.debugLog("CatalogResource.copyFileToSftp: file not availalbe. Can be a developer process. Return 500 [file: " + sFile + "]");
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				return oResult;											
			}

		}
		
		try {
			
			// Crete the ingest parameter
			String sProcessObjId = Utils.getRandomName();

			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			if (!Utils.isNullOrEmpty(sRelativePath)) {
				oParameter.setRelativePath(sRelativePath);
			}
			oParameter.setProcessObjId(sProcessObjId);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			
			return Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.COPYTOSFTP.name(), oFilePath.getName(), oParameter, sParentProcessWorkspaceId);

		} catch (Exception oEx) {
			WasdiLog.errorLog("CatalogueResource.copyFileToSftp: " + oEx);
		}

		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		return oResult;
	}
	
	/**
	 * Send a file to an external SFTP server 
	 * @param sSessionId User Session
	 * @param sWorkspaceId Worksapce Id
	 * @param sParentProcessWorkspaceId Proc Id of the parent process
	 * @param oFtpTransferVM View Model of the info to move the file (filename, dest server address and credentials, paths..)
	 * @return
	 */
	@PUT
	@Path("/upload/ftp")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult ftpTransferFile(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("parent") String sParentProcessWorkspaceId,
			FtpTransferViewModel oFtpTransferVM) {
		
		WasdiLog.debugLog("CatalogResource.ftpTransferFile");

		//input validation
		if(null == oFtpTransferVM) {
			WasdiLog.warnLog("CatalogResource.ftpTransferFile: invalid input View Model");
			// check appropriateness
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("Null arguments");
			return oResult;
		}
		
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if(null==oUser) {
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			WasdiLog.warnLog("CatalogResource.ftpTransferFile: invalid session");
			oResult.setStringValue("Invalid Session");
			return oResult;
		}
		
		if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
			WasdiLog.warnLog("CatalogResource.ftpTransferFile: user cannot access the workspace");
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;			
		}		
				
		String sUserId = oUser.getUserId();

		try {
			WasdiLog.debugLog("CatalogResource.ftpTransferFile: prepare parameters");
			
			String sProcessObjId = Utils.getRandomName();
			String sFileName = oFtpTransferVM.getFileName();
			
			FtpUploadParameters oParam = new FtpUploadParameters();
			oParam.setFtpServer(oFtpTransferVM.getServer());
			oParam.setPort(oFtpTransferVM.getPort());
			oParam.setSftp(oFtpTransferVM.getSftp());
			oParam.setUsername(oFtpTransferVM.getUser());
			oParam.setPassword(oFtpTransferVM.getPassword());
			oParam.setRemotePath(oFtpTransferVM.getDestinationAbsolutePath());
			
			oParam.setLocalFileName(sFileName);
			oParam.setExchange(sWorkspaceId);
			oParam.setWorkspace(sWorkspaceId);
			oParam.setProcessObjId(sProcessObjId);
			oParam.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
						
			return Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.FTPUPLOAD.name(), sFileName, oParam, sParentProcessWorkspaceId);

		} catch (Exception oEx) {
			WasdiLog.errorLog("CatalogueResource.ftpTransferFile: " + oEx);
			PrimitiveResult oRes = PrimitiveResult.getInvalidInstance();
			oRes.setStringValue(oEx.toString());
			return oRes;
		}
	}
	
	@GET
	@Path("/properties")
	@Produces({"application/json", "text/xml"})
	public Response getProductProperties(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("file") String sFileName, @QueryParam("getchecksum") Boolean bGetChecksum) {
		
		WasdiLog.debugLog("CatalogResource.getProductProperties ws="+sWorkspaceId + " file: " + sFileName);
		
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if(oUser==null) {
			WasdiLog.warnLog("CatalogResource.getProductProperties: invalid user or session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			WasdiLog.warnLog("CatalogResource.getProductProperties: invalid workspace");
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		// Check if there is no malicius attemp
		if (sWorkspaceId.contains("/") || sWorkspaceId.contains("\\")) {
			WasdiLog.warnLog("CatalogResource.getProductProperties: Injection attempt from users");
			return Response.status(Status.BAD_REQUEST).build();
		}		
		
		if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
			WasdiLog.warnLog("CatalogResource.getProductProperties: user cannot access the workspace");
			return Response.status(Status.FORBIDDEN).build();
		}
		
		if (Utils.isNullOrEmpty(sFileName)) {
			WasdiLog.warnLog("CatalogResource.getProductProperties: invalid file name");
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		// Check if there is no malicius attemp
		if (sFileName.contains("/") || sFileName.contains("\\")) {
			WasdiLog.warnLog("CatalogResource.getProductProperties: Injection attempt from users");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		try {
			
			// Get the user Id
			String sUserId = oUser.getUserId();
			
			// Get the workspace path and the full path
			String sWorkspacePath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId);
			String sFullFilePath = sWorkspacePath + sFileName;
			
			// Check if we have the file in the db
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(sFullFilePath);
			
			if (oDownloadedFile==null) {
				WasdiLog.warnLog("CatalogResource.getProductProperties: file not found " + sFullFilePath);
				return Response.status(Status.BAD_REQUEST).build();				
			}
			
			// Check if the file exists
			File oFile = new File(sFullFilePath);
			
			if (!oFile.exists()) {
				WasdiLog.warnLog("CatalogResource.getProductProperties: entry is in the db but file not found " + sFullFilePath);
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			// Create the return view model
			ProductPropertiesViewModel oProductPropertiesViewModel = new ProductPropertiesViewModel();
			
			// Set file name
			oProductPropertiesViewModel.setFileName(sFileName);
			
			// Set friendly name
			if (oDownloadedFile.getProductViewModel()!=null) {
				oProductPropertiesViewModel.setFriendlyName(oDownloadedFile.getProductViewModel().getProductFriendlyName());
			}
			if (Utils.isNullOrEmpty(oProductPropertiesViewModel.getFriendlyName())) {
				oProductPropertiesViewModel.setFriendlyName(sFileName);
			}
			
			// Last update timestamp
			oProductPropertiesViewModel.setLastUpdateTimestampMs(oFile.lastModified());
			// Default style
			oProductPropertiesViewModel.setStyle(oDownloadedFile.getDefaultStyle());
			// Get the size
			oProductPropertiesViewModel.setSize(Files.size(oFile.toPath()));
			
			if (bGetChecksum == null) {
				bGetChecksum = false;
			}
			
			if (bGetChecksum) {
				// Get the checksum
				String sChecksum = generateMD5(oFile);
				oProductPropertiesViewModel.setChecksum(sChecksum);
				WasdiLog.debugLog("CatalogResource.getProductProperties: returning view model with checksum " + sChecksum);
			}
			else {
				WasdiLog.debugLog("CatalogResource.getProductProperties: checksum not requested	");
			}
			
			return Response.ok(oProductPropertiesViewModel).build();

		} catch (Exception oEx) {
			WasdiLog.errorLog("CatalogResource.getProductProperties: ", oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Generate MD5 of a File
	 * @param oFile File
	 * @return MD5 checksum
	 */
	protected String generateMD5(File oFile){
		
	    if(oFile==null){
	        return "";
	    }
	    
	    MessageDigest oMessageDigest;
	    
	    InputStream oInputStream = null;
	    
	    try {
	    	oInputStream = new FileInputStream(oFile);
	        int iRead =0;
	        byte[] ayBuffer = new byte[4096];
	        oMessageDigest = MessageDigest.getInstance("MD5");
	        while((iRead = oInputStream.read(ayBuffer))>0){
	            oMessageDigest.update(ayBuffer,0,iRead);
	        }
	        byte[] ayHashValue = oMessageDigest.digest();
	        String sChecksum = new BigInteger(1, ayHashValue).toString(16);
	        return sChecksum;
	    } 
	    catch (Exception oEx) {
	    	WasdiLog.errorLog("CatalogResource.generateMD5: error ", oEx);
	        return "";
	    }
	    finally{
	        try {
	            if(oInputStream!=null)oInputStream.close();
	        } 
	        catch (Exception oEx) {
	        	WasdiLog.errorLog("CatalogResource.generateMD5: error ", oEx);
	        }
	    } 
	}
	
	/**
	 * Get the file object corresponding to the input requests.
	 * It returns null if the file is not registered in WASDI and or if the file is not physically present
	 * @param sFileName
	 * @return A File object if the file can be download, NULL if the file does not exist or is unreadable
	 */
	private File getEntryFile(String sFileName, String sWorkspace)
	{
		WasdiLog.debugLog("CatalogResources.getEntryFile( fileName : " + sFileName + " )");
		try {
			String sTargetFilePath = PathsConfig.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace) + sFileName;

			DownloadedFilesRepository oRepo = new DownloadedFilesRepository();
			DownloadedFile oDownloadedFile = oRepo.getDownloadedFileByPath(sTargetFilePath);

			if (oDownloadedFile == null) {
				oDownloadedFile = oRepo.getDownloadedFileByPath(WasdiFileUtils.fixPathSeparator(sTargetFilePath));
			}

			if (oDownloadedFile == null) 
			{
				WasdiLog.debugLog("CatalogResources.getEntryFile: file " + sFileName + " not found in path " + sTargetFilePath);
				return null;
			}
			
			File oFile = new File(sTargetFilePath);

			if( oFile.canRead() == true) {
				return oFile;
			}
			else {
				WasdiLog.debugLog("CatalogResources.getEntryFile: cannot read file " + sFileName + " from " + sTargetFilePath + ", returning null");
				return null; 
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CatalogResources.getEntryFile: exception: ", oEx);
		}
		
		return null;
	}
	
	/**
	 * Zip a SNAP .dim file
	 * @param oInitialFile File attached to the .dim
	 * @return stream with the zipped file
	 */
	private Response zipBeanDimapFile(File oInitialFile) {
		
		Stack<File> aoFileStack = new Stack<File>();
		aoFileStack.push(oInitialFile);

		
		String sBasePath = oInitialFile.getAbsolutePath();
		int iLast = sBasePath.lastIndexOf(".dim");
		String sDir = sBasePath.substring(0, iLast) + ".data";
		
		WasdiLog.debugLog("CatalogResources.zipBeanDimapFile: sDir = " + sDir);
		
		File oFile = new File(sDir);
		aoFileStack.push(oFile);
		
		iLast = sBasePath.lastIndexOf(oInitialFile.getName());
		sBasePath = sBasePath.substring(0, iLast);
		
		if(!sBasePath.endsWith("/") && !sBasePath.endsWith("\\")) {
			sBasePath = sBasePath + "/";
		}
		
		WasdiLog.debugLog("CatalogResources.zipBeanDimapFile: updated sBasePath = " + sBasePath);
		
		int iBaseLen = sBasePath.length();
		Map<String, File> aoFileEntries = new HashMap<>();
		
		while(aoFileStack.size()>=1) {
			//Wasdi.DebugLog("CatalogResources.zipBeanDimapFile: pushing files into stack");
			oFile = aoFileStack.pop();
			String sAbsolutePath = oFile.getAbsolutePath();
			
			WasdiLog.debugLog("CatalogResources.zipBeanDimapFile: sAbsolute Path " + sAbsolutePath);

			if(oFile.isDirectory()) {
				if(!sAbsolutePath.endsWith("/") && !sAbsolutePath.endsWith("\\")) {
					sAbsolutePath = sAbsolutePath + "/";
				}
				File[] aoChildren = oFile.listFiles();
				for (File oChild : aoChildren) {
					aoFileStack.push(oChild);
				}
			}
			
			WasdiLog.debugLog("CatalogResources.zipBeanDimapFile: sAbsolute Path 2 " + sAbsolutePath);
			
			String sRelativePath = sAbsolutePath.substring(iBaseLen);
			WasdiLog.debugLog("CatalogResources.zipBeanDimapFile: adding file " + sRelativePath +" for compression");
			aoFileEntries.put(sRelativePath,oFile);
		}
		WasdiLog.debugLog("CatalogResources.zipBeanDimapFile: done preparing map, added " + aoFileEntries.size() + " files");
					
		

		// Set response headers and return 
		String sFileName = oInitialFile.getName();
		
		WasdiLog.debugLog("CatalogResources.zipBeanDimapFile: sFileName1 " + sFileName);
		
		sFileName = sFileName.substring(0, sFileName.lastIndexOf(".dim") ) + ".zip";
		
		WasdiLog.debugLog("CatalogResources.zipBeanDimapFile: sFileName2 Path " + sFileName);
		
		return zipOnTheFly(aoFileEntries, sFileName);
	}

	/**
	 * @param aoFileEntries
	 * @param sFileName
	 * @return
	 */
	private Response zipOnTheFly(Map<String, File> aoFileEntries, String sFileName) {
		ZipStreamingOutput oStream = new ZipStreamingOutput(aoFileEntries);
		ResponseBuilder oResponseBuilder = Response.ok(oStream);
		oResponseBuilder.header("Content-Disposition", "attachment; filename=\""+ sFileName +"\"");
//		Long lLength = 0L;
//		for (String sFile : aoFileEntries.keySet()) {
//			File oTempFile = aoFileEntries.get(sFile);
//			if(!oTempFile.isDirectory()) {
//				//NOTE: this way we are cheating, it is an upper bound, not the real size!
//				lLength += oTempFile.length();
//			}
//		}
		//oResponseBuilder.header("Content-Length", lLength);
		WasdiLog.debugLog("CatalogResources.zipOnTheFly: return ");
		return oResponseBuilder.build();
	}
	
	/**
	 * Zip a shape file
	 * @param oInitialFile file .shp
	 * @return stream with the zipped shape file
	 */
	private Response zipShapeFile(File oInitialFile) {
		
		// Remove extension
		final String sNameToFind = WasdiFileUtils.getFileNameWithoutLastExtension(oInitialFile.getName());
		
		// Get parent folder
		File oFolder = oInitialFile.getParentFile();
		
		// Find all the files of the shape
		File[] aoMatchingFiles = oFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(sNameToFind) && !(name.endsWith(".zip"));
			}
		});
		
		// Found something?
		if (aoMatchingFiles == null) {
			WasdiLog.debugLog("CatalogResources.zipShapeFile: not found = " + sNameToFind);
			return null;
		}
		
		if (aoMatchingFiles.length == 0) {
			WasdiLog.debugLog("CatalogResources.zipShapeFile: not found = " + sNameToFind);
			return null;
		}
		
		Map<String, File> aoFileEntries = new HashMap<>();
		
		for (int i=0; i<aoMatchingFiles.length; i++) {
			WasdiLog.debugLog("CatalogResources.zipShapeFile: adding " + aoMatchingFiles[i].getName() + " to zip");
			aoFileEntries.put(aoMatchingFiles[i].getName(), aoMatchingFiles[i]);
		}
		

		WasdiLog.debugLog("CatalogResources.zipShapeFile: done preparing map, added " + aoFileEntries.size() + " files");
					
		ZipStreamingOutput oStream = new ZipStreamingOutput(aoFileEntries);

		// Set response headers and return 
		ResponseBuilder oResponseBuilder = Response.ok(oStream);
		String sFileName = oInitialFile.getName();
		
		WasdiLog.debugLog("CatalogResources.zipShapeFile: sFileName " + sFileName);
		
		sFileName = sFileName.replace(".shp", ".zip");
		sFileName = sFileName.replace(".SHP", ".zip");
				
		WasdiLog.debugLog("CatalogResources.zipShapeFile: sFileName after substring " + sFileName);
		
		oResponseBuilder.header("Content-Disposition", "attachment; filename=\""+ sFileName +"\"");
		oResponseBuilder.header("Access-Control-Expose-Headers", "Content-Disposition");
		Long lLength = 0L;
		for (String sFile : aoFileEntries.keySet()) {
			File oTempFile = aoFileEntries.get(sFile);
			if(!oTempFile.isDirectory()) {
				//NOTE: this way we are cheating, it is an upper bound, not the real size!
				lLength += oTempFile.length();
			}
		}
		
		//oResponseBuilder.header("Content-Length", lLength);
		return oResponseBuilder.build();
	}
	
	private Response zipSentinel3(File oInitialFile) {
		if(oInitialFile == null) {
			WasdiLog.debugLog("CatalogResource.zipSentinel3: passed a null file, aborting");
			return Response.serverError().build();
		}
		String sZippedOutputFileName = oInitialFile.getName().toUpperCase().replace(".SEN3", ".zip");
		return zipProductFolder(oInitialFile, sZippedOutputFileName);
	}
	
	
	private Response zipSentinel6(File oInitialFile) {
		if(oInitialFile == null) {
			WasdiLog.debugLog("CatalogResource.zipSentinel6: passed a null file, aborting");
			return Response.serverError().build();
		}
		String sZippedOutputFileName = oInitialFile.getName().toUpperCase().replace(".SEN6", ".zip");
		return zipProductFolder(oInitialFile, sZippedOutputFileName);
	}
	
	private Response zipLandsat(File oInitialFile) {
		if(oInitialFile == null) {
			WasdiLog.debugLog("CatalogResource.zipLandsat5: passed a null file, aborting");
			return Response.serverError().build();
		}
		String sZippedOutputFileName = oInitialFile.getName() + ".zip";
		return zipProductFolder(oInitialFile, sZippedOutputFileName);
	}
	
	private Response zipProductFolder(File oFile, String sZippedOutputFileName) {
		if(oFile == null) {
			WasdiLog.debugLog("CatalogResource.zipProductFolder: passed a null file, aborting");
			return Response.serverError().build();
		}
		if(!oFile.isDirectory()) {
			WasdiLog.debugLog("CatalogResource.zipProductFolder: file " + oFile.getAbsolutePath() + " is not a directory, aborting");
			return Response.serverError().build();
		}
		Map<String, File> aoFileEntries = new HashMap<>();
		
		//collect all files in the directory
		try( Stream<java.nio.file.Path> paths = Files.walk(oFile.toPath())){
			paths
			.filter(oPath -> !Files.isDirectory(oPath))
			.forEach(oPath -> {
				String sPath = oFile.getName() + File.separator + oPath.toFile().getName();
				WasdiLog.debugLog("CatalogResource.zipProductFolder: adding file: " + sPath);
				aoFileEntries.put(sPath, oPath.toFile());
			});
		} catch (Exception oE) {
			WasdiLog.errorLog("CatalogResource.zipProductFolder: exception while adding files to zip", oE);
		}

		return zipOnTheFly(aoFileEntries, sZippedOutputFileName);
	}

	/**
	 * Zip a shape file
	 * @param oInitialFile file .
	 * @return stream with the zipped shape file
	 */
	private Response zipAsciiFile(File oInitialFile) {

		if (!oInitialFile.getName().endsWith(".asc")) {
			WasdiLog.warnLog("CatalogResources.zipAsciiFile: the file is not a .asc file");
			return null;
		}

		WasdiLog.debugLog("CatalogResources.zipAsciiFile: looking for.prj file associated to " + oInitialFile.getAbsolutePath());

		// Remove extension
		final String sNameToFind = WasdiFileUtils.getFileNameWithoutLastExtension(oInitialFile.getName()) + ".prj";

		WasdiLog.debugLog("CatalogResources.zipAsciiFile: file to look for: " + sNameToFind);

		// Get parent folder
		File oFolder = oInitialFile.getParentFile();

		File oPrjFile = null;
		for (File oFile : oFolder.listFiles()) {
			if (oFile.getName().equals(sNameToFind)) {
				oPrjFile = oFile;
				break;
			}
		}

		if (oPrjFile == null) {
			WasdiLog.warnLog("CatalogResources.zipAsciiFile: .prj file not found");
			return null;
		}

		WasdiLog.debugLog("CatalogResources.zipAsciiFile: found .prj file " + oPrjFile.getAbsolutePath());

		try {
			Map<String, File> aoFileEntries = new HashMap<>();
			aoFileEntries.put(oInitialFile.getName(), oInitialFile);
			aoFileEntries.put(oPrjFile.getName(), oPrjFile);
			
			String sZipFileName = oInitialFile.getName().replace(".asc", ".zip");
			
			ZipStreamingOutput oStream = new ZipStreamingOutput(aoFileEntries);
			ResponseBuilder oResponseBuilder = Response.ok(oStream);
			
			oResponseBuilder.header("Content-Disposition", "attachment; filename=\"" + sZipFileName + "\"");
			oResponseBuilder.header("Access-Control-Expose-Headers", "Content-Disposition");

			WasdiLog.debugLog("CatalogResources.zipAsciiFile: zip file ready");
			
			return oResponseBuilder.build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("CatalogResource.zipAsciiFile: error while zipping file ", oEx);
		}

		return null;
	}

	/**
	 * Zip a file and return the stream
	 * @param oInitialFile File to zip
	 * @return stream
	 */
	private Response prepareAndReturnZip(File oInitialFile) {
		WasdiLog.debugLog("CatalogResources.prepareAndReturnZip");
		if (null == oInitialFile) {
			WasdiLog.debugLog("CatalogResources.prepareAndReturnZip: oFile is null");
			return null;
		}
		try {
			WasdiLog.debugLog("CatalogResources.prepareAndReturnZip: init");
			String sBasePath = oInitialFile.getAbsolutePath();
			WasdiLog.debugLog("CatalogResources.prepareAndReturnZip: sBasePath = " + sBasePath);
			sBasePath = sBasePath.toLowerCase();
			if (sBasePath.endsWith(".dim")) {
				return zipBeanDimapFile(oInitialFile);
			} else if (sBasePath.endsWith(".shp")) {
				return zipShapeFile(oInitialFile);
			} else if(MissionUtils.isSentinel3Directory(oInitialFile)) {
				return zipSentinel3(oInitialFile);
			} else if (MissionUtils.isSentinel6Directory(oInitialFile)) {
				return zipSentinel6(oInitialFile);
			} else if (MissionUtils.isLandsat5File(oInitialFile) 
					|| MissionUtils.isLandsat7File(oInitialFile) ) {
				return zipLandsat(oInitialFile);
			}
			else if (MissionUtils.isAsciiFile(oInitialFile)) {
				return zipAsciiFile(oInitialFile);
			}
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("CatalogResources.prepareAndReturnZip: " + oEx);
		} 
		return null;
	}
	

	/**
	 * Checks if the file must be zipped or not
	 * @param oFile File to check
	 * @return True if it must be zipped, false otherwise
	 */
	private boolean mustBeZipped(File oFile) {
		WasdiLog.debugLog("CatalogResources.mustBeZipped");
		
		if(null==oFile) {
			WasdiLog.debugLog("CatalogResources.mustBeZipped: oFile is null");
			throw new NullPointerException("File is null");
		}
		boolean bRet = false;

		String sName = oFile.getName().toLowerCase();
		
		if(!Utils.isNullOrEmpty(sName)) {
			return (
					//dim files are the output of SNAP operations
					sName.endsWith(".dim") ||
					WasdiFileUtils.isShapeFile(oFile) ||
					MissionUtils.isSentinel3Directory(oFile) ||
					MissionUtils.isSentinel6Directory(oFile) ||
					MissionUtils.isLandsat5File(oFile) ||
					MissionUtils.isLandsat7File(oFile) || 
					MissionUtils.isAsciiFile(oFile)					
					);
		}
		return bRet;
	}	

}
