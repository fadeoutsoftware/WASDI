package wasdi.processors;

import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.log.WasdiLog;

public class PipOneShotProcessorEngine extends DockerProcessorEngine {
	
	@Override
	public boolean deploy(ProcessorParameter oParameter) {
		WasdiLog.debugLog("PipOneShotProcessorEngine.deploy");
		return super.deploy(oParameter);
	}
	
	@Override
	public boolean run(ProcessorParameter oParameter) {
		WasdiLog.debugLog("PipOneShotProcessorEngine.run");
		return super.run(oParameter);
	}
	

	@Override
	protected IPackageManager getPackageManager(String sIp, int iPort) {
		return null;
	}

}
