package wasdi.processors;

import java.util.ArrayList;

public class UbuntuPythonProcessorEngine extends DockerProcessorEngine {

	public UbuntuPythonProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath) {
		super(sWorkingRootPath, sDockerTemplatePath);
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
