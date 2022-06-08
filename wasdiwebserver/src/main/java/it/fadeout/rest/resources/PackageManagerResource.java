package it.fadeout.rest.resources;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import it.fadeout.Wasdi;
import wasdi.shared.apiclients.pip.PipApiClient;
import wasdi.shared.business.Package;
import wasdi.shared.business.PackageManager;
import wasdi.shared.business.Processor;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;

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
	public List<Package> getListPackages(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName, @QueryParam("flag") String sFlag) throws Exception {
		Utils.debugLog("PackageManagerResource.getListPackages( " + "Name: " + sName + ", " + "Flag: " + sFlag + " )");

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessorToRun = oProcessorRepository.getProcessorByName(sName);

		String sIp = "127.0.0.1";
		int sPort = oProcessorToRun.getPort();

		List<Package> aoPackages = Collections.emptyList();

		try {
			PipApiClient pipApiClient = new PipApiClient(sIp, sPort);

			aoPackages = pipApiClient.listPackages(sFlag);
		} catch (Exception oEx) {
			Utils.debugLog("PackageManagerResource.getListPackages: " + oEx);
		}

		return aoPackages;
	}

	@GET
	@Path("/managerVersion")
	public PackageManager getManagerVersion(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName) throws Exception {
		Utils.debugLog("PackageManagerResource.getManagerVersion( " + "Name: " + sName + " )");

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessorToRun = oProcessorRepository.getProcessorByName(sName);

		String sIp = "127.0.0.1";
		int iPort = oProcessorToRun.getPort();

		PackageManager oPackageManager = null;

		try {
			PipApiClient pipApiClient = new PipApiClient(sIp, iPort);

			oPackageManager = pipApiClient.getManagerVersion();
		} catch (Exception oEx) {
			Utils.debugLog("PackageManagerResource.getManagerVersion: " + oEx);
		}

		return oPackageManager;
	}

}
