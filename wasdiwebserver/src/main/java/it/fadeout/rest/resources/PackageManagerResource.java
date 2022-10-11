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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.type.TypeReference;

import it.fadeout.Wasdi;
import it.fadeout.threads.UpdateProcessorEnvironmentWorker;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Node;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.managers.CondaPackageManagerImpl;
import wasdi.shared.managers.IPackageManager;
import wasdi.shared.managers.PipPackageManagerImpl;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.processors.PackageManagerFullInfoViewModel;
import wasdi.shared.viewmodels.processors.PackageManagerViewModel;
import wasdi.shared.viewmodels.processors.PackageViewModel;

@Path("/packageManager")
public class PackageManagerResource {

	/**
	 * Reads the packageInfo JSON file of a processor
	 * @param sProcessorName Name of the processor
	 * @return String with the JSON representation of packages, or an error message
	 */
	protected String readPackagesInfoFile(String sProcessorName) throws Exception {

		String sOutput = "{\"warning\": \"the packagesInfo.json file for the processor " + sProcessorName + " was not found\"}";

		try {

			Utils.debugLog("PackageManagerResource.readPackagesInfoFile: read Processor " + sProcessorName);

			// Take path of the processor
			String sProcessorPath = Wasdi.getDownloadPath() + "processors/" + sProcessorName;
			java.nio.file.Path oDirPath = java.nio.file.Paths.get(sProcessorPath).toAbsolutePath().normalize();
			File oDirFile = oDirPath.toFile();

			if (!WasdiFileUtils.fileExists(oDirFile) || !oDirFile.isDirectory()) {
				Utils.debugLog("PackageManagerResource.readPackagesInfoFile: directory " + oDirPath.toString() + " not found");
				return "{\"error\": \"directory " + oDirPath.toString() + " not found\"}";
			} 
			
			// Read the file
			String sAbsoluteFilePath = oDirFile.getAbsolutePath() + "/packagesInfo.json";
			if (WasdiFileUtils.fileExists(sAbsoluteFilePath)) {
				sOutput = WasdiFileUtils.fileToText(sAbsoluteFilePath);
			}

		} catch (Exception oEx) {
			Utils.debugLog("PackageManagerResource.readPackagesInfoFile: " + oEx);
		}

		return sOutput;
	}

	/**
	 * Reads the envActions txt file of a processor
	 * @param sProcessorName Name of the processor
	 * @return String with the list of instructions/actions, or an error message
	 */
	protected String readEnvironmentActionsFile(String sProcessorName) throws Exception {

		String sOutput = "warning: the envActionsList.txt file for the processor " + sProcessorName + " was not found}";

		try {

			Utils.debugLog("PackageManagerResource.readEnvActionsFile: read Processor " + sProcessorName);

			// Take path of the processor
			String sProcessorPath = Wasdi.getDownloadPath() + "processors/" + sProcessorName;
			java.nio.file.Path oDirPath = java.nio.file.Paths.get(sProcessorPath).toAbsolutePath().normalize();
			File oDirFile = oDirPath.toFile();

			if (!WasdiFileUtils.fileExists(oDirFile) || !oDirFile.isDirectory()) {
				Utils.debugLog("PackageManagerResource.readEnvActionsFile: directory " + oDirPath.toString() + " not found");
				return "error: directory " + oDirPath.toString() + " not found";
			} else {
				Utils.debugLog("PackageManagerResource.readEnvActionsFile: directory " + oDirPath.toString() + " found");
			}
			
			// Read the file
			String sAbsoluteFilePath = oDirFile.getAbsolutePath() + "/envActionsList.txt";
			if (WasdiFileUtils.fileExists(sAbsoluteFilePath)) {
				sOutput = WasdiFileUtils.fileToText(sAbsoluteFilePath);
				Utils.debugLog("PackageManagerResource.readEnvActionsFile: file " + sAbsoluteFilePath + " found:\n" + sOutput);
			}

		} catch (Exception oEx) {
			Utils.debugLog("PackageManagerResource.readEnvActionsFile: " + oEx);
		}

		return sOutput;
	}

	@GET
	@Path("/listPackages")
	public Response getListPackages(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName) throws Exception {
		Utils.debugLog("PackageManagerResource.getListPackages( " + "Name: " + sName + ", " + " )");
		
		List<PackageViewModel> aoPackages = new ArrayList<>();
		
		// Check session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			
			Utils.debugLog("PackageManagerResource.getListPackages: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		String sContentAsJson = readPackagesInfoFile(sName);

		if (Utils.isNullOrEmpty(sContentAsJson)) {
			Utils.debugLog("PackageManagerResource.getListPackages: " + "the packagesInfo.json is null or empty");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		PackageManagerFullInfoViewModel oPackageManagerFullInfoViewModel = MongoRepository.s_oMapper.readValue(sContentAsJson, new TypeReference<PackageManagerFullInfoViewModel>(){});

		if (oPackageManagerFullInfoViewModel == null) {
			Utils.debugLog("PackageManagerResource.getListPackages: the packagesInfo.json content could not be parsed");
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

	@GET
	@Path("/environmentActions")
	public Response getEnvironmentActionsList(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName) throws Exception {
		Utils.debugLog("PackageManagerResource.getEnvironmentActionsList( " + "Name: " + sName + ", " + " )");

		List<String> asEnvActions = new ArrayList<>();

		// Check session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			Utils.debugLog("PackageManagerResource.getEnvironmentActionsList: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if (WasdiConfig.Current.nodeCode.equals("wasdi") == false) {
			Utils.debugLog("PackageManagerResource.getEnvironmentActionsList: this API is for the main node");
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		String sContent = readEnvironmentActionsFile(sName);

		if (Utils.isNullOrEmpty(sContent)) {
			Utils.debugLog("PackageManagerResource.getEnvironmentActionsList: " + "the envActionsList.txt is null or empty");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		asEnvActions = parseEnvironmentActionsContent(sContent);

		return Response.ok(asEnvActions).build();
	}

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

	@GET
	@Path("/managerVersion")
	public Response getManagerVersion(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName) throws Exception {
		Utils.debugLog("PackageManagerResource.getManagerVersion( " + "Name: " + sName + " )");
		
		// Check session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			Utils.debugLog("PackageManagerResource.getManagerVersion: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if (Utils.isNullOrEmpty(sName)) {
			Utils.debugLog("PackageManagerResource.getManagerVersion: invalid app name");
			return Response.status(Status.BAD_REQUEST).build();
		}

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessorToRun = oProcessorRepository.getProcessorByName(sName);
		
		if (oProcessorToRun==null) {
			Utils.debugLog("PackageManagerResource.getManagerVersion: processor not found " + sName);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		PackageManagerViewModel oPackageManagerVM = null;

		try {
			IPackageManager oPackageManager = getPackageManager(oProcessorToRun);

			oPackageManagerVM = oPackageManager.getManagerVersion();
		} catch (Exception oEx) {
			Utils.debugLog("PackageManagerResource.getManagerVersion: " + oEx);
		}

		return Response.ok(oPackageManagerVM).build();
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
	public Response environmentUpdate(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorId") String sProcessorId,
			@QueryParam("workspace") String sWorkspaceId,
			@QueryParam("updateCommand") String sUpdateCommand) {
		Utils.debugLog("PackageManagerResource.environmentupdate( Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " updateCommand: " + sUpdateCommand + " )");

		try {
			// Check Session
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser==null) {
				Utils.debugLog("ProcessorResources.environmentupdate( Session: " + sSessionId + ", Processor: " + sProcessorId + ", WS: " + sWorkspaceId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sUpdateCommand)) {
				return Response.status(Status.BAD_REQUEST).build();
			}

			String sUserId = oUser.getUserId();
			
			Utils.debugLog("PackageManagerResource.environmentupdate: get Processor");	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToForceUpdate = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessorToForceUpdate == null) {
				Utils.debugLog("PackageManagerResource.environmentupdate: unable to find processor " + sProcessorId);
				return Response.serverError().build();
			}
			
			if (!oProcessorToForceUpdate.getUserId().equals(oUser.getUserId())) {
				
				UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
				
				if (!oUserResourcePermissionRepository.isProcessorSharedWithUser(sUserId, sProcessorId)) {
					Utils.debugLog("PackageManagerResource.environmentupdate: processor not of user " + oProcessorToForceUpdate.getUserId());
					return Response.status(Status.UNAUTHORIZED).build();					
				}
			}

			// Schedule the process to run the operation in the environment
			String sProcessObjId = Utils.getRandomName();
			
			String sPath = WasdiConfig.Current.paths.serializationPath;
			
			// Get the dedicated special workpsace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, Wasdi.s_sMyNodeCode);
			
			Utils.debugLog("PackageManagerResource.environmentupdate: create local operation");
			
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
			Utils.debugLog("PackageManagerResource.environmentupdate( sJson: " + sJson + " )");

			oProcessorParameter.setProcessorType(oProcessorToForceUpdate.getType());
			oProcessorParameter.setSessionID(sSessionId);
			oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			
			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.ENVIRONMENTUPDATE.name(), oProcessorToForceUpdate.getName(), sPath, oProcessorParameter);
			
			if (Wasdi.s_sMyNodeCode.equals("wasdi")) {
				
				// In the main node: start a thread to update all the computing nodes
				
				try {
					Utils.debugLog("PackageManagerResource.environmentupdate: this is the main node, starting Worker to update computing nodes");
					
					//This is the main node: forward the request to other nodes
					UpdateProcessorEnvironmentWorker oUpdateWorker = new UpdateProcessorEnvironmentWorker();
					
					NodeRepository oNodeRepo = new NodeRepository();
					List<Node> aoNodes = oNodeRepo.getNodesList();
					
					oUpdateWorker.init(aoNodes, sSessionId, sWorkspaceId, sProcessorId, sUpdateCommand);
					oUpdateWorker.start();
					
					Utils.debugLog("PackageManagerResource.environmentupdate: Worker started");						
				}
				catch (Exception oEx) {
					Utils.debugLog("PackageManagerResource.environmentupdate: error starting UpdateProcessorEnvironmentWorker " + oEx.toString());
				}
			}			
			
			if (oRes.getBoolValue()) {
				return Response.ok().build();
			}
			else {
				return Response.serverError().build();
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("PackageManagerResource.environmentupdate: " + oEx);
			return Response.serverError().build();
		}
	}	

	private IPackageManager getPackageManager(Processor oProcessor) {
		IPackageManager oPackageManager = null;

		String sType = oProcessor.getType();

		String sIp = WasdiConfig.Current.dockers.internalDockersBaseAddress;
		int iPort = oProcessor.getPort();

		if (sType.equals(ProcessorTypes.UBUNTU_PYTHON37_SNAP)) {
			oPackageManager = new PipPackageManagerImpl(sIp, iPort);
		} else if (sType.equals(ProcessorTypes.CONDA)) {
			oPackageManager = new CondaPackageManagerImpl(sIp, iPort);
		} else {
			throw new UnsupportedOperationException("The functionality is not yet implemented for this processor engine!");
		}

		return oPackageManager;
	}

}
