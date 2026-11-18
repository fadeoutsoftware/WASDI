package wasdi.processors.drivers;

import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.parameters.ProcessorParameter;

public class GPodRuntimeDriver extends ContainerRuntimeDriver {

	protected GPodRuntimeDriver(WasdiProcessorEngine oEngine) {
		super(oEngine);
	}

	@Override
	public String buildImage(ProcessorParameter oParameter) {
		return null;
	}

	@Override
	public String pushImage(ProcessorParameter oParameter, String sImageName) {
		return null;
	}

	@Override
	public String run(ProcessorParameter oParameter, String sTag, String sParams) {
		return null;
	}

	@Override
	public String waitForCompletion(ProcessorParameter oParameter) {
		return null;
	}

	@Override
	public String getStatus(String sProcessWorkspaceId) {
		return null;
	}

	@Override
	public boolean stop(String sProcessWorkspaceId) {
		return false;
	}

}
