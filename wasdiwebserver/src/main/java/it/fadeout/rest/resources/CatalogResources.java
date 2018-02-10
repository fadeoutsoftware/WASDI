package it.fadeout.rest.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.server.internal.process.AsyncContext;
import org.glassfish.jersey.server.internal.process.RespondingContext;

import it.fadeout.Wasdi;
import wasdi.shared.business.Catalog;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.DownloadedFileCategory;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.CatalogRepository;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.CatalogViewModel;
import wasdi.shared.viewmodels.ProductViewModel;

@Path("/catalog")
public class CatalogResources {

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

		File file = new File(entry.getFilePath());
		ResponseBuilder resp = null;
		if (!file.canRead()) {
			Wasdi.DebugLog("CatalogResources.DownloadEntry: file not readable");
			resp = Response.serverError();
		} 
		else {
			Wasdi.DebugLog("CatalogResources.DownloadEntry: file ok return content");
			resp = Response.ok(file);
			resp.header("Content-Disposition", "attachment; filename="+ entry.getFileName());
		}
		
		Wasdi.DebugLog("CatalogResources.DownloadEntry: done, return");
		return resp.build();
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
	

	@POST
	@Path("/assimilation")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public String Assimilation(
			@FormDataParam("humidity") InputStream humidityFile,
			@FormDataParam("humidity") FormDataContentDisposition humidityFileMetaData,
			@HeaderParam("x-session-token") String sessionId,
			@QueryParam("midapath") String midaPath) {
		
		Wasdi.DebugLog("CatalogResources.Assimilation");

		User user = Wasdi.GetUserFromSession(sessionId);
		try {

//			//check authentication
//			if (user == null || Utils.isNullOrEmpty(user.getUserId())) {
//				return Response.status(Status.UNAUTHORIZED).build();				
//			}
			
			//build and check paths
			File assimilationWD = new File(m_oServletConfig.getInitParameter("AssimilationWDPath"));
			if (!assimilationWD.isDirectory()) {				
				System.out.println("CatalogResources.Assimilation: ERROR: Invalid directory: " + assimilationWD.getAbsolutePath());
				throw new InternalServerErrorException("invalid directory in assimilation settings");				
			}						
			File midaTifFile = new File(midaPath);
			if (!midaTifFile.canRead()) {
				System.out.println("CatalogResources.Assimilation: ERROR: Invalid mida path: " + midaTifFile.getAbsolutePath());
				throw new InternalServerErrorException("invalid path in assimilation settings");
			}						
			File humidityTifFile = new File(assimilationWD, UUID.randomUUID().toString() + ".tif");
			File resultDir = new File(m_oServletConfig.getInitParameter("AssimilationResultPath"));
			File resultTifFile = new File(resultDir, UUID.randomUUID().toString() + ".tif");
			
			
			//save uploaded file			
			int read = 0;
			byte[] bytes = new byte[1024];
			OutputStream out = new FileOutputStream(humidityTifFile);
			while ((read = humidityFile.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();

			//execute assimilation
			if (launchAssimilation(midaTifFile, humidityTifFile, resultTifFile)) {
				String url = "wasdidownloads/" + resultTifFile.getName();
				return url;				
			}
			
			throw new InternalServerErrorException("unable to execute assimilation");
			
		} catch (Exception e) {
			System.out.println("CatalogResources.Assimilation: error launching assimilation " + e.getMessage());
			e.printStackTrace();
			throw new InternalServerErrorException("error launching assimilation: " + e.getMessage());
		}

	}

	private boolean launchAssimilation(File midaTifFile, File humidityTifFile, File resultTifFile) {

		try {
			
			String cmd[] = new String[] {
					m_oServletConfig.getInitParameter("AssimilationScript"),
					midaTifFile.getAbsolutePath(),
					humidityTifFile.getAbsolutePath(),
					resultTifFile.getAbsolutePath()
			};
			
			System.out.println("CatalogResources.LaunchAssimilation: shell exec " + Arrays.toString(cmd));

			Process proc = Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while((line=input.readLine()) != null) {
            	System.out.println("CatalogResources.LaunchAssimilation: Assimilation stdout: " + line);
            }
			if (proc.waitFor() != 0) return false;
		} catch (Exception oEx) {
			System.out.println("CatalogResources.LaunchAssimilation: error during assimilation process " + oEx.getMessage());
			oEx.printStackTrace();
			return false;
		}

		return true;
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
	
	
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadModel(@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition fileMetaData, @HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) throws Exception
	{ 
		Wasdi.DebugLog("CatalogResources.UploadModel");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();

		String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
		if (!sDownloadRootPath.endsWith("/"))
			sDownloadRootPath += "/";

		String sDownloadPath = sDownloadRootPath + oUser.getUserId()+ "/" + sWorkspaceId + "/" + "CONTINUUM";

		if(!Files.exists(Paths.get(sDownloadPath)))
		{
			if (Files.createDirectories(Paths.get(sDownloadPath))== null)
			{
				System.out.println("CatalogResources.uploadMapFile: Directory " + sDownloadPath + " not created");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

		}

		try
		{
			int read = 0;
			byte[] bytes = new byte[1024];

			OutputStream out = new FileOutputStream(new File(sDownloadPath + "/" + fileMetaData.getFileName()));
			while ((read = fileInputStream.read(bytes)) != -1) 
			{
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();

			/* TODO: Abilitare questa parte se si vuole far partire l'assimilazione dopo l'upload del file (E' ancora da testare) 
			 * 
			 * 
			String sAssimilationContinuumPath =   m_oServletConfig.getInitParameter("AssimilationContinuumPath");
			String sMulesmeEstimatePath =   m_oServletConfig.getInitParameter("MulesmeStimePath");
			if (!sAssimilationContinuumPath.endsWith("/"))
				sAssimilationContinuumPath += "/";
			if (!sMulesmeEstimatePath.endsWith("/"))
				sMulesmeEstimatePath += "/";


			//Continuum format SoilMoistureItaly_20160801230000
			//Get continuum date
			SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyyMMdd");
			String sFileName = fileMetaData.getFileName();
			String sDate = sFileName.split("_")[1].substring(0, 7);  
			Calendar oContinuumDate = Calendar.getInstance();
			Calendar oMulesmeDate = Calendar.getInstance();
			oContinuumDate.setTime(oDateFormat.parse(sDate));
			oMulesmeDate.setTime(oDateFormat.parse(sDate));
			oMulesmeDate.add(Calendar.DATE, 1);  // number of days to add
			String sMulesmeDate = oDateFormat.format(oMulesmeDate); 

			//Search in catalog soil moisture map with date = continuum date + 1 day
			CatalogRepository oRepo = new CatalogRepository();
			Catalog oCatalog = oRepo.GetCatalogsByDate(sMulesmeDate);
			if (oCatalog != null)
			{
				//Copy file into Mulesme Stime path
				Files.move(Paths.get(oCatalog.getFilePath()), Paths.get(sMulesmeEstimatePath));
				//Copy file from CONTINUUM to assimilation path
				Files.move(Paths.get(sDownloadPath + "/" + fileMetaData.getFileName()), Paths.get(sAssimilationContinuumPath));
				//launch assimilation
				LaunchAssimilation();
			}
			 */



		} catch (IOException e) 
		{
			throw new WebApplicationException("CatalogResources.uploadModel: Error while uploading file. Please try again !!");
		}




		return Response.ok().build();
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
