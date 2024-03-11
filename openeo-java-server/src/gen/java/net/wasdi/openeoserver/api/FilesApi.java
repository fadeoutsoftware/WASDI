package net.wasdi.openeoserver.api;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.swagger.annotations.ApiParam;
import net.wasdi.openeoserver.WasdiOpenEoServer;
import net.wasdi.openeoserver.viewmodels.Error;
import net.wasdi.openeoserver.viewmodels.Link;
import net.wasdi.openeoserver.viewmodels.WorkspaceFiles;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.Node;
import wasdi.shared.business.OpenEOJob;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.users.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.OpenEOJobRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.products.ProductPropertiesViewModel;

@Path("/files")
public class FilesApi  {

	public FilesApi(@Context ServletConfig servletContext) {
	}

	@javax.ws.rs.GET
	@Produces({ "application/json" })
	public Response listFiles(@QueryParam("limit")  @Min(1) Integer limit,@HeaderParam("Authorization") String sAuthorization) {
		
		if (Utils.isNullOrEmpty(sAuthorization)) {
			WasdiLog.debugLog("CredentialsApi.authenticateBasic: no credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();			
		}
		
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.FORBIDDEN).entity(Error.getForbideenError()).build();    		
    	}
    	
    	WorkspaceFiles oWorkspaceFiles = new WorkspaceFiles();
    	
    	int iLimit = -1;
    	if (limit != null)  {
    		iLimit = (int) limit;
    	}

    	try {
    		
    		String sSessionId = sAuthorization.substring("Bearer basic//".length());
    		
    		OpenEOJobRepository oOpenEOJobRepository = new OpenEOJobRepository();
    		List<OpenEOJob> aoJobs =oOpenEOJobRepository.getOpenEOJobsByUser(oUser.getUserId());
    		
    		ArrayList<String> asWorkspaces = new ArrayList<>();
    		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
    		NodeRepository oNodeRepository = new NodeRepository();
    		HashMap<String, String> asWorkspaceAddresses = new HashMap<>();
    		
    		for (OpenEOJob oJob : aoJobs) {
				String sWorkspaceId = oJob.getWorkspaceId();
				
				if (!asWorkspaces.contains(sWorkspaceId)) {
					asWorkspaces.add(sWorkspaceId);
					Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
					Node oNode = oNodeRepository.getNodeByCode(oWorkspace.getNodeCode());
					asWorkspaceAddresses.put(sWorkspaceId, oNode.getNodeBaseAddress());
				}
			}
    		
    		DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
    		
    		int iAdded = 0;
    		
    		for (String sWorkspaceId : asWorkspaces) {
				List<DownloadedFile> aoWasdiFiles = oDownloadedFilesRepository.getByWorkspace(sWorkspaceId);
				
				for (DownloadedFile oWasdiFile : aoWasdiFiles) {
					net.wasdi.openeoserver.viewmodels.File oOpenEOFile = new net.wasdi.openeoserver.viewmodels.File();
					oOpenEOFile.path = sWorkspaceId + "/" + oWasdiFile.getFileName();
					String sPropApiUrl = asWorkspaceAddresses.get(sWorkspaceId);
					if (!sPropApiUrl.endsWith("/")) sPropApiUrl += "/";
					sPropApiUrl += "catalog/properties?workspace=" + sWorkspaceId + "&file=" +oWasdiFile.getFileName();
					
					HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sPropApiUrl, HttpUtils.getStandardHeaders(sSessionId));
					
					if (oHttpCallResponse.getResponseCode()==200) {
						// call the API to get the properties 
						String sResult = oHttpCallResponse.getResponseBody();
						
			        	// Get back the primitive result: it does not work right to go in the catch 
						ProductPropertiesViewModel oProductPropertiesViewModel = MongoRepository.s_oMapper.readValue(sResult,ProductPropertiesViewModel.class);
						
						oOpenEOFile.size = oProductPropertiesViewModel.getSize();
						Date oDate = new Date(oProductPropertiesViewModel.getLastUpdateTimestampMs());
						oOpenEOFile.modified = oDate.toString();
						
						oWorkspaceFiles.addFilesItem(oOpenEOFile);
						
						iAdded ++;
						if (iLimit!=-1) {
							if (iAdded >= iLimit) break;
						}
					}
				}
				
				if (iLimit!=-1) {
					if (iAdded >= iLimit) break;
				}				
			}
    		
			if (iLimit!=-1) {
				String sBaseAddress = WasdiConfig.Current.openEO.baseAddress;
				if (!sBaseAddress.endsWith("/")) sBaseAddress += "/";

				Link oLink = new Link();
				oLink.setHref(new URI(sBaseAddress + "files"));
				oLink.setRel("next");
				oLink.setTitle("Files page");
				oLink.setType("application/json");
				oWorkspaceFiles.addLinksItem(oLink);
			}
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("FilesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("FilesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
		return Response.ok().entity(oWorkspaceFiles).build();
	}
	
	@javax.ws.rs.DELETE
	@Path("/{path}")
	@Produces({ "application/json" })
	public Response deleteFile(@PathParam("path") @NotNull  String path, @HeaderParam("Authorization") String sAuthorization) {
		
		if (Utils.isNullOrEmpty(sAuthorization)) {
			WasdiLog.debugLog("CredentialsApi.authenticateBasic: no credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();			
		}
		
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.FORBIDDEN).entity(Error.getForbideenError()).build();    		
    	}

    	try {
    		
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("FilesApi.delete error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("FilesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
		return Response.ok().build();
	}
	
	@javax.ws.rs.GET
	@Path("/{path}")
	@Produces({ "application/octet-stream", "application/json" })
	public Response downloadFile(@PathParam("path") @NotNull  String path,@HeaderParam("Authorization") String sAuthorization) {
		
		if (Utils.isNullOrEmpty(sAuthorization)) {
			WasdiLog.debugLog("CredentialsApi.authenticateBasic: no credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();			
		}
		
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.FORBIDDEN).entity(Error.getForbideenError()).build();    		
    	}
    	
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("FilesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("FilesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
		return Response.ok().build();
	}
	
	@javax.ws.rs.PUT
	@Path("/{path}")
	@Consumes({ "application/octet-stream" })
	@Produces({ "application/json" })
	public Response uploadFile(@PathParam("path") @NotNull  String path,@ApiParam(value = "", required = true) @NotNull  File body,@HeaderParam("Authorization") String sAuthorization) {
		
		if (Utils.isNullOrEmpty(sAuthorization)) {
			WasdiLog.debugLog("CredentialsApi.authenticateBasic: no credentials");
			return Response.status(Status.UNAUTHORIZED).entity(Error.getUnathorizedError()).build();			
		}
		
    	User oUser = WasdiOpenEoServer.getUserFromAuthenticationHeader(sAuthorization);
    	
    	if (oUser == null) {
			WasdiLog.debugLog("CollectionsApi.describeCollection: invalid credentials");
			return Response.status(Status.FORBIDDEN).entity(Error.getForbideenError()).build();    		
    	}
		
    	try {
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("FilesApi.method error: " , oEx);    		    		
    		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Error.getError("FilesApi.method", "InternalServerError", oEx.getMessage())).build();
		}
    	
		return Response.ok().build();
	}
}
