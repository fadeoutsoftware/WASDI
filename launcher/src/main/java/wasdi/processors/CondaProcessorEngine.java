package wasdi.processors;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.packagemanagers.CondaOneShotPackageManager;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Processor Engine dedicated to a python Conda Application, run as a one-shot container
 * @author p.campanella
 *
 */
public class CondaProcessorEngine extends OneShotProcessorEngine {
	
	public CondaProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.CONDA);		
	}
	
	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		IPackageManager oPackageManager = new CondaOneShotPackageManager(sUrl);

		return oPackageManager;
	}

	/**
	 * Users often upload a Conda manifest under its "standard" name (environment.yml/.yaml)
	 * instead of the env.yml our Docker template expects: normalize it here so the build does not fail.
	 */
	private static final String[] ENV_FILE_ALTERNATIVE_NAMES = { "environment.yml", "environment.yaml", "env.yaml" };

	@Override
	protected void onAfterUnzipProcessor(String sProcessorFolder) {
		super.onAfterUnzipProcessor(sProcessorFolder);

		try {
			File oEnvFile = new File(sProcessorFolder + "env.yml");

			// An alternative name present in *this* upload always wins: it is the source of truth for the
			// current update, while env.yml may just be a stale rename left over from a previous deploy.
			for (String sAlternativeName : ENV_FILE_ALTERNATIVE_NAMES) {
				File oAlternativeFile = new File(sProcessorFolder + sAlternativeName);

				if (oAlternativeFile.exists()) {
					if (oEnvFile.exists()) {
						WasdiLog.infoLog("CondaProcessorEngine.onAfterUnzipProcessor: found " + sAlternativeName + ", replacing the existing env.yml with it");
						processWorkspaceLog("Found " + sAlternativeName + ", replacing the existing env.yml with it");
						oEnvFile.delete();
					}
					else {
						WasdiLog.infoLog("CondaProcessorEngine.onAfterUnzipProcessor: found " + sAlternativeName + ", renaming it to env.yml");
						processWorkspaceLog("Found " + sAlternativeName + ", renaming to env.yml");
					}

					if (!oAlternativeFile.renameTo(oEnvFile)) {
						WasdiLog.errorLog("CondaProcessorEngine.onAfterUnzipProcessor: impossible to rename " + sAlternativeName + " to env.yml");
						processWorkspaceLog("Impossible to rename " + sAlternativeName + " to env.yml");
					}

					return;
				}
			}

			if (oEnvFile.exists()) {
				WasdiLog.infoLog("CondaProcessorEngine.onAfterUnzipProcessor: env.yml already present, keeping it");
			}
			else {
				WasdiLog.infoLog("CondaProcessorEngine.onAfterUnzipProcessor: no env.yml or known alternative found, proceeding without a Conda environment manifest");
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CondaProcessorEngine.onAfterUnzipProcessor: exception ", oEx);
		}
	}

	/**
	 * Adds or removes a dependency directly in env.yml, then triggers a redeploy to rebuild the image.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean environmentUpdate(ProcessorParameter oParameter) {

		if (oParameter == null) {
			WasdiLog.errorLog("CondaProcessorEngine.environmentUpdate: oParameter is null");
			return false;
		}

		if (Utils.isNullOrEmpty(oParameter.getJson())) {
			WasdiLog.errorLog("CondaProcessorEngine.environmentUpdate: update command is null or empty");
			return false;
		}

		if (!WasdiConfig.Current.isMainNode()) {
			WasdiLog.errorLog("CondaProcessorEngine.environmentUpdate: this processor manipulate environment only on the main node");
			return true;
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessWorkspace oProcessWorkspace = null;

		try {
			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = m_oProcessWorkspace;

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			String sProcessorName = oParameter.getName();
			String sProcessorId = oParameter.getProcessorID();

			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

			if (oProcessor == null) {
				WasdiLog.errorLog("CondaProcessorEngine.environmentUpdate: oProcessor is null [" + sProcessorId + "]");
				return false;
			}

			WasdiLog.infoLog("CondaProcessorEngine.environmentUpdate: update env for " + sProcessorName);

			JSONObject oJsonItem = new JSONObject(oParameter.getJson());
			Object oUpdateCommand = oJsonItem.get("updateCommand");

			if (oUpdateCommand == null || oUpdateCommand.equals(org.json.JSONObject.NULL)) {
				WasdiLog.debugLog("CondaProcessorEngine.environmentUpdate: refresh of the list of libraries.");
				return true;
			}

			String sUpdateCommand = (String) oUpdateCommand;
			WasdiLog.debugLog("CondaProcessorEngine.environmentUpdate: sUpdateCommand: " + sUpdateCommand);

			String[] asParts = sUpdateCommand.split("/");
			String sPackage = "";
			boolean bAdd = true;

			if (asParts.length >= 2) {
				sPackage = asParts[1];
				if (asParts[0].equals("removePackage")) {
					bAdd = false;
				}
			}

			if (Utils.isNullOrEmpty(sPackage)) {
				WasdiLog.debugLog("CondaProcessorEngine.environmentUpdate: cannot extract the name of the package, error");
				return false;
			}

			// A version, if present, pins the exact release; otherwise Conda resolves the latest one
			String sVersion = asParts.length >= 3 ? asParts[2] : "";
			String sDependencySpec = Utils.isNullOrEmpty(sVersion) ? sPackage : sPackage + "=" + sVersion;

			WasdiLog.debugLog("CondaProcessorEngine.environmentUpdate: " + (bAdd ? "Adding" : "Removing") + " Package " + sDependencySpec);

			String sProcessorPath = PathsConfig.getProcessorFolder(sProcessorName);
			File oEnvFile = new File(sProcessorPath + "env.yml");

			if (!oEnvFile.exists()) {
				WasdiLog.errorLog("CondaProcessorEngine.environmentUpdate: env.yml not found, cannot manage packages for this app");
				return false;
			}

			ObjectMapper oYamlMapper = new ObjectMapper(new YAMLFactory());
			Map<String, Object> oEnv = oYamlMapper.readValue(oEnvFile, LinkedHashMap.class);

			List<Object> aoDependencies = (List<Object>) oEnv.get("dependencies");
			if (aoDependencies == null) {
				aoDependencies = new ArrayList<>();
				oEnv.put("dependencies", aoDependencies);
			}

			// Remove any existing entry with the same name, in the top-level list and in the nested pip: list
			removeDependencyByName(aoDependencies, sPackage);

			if (bAdd) {
				aoDependencies.add(sDependencySpec);
			}

			oYamlMapper.writeValue(oEnvFile, oEnv);

			WasdiLog.debugLog("CondaProcessorEngine.environmentUpdate: env.yml updated, starting re-deploy");
			this.redeploy(oParameter);

			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("CondaProcessorEngine.environmentUpdate Exception", oEx);
			try {
				if (oProcessWorkspace != null) {
					if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
						oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
					}
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
			} catch (Exception e) {
				WasdiLog.errorLog("CondaProcessorEngine.environmentUpdate Exception", e);
			}
			return false;
		}
	}

	/**
	 * Extracts the bare package name from a Conda/pip dependency spec (e.g. "numpy=1.26.4" -> "numpy")
	 */
	private String getDependencyName(String sSpec) {
		return sSpec.split("[<>=!\\s]")[0].trim();
	}

	/**
	 * Removes any dependency matching sTargetName, both as a plain Conda entry and inside the nested pip: list
	 */
	@SuppressWarnings("unchecked")
	private void removeDependencyByName(List<Object> aoDependencies, String sTargetName) {
		Iterator<Object> oIterator = aoDependencies.iterator();

		while (oIterator.hasNext()) {
			Object oEntry = oIterator.next();

			if (oEntry instanceof String) {
				if (getDependencyName((String) oEntry).equalsIgnoreCase(sTargetName)) {
					oIterator.remove();
				}
			}
			else if (oEntry instanceof Map) {
				Map<String, Object> oEntryMap = (Map<String, Object>) oEntry;
				Object oPipEntry = oEntryMap.get("pip");

				if (oPipEntry instanceof List) {
					List<Object> aoPipDependencies = (List<Object>) oPipEntry;
					aoPipDependencies.removeIf(oPipDep -> oPipDep instanceof String && getDependencyName((String) oPipDep).equalsIgnoreCase(sTargetName));

					if (aoPipDependencies.isEmpty()) {
						oIterator.remove();
					}
				}
			}
		}
	}
}
