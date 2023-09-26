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
import wasdi.shared.business.Workspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.packagemanagers.CondaPackageManagerImpl;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.packagemanagers.PipPackageManagerImpl;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.processors.PackageManagerFullInfoViewModel;
import wasdi.shared.viewmodels.processors.PackageManagerViewModel;
import wasdi.shared.viewmodels.processors.PackageViewModel;

@Path("/packageManager")
public class PackageManagerResource {
	
	@GET
	@Path("/listPackages")
	public Response getListPackages(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName) throws Exception {
		WasdiLog.debugLog("PackageManagerResource.getListPackages( " + "Name: " + sName + ", " + " )");
		
		List<PackageViewModel> aoPackages = new ArrayList<>();
		
		// Check session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			WasdiLog.warnLog("PackageManagerResource.getListPackages: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
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

	@GET
	@Path("/environmentActions")
	public Response getEnvironmentActionsList(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName) throws Exception {
		WasdiLog.debugLog("PackageManagerResource.getEnvironmentActionsList( " + "Name: " + sName + ", " + " )");

		List<String> asEnvActions = new ArrayList<>();

		// Check session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			WasdiLog.warnLog("PackageManagerResource.getEnvironmentActionsList: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
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
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		asEnvActions = parseEnvironmentActionsContent(sContent);

		return Response.ok(asEnvActions).build();
	}
	
	@GET
	@Path("/managerVersion")
	public Response getManagerVersion(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName) throws Exception {
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
			IPackageManager oPackageManager = getPackageManager(oProcessorToRun);

			oPackageManagerVM = oPackageManager.getManagerVersion();
		} catch (Exception oEx) {
			WasdiLog.errorLog("PackageManagerResource.getManagerVersion: " + oEx);
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
			
			// Schedule the process to run the operation in the environment
			String sProcessObjId = Utils.getRandomName();
			
			String sPath = WasdiConfig.Current.paths.serializationPath;
			
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
			
			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.ENVIRONMENTUPDATE.name(), oProcessorToForceUpdate.getName(), sPath, oProcessorParameter);
			
			if (WasdiConfig.Current.isMainNode()) {
				
				// In the main node: start a thread to update all the computing nodes
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
	 * Get the appropriate Package Manager Instance from the Processor (type)
	 * @param oProcessor The Processor we want to access the Package Manager
	 * @return Package Manager Instance
	 */
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
	
	/**
	 * Reads the packageInfo JSON file of a processor
	 * @param sProcessorName Name of the processor
	 * @return String with the JSON representation of packages, or an error message
	 */
	protected String readPackagesInfoFile(String sProcessorName) throws Exception {

		String sOutput = "{\"warning\": \"the packagesInfo.json file for the processor " + sProcessorName + " was not found\"}";

		try {

			WasdiLog.debugLog("PackageManagerResource.readPackagesInfoFile: read Processor " + sProcessorName);

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

		String sOutput = "warning: the envActionsList.txt file for the processor " + sProcessorName + " was not found}";

		try {

			WasdiLog.debugLog("PackageManagerResource.readEnvActionsFile: read Processor " + sProcessorName);

			// Take path of the processor
			String sProcessorPath = PathsConfig.getProcessorFolder(sProcessorName);
			java.nio.file.Path oDirPath = java.nio.file.Paths.get(sProcessorPath).toAbsolutePath().normalize();
			File oDirFile = oDirPath.toFile();

			if (!WasdiFileUtils.fileExists(oDirFile) || !oDirFile.isDirectory()) {
				WasdiLog.debugLog("PackageManagerResource.readEnvActionsFile: directory " + oDirPath.toString() + " not found");
				return "error: directory " + oDirPath.toString() + " not found";
			} else {
				WasdiLog.debugLog("PackageManagerResource.readEnvActionsFile: directory " + oDirPath.toString() + " found");
			}
			
			// Read the file
			String sAbsoluteFilePath = oDirFile.getAbsolutePath() + "/envActionsList.txt";
			if (WasdiFileUtils.fileExists(sAbsoluteFilePath)) {
				sOutput = WasdiFileUtils.fileToText(sAbsoluteFilePath);
				WasdiLog.debugLog("PackageManagerResource.readEnvActionsFile: file " + sAbsoluteFilePath + " found:\n" + sOutput);
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
