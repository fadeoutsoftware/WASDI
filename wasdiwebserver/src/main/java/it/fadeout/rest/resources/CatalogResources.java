package it.fadeout.rest.resources;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

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
import wasdi.shared.business.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.parameters.FtpUploadParameters;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.products.FtpTransferViewModel;

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
			@QueryParam("workspace") String sWorkspaceId)
	{			

		Utils.debugLog("CatalogResources.DownloadEntryByName( FileName: " + sFileName + ", Ws: " + sWorkspaceId);
		
		try {
			
			// Check session
			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}

			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser == null) {
				Utils.debugLog("CatalogResources.DownloadEntryByName: user not authorized");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// Get the File object
			File oFile = this.getEntryFile(sFileName, sWorkspaceId);
			
			ResponseBuilder oResponseBuilder = null;
			
			if(oFile == null) {
				// File invalid
				Utils.debugLog("CatalogResources.DownloadEntryByName: file not readable");
				oResponseBuilder = Response.serverError();	
			} 
			else {
				
				// Ok send the file to the user
				FileStreamingOutput oStream;
				
				// Some files, like .shp or .dim, are not sigle files but folders: see if we need to make a zip
				boolean bMustZip = mustBeZipped(oFile); 
				
				if(bMustZip) {
					// Yes, zip and stream on the fly
					Utils.debugLog("CatalogResources.DownloadEntryByName: file " + oFile.getName() + " must be zipped");
					return zipOnTheFlyAndStream(oFile);
				} else {
					// No, just return the stream
					Utils.debugLog("CatalogResources.DownloadEntryByName: no need to zip file " + oFile.getName());
					oStream = new FileStreamingOutput(oFile);
					Utils.debugLog("CatalogResources.DownloadEntryByName: file ok return content");
					oResponseBuilder = Response.ok(oStream);
					oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oFile.getName());
					//oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
				}
			}
			
			Utils.debugLog("CatalogResources.DownloadEntryByName: done, return");
			
			return oResponseBuilder.build();
		} catch (Exception e) {
			Utils.debugLog("CatalogResources.DownloadEntryByName: " + e);
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
		
		Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: sDir = " + sDir);
		
		File oFile = new File(sDir);
		aoFileStack.push(oFile);
		
		iLast = sBasePath.lastIndexOf(oInitialFile.getName());
		sBasePath = sBasePath.substring(0, iLast);
		
		if(!sBasePath.endsWith("/") && !sBasePath.endsWith("\\")) {
			sBasePath = sBasePath + "/";
		}
		
		Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: updated sBasePath = " + sBasePath);
		
		int iBaseLen = sBasePath.length();
		Map<String, File> aoFileEntries = new HashMap<>();
		
		while(aoFileStack.size()>=1) {
			//Wasdi.DebugLog("CatalogResources.zipOnTheFlyAndStream: pushing files into stack");
			oFile = aoFileStack.pop();
			String sAbsolutePath = oFile.getAbsolutePath();
			
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: sAbsolute Path " + sAbsolutePath);

			if(oFile.isDirectory()) {
				if(!sAbsolutePath.endsWith("/") && !sAbsolutePath.endsWith("\\")) {
					sAbsolutePath = sAbsolutePath + "/";
				}
				File[] aoChildren = oFile.listFiles();
				for (File oChild : aoChildren) {
					aoFileStack.push(oChild);
				}
			}
			
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: sAbsolute Path 2 " + sAbsolutePath);
			
			String sRelativePath = sAbsolutePath.substring(iBaseLen);
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: adding file " + sRelativePath +" for compression");
			aoFileEntries.put(sRelativePath,oFile);
		}
		Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: done preparing map, added " + aoFileEntries.size() + " files");
					
		ZipStreamingOutput oStream = new ZipStreamingOutput(aoFileEntries);

		// Set response headers and return 
		ResponseBuilder oResponseBuilder = Response.ok(oStream);
		String sFileName = oInitialFile.getName();
		
		Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: sFileName1 " + sFileName);
		
		sFileName = sFileName.substring(0, sFileName.lastIndexOf(".dim") ) + ".zip";
		
		Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: sFileName2 Path " + sFileName);
		
		oResponseBuilder.header("Content-Disposition", "attachment; filename=\""+ sFileName +"\"");
		Long lLength = 0L;
		for (String sFile : aoFileEntries.keySet()) {
			File oTempFile = aoFileEntries.get(sFile);
			if(!oTempFile.isDirectory()) {
				//NOTE: this way we are cheating, it is an upper bound, not the real size!
				lLength += oTempFile.length();
			}
		}
		//oResponseBuilder.header("Content-Length", lLength);
		Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: return ");
		return oResponseBuilder.build();
	}
	
	/**
	 * Zip a shape file
	 * @param oInitialFile file .shp
	 * @return stream with the zipped shape file
	 */
	private Response zipShapeFile(File oInitialFile) {
		
		// Remove extension
		final String sNameToFind = Utils.getFileNameWithoutLastExtension(oInitialFile.getName());
		
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
			Utils.debugLog("CatalogResources.zipShapeFile: not found = " + sNameToFind);
			return null;
		}
		
		if (aoMatchingFiles.length == 0) {
			Utils.debugLog("CatalogResources.zipShapeFile: not found = " + sNameToFind);
			return null;
		}
		
		Map<String, File> aoFileEntries = new HashMap<>();
		
		for (int i=0; i<aoMatchingFiles.length; i++) {
			Utils.debugLog("CatalogResources.zipShapeFile: adding " + aoMatchingFiles[i].getName() + " to zip");
			aoFileEntries.put(aoMatchingFiles[i].getName() ,aoMatchingFiles[i]);
		}
		

		Utils.debugLog("CatalogResources.zipShapeFile: done preparing map, added " + aoFileEntries.size() + " files");
					
		ZipStreamingOutput oStream = new ZipStreamingOutput(aoFileEntries);

		// Set response headers and return 
		ResponseBuilder oResponseBuilder = Response.ok(oStream);
		String sFileName = oInitialFile.getName();
		
		Utils.debugLog("CatalogResources.zipShapeFile: sFileName " + sFileName);
		
		sFileName = sFileName.replace(".shp", ".zip");
		sFileName = sFileName.replace(".SHP", ".zip");
				
		Utils.debugLog("CatalogResources.zipShapeFile: sFileName after substring " + sFileName);
		
		oResponseBuilder.header("Content-Disposition", "attachment; filename=\""+ sFileName +"\"");
		Long lLength = 0L;
		for (String sFile : aoFileEntries.keySet()) {
			File oTempFile = aoFileEntries.get(sFile);
			if(!oTempFile.isDirectory()) {
				//NOTE: this way we are cheating, it is an upper bound, not the real size!
				lLength += oTempFile.length();
			}
		}
		oResponseBuilder.header("Content-Length", lLength);
		Utils.debugLog("CatalogResources.zipShapeFile: return ");
		return oResponseBuilder.build();
	}
	
	private Response zipSentinel3(File oInitialFile) {
		if(null==oInitialFile) {
			Utils.debugLog("CatalogResource.zipSentinel3: passed a null file, aborting");
			return Response.serverError().build();
		}
		if(!oInitialFile.isDirectory()) {
			Utils.debugLog("CatalogResource.zipSentinel3: file " + oInitialFile.getAbsolutePath() + " is not a directory, aborting");
			return Response.serverError().build();
		}
		
		
		throw new UnsupportedOperationException("Implement this method!");
	}
	
	/**
	 * Zip a file and return the stream 
	 * @param oInitialFile File to zip
	 * @return stream
	 */
	private Response zipOnTheFlyAndStream(File oInitialFile) {
		Utils.debugLog("CatalogResources.zipOnTheFlyAndStream");
		if(null==oInitialFile) {
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: oFile is null");
			return null;
		}		
		try {
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: init");
			String sBasePath = oInitialFile.getAbsolutePath();
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: sBasePath = " + sBasePath);
			sBasePath = sBasePath.toLowerCase();
			if (sBasePath.endsWith(".dim")) {
				return zipBeanDimapFile(oInitialFile);
			} else if (sBasePath.endsWith(".shp")) {
				return zipShapeFile(oInitialFile);
			} else if(WasdiFileUtils.isSentinel3Directory(oInitialFile)) {
				return zipSentinel3(oInitialFile);
			} 
		} 
		catch (Exception e) {
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: " + e);
		} 
		return null;
	}
	

	/**
	 * Checks if the file must be zipped or not
	 * @param oFile File to check
	 * @return True if it must be zipped, false otherwise
	 */
	private boolean mustBeZipped(File oFile) {
		Utils.debugLog("CatalogResources.mustBeZipped");
		
		if(null==oFile) {
			Utils.debugLog("CatalogResources.mustBeZipped: oFile is null");
			throw new NullPointerException("File is null");
		}
		boolean bRet = false;

		String sName = oFile.getName().toLowerCase();
		
		if(!Utils.isNullOrEmpty(sName)) {
			return (
					//dim files are the output of SNAP operations
					sName.endsWith(".dim") ||
					//sName.endsWith(".shp") ||
					WasdiFileUtils.isShapeFile(oFile) ||
					WasdiFileUtils.isSentinel3Directory(oFile)
					);
		}
		return bRet;
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
	public Response checkFileByNode(@QueryParam("token") String sSessionId, @QueryParam("filename") String sFileName, @QueryParam("workspace") String sWorkspaceId)
	{	
		
		Utils.debugLog("CatalogResources.checkFileByNode");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("CatalogResources.checkFileByNode: user not authorized");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		try {
			String sTargetFilePath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId) + sFileName;

			File oFile = new File(sTargetFilePath);
			
			Utils.debugLog("CatalogResources.checkFileByNode: path " + sTargetFilePath);
			
			boolean bExists = oFile.exists();
			
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setBoolValue(bExists);
			
			Utils.debugLog("CatalogResources.checkFileByNode " + sFileName + ": " + bExists);
			
			return Response.ok(oResult).build();					
		}
		catch (Exception oEx) {
			Utils.debugLog("CatalogResources.checkFileByNode: exception " + oEx.toString());
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
	public Response checkDownloadEntryAvailabilityByName(@QueryParam("token") String sSessionId, @QueryParam("filename") String sFileName, @QueryParam("workspace") String sWorkspaceId)
	{
		Utils.debugLog("CatalogResources.CheckDownloadEntryAvailabilityByName");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("CatalogResources.DownloadEntryByName: user not authorized");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		File oFile = this.getEntryFile(sFileName,sWorkspaceId);
		
		if(oFile == null) {
			return Response.serverError().build();	
		}

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(oFile != null);
		return Response.ok(oResult).build();		

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
	public Response ingestFile(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspaceId, @QueryParam("parent") String sParentProcessWorkspaceId, @QueryParam("style") String sStyle) {
		
		if (Utils.isNullOrEmpty(sParentProcessWorkspaceId)) sParentProcessWorkspaceId = "";
		if (Utils.isNullOrEmpty(sStyle)) sStyle = "";

		Utils.debugLog("CatalogResource.IngestFile File: " + sFile + " Ws: " + sWorkspaceId + " ParentId " + sParentProcessWorkspaceId + " Style " + sStyle);

		// Check user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();		
		String sUserId = oUser.getUserId();		

		// Find the sftp folder
		String sUserBaseDir = WasdiConfig.Current.paths.sftpRootPath;

		File oUserBaseDir = new File(sUserBaseDir);
		File oFilePath = new File(new File(new File(oUserBaseDir, sUserId), "uploads"), sFile);

		// Is the file available?
		if (!oFilePath.canRead()) {
			Utils.debugLog("CatalogResource.IngestFile: ERROR: unable to access uploaded file " + oFilePath.getAbsolutePath());
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

			String sPath = WasdiConfig.Current.paths.serializationPath;
			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.INGEST.name(), oFilePath.getName(), sPath, oParameter, sParentProcessWorkspaceId);
			
			if (oRes.getBoolValue()) {
				return Response.ok().build();
			}
			else {
				return Response.serverError().build();
			}

		} catch (Exception e) {
			Utils.debugLog("DownloadResource.Download: " + e);
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
	public PrimitiveResult ingestFileInWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspaceId, @QueryParam("parent") String sParentProcessWorkspaceId, @QueryParam("style") String sStyle) {
		
		// Create the result object
		PrimitiveResult oResult = new PrimitiveResult();

		Utils.debugLog("CatalogResource.IngestFileInWorkspace: file " + sFile + " workspace: " + sWorkspaceId);

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;		
		}
		
		// Get the user account
		String sUserId = oUser.getUserId();
		
		// Get the file path		
		String sFilePath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId) + sFile;
		
		Utils.debugLog("CatalogResource.IngestFileInWorkspace: computed file path: " + sFilePath);
		
		File oFilePath = new File(sFilePath);
		
		// Check if the file exists 
		if (!oFilePath.canRead()) {
			Utils.debugLog("CatalogResource.IngestFileInWorkspace: file not found. Check if it is an extension problem");

			String [] asSplittedFileName = sFile.split("\\.");

			if (asSplittedFileName.length == 1) {

				Utils.debugLog("CatalogResource.IngestFileInWorkspace: file without exension, try .dim");

				sFile = sFile + ".dim";
				sFilePath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId) + sFile;
				oFilePath = new File(sFilePath);

				if (!oFilePath.canRead()) {
					Utils.debugLog("CatalogResource.IngestFileInWorkspace: file not availalbe. Can be a developer process. Return 500 [file: " + sFile + "]");
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;							
				}
			}
			else {
				Utils.debugLog("CatalogResource.IngestFileInWorkspace: file with exension but not available");
				Utils.debugLog("CatalogResource.IngestFileInWorkspace: file not availalbe. Can be a developer process. Return 500 [file: " + sFile + "]");
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

			String sPath = WasdiConfig.Current.paths.serializationPath;
			return Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.INGEST.name(), oFilePath.getName(), sPath, oParameter, sParentProcessWorkspaceId);

		} catch (Exception e) {
			Utils.debugLog("CatalogueResource.IngestFileInWorkspace: " + e);
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

		Utils.debugLog("CatalogResource.copyFileToSftp: file " + sFile + " from workspace: " + sWorkspaceId);

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;		
		}
		
		// Get the user account
		String sUserId = oUser.getUserId();
		
		// Get the file path		
		String sFilePath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId) + sFile;
		
		Utils.debugLog("CatalogResource.copyFileToSftp: computed file path: " + sFilePath);
		
		File oFilePath = new File(sFilePath);
		
		// Check if the file exists 
		if (!oFilePath.canRead()) {
			Utils.debugLog("CatalogResource.copyFileToSftp: file not found. Check if it is an extension problem");

			String [] asSplittedFileName = sFile.split("\\.");

			if (asSplittedFileName.length == 1) {

				Utils.debugLog("CatalogResource.copyFileToSftp: file without exension, try .dim");

				sFile = sFile + ".dim";
				sFilePath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspaceId), sWorkspaceId) + sFile;
				oFilePath = new File(sFilePath);

				if (!oFilePath.canRead()) {
					Utils.debugLog("CatalogResource.copyFileToSftp: file not availalbe. Can be a developer process. Return 500 [file: " + sFile + "]");
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;							
				}
			}
			else {
				Utils.debugLog("CatalogResource.copyFileToSftp: file with exension but not available");
				Utils.debugLog("CatalogResource.copyFileToSftp: file not availalbe. Can be a developer process. Return 500 [file: " + sFile + "]");
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

			String sPath = WasdiConfig.Current.paths.serializationPath;
			return Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.COPYTOSFTP.name(), oFilePath.getName(), sPath, oParameter, sParentProcessWorkspaceId);

		} catch (Exception e) {
			Utils.debugLog("CatalogueResource.copyFileToSftp: " + e);
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
		
		Utils.debugLog("CatalogResource.ftpTransferFile");

		//input validation
		if(null == oFtpTransferVM) {
			// check appropriateness
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("Null arguments");
			return oResult;
		}
		
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if(null==oUser) {
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("Invalid Session");
			return oResult;
		}
		
		String sUserId = oUser.getUserId();

		try {
			Utils.debugLog("CatalogResource.ftpTransferFile: prepare parameters");
			
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

			String sPath = WasdiConfig.Current.paths.serializationPath;
						
			return Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.FTPUPLOAD.name(), sFileName, sPath, oParam, sParentProcessWorkspaceId);

		} catch (Exception e) {
			Utils.debugLog("CatalogueResource.ftpTransferFile: " + e);
			PrimitiveResult oRes = PrimitiveResult.getInvalidInstance();
			oRes.setStringValue(e.toString());
			return oRes;
		}
	}
	
	/**
	 * Get the file object corrisponding to the input requests.
	 * It returns null if the file is not registered in WASDI and or if the file is not physically present
	 * @param sFileName
	 * @return A File object if the file can be download, NULL if the file does not exist or is unreadable
	 */
	private File getEntryFile(String sFileName, String sWorkspace)
	{
		Utils.debugLog("CatalogResources.getEntryFile( fileName : " + sFileName + " )");
				
		String sTargetFilePath = Wasdi.getWorkspacePath(Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace) + sFileName;

		DownloadedFilesRepository oRepo = new DownloadedFilesRepository();
		DownloadedFile oDownloadedFile = oRepo.getDownloadedFileByPath(sTargetFilePath);

		if (oDownloadedFile == null) 
		{
			Utils.debugLog("CatalogResources.getEntryFile: file " + sFileName + " not found in path " + sTargetFilePath);
			return null;
		}
		
		File oFile = new File(sTargetFilePath);

		if( oFile.canRead() == true) {
			return oFile;
		}
		else {
			Utils.debugLog("CatalogResources.getEntryFile: cannot read file " + sFileName + " from " + sTargetFilePath + ", returning null");
			return null; 
		}
	}

}
