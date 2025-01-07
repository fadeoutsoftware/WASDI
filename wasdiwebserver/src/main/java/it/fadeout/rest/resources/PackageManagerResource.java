package it.fadeout.rest.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.type.TypeReference;

import it.fadeout.Wasdi;
import it.fadeout.threads.UpdateProcessorEnvironmentWorker;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Node;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.users.User;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.packagemanagers.PackageManagerUtils;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.processors.PackageManagerFullInfoViewModel;
import wasdi.shared.viewmodels.processors.PackageManagerViewModel;
import wasdi.shared.viewmodels.processors.PackageViewModel;

/**
 * Package Manager Resource
 * 
 * Offers the API to allow the users to interact with the package manager of their application hosted in WASDI.
 * 
 * 	.get the list of packages
 * 	.execute actions on the package manager
 * 	.get the version of the package manager itself
 * 	.get the list of actions executed on a single application (history of users' operations)
 * 
 * 
 * @author p.campanella
 *
 */
@Path("/packageManager")
public class PackageManagerResource {
	
	/**
	 * Get the list of packages in an application
	 * 
	 * @param sSessionId Session Id
	 * @param sName Application Name
	 * @return list of PackageViewModel
	 * @throws Exception
	 */
	@GET
	@Path("/listPackages")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response getListPackages(@HeaderParam("x-session-token") String sSessionId, @QueryParam("name") String sName) {
		WasdiLog.debugLog("PackageManagerResource.getListPackages( " + "Name: " + sName + ", " + " )");
		
		// Check session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			WasdiLog.warnLog("PackageManagerResource.getListPackages: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		try {
			List<PackageViewModel> aoPackages = new ArrayList<>();
			
			
			if (!PermissionsUtils.canUserAccessProcessorByName(oUser.getUserId(), sName)) {
				WasdiLog.warnLog("PackageManagerResource.getListPackages: user cannot access the processor");
				return Response.status(Status.FORBIDDEN).build();			
			}
			
			String sContentAsJson = readPackagesInfoFile(sName);

			if (Utils.isNullOrEmpty(sContentAsJson)) {
				WasdiLog.warnLog("PackageManagerResource.getListPackages: " + "the packagesInfo.json is null or empty");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			PackageManagerFullInfoViewModel oPackageManagerFullInfoViewModel = MongoRepository.s_oMapper.readValue(sContentAsJson, new TypeReference<PackageManagerFullInfoViewModel>(){});

			if (oPackageManagerFullInfoViewModel == null) {
				WasdiLog.warnLog("PackageManagerResource.getListPackages: the packagesInfo.json content could not be parsed");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			List<PackageViewModel> aoPackagesOutdated = oPackageManagerFullInfoViewModel.getOutdated();

			if (aoPackagesOutdated != null) {
				aoPackages.addAll(aoPackagesOutdated);
			}

			List<PackageViewModel> aoPackagesUptodate = oPackageManagerFullInfoViewModel.getUptodate();

			if (aoPackagesUptodate != null) {
				aoPackages.addAll(aoPackagesUptodate);
			}

			List<PackageViewModel> aoPackagesAll = oPackageManagerFullInfoViewModel.getAll();

			if (aoPackagesAll != null) {
				aoPackages.addAll(aoPackagesAll);
			}

			Comparator<PackageViewModel> oComparator = Comparator.comparing(PackageViewModel::getPackageName, String.CASE_INSENSITIVE_ORDER);
			Collections.sort(aoPackages, oComparator);

			return Response.ok(aoPackages).build();			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PackageManagerResource.getListPackages: exception ", oEx);
			return Response.serverError().build();
		}		
	}

	/**
	 * Get the list of actions executed on an application
	 * 
	 * @param sSessionId Session Id
	 * @param sName Name of the application
	 * @return List of strings
	 * @throws Exception
	 */
	@GET
	@Path("/environmentActions")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response getEnvironmentActionsList(@HeaderParam("x-session-token") String sSessionId, @QueryParam("name") String sName) {
		WasdiLog.debugLog("PackageManagerResource.getEnvironmentActionsList( " + "Name: " + sName + ", " + " )");
		
		// Check session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			WasdiLog.warnLog("PackageManagerResource.getEnvironmentActionsList: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		try {
			List<String> asEnvActions = new ArrayList<>();

			
			if (!PermissionsUtils.canUserAccessProcessorByName(oUser.getUserId(), sName)) {
				WasdiLog.warnLog("PackageManagerResource.getEnvironmentActionsList: user cannot access the processor");
				return Response.status(Status.FORBIDDEN).build();			
			}		
			
			if (WasdiConfig.Current.isMainNode() == false) {
				WasdiLog.warnLog("PackageManagerResource.getEnvironmentActionsList: this API is for the main node");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			String sContent = readEnvironmentActionsFile(sName);

			if (Utils.isNullOrEmpty(sContent)) {
				WasdiLog.warnLog("PackageManagerResource.getEnvironmentActionsList: " + "the envActionsList.txt is null or empty");
				return Response.ok(new ArrayList<String>()).build();
			}

			asEnvActions = parseEnvironmentActionsContent(sContent);

			return Response.ok(asEnvActions).build();			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PackageManagerResource.getEnvironmentActionsList: exception ", oEx);
			return Response.serverError().build();
		}
	}
	
	/**
	 * Get the version of the Package Manager of an application
	 * @param sSessionId Session Id
	 * @param sName Name of the application
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("/managerVersion")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response getManagerVersion(@HeaderParam("x-session-token") String sSessionId, @QueryParam("name") String sName) {
		WasdiLog.debugLog("PackageManagerResource.getManagerVersion( " + "Name: " + sName + " )");
		
		// Check session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			WasdiLog.warnLog("PackageManagerResource.getManagerVersion: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if (Utils.isNullOrEmpty(sName)) {
			WasdiLog.warnLog("PackageManagerResource.getManagerVersion: invalid app name");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		if (!PermissionsUtils.canUserAccessProcessorByName(oUser.getUserId(), sName)) {
			WasdiLog.warnLog("PackageManagerResource.getManagerVersion: user cannot access the processor");
			return Response.status(Status.FORBIDDEN).build();			
		}		
		
		try {
			// Trying to read the Package Manager info from the packagesInfo.json file.
			// If the info is valid, reply using it
			String sContentAsJson = readPackagesInfoFile(sName);

			if (!Utils.isNullOrEmpty(sContentAsJson)) {

				PackageManagerFullInfoViewModel oPackageManagerFullInfoViewModel = MongoRepository.s_oMapper.readValue(sContentAsJson, new TypeReference<PackageManagerFullInfoViewModel>(){});

				if (oPackageManagerFullInfoViewModel != null) {
					PackageManagerViewModel oPackageManagerVM = oPackageManagerFullInfoViewModel.getPackageManager();

					return Response.ok(oPackageManagerVM).build();
				}
			}

			// Otherwise, if the Package Manager info from the packagesInfo.json file is not valid, make a live call.
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToRun = oProcessorRepository.getProcessorByName(sName);
			
			if (oProcessorToRun==null) {
				WasdiLog.warnLog("PackageManagerResource.getManagerVersion: processor not found " + sName);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}

			PackageManagerViewModel oPackageManagerVM = null;

			try {
				IPackageManager oPackageManager = PackageManagerUtils.getPackageManagerByProcessor(oProcessorToRun);
				
				if (oPackageManager == null) {
					return Response.ok().build();
				}
				oPackageManagerVM = oPackageManager.getManagerVersion();
			} catch (Exception oEx) {
				WasdiLog.errorLog("PackageManagerResource.getManagerVersion: " + oEx);
			}

			return Response.ok(oPackageManagerVM).build();			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PackageManagerResource.getManagerVersion: exception ", oEx);
			return Response.serverError().build();
		}
	}
	
	/**
	 * Force the update of the environment of a processor
	 * 
	 * @param sSessionId User Session Id
	 * @param sProcessorId Processor Id
	 * @param sWorkspaceId Workspace Id
	 * @param sUpdateCommand Workspace Id
	 * @return std http response
	 */
	@GET
	@Path("/environmentupdate")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response environmentUpdate(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorId") String sProcessorId,
			@QueryParam("workspace") String sWorkspaceId,
			@QueryParam("updateCommand") String sUpdateCommand) {
		WasdiLog.debugLog("PackageManagerResource.environmentupdate( Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " updateCommand: " + sUpdateCommand + " )");

		try {
			// Check Session
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				WasdiLog.warnLog("ProcessorResources.environmentupdate( Session: " + sSessionId + ", Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			WasdiLog.debugLog("PackageManagerResource.environmentupdate: get Processor");	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToForceUpdate = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessorToForceUpdate == null) {
				WasdiLog.warnLog("PackageManagerResource.environmentupdate: unable to find processor " + sProcessorId);
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!PermissionsUtils.canUserWriteProcessor(oUser.getUserId(), oProcessorToForceUpdate.getProcessorId())) {
				WasdiLog.warnLog("PackageManagerResource.environmentupdate: user cannot write the processor");
				return Response.status(Status.FORBIDDEN).build();			
			}
			
			IPackageManager oPackageManager = PackageManagerUtils.getPackageManagerByProcessor(oProcessorToForceUpdate);
			
			if (oPackageManager==null) {
				WasdiLog.warnLog("PackageManagerResource.environmentupdate: processor " + sProcessorId + " does not has a Package Manager");
				return Response.status(Status.BAD_REQUEST).build();				
			}
			
			if (!Utils.isNullOrEmpty(sUpdateCommand)) {
				String [] asParts = sUpdateCommand.split("/");
				if (asParts != null) {
					if (asParts.length>1) {
						String sPackage = asParts[1];
						WasdiLog.debugLog("PackageManagerResource.environmentupdate: Action on package " + sPackage);
						
						if (oPackageManager.isValidPackage(sPackage)==false) {
							WasdiLog.warnLog("PackageManagerResource.environmentupdate: " + sPackage + " is not recognized as valid");
							return Response.status(Status.BAD_REQUEST).build();
						}
					}
				}
			}
			
			// Schedule the process to run the operation in the environment
			String sProcessObjId = Utils.getRandomName();
			
			// Get the dedicated special workpsace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, WasdiConfig.Current.nodeCode);
			
			WasdiLog.debugLog("PackageManagerResource.environmentupdate: create local operation");
			String sUserId = oUser.getUserId();

			ProcessorParameter oProcessorParameter = new ProcessorParameter();
			oProcessorParameter.setName(oProcessorToForceUpdate.getName());
			oProcessorParameter.setProcessorID(oProcessorToForceUpdate.getProcessorId());
			oProcessorParameter.setWorkspace(oWorkspace.getWorkspaceId());
			oProcessorParameter.setUserId(sUserId);
			oProcessorParameter.setExchange(sWorkspaceId);
			oProcessorParameter.setProcessObjId(sProcessObjId);
			
			Map<String, String> asCommand = new HashMap<>();
			asCommand.put("updateCommand", sUpdateCommand);
			String sJson = MongoRepository.s_oMapper.writeValueAsString(asCommand);
			oProcessorParameter.setJson(sJson);
			oProcessorParameter.setProcessorType(oProcessorToForceUpdate.getType());
			oProcessorParameter.setSessionID(sSessionId);
			oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			
			// Trigger the action
			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.ENVIRONMENTUPDATE.name(), oProcessorToForceUpdate.getName(), oProcessorParameter);

			if (!Utils.isNullOrEmpty(sUpdateCommand) && WasdiConfig.Current.isMainNode()) {
				// In the main node with a real action: start a thread to update all the computing nodes
				try {
					WasdiLog.debugLog("PackageManagerResource.environmentupdate: this is the main node, starting Worker to update computing nodes");
					
					//This is the main node: forward the request to other nodes
					UpdateProcessorEnvironmentWorker oUpdateWorker = new UpdateProcessorEnvironmentWorker();
					
					NodeRepository oNodeRepo = new NodeRepository();
					List<Node> aoNodes = oNodeRepo.getNodesList();
					
					oUpdateWorker.init(aoNodes, sSessionId, sWorkspaceId, sProcessorId, sUpdateCommand);
					oUpdateWorker.start();
					
					WasdiLog.debugLog("PackageManagerResource.environmentupdate: Worker started");						
				}
				catch (Exception oEx) {
					WasdiLog.errorLog("PackageManagerResource.environmentupdate: error starting UpdateProcessorEnvironmentWorker " + oEx.toString());
				}				
			}
			else {
				WasdiLog.debugLog("PackageManagerResource.environmentupdate: this is only a refresh, run only on main node");
			}
			
			if (oRes.getBoolValue()) {
				return Response.ok().build();
			}
			else {
				return Response.serverError().build();
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PackageManagerResource.environmentupdate: " + oEx);
			return Response.serverError().build();
		}
	}	
	
	
	/**
	 * Get the list of actions executed on an application
	 * 
	 * @param sSessionId Session Id
	 * @param sName Name of the application
	 * @return List of strings
	 * @throws Exception
	 */
	@GET
	@Path("/reset")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response resetActionList(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId, @QueryParam("workspace") String sWorkspaceId) {
		WasdiLog.debugLog("PackageManagerResource.resetActionList( " + "processorId: " + sProcessorId + ", " + " )");
		
		// Check session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			WasdiLog.warnLog("PackageManagerResource.resetActionList: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		try {
			
			if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				WasdiLog.warnLog("PackageManagerResource.resetActionList: user cannot access the workspace");
				return Response.status(Status.FORBIDDEN).build();
			}
			
			if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), sProcessorId)) {
				WasdiLog.warnLog("PackageManagerResource.resetActionList: user cannot access the processor");
				return Response.status(Status.FORBIDDEN).build();			
			}		
			
			if (WasdiConfig.Current.isMainNode() == false) {
				WasdiLog.warnLog("PackageManagerResource.resetActionList: this API is for the main node");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			String sProcessorPath = PathsConfig.getProcessorFolder(oProcessor);
			String sEnvActionFile = sProcessorPath + "/envActionsList.txt";
			
			File oEnvActionFile = new File(sEnvActionFile);
			
			if (oEnvActionFile.exists()) {
				WasdiLog.debugLog("PackageManagerResource.resetActionList: envActionsList for app " + oProcessor.getName() + " found");
				
				if (oEnvActionFile.delete()) {
					WasdiLog.debugLog("PackageManagerResource.resetActionList: envActionsList for app " + oProcessor.getName() + " deleted");
				}
				else {
					WasdiLog.errorLog("PackageManagerResource.getEnvironmentActionsList: impossible to delete the envActionsList file!");
					return Response.serverError().build();					
				}
			}
			else {
				WasdiLog.warnLog("PackageManagerResource.resetActionList: envActionsList for app " + oProcessor.getName() + " NOT found");
			}

			return Response.ok().build();			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PackageManagerResource.getEnvironmentActionsList: exception ", oEx);
			return Response.serverError().build();
		}
	}
	
	/**
	 * Reads the packageInfo JSON file of a processor
	 * @param sProcessorName Name of the processor
	 * @return String with the JSON representation of packages, or an error message
	 */
	protected String readPackagesInfoFile(String sProcessorName) throws Exception {

		String sOutput = "{\"warning\": \"the packagesInfo.json file for the processor " + sProcessorName + " was not found\"}";

		try {
			// Take path of the processor
			String sProcessorPath = PathsConfig.getProcessorFolder(sProcessorName);
			java.nio.file.Path oDirPath = java.nio.file.Paths.get(sProcessorPath).toAbsolutePath().normalize();
			File oDirFile = oDirPath.toFile();

			if (!WasdiFileUtils.fileExists(oDirFile) || !oDirFile.isDirectory()) {
				WasdiLog.debugLog("PackageManagerResource.readPackagesInfoFile: directory " + oDirPath.toString() + " not found");
				return "{\"error\": \"directory " + oDirPath.toString() + " not found\"}";
			} 
			
			// Read the file
			String sAbsoluteFilePath = oDirFile.getAbsolutePath() + "/packagesInfo.json";
			if (WasdiFileUtils.fileExists(sAbsoluteFilePath)) {
				sOutput = WasdiFileUtils.fileToText(sAbsoluteFilePath);
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("PackageManagerResource.readPackagesInfoFile: " + oEx);
		}

		return sOutput;
	}

	/**
	 * Reads the envActions txt file of a processor
	 * @param sProcessorName Name of the processor
	 * @return String with the list of instructions/actions, or an error message
	 */
	protected String readEnvironmentActionsFile(String sProcessorName) throws Exception {

		String sOutput = "";

		try {

			WasdiLog.debugLog("PackageManagerResource.readEnvActionsFile: read actions for Processor " + sProcessorName);

			// Take path of the processor
			String sProcessorPath = PathsConfig.getProcessorFolder(sProcessorName);
			java.nio.file.Path oDirPath = java.nio.file.Paths.get(sProcessorPath).toAbsolutePath().normalize();
			File oDirFile = oDirPath.toFile();

			if (!WasdiFileUtils.fileExists(oDirFile) || !oDirFile.isDirectory()) {
				WasdiLog.debugLog("PackageManagerResource.readEnvActionsFile: directory " + oDirPath.toString() + " not found");
				return "";
			}
			
			// Read the file
			String sAbsoluteFilePath = oDirFile.getAbsolutePath() + "/envActionsList.txt";
			if (WasdiFileUtils.fileExists(sAbsoluteFilePath)) {
				sOutput = WasdiFileUtils.fileToText(sAbsoluteFilePath);
				WasdiLog.debugLog("PackageManagerResource.readEnvActionsFile: file " + sAbsoluteFilePath + " found:\n" + sOutput);
			}
			else {
				WasdiLog.debugLog("PackageManagerResource.readEnvActionsFile: no actions for this processor ");
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("PackageManagerResource.readEnvActionsFile: " + oEx);
		}

		return sOutput;
	}
	
	/**
	 * Split the content of a json actions file in a list of strings
	 * @param sContent
	 * @return
	 */
	private static List<String> parseEnvironmentActionsContent(String sContent) {
		List<String> asEnvActions = new ArrayList<>();

		if (!Utils.isNullOrEmpty(sContent)) {
			String[] asRows = sContent.split("\n");

			for (String sRow : asRows ) {
				if (!sRow.trim().isEmpty()) {
					asEnvActions.add(sRow.trim());
				}
			}
		}

		return asEnvActions;
	}

}
