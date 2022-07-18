package it.fadeout.rest.resources;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import it.fadeout.Wasdi;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.managers.CondaPackageManagerImpl;
import wasdi.shared.managers.IPackageManager;
import wasdi.shared.managers.PipPackageManagerImpl;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.viewmodels.processors.PackageManagerViewModel;
import wasdi.shared.viewmodels.processors.PackageViewModel;

@Path("/packageManager")
public class PackageManagerResource {

	@GET
	@Path("/packagesInfo")
	public String readPackagesInfoFile(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName) throws Exception {
		Utils.debugLog("PackageManagerResource.getManagerVersion( " + "Name: " + sName + " )");

		String sOutput = "{\"warning\": \"the packagesInfo.json file for the processor " + sName + " was not found\"}";

		try {
			// Session checking
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("PackageManagerResource.getManagerVersion( Session: " + sSessionId + ", Name: " + sName + " ): invalid session");
				return "{\"error\": \"Invalid session\"}";
			}

			Utils.debugLog("PackageManagerResource.readPackagesInfoFile: read Processor " + sName);

			// Take path
			String sProcessorPath = Wasdi.getDownloadPath() + "processors/" + sName;
			java.nio.file.Path oDirPath = java.nio.file.Paths.get(sProcessorPath).toAbsolutePath().normalize();
			File oDirFile = oDirPath.toFile();

			if (!WasdiFileUtils.fileExists(oDirFile) || !oDirFile.isDirectory()) {
				Utils.debugLog("PackageManagerResource.readPackagesInfoFile: directory " + oDirPath.toString() + " not found");
				return "{\"error\": \"directory " + oDirPath.toString() + " not found\"}";
			} else {
				Utils.debugLog("PackageManagerResource.readPackagesInfoFile: directory " + oDirPath.toString() + " found");
			}

			String sAbsoluteFilePath = oDirFile.getAbsolutePath() + "/packagesInfo.json";
			if (WasdiFileUtils.fileExists(sAbsoluteFilePath)) {
				sOutput = WasdiFileUtils.fileToText(sAbsoluteFilePath);
				Utils.debugLog("PackageManagerResource.readPackagesInfoFile: file " + sAbsoluteFilePath + " found:\n" + sOutput);
			}

		} catch (Exception oEx) {
			Utils.debugLog("PackageManagerResource.readPackagesInfoFile: " + oEx);
		}

		return sOutput;
	}

	@GET
	@Path("/listPackages")
	public List<PackageViewModel> getListPackages(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName, @QueryParam("flag") String sFlag) throws Exception {
		Utils.debugLog("PackageManagerResource.getListPackages( " + "Name: " + sName + ", " + "Flag: " + sFlag + " )");

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessorToRun = oProcessorRepository.getProcessorByName(sName);

		List<PackageViewModel> aoPackages = Collections.emptyList();

		try {
			IPackageManager oPackageManager = getPackageManager(oProcessorToRun);

			aoPackages = oPackageManager.listPackages(sFlag);
		} catch (Exception oEx) {
			Utils.debugLog("PackageManagerResource.getListPackages: " + oEx);
		}

		return aoPackages;
	}

	@GET
	@Path("/managerVersion")
	public PackageManagerViewModel getManagerVersion(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName) throws Exception {
		Utils.debugLog("PackageManagerResource.getManagerVersion( " + "Name: " + sName + " )");

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessorToRun = oProcessorRepository.getProcessorByName(sName);

		PackageManagerViewModel oPackageManagerVM = null;

		try {
			IPackageManager oPackageManager = getPackageManager(oProcessorToRun);

			oPackageManagerVM = oPackageManager.getManagerVersion();
		} catch (Exception oEx) {
			Utils.debugLog("PackageManagerResource.getManagerVersion: " + oEx);
		}

		return oPackageManagerVM;
	}

	@GET
	@Path("/executeCommand")
	public String executeCommand(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName,
			@QueryParam("command") String sCommand) throws Exception {
		Utils.debugLog("PackageManagerResource.executeCommand( " + "Name: " + sName + "; " + "Command: " + sCommand + " )");

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessorToRun = oProcessorRepository.getProcessorByName(sName);

		String sCommandExecutionOutput = null;

		try {
			IPackageManager oPackageManager = getPackageManager(oProcessorToRun);

			sCommandExecutionOutput = oPackageManager.executeCommand(sCommand);
		} catch (Exception oEx) {
			Utils.debugLog("PackageManagerResource.getManagerVersion: " + oEx);
		}

		return sCommandExecutionOutput;
	}

	private IPackageManager getPackageManager(Processor oProcessor) {
		IPackageManager oPackageManager = null;

		String sType = oProcessor.getType();

		String sIp = "127.0.0.1";
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
