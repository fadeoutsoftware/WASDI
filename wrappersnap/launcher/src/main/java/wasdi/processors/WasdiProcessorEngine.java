package wasdi.processors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;

public abstract class WasdiProcessorEngine {
	
	protected String m_sWorkingRootPath = "";
	protected String m_sDockerTemplatePath = "";
	
	
	public static WasdiProcessorEngine GetProcessorEngine(String sType,String sWorkingRootPath, String sDockerTemplatePath) {
		
		if (Utils.isNullOrEmpty(sType)) {
			sType = ProcessorTypes.UBUNTU_PYTHON27_SNAP;
		}
		
		if (sType.equals(ProcessorTypes.UBUNTU_PYTHON27_SNAP)) {
			return new UbuntuPythonProcessorEngine(sWorkingRootPath,sDockerTemplatePath);
		}
		else if (sType.equals(ProcessorTypes.IDL)) {
			return new IDLProcessorEngine(sWorkingRootPath,sDockerTemplatePath);
		}
		else if (sType.equals(ProcessorTypes.UBUNTU_PYTHON37_SNAP)) {
			return new UbuntuPython37ProcessorEngine(sWorkingRootPath,sDockerTemplatePath);
		}
		else {
			return new UbuntuPythonProcessorEngine(sWorkingRootPath, sDockerTemplatePath);
		}
	}
	
	public WasdiProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath) {
		m_sWorkingRootPath = sWorkingRootPath;
		m_sDockerTemplatePath = sDockerTemplatePath;
	}
	
	/**
	 * Deploy a new Processor in WASDI
	 * @param oParameter
	 */
	public abstract boolean DeployProcessor(ProcessorParameter oParameter);
	
	public abstract boolean run(ProcessorParameter oParameter);
	
	public abstract boolean delete(ProcessorParameter oParameter);
	
	public void shellExec(String sCommand, List<String> asArgs) {
		shellExec(sCommand,asArgs,true);
	}
	
	public void shellExec(String sCommand, List<String> asArgs, boolean bWait) {
		try {
			if (asArgs==null) asArgs = new ArrayList<String>();
			asArgs.add(0, sCommand);
			ProcessBuilder pb = new ProcessBuilder(asArgs.toArray(new String[0]));
			pb.redirectErrorStream(true);
			Process process = pb.start();
			if (bWait) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null)
					LauncherMain.s_oLogger.debug("[docker]: " + line);
				process.waitFor();				
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
