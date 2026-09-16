package wasdi.processors.drivers;

import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.parameters.ProcessorParameter;

/**
 * Abstracts the environment used to build and run a processor's container: a local Docker
 * daemon today, potentially a third-party GPU cloud in the future. Implementations decide
 * how the same build -> run -> monitor -> stop lifecycle maps onto their specific APIs.
 */
public abstract class ContainerRuntimeDriver {

	/**
	 * Minimum viable link back to the owning engine, used only for cross-cutting state
	 * (the process workspace logger) that has no place in the method signatures below.
	 * Everything else must come from the ProcessorParameter/id passed to each method.
	 */
	protected final WasdiProcessorEngine m_oEngine;

	protected ContainerRuntimeDriver(WasdiProcessorEngine oEngine) {
		m_oEngine = oEngine;
	}

	/**
	 * Builds the processor image from its already-assembled build context and uploads it to the registry.
	 * @param oParameter Processor Parameter
	 * @return the image tag/reference to pass to run(), or null on failure
	 */
	public abstract String buildImage(ProcessorParameter oParameter);

	/**
	 * Uploads an already-built image to the registry.
	 * @param oParameter Processor Parameter
	 * @param sImageName Image name/tag returned by buildImage()
	 * @return the pushed image address, or null on failure
	 */
	public abstract String pushImage(ProcessorParameter oParameter, String sImageName);

	/**
	 * Starts execution of the given image and confirms the resource actually launched.
	 * @param oParameter Processor Parameter
	 * @param sTag Image tag/reference returned by prepareContainerImage
	 * @param sParams Encoded run parameters (unused by the local driver, reserved for cloud drivers)
	 * @return an opaque runtime id (container name for local Docker), or null on failure
	 */
	public abstract String run(ProcessorParameter oParameter, String sTag, String sParams);

	/**
	 * Blocks until the process reaches a terminal WASDI status or its configured timeout elapses.
	 * @param oParameter Processor Parameter
	 * @return the final WASDI ProcessStatus name
	 */
	public abstract String waitForCompletion(ProcessorParameter oParameter);

	/**
	 * @param sProcessWorkspaceId Process Workspace Id
	 * @return the current WASDI-normalized status (RUNNING/DONE/ERROR/STOPPED)
	 */
	public abstract String getStatus(String sProcessWorkspaceId);

	/**
	 * Stops/decommissions the running resource.
	 * @param sProcessWorkspaceId Process Workspace Id
	 * @return true if the operation succeeded
	 */
	public abstract boolean stop(String sProcessWorkspaceId);
}

