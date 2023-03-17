package wasdi.processors;

import wasdi.shared.packagemanagers.IPackageManager;

public class PipOneShotProcessorEngine extends DockerProcessorEngine {

	@Override
	protected IPackageManager getPackageManager(String sIp, int iPort) {
		return null;
	}

}
