package wasdi.processors;

import java.util.ArrayList;

public class OctaveProcessorEngine extends DockerProcessorEngine{

	public OctaveProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath)  {
		super(sWorkingRootPath,sDockerTemplatePath);

		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "octave";
		
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
