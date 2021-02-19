package it.fadeout.rest.resources;

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import it.fadeout.rest.resources.largeFileDownload.ZipStreamingOutput;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Catalog;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.DownloadedFileCategory;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.CatalogRepository;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.FtpUploadParameters;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.CatalogViewModel;
import wasdi.shared.viewmodels.FtpTransferViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProductViewModel;

@Path("/catalog")
public class CatalogResources {

	CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();

	@Context
	ServletConfig m_oServletConfig;


	@GET
	@Path("categories")
	@Produces({"application/json"})
	public ArrayList<String> getCategories(@HeaderParam("x-session-token") String sSessionId) {
		Utils.debugLog("CatalogResources.GetCategories");
		
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			Utils.debugLog("CatalogResources.GetCategories: session invalid");
			return null;
		}

		ArrayList<String> aoCategories = new ArrayList<String>();
		for ( DownloadedFileCategory oCategory : DownloadedFileCategory.values()) {
			aoCategories.add(oCategory.name());
		}
		
		return aoCategories; 
	}


	@GET
	@Path("entries")
	@Produces({"application/json"})
	public ArrayList<DownloadedFile> getEntries(@HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("from") String sFrom, 
			@QueryParam("to") String sTo,
			@QueryParam("freetext") String sFreeText,
			@QueryParam("category") String sCategory
			) {

		Utils.debugLog("CatalogResources.GetEntries");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		String sUserId = oUser.getUserId();

		SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyyMMddHHmm");
		try {
			Date dtFrom = (sFrom==null || sFrom.isEmpty())?null:oDateFormat.parse(sFrom);
			Date dtTo = (sTo==null || sTo.isEmpty())?null:oDateFormat.parse(sTo);
			return searchEntries(dtFrom, dtTo, sFreeText, sCategory, sUserId);
		} catch (ParseException e) {
			Utils.debugLog("CatalogResources.GetEntries: " + e);
			throw new InternalServerErrorException("invalid date: " + e);
		}		
	}


	@POST
	@Path("downloadentry")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadEntry(@HeaderParam("x-session-token") String sSessionId, DownloadedFile oEntry) {

		Utils.debugLog("CatalogResources.DownloadEntry");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("CatalogResources.DownloadEntry: user not authorized");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		File oFile = new File(oEntry.getFilePath());

		ResponseBuilder oResponseBuilder = null;
		if (!oFile.canRead()) {
			Utils.debugLog("CatalogResources.DownloadEntry: file not readable");
			oResponseBuilder = Response.serverError();
		} 
		else {
			Utils.debugLog("CatalogResources.DownloadEntry: file ok return content");
			oResponseBuilder = Response.ok(oFile);
			oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oEntry.getFileName());
		}

		Utils.debugLog("CatalogResources.DownloadEntry: done, return");
		return oResponseBuilder.build();
	}


	/**
	 * Get the entry file to download
	 * @param sFileName
	 * @return A File object if the file can be download, NULL if the file does not exist or is unreadable
	 */
	private File getEntryFile(String sFileName, String sUserId, String sWorkspace)
	{
		Utils.debugLog("CatalogResources.getEntryFile( fileName : " + sFileName + " )");
				
		String sTargetFilePath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace) + sFileName;

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


	@GET
	@Path("downloadbyname")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadEntryByName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("token") String sTokenSessionId,
			@QueryParam("filename") String sFileName,
			@QueryParam("workspace") String sWorkspace)
	{			

		Utils.debugLog("CatalogResources.DownloadEntryByName( FileName: " + sFileName + ", Ws: " + sWorkspace);
		
		try {
						
			if( Utils.isNullOrEmpty(sSessionId) == false) {
				sTokenSessionId = sSessionId;
			}

			User oUser = Wasdi.getUserFromSession(sTokenSessionId);

			if (oUser == null) {
				Utils.debugLog("CatalogResources.DownloadEntryByName: user not authorized");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			File oFile = this.getEntryFile(sFileName, oUser.getUserId(), sWorkspace);
			
			ResponseBuilder oResponseBuilder = null;
			if(oFile == null) {
				Utils.debugLog("CatalogResources.DownloadEntryByName: file not readable");
				oResponseBuilder = Response.serverError();	
			} else {
				//InputStream oStream = null;
				FileStreamingOutput oStream;
				boolean bMustZip = mustBeZipped(oFile); 
				if(bMustZip) {
					Utils.debugLog("CatalogResources.DownloadEntryByName: file " + oFile.getName() + " must be zipped");
					return zipOnTheFlyAndStream(oFile);
				} else {
					Utils.debugLog("CatalogResources.DownloadEntryByName: no need to zip file " + oFile.getName());
					oStream = new FileStreamingOutput(oFile);
					//oStream = new FileInputStream(oFile);
					Utils.debugLog("CatalogResources.DownloadEntryByName: file ok return content");
					oResponseBuilder = Response.ok(oStream);
					oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oFile.getName());
					oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
				}
			}
			Utils.debugLog("CatalogResources.DownloadEntryByName: done, return");
			Utils.debugLog(new EndMessageProvider().getGood());
			return oResponseBuilder.build();
		} catch (Exception e) {
			Utils.debugLog("CatalogResources.DownloadEntryByName: " + e);
		}
		return null;
	}

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
		oResponseBuilder.header("Content-Length", lLength);
		Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: return ");
		return oResponseBuilder.build();
	}
	
	
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
			
			if (sBasePath.endsWith(".dim")) {
				return zipBeanDimapFile(oInitialFile);
			}
			else if (sBasePath.toLowerCase().endsWith(".shp")) {
				return zipShapeFile(oInitialFile);
			}
		} 
		catch (Exception e) {
			Utils.debugLog("CatalogResources.zipOnTheFlyAndStream: " + e);
		} 
		return null;
	}


	private boolean mustBeZipped(File oFile) {
		Utils.debugLog("CatalogResources.mustBeZipped");
		
		if(null==oFile) {
			Utils.debugLog("CatalogResources.mustBeZipped: oFile is null");
			throw new NullPointerException("File is null");
		}
		boolean bRet = false;

		String sName = oFile.getName();
		
		if(!Utils.isNullOrEmpty(sName)) {
			
			if(sName.endsWith(".dim")) {
				bRet = true;
			}
			else if (sName.endsWith(".shp")) {
				bRet = true;
			}
		}
		return bRet;
	}


	@GET
	@Path("fileOnNode")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response checkFileByNode(@QueryParam("token") String sSessionId, @QueryParam("filename") String sFileName, @QueryParam("workspace") String sWorkspace)
	{	
		
		Utils.debugLog("CatalogResources.checkFileByNode");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("CatalogResources.checkFileByNode: user not authorized");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		try {
			String sTargetFilePath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace) + sFileName;

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
	
	@GET
	@Path("checkdownloadavaialibitybyname")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response checkDownloadEntryAvailabilityByName(@QueryParam("token") String sSessionId, @QueryParam("filename") String sFileName, @QueryParam("workspace") String sWorkspace)
	{
		Utils.debugLog("CatalogResources.CheckDownloadEntryAvailabilityByName");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("CatalogResources.DownloadEntryByName: user not authorized");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		File oFile = this.getEntryFile(sFileName, oUser.getUserId(),sWorkspace);
		
		if(oFile == null) {
			return Response.serverError().build();	
		}

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(oFile != null);
		return Response.ok(oResult).build();		

	}

	/**
	 * Search entries in the internal WASDI Catalogue
	 * @param oFrom Date From
	 * @param oTo Date To
	 * @param sFreeText Free Text query
	 * @param sCategory Category
	 * @param sUserId User calling
	 * @return
	 */
	private ArrayList<DownloadedFile> searchEntries(Date oFrom, Date oTo, String sFreeText, String sCategory, String sUserId) {
		ArrayList<DownloadedFile> aoEntries = new ArrayList<DownloadedFile>();

		WorkspaceRepository oWorkspaceRepo = new WorkspaceRepository();
		ProductWorkspaceRepository oProductWorkspaceRepo = new ProductWorkspaceRepository();
		DownloadedFilesRepository oDownloadedFilesRepo = new DownloadedFilesRepository();

		//get all my workspaces
		List<Workspace> aoWorkspacesList = oWorkspaceRepo.getWorkspaceByUser(sUserId);
		Map<String, Workspace> aoWorkspaces = new HashMap<String, Workspace>();
		for (Workspace wks : aoWorkspacesList) {
			aoWorkspaces.put(wks.getWorkspaceId(), wks);
		}

		//retrieve all compatible files
		List<DownloadedFile> aoDownloadedFiles = oDownloadedFilesRepo.search(oFrom, oTo, sFreeText, sCategory);

		for (DownloadedFile oDownloadedFile : aoDownloadedFiles) {

			//check if the product is in my workspace
			ProductViewModel oProductViewModel = oDownloadedFile.getProductViewModel();

			boolean bIsOwnedByUser = sCategory.equals(DownloadedFileCategory.PUBLIC.name());
			if (!bIsOwnedByUser) {
				if (oProductViewModel != null) {
					oProductViewModel.setMetadata(null);
					List<String> aoProductWorkspaces = oProductWorkspaceRepo.getWorkspaces(oProductViewModel.getFileName()); //TODO check if productName should be used					
					for (String sProductWorkspace : aoProductWorkspaces) {
						if (aoWorkspaces.containsKey(sProductWorkspace)) {
							bIsOwnedByUser = true;
							break;
						}
					}
				}
			}

			if (bIsOwnedByUser) {	
				aoEntries.add(oDownloadedFile);
			}

		}

		return aoEntries;
	}


	@GET
	@Path("")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<CatalogViewModel> getCatalogs(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) {

		Utils.debugLog("CatalogResources.GetCatalogues");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ArrayList<CatalogViewModel> aoCatalogList = new ArrayList<CatalogViewModel>();

		try {
			// Domain Check
			if (oUser == null) {
				return aoCatalogList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoCatalogList;
			}

			// Create repo
			CatalogRepository oRepository = new CatalogRepository();

			// Get Process List
			List<Catalog> aoCatalogs = oRepository.getCatalogs();

			// For each
			for (int iCatalog=0; iCatalog<aoCatalogs.size(); iCatalog++) {
				// Create View Model
				CatalogViewModel oViewModel = new CatalogViewModel();
				Catalog oCatalog = aoCatalogs.get(iCatalog);

				oViewModel.setDate(oCatalog.getDate());
				oViewModel.setFilePath(oCatalog.getFilePath());
				oViewModel.setFileName(oCatalog.getFileName());

				aoCatalogList.add(oViewModel);

			}

		}
		catch (Exception oEx) {
			Utils.debugLog("CatalogResources.GetCatalogs: " + oEx);
		}

		return aoCatalogList;
	}


	/**
	 * Ingest a new file from sftp in to a target Workspace
	 * @param sSessionId User session token header
	 * @param sFile name of the file to ingest
	 * @param sWorkspace target workspace
	 * @return Http Response
	 */
	@PUT
	@Path("/upload/ingest")
	@Produces({"application/json", "text/xml"})
	public Response ingestFile(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspace, @QueryParam("parent") String sParentProcessWorkspaceId, @QueryParam("style") String sStyle) {
		
		if (Utils.isNullOrEmpty(sParentProcessWorkspaceId)) sParentProcessWorkspaceId = "";
		if (Utils.isNullOrEmpty(sStyle)) sStyle = "";

		Utils.debugLog("CatalogResource.IngestFile File: " + sFile + " Ws: " + sWorkspace + " ParentId " + sParentProcessWorkspaceId + " Style " + sStyle);

		// Check user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();		
		String sUserId = oUser.getUserId();		

		// Find the sftp folder
		String sUserBaseDir = m_oServletConfig.getInitParameter("sftpManagementUserDir");

		File oUserBaseDir = new File(sUserBaseDir);
		File oFilePath = new File(new File(new File(oUserBaseDir, sUserId), "uploads"), sFile);

		// Is the file available?
		if (!oFilePath.canRead()) {
			Utils.debugLog("CatalogResource.IngestFile: ERROR: unable to access uploaded file " + oFilePath.getAbsolutePath());
			return Response.serverError().build();
		}
		try {						
			// Generate the unique process id
			String sProcessObjId = Utils.GetRandomName();
			
			// Ingest file parameter
			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspace);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			oParameter.setStyle(sStyle);
			oParameter.setProcessObjId(sProcessObjId);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspace));

			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
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
	 * @param sWorkspace Id of the target workspace 
	 * @return Primitive Result with boolValue true or false and Http Code in intValue
	 */
	@GET
	@Path("/upload/ingestinws")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult ingestFileInWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspace, @QueryParam("parent") String sParentProcessWorkspaceId) {
		
		// Create the result object
		PrimitiveResult oResult = new PrimitiveResult();

		Utils.debugLog("CatalogResource.IngestFileInWorkspace: file " + sFile + " workspace: " + sWorkspace);

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
		String sFilePath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace) + sFile;
		
		Utils.debugLog("CatalogResource.IngestFileInWorkspace: computed file path: " + sFilePath);
		
		File oFilePath = new File(sFilePath);
		
		// Check if the file exists 
		if (!oFilePath.canRead()) {
			Utils.debugLog("CatalogResource.IngestFileInWorkspace: file not found. Check if it is an extension problem");

			String [] asSplittedFileName = sFile.split("\\.");

			if (asSplittedFileName.length == 1) {

				Utils.debugLog("CatalogResource.IngestFileInWorkspace: file without exension, try .dim");

				sFile = sFile + ".dim";
				sFilePath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace) + sFile;
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
			String sProcessObjId = Utils.GetRandomName();

			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspace);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			//set the process object Id to params
			oParameter.setProcessObjId(sProcessObjId);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspace));

			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
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
	 * @param sWorkspace Id of the workspace 
	 * @return Primitive Result with boolValue true or false and Http Code in intValue
	 */
	@GET
	@Path("/copytosfpt")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult copyFileToSftp(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspace, @QueryParam("parent") String sParentProcessWorkspaceId) {
		
		// Create the result object
		PrimitiveResult oResult = new PrimitiveResult();

		Utils.debugLog("CatalogResource.copyFileToSftp: file " + sFile + " from workspace: " + sWorkspace);

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
		String sFilePath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace) + sFile;
		
		Utils.debugLog("CatalogResource.copyFileToSftp: computed file path: " + sFilePath);
		
		File oFilePath = new File(sFilePath);
		
		// Check if the file exists 
		if (!oFilePath.canRead()) {
			Utils.debugLog("CatalogResource.copyFileToSftp: file not found. Check if it is an extension problem");

			String [] asSplittedFileName = sFile.split("\\.");

			if (asSplittedFileName.length == 1) {

				Utils.debugLog("CatalogResource.copyFileToSftp: file without exension, try .dim");

				sFile = sFile + ".dim";
				sFilePath = Wasdi.getWorkspacePath(m_oServletConfig, Wasdi.getWorkspaceOwner(sWorkspace), sWorkspace) + sFile;
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
			String sProcessObjId = Utils.GetRandomName();

			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspace);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			oParameter.setProcessObjId(sProcessObjId);
			oParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspace));

			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			return Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.COPYTOSFTP.name(), oFilePath.getName(), sPath, oParameter, sParentProcessWorkspaceId);

		} catch (Exception e) {
			Utils.debugLog("CatalogueResource.copyFileToSftp: " + e);
		}

		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		return oResult;
	}
	
	@PUT
	@Path("/upload/ftp")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult ftpTransferFile(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspace, @QueryParam("parent") String sParentProcessWorkspaceId,
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
			
			String sProcessObjId = Utils.GetRandomName();
			String sFileName = oFtpTransferVM.getFileName();
			
			FtpUploadParameters oParam = new FtpUploadParameters();
			oParam.setFtpServer(oFtpTransferVM.getServer());
			oParam.setPort(oFtpTransferVM.getPort());
			oParam.setSftp(oFtpTransferVM.getSftp());
			oParam.setUsername(oFtpTransferVM.getUser());
			oParam.setPassword(oFtpTransferVM.getPassword());
			oParam.setRemotePath(oFtpTransferVM.getDestinationAbsolutePath());
			
			oParam.setLocalFileName(sFileName);
			oParam.setExchange(sWorkspace);
			oParam.setWorkspace(sWorkspace);
			oParam.setProcessObjId(sProcessObjId);
			oParam.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspace));

			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
						
			return Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.FTPUPLOAD.name(), sFileName, sPath, oParam, sParentProcessWorkspaceId);

		} catch (Exception e) {
			Utils.debugLog("CatalogueResource.ftpTransferFile: " + e);
			PrimitiveResult oRes = PrimitiveResult.getInvalidInstance();
			oRes.setStringValue(e.toString());
			return oRes;
		}
	}

}
