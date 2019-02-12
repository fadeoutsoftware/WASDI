package it.fadeout.rest.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

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

import com.sun.jna.platform.FileUtils;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Catalog;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.DownloadedFileCategory;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.CatalogRepository;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.FtpUploadParameters;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.viewmodels.CatalogViewModel;
import wasdi.shared.viewmodels.FtpTransferViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProductViewModel;

@Path("/catalog")
public class CatalogResources {

	//XXX replace with dependency injection
	//MAYBE two different policy: one for google one for username/password
	CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();
	
	@Context
	ServletConfig m_oServletConfig;

	
	@GET
	@Path("categories")
	@Produces({"application/json"})
	public ArrayList<String> GetCategories(@HeaderParam("x-session-token") String sSessionId) {
		Wasdi.DebugLog("CatalogResources.GetCategories");
		
		ArrayList<String> categories = new ArrayList<String>();
		for ( DownloadedFileCategory c : DownloadedFileCategory.values()) {
			categories.add(c.name());
		}
		return categories; 
	}
	
	
	@GET
	@Path("entries")
	@Produces({"application/json"})
	public ArrayList<DownloadedFile> GetEntries(@HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("from") String sFrom, 
			@QueryParam("to") String sTo,
			@QueryParam("freetext") String sFreeText,
			@QueryParam("category") String sCategory
			) {
		
		Wasdi.DebugLog("CatalogResources.GetEntries");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		String sUserId = oUser.getUserId();

		SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyyMMddHHmm");
		try {
			Date dtFrom = (sFrom==null || sFrom.isEmpty())?null:oDateFormat.parse(sFrom);
			Date dtTo = (sTo==null || sTo.isEmpty())?null:oDateFormat.parse(sTo);
			return searchEntries(dtFrom, dtTo, sFreeText, sCategory, sUserId);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new InternalServerErrorException("invalid date: " + e.getMessage());
		}		
	}

	
	@POST
	@Path("downloadentry")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response DownloadEntry(@HeaderParam("x-session-token") String sSessionId, DownloadedFile oEntry) {
		
		Wasdi.DebugLog("CatalogResources.DownloadEntry");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		if (oUser == null) {
			Wasdi.DebugLog("CatalogResources.DownloadEntry: user not authorized");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		File oFile = new File(oEntry.getFilePath());
		
		ResponseBuilder oResponseBuilder = null;
		if (!oFile.canRead()) {
			Wasdi.DebugLog("CatalogResources.DownloadEntry: file not readable");
			oResponseBuilder = Response.serverError();
		} 
		else {
			Wasdi.DebugLog("CatalogResources.DownloadEntry: file ok return content");
			oResponseBuilder = Response.ok(oFile);
			oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oEntry.getFileName());
		}
		
		Wasdi.DebugLog("CatalogResources.DownloadEntry: done, return");
		return oResponseBuilder.build();
	}
	
	
	/**
	 * Get the entry file to download
	 * @param sFileName
	 * @return A File object if the file can be download, NULL if the file does not exist or is undreadable
	 */
	private File getEntryFile(String sFileName)
	{
		DownloadedFilesRepository oRepo = new DownloadedFilesRepository();
		DownloadedFile oDownloadedFile = oRepo.GetDownloadedFile(sFileName);
		
		if (oDownloadedFile == null) 
		{
			Wasdi.DebugLog("CatalogResources.getEntryFile: file " + sFileName + " not found");
			return null;
		}
		
		String sFilePath = oDownloadedFile.getFilePath();

		File oFile = new File(sFilePath);
		//File oFile = new File("C:\\temp\\wasdi\\S1A_IW_GRDH_1SDV_20180605T051925_20180605T051950_022217_026754_8ADD.zip");
		
		if( oFile.canRead() == true)
		{
			return oFile;
		}
		else {
			return null; 
		}
	}
	
	
	@GET
	@Path("downloadbyname")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response DownloadEntryByName(@HeaderParam("x-session-token") String sSessionId, @QueryParam("token") String sTokenSessionId, @QueryParam("filename") String sFileName)
	{			
		Wasdi.DebugLog("CatalogResources.DownloadEntryByName" + sSessionId);
		
		if( Utils.isNullOrEmpty(sSessionId) == false) {
			sTokenSessionId = sSessionId;
		}
		
		User oUser = Wasdi.GetUserFromSession(sTokenSessionId);
		
		if (oUser == null) {
			Wasdi.DebugLog("CatalogResources.DownloadEntryByName: user not authorized");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		ResponseBuilder oResponseBuilder = null;
		File oFile = this.getEntryFile(sFileName);
		if(oFile == null) {
			Wasdi.DebugLog("CatalogResources.DownloadEntryByName: file not readable");
			oResponseBuilder = Response.serverError();	
		} else {
			if(mustBeZipped(oFile)) {
				oFile = getZippedFile(oFile);
			}
			Wasdi.DebugLog("CatalogResources.DownloadEntryByName: file ok return content");
			oResponseBuilder = Response.ok(oFile);
			oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oFile.getName());
		}
		
		Wasdi.DebugLog("CatalogResources.DownloadEntryByName: done, return");
		return oResponseBuilder.build();
	}
	
	
	
	private boolean mustBeZipped(File oFile) {
		Wasdi.DebugLog("CatalogResources.mustBeZipped");
		if(null==oFile) {
			Wasdi.DebugLog("CatalogResources.mustBeZipped: oFile is null");
			throw new NullPointerException("File is null");
		}
		boolean bRet = false;
		
		String sName = oFile.getName(); 
		if(!Utils.isNullOrEmpty(sName)) {
			if(sName.endsWith(".dim")) {
				bRet = true;
			}
		}
		return bRet;
	}
	
	private File getZippedFile(File oFileToBeZipped) {
		Wasdi.DebugLog("CatalogResources.getZippedFile");
		if(null==oFileToBeZipped) {
			Wasdi.DebugLog("CatalogResources.getZippedFile: oFile is null");
			return null;
		}
		File oResult = null;
		try {
			String sBaseName = oFileToBeZipped.getName();
			sBaseName = sBaseName.substring(0, sBaseName.lastIndexOf(".dim"));
			
			String sAbsolutePathWithName = oFileToBeZipped.getCanonicalPath();
			String sBasePath = sAbsolutePathWithName.substring(0, sAbsolutePathWithName.lastIndexOf(".dim"));
			String sDirToBeZipped = sBasePath + ".data";
			String sZipFileName = sBasePath + ".zip";
			
			FileOutputStream oStream = new FileOutputStream(sZipFileName);
			ZipOutputStream oZip = new ZipOutputStream(oStream);
			File oDirToBeZipped = new File(sDirToBeZipped);
			
		
			WasdiFileUtils oFileUtils = new WasdiFileUtils();
			oFileUtils.zipFile(oDirToBeZipped, sBaseName + ".data/", oZip);
			
			oFileUtils.zipFile(oFileToBeZipped, sBaseName + ".dim", oZip);
			oZip.close();
			oStream.close();
			oResult = new File(sZipFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return oResult;
	}


	@GET
	@Path("checkdownloadavaialibitybyname")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response checkDownloadEntryAvailabilityByName(@QueryParam("token") String sSessionId, @QueryParam("filename") String sFileName)
	{			
		Wasdi.DebugLog("CatalogResources.CheckDownloadEntryAvailabilityByName");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		if (oUser == null) {
			Wasdi.DebugLog("CatalogResources.DownloadEntryByName: user not authorized");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		File oFile = this.getEntryFile(sFileName);
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
		List<Workspace> aoWorkspacesList = oWorkspaceRepo.GetWorkspaceByUser(sUserId);
		Map<String, Workspace> aoWorkspaces = new HashMap<String, Workspace>();
		for (Workspace wks : aoWorkspacesList) {
			aoWorkspaces.put(wks.getWorkspaceId(), wks);
		}
		
		//retrieve all compatible files
		List<DownloadedFile> aoDownloadedFiles = oDownloadedFilesRepo.Search(oFrom, oTo, sFreeText, sCategory);
		
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
	public ArrayList<CatalogViewModel> GetCatalogs(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) {
		
		Wasdi.DebugLog("CatalogResources.GetCatalogues");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

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
			List<Catalog> aoCatalogs = oRepository.GetCatalogs();

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
			System.out.println("CatalogResources.GetCatalogs: error retrieving catalogs " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return aoCatalogList;
	}
	

	
	@PUT
	@Path("/upload/ingest")
	@Produces({"application/json", "text/xml"})
	public Response IngestFile(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspace) {
		
		Wasdi.DebugLog("CatalogResource.IngestFile");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();		
		String sAccount = oUser.getUserId();		
		
		String sUserBaseDir = m_oServletConfig.getInitParameter("sftpManagementUserDir");
		
		File oUserBaseDir = new File(sUserBaseDir);
		File oFilePath = new File(new File(new File(oUserBaseDir, sAccount), "uploads"), sFile);
		
		if (!oFilePath.canRead()) {
			System.out.println("CatalogResource.IngestFile: ERROR: unable to access uploaded file " + oFilePath.getAbsolutePath());
			return Response.serverError().build();
		}
		try {
			ProcessWorkspace oProcess = null;
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			
			String sProcessObjId = Utils.GetRandomName();
			
			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sAccount);
			oParameter.setExchange(sWorkspace);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			//set the process object Id to params
			oParameter.setProcessObjId(sProcessObjId);
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + sProcessObjId;
			SerializationUtils.serializeObjectToXML(sPath, oParameter);
		
			try
			{
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.INGEST.name());
				oProcess.setProductName(oFilePath.getName());
				oProcess.setWorkspaceId(sWorkspace);
				oProcess.setUserId(sAccount);
				oProcess.setProcessObjId(sProcessObjId);
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcess);
				Wasdi.DebugLog("CatalogueResource.IngestFile: Process Scheduled for Launcher");
			}
			catch(Exception oEx){
				System.out.println("DownloadResource.Download: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				return Response.serverError().build();
			}
				
			return Response.ok().build();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Response.serverError().build();
		
	}
	
	@GET
	@Path("/upload/ingestinws")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult IngestFileInWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("file") String sFile, @QueryParam("workspace") String sWorkspace) {
		
		PrimitiveResult oResult = new PrimitiveResult();
		
		Wasdi.DebugLog("CatalogResource.IngestFileInWorkspace");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
			oResult.setBoolValue(false);
			oResult.setIntValue(401);
			return oResult;		
		}
		String sAccount = oUser.getUserId();		
		
		String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
		if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
		
		File oUserBaseDir = new File(sDownloadRootPath+sAccount+ "/" +sWorkspace+"/");
		
		File oFilePath = new File(oUserBaseDir, sFile);
		
		if (!oFilePath.canRead()) {
			Wasdi.DebugLog("CatalogResource.IngestFileInWorkspace: file not found. Check if it is an exension problem");
			
			String [] asSplittedFileName = sFile.split("\\.");
			
			if (asSplittedFileName.length == 1) {
				
				Wasdi.DebugLog("CatalogResource.IngestFileInWorkspace: file without exension, try .dim");
				
				sFile = sFile + ".dim";
				oFilePath = new File(oUserBaseDir, sFile);
				
				if (!oFilePath.canRead()) {
					Wasdi.DebugLog("CatalogResource.IngestFileInWorkspace: file not availalbe. Can be a developer process. Return 500 [file: " + sFile + "]");
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;							
				}
			}
			else {
				Wasdi.DebugLog("CatalogResource.IngestFileInWorkspace: file with exension but not available");
				Wasdi.DebugLog("CatalogResource.IngestFileInWorkspace: file not availalbe. Can be a developer process. Return 500 [file: " + sFile + "]");
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				return oResult;											
			}
			
		}
		try {
			ProcessWorkspace oProcess = null;
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			
			String sProcessObjId = Utils.GetRandomName();
			
			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sAccount);
			oParameter.setExchange(sWorkspace);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			//set the process object Id to params
			oParameter.setProcessObjId(sProcessObjId);

			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + sProcessObjId;
			SerializationUtils.serializeObjectToXML(sPath, oParameter);
		
			try
			{
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.INGEST.name());
				oProcess.setProductName(oFilePath.getName());
				oProcess.setWorkspaceId(sWorkspace);
				oProcess.setUserId(sAccount);
				oProcess.setProcessObjId(sProcessObjId);
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcess);
				Wasdi.DebugLog("CatalogueResource.IngestFileInWorkspace: Process Scheduled for Launcher");
			}
			catch(Exception oEx){
				System.out.println("CatalogueResource.IngestFileInWorkspace: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				return oResult;		
			}
				
			oResult.setBoolValue(true);
			oResult.setIntValue(200);
			oResult.setStringValue(oProcess.getProcessObjId());
			return oResult;		
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		oResult.setBoolValue(false);
		oResult.setIntValue(500);
		return oResult;
	}
		
	@PUT
	@Path("/upload/ftp")
	@Produces({"application/json", "text/xml"})
	public PrimitiveResult ftpTransferFile(@HeaderParam("x-session-token") String sSessionId,  @QueryParam("workspace") String sWorkspace, FtpTransferViewModel oFtpTransferVM) {
		Wasdi.DebugLog("CatalogResource.ftpTransferFile");

		//input validation
		if(null == sSessionId || null == oFtpTransferVM) {
			// check appropriateness
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("Null arguments");
			return oResult;
		}
		if(!m_oCredentialPolicy.validSessionId(sSessionId)) {
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("sSessionId badly formatted");
			return oResult;
		}
		SessionRepository oSessionRep = new SessionRepository();
		UserSession oSession = oSessionRep.GetSession(sSessionId);
		
		if(null==oSession) {
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("Invalid Session");
			return oResult;
		}
		String sUserId = oSession.getUserId();
		if(null==sUserId) {
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("Null User");
			return oResult;
		}
		
		if (sWorkspace==null)  {
			PrimitiveResult oResult = PrimitiveResult.getInvalidInstance();
			oResult.setStringValue("Null Workspace");
			return oResult;			
		}
		
		
		try {
			Wasdi.DebugLog("CatalogResource.ftpTransferFile: prepare parameters");
			FtpUploadParameters oParams = new FtpUploadParameters();
			oParams.setFtpServer(oFtpTransferVM.getServer());
			
			//TODO move here checks on server name from the viewModel
			oParams.setM_iPort(oFtpTransferVM.getPort());
			oParams.setM_sUsername(oFtpTransferVM.getUser());
			oParams.setM_sPassword(oFtpTransferVM.getPassword());
			oParams.setM_sRemotePath(oFtpTransferVM.getDestinationAbsolutePath());
			String sFileName = oFtpTransferVM.getFileName();
			oParams.setM_sRemoteFileName(sFileName);
			oParams.setM_sLocalFileName(sFileName);
			oParams.setExchange(sWorkspace);
			oParams.setWorkspace(sWorkspace);
			
			//TODO replace with new kind of repository: FtpUploadRepository, see TODO below...
			DownloadedFilesRepository oDownRepo = new DownloadedFilesRepository();
			String sFullLocalPath = oDownRepo.GetDownloadedFile(sFileName).getFilePath();
			
			/*
			//The DB stores the full path, i.e., dirs + filename. Therefore it has to be removed
			//there should be no ending slashes, but just in case...
			while(sFullLocalPath.endsWith("/")) {
				sFullLocalPath = sFullLocalPath.substring(0, sFullLocalPath.length()-1);
			}
			//here we are: remove the filename suffix and keep just the directories
			//note: this makes sense just as long as the path format in the DB is with the file name at the end
			if(sFullLocalPath.endsWith(sFileName) ) {
				Integer iLen = sFullLocalPath.length() - sFileName.length();
				sFullLocalPath = sFullLocalPath.substring(0, iLen);
			}
			*/
			
			oParams.setM_sLocalPath(sFullLocalPath);
					
			Wasdi.DebugLog("CatalogResource.ftpTransferFile: prepare process");
			ProcessWorkspace oProcess = new ProcessWorkspace();
			oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
			oProcess.setOperationType(LauncherOperations.FTPUPLOAD.name());
			//oProcess.setProductName(sFileUrl);
			oProcess.setWorkspaceId(sWorkspace);
			oProcess.setUserId(sUserId);
			oProcess.setProcessObjId(Utils.GetRandomName());
			oProcess.setStatus(ProcessStatus.CREATED.name());
			oParams.setProcessObjId(oProcess.getProcessObjId());
			
			Wasdi.DebugLog("CatalogResource.ftpTransferFile: serialize parameters");
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + oProcess.getProcessObjId();
			SerializationUtils.serializeObjectToXML(sPath, oParams);
			
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			oRepository.InsertProcessWorkspace(oProcess);
			Wasdi.DebugLog("CatalogueResource.ftpTransferFile: Process Scheduled for Launcher");
			
		} catch (Exception e) {
			e.printStackTrace();
			PrimitiveResult oRes = PrimitiveResult.getInvalidInstance();
			oRes.setStringValue(e.getMessage());
			return oRes;
		}
		
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(true);
		return oResult;
	}
	
}
