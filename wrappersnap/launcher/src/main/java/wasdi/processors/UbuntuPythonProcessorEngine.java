package wasdi.processors;

import java.util.ArrayList;

public class UbuntuPythonProcessorEngine extends DockerProcessorEngine {

	public UbuntuPythonProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser) {
		super(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
		
		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "python27";		
	}

	@Override
	protected void handleRunCommand(String sCommand, ArrayList<String> asArgs) {
		
	}

	@Override
	protected void handleBuildCommand(String sCommand, ArrayList<String> asArgs) {
		
	}

	@Override
	protected void handleUnzippedProcessor(String sProcessorFolder) {
		
	}
	

}
