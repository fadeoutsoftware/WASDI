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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Catalog;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.data.CatalogRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.CatalogViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProcessWorkspaceViewModel;

@Path("/catalog")
public class CatalogResources {

	@Context
	ServletConfig m_oServletConfig;

	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadModel(@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition fileMetaData, @HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) throws Exception
	{ 
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

	@GET
	@Path("")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<CatalogViewModel> GetCatalogs(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) {

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


	@GET
	@Path("/assimilation")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response Assimilation(@HeaderParam("x-session-token") String sSessionId) {

		User oUser = Wasdi.GetUserFromSession(sSessionId);
		try {
			// Domain Check
			if (oUser == null) {
				Response.status(Status.UNAUTHORIZED).build();
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Response.status(Status.UNAUTHORIZED).build();
			}

			if (LaunchAssimilation())
			{
				System.out.println("CatalogResources.Assimilation: Assimilation end with success ");
				
				StreamingOutput fileStream =  new StreamingOutput() 
				{
					@Override
					public void write(java.io.OutputStream output) throws IOException, WebApplicationException 
					{
						
						System.out.println("CatalogResources.Assimilation: write output file ");
						
						try
						{
							String sAssimilationPath =   m_oServletConfig.getInitParameter("AssimilationPath");
							if (!sAssimilationPath.endsWith("/"))
								sAssimilationPath += "/";

							File oFolder = new File(sAssimilationPath);
							FilenameFilter oFilter = new FilenameFilter() {

								@Override
								public boolean accept(File dir, String name) {
									// TODO Auto-generated method stub
									if (name.toLowerCase().endsWith(".tif"))
										return true;

									return false;
								}
							};
							File[] aoFiles = oFolder.listFiles(oFilter);
							//take first 
							if (aoFiles != null && aoFiles.length > 0)
							{
								System.out.println("CatalogResources.Assimilation: file to download " + aoFiles[0].getName());
								java.nio.file.Path oPath = Paths.get(aoFiles[0].getPath());
								byte[] data = Files.readAllBytes(oPath);
								output.write(data);
								output.flush();
							}
						} 
						catch (Exception e) 
						{
							throw new WebApplicationException("File Not Found !!");
						}
					}
				};
				
				return Response
						.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
						.header("content-disposition","attachment; filename = Assimilation.tif")
						.build();
			}
		}
		catch (Exception oEx) {
			System.out.println("CatalogResources.Assimilation: error launching assimilation " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return Response.ok().build();
	}

	private boolean LaunchAssimilation() {

		try {

			System.out.println("CatalogResources.LaunchAssimilation: shell exec " + m_oServletConfig.getInitParameter("AssimilationCommandPath"));

			Process oProc = Runtime.getRuntime().exec(m_oServletConfig.getInitParameter("AssimilationCommandPath"));
			BufferedReader input = new BufferedReader(new InputStreamReader(oProc.getInputStream()));
            String sLine;

            while((sLine=input.readLine()) != null) {
            	System.out.println("CatalogResources.LaunchAssimilation: " + sLine);
            }
			if (oProc.waitFor() != 0)
				return false;

		}
		catch (Exception oEx) {
			System.out.println("CatalogResources.LaunchAssimilation: error during assimilation process " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return true;
	}

}
