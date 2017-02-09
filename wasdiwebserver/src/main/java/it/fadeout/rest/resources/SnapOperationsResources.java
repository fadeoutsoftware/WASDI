package it.fadeout.rest.resources;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.parameters.PublishParameters;
import wasdi.shared.parameters.RangeDopplerGeocodingParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;

@Path("/snap")
public class SnapOperationsResources {
	
	@Context
	ServletConfig m_oServletConfig;

	@GET
	@Path("terrain")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response TerrainCorrection(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sSourceProductName") String sSourceProductName, @QueryParam("sDestinationProductName") String sDestinationProductName, @QueryParam("sWorkspaceId") String sWorkspaceId) throws IOException
	{
		try {

			if (Utils.isNullOrEmpty(sSessionId)) return Response.status(401).build();

			User oUser = Wasdi.GetUserFromSession(sSessionId);

			if (oUser==null) return Response.status(401).build();
			if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();


			String sUserId = oUser.getUserId();

			//Update process list
			String sProcessId = "";
			try
			{
				ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
				ProcessWorkspace oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
				oProcess.setOperationType(LauncherOperations.TERRAIN);
				oProcess.setProductName(sSourceProductName);
				oProcess.setWorkspaceId(sWorkspaceId);
				oProcess.setUserId(sUserId);
				sProcessId = oRepository.InsertProcessWorkspace(oProcess);
			}
			catch(Exception oEx){
				System.out.println("DownloadResource.Download: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
			}

			String sPath = m_oServletConfig.getInitParameter("SerializationPath") + Wasdi.GetSerializationFileName();

			RangeDopplerGeocodingParameter oParameter = new RangeDopplerGeocodingParameter(); 
			oParameter.setSourceProductName(sSourceProductName);
			oParameter.setDestinationProductName(sDestinationProductName);
			oParameter.setWorkspace(sWorkspaceId);
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setProcessObjId(sProcessId);
			
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

			String sLauncherPath = m_oServletConfig.getInitParameter("LauncherPath");

			String sShellExString = "java -jar " + sLauncherPath +" -operation " + LauncherOperations.TERRAIN + " -parameter " + sPath;

			System.out.println("DownloadResource.Download: shell exec " + sShellExString);

			Process oProc = Runtime.getRuntime().exec(sShellExString);


		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.ok().build();

	}
}
