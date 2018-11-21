package it.fadeout.rest.resources;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import wasdi.shared.parameters.FtpTransferParameters;
import wasdi.shared.parameters.FtpTransferParameters.FtpDirection;
import wasdi.shared.parameters.IngestFileParameter;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
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
			@QueryParam("from") String from, 
			@QueryParam("to") String to,
			@QueryParam("freetext") String freeText,
			@QueryParam("category") String category
			) {
		
		Wasdi.DebugLog("CatalogResources.GetEntries");
		
		User me = Wasdi.GetUserFromSession(sSessionId);
		String userId = me.getUserId();
//		String userId = "paolo";

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
		try {
			Date dtFrom = (from==null || from.isEmpty())?null:format.parse(from);
			Date dtTo = (to==null || to.isEmpty())?null:format.parse(to);
			return searchEntries(dtFrom, dtTo, freeText, category, userId);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new InternalServerErrorException("invalid date: " + e.getMessage());
		}		
	}

	
	@POST
	@Path("downloadentry")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response DownloadEntry(@HeaderParam("x-session-token") String sSessionId, DownloadedFile entry) {
		
		Wasdi.DebugLog("CatalogResources.DownloadEntry");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		if (oUser == null) {
			Wasdi.DebugLog("CatalogResources.DownloadEntry: user not authorized");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		File oFile = new File(entry.getFilePath());
		
		ResponseBuilder oResponseBuilder = null;
		if (!oFile.canRead()) {
			Wasdi.DebugLog("CatalogResources.DownloadEntry: file not readable");
			oResponseBuilder = Response.serverError();
		} 
		else {
			Wasdi.DebugLog("CatalogResources.DownloadEntry: file ok return content");
			oResponseBuilder = Response.ok(oFile);
			oResponseBuilder.header("Content-Disposition", "attachment; filename="+ entry.getFileName());
		}
		
		Wasdi.DebugLog("CatalogResources.DownloadEntry: done, return");
		return oResponseBuilder.build();
	}
	
	@GET
	@Path("downloadbyname")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response DownloadEntryByName(@HeaderParam("x-session-token") String sSessionId, @QueryParam("filename") String sFileName) {
		
		Wasdi.DebugLog("CatalogResources.DownloadEntryByName");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		if (oUser == null) {
			Wasdi.DebugLog("CatalogResources.DownloadEntryByName: user not authorized");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		DownloadedFilesRepository oRepo = new DownloadedFilesRepository();
		DownloadedFile oDownloadedFile = oRepo.GetDownloadedFile(sFileName);
		
		if (oDownloadedFile == null) {
			Wasdi.DebugLog("CatalogResources.DownloadEntryByName: file " + sFileName + " not found");
			return Response.serverError().build();			
		}

		File oFile = new File(oDownloadedFile.getFilePath());
		
		ResponseBuilder oResponseBuilder = null;
		if (!oFile.canRead()) {
			Wasdi.DebugLog("CatalogResources.DownloadEntryByName: file not readable");
			oResponseBuilder = Response.serverError();
		} 
		else {
			Wasdi.DebugLog("CatalogResources.DownloadEntryByName: file ok return content");
			oResponseBuilder = Response.ok(oFile);
			oResponseBuilder.header("Content-Disposition", "attachment; filename="+ oDownloadedFile.getFileName());
		}
		
		Wasdi.DebugLog("CatalogResources.DownloadEntryByName: done, return");
		return oResponseBuilder.build();
	}
	
	
	private ArrayList<DownloadedFile> searchEntries(Date from, Date to, String freeText, String category, String userId) {
		ArrayList<DownloadedFile> entries = new ArrayList<DownloadedFile>();
		
		WorkspaceRepository wksRepo = new WorkspaceRepository();
		ProductWorkspaceRepository prodWksRepo = new ProductWorkspaceRepository();
		DownloadedFilesRepository downFilesRepo = new DownloadedFilesRepository();

		//get all my workspaces
		List<Workspace> myWorkspacesList = wksRepo.GetWorkspaceByUser(userId);
		Map<String, Workspace> myWorkspaces = new HashMap<String, Workspace>();
		for (Workspace wks : myWorkspacesList) {
			myWorkspaces.put(wks.getWorkspaceId(), wks);
		}
		
		//retrieve all compatible files
		List<DownloadedFile> files = downFilesRepo.Search(from, to, freeText, category);
		
		for (DownloadedFile df : files) {
			
			//check if the product is in my workspace
			ProductViewModel pvm = df.getProductViewModel();
			//boolean isPublic = category.equals(DownloadedFileCategory.PUBLIC)fileWorkspaces.isEmpty();
			boolean isMine = category.equals(DownloadedFileCategory.PUBLIC.name());
			if (!isMine) {
				if (pvm != null) {
					pvm.setMetadata(null);
					List<String> fileWorkspaces = prodWksRepo.getWorkspaces(pvm.getFileName()); //TODO check if productName should be used					
					for (String wks : fileWorkspaces) {
						if (myWorkspaces.containsKey(wks)) {
							isMine = true;
							break;
						}
					}
				}
			}
			
//			if (isMine || isPublic) {
			if (isMine) {
				
				entries.add(df);
			}
			
		}
		return entries;
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
			System.out.println("AuthResource.IngestFile: ERROR: unable to access uploaded file " + oFilePath.getAbsolutePath());
			return Response.serverError().build();
		}
		try {
			ProcessWorkspace oProcess = null;
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sAccount);
			oParameter.setExchange(sWorkspace);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			try
			{
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.INGEST.name());
				oProcess.setProductName(oFilePath.getName());
				oProcess.setWorkspaceId(sWorkspace);
				oProcess.setUserId(sAccount);
				oProcess.setProcessObjId(Utils.GetRandomName());
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcess);
				//set the process object Id to params
				oParameter.setProcessObjId(oProcess.getProcessObjId());
			}
			catch(Exception oEx){
				System.out.println("DownloadResource.Download: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				return Response.serverError().build();
			}
	
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + oProcess.getProcessObjId();
			//TODO move it before inserting the new process into DB
			SerializationUtils.serializeObjectToXML(sPath, oParameter);
	
			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");
			String sJavaExe = m_oServletConfig.getInitParameter("JavaExe");
	
			String sShellExString = sJavaExe + " -jar " + sLauncherPath +" -operation " + LauncherOperations.INGEST + " -parameter " + sPath;
	
			System.out.println("DownloadResource.Download: shell exec " + sShellExString);
	
			Process oProc = Runtime.getRuntime().exec(sShellExString);
			
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
			IngestFileParameter oParameter = new IngestFileParameter();
			oParameter.setWorkspace(sWorkspace);
			oParameter.setUserId(sAccount);
			oParameter.setExchange(sWorkspace);
			oParameter.setFilePath(oFilePath.getAbsolutePath());
			
			try
			{
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.INGEST.name());
				oProcess.setProductName(oFilePath.getName());
				oProcess.setWorkspaceId(sWorkspace);
				oProcess.setUserId(sAccount);
				oProcess.setProcessObjId(Utils.GetRandomName());
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcess);
				//set the process object Id to params
				oParameter.setProcessObjId(oProcess.getProcessObjId());
			}
			catch(Exception oEx){
				System.out.println("CatalogueResource.IngestFileInWorkspace: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				return oResult;		
			}
	
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + oProcess.getProcessObjId();
			//TODO move it before inserting the new process into DB
			SerializationUtils.serializeObjectToXML(sPath, oParameter);
	
			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");
			String sJavaExe = m_oServletConfig.getInitParameter("JavaExe");
	
			String sShellExString = sJavaExe + " -jar " + sLauncherPath +" -operation " + LauncherOperations.INGEST + " -parameter " + sPath;
	
			System.out.println("CatalogueResource.IngestFileInWorkspace: shell exec " + sShellExString);
	
			Process oProc = Runtime.getRuntime().exec(sShellExString);
			
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
	//TODO change return type
	public PrimitiveResult ftpTransferFile(@HeaderParam("x-session-token") String sSessionId, FtpTransferViewModel oFtpTransferVM) {
		Wasdi.DebugLog("CatalogResource.ftpTransferFile");

		//input validation
		if(null == sSessionId || null == oFtpTransferVM) {
			//TODO check appropriateness
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
		
		
		try {
			Wasdi.DebugLog("CatalogResource.ftpTransferFile: prepare parameters");
			FtpTransferParameters oParams = new FtpTransferParameters();
			oParams.setM_eDirection(FtpDirection.UPLOAD);
			oParams.setM_sFtpServer(oFtpTransferVM.getServer());
			oParams.setM_iPort(oFtpTransferVM.getPort());
			oParams.setM_sUsername(oFtpTransferVM.getUser());
			oParams.setM_sPassword(oFtpTransferVM.getPassword());
			oParams.setM_sRemotePath(oFtpTransferVM.getDestinationAbsolutePath());
			oParams.setM_sRemoteFileName(oFtpTransferVM.getFileName());
			oParams.setM_sLocalFileName(oFtpTransferVM.getFileName());
			DownloadedFilesRepository oRepo = new DownloadedFilesRepository();
			oParams.setM_sLocalPath(oRepo.GetDownloadedFile(oParams.getM_sLocalFileName()).getFilePath());
	
			Wasdi.DebugLog("CatalogResource.ftpTransferFile: prepare process");
			
			
			ProcessWorkspace oProcess = new ProcessWorkspace();
			oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
			oProcess.setOperationType(LauncherOperations.UPLOADVIAFTP.name());
			//oProcess.setProductName(sFileUrl);
			//oProcess.setWorkspaceId(sWorkspaceId);
			oProcess.setUserId(sUserId);
			oProcess.setProcessObjId(Utils.GetRandomName());
			oProcess.setStatus(ProcessStatus.CREATED.name());
			oParams.setProcessObjId(oProcess.getProcessObjId());
			
			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + oProcess.getProcessObjId();
			SerializationUtils.serializeObjectToXML(sPath, oParams);
			
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			
			
			//TODO get file size (length) and other properties by reading from DB
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
	
	public static void main(String[] args) throws Exception {
		
		MongoRepository.SERVER_PORT = 27018;
		
		String userId = "paolo";
		String from = "";//"201709200000";
		String to = "";//"201709222200";
		String freeText = "";
		String category = "PUBLIC";

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
		Date dtFrom = (from==null || from.isEmpty())?null:format.parse(from);
		Date dtTo = (to==null || to.isEmpty())?null:format.parse(to);
		ArrayList<DownloadedFile> entries = new CatalogResources().searchEntries(dtFrom, dtTo, freeText, category, userId);
		
		for (DownloadedFile df : entries) {
			System.out.println(df.getRefDate() + " --> " + df.getFilePath());
		}
		
	}
	
}
