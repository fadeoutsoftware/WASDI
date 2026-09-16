package wasdi.processors;

import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.docker.DockerUtils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Local Docker implementation of ContainerRuntimeDriver: delegates to the already-tested
 * DockerUtils and to the owning engine's existing build/run/wait helpers, unchanged in behavior.
 */
public class LocalDockerDriver extends ContainerRuntimeDriver {

	/**
	 * Narrower, Docker-specific reference: the abstract base only guarantees a WasdiProcessorEngine.
	 */
	private final DockerProcessorEngine m_oDockerEngine;

	/**
	 * Container name returned by run(), reused by waitForCompletion() within the same lifecycle
	 */
	private String m_sContainerName;

	public LocalDockerDriver(DockerProcessorEngine oEngine) {
		super(oEngine);
		m_oDockerEngine = oEngine;
	}

	@Override
	public String prepareContainerImage(ProcessorParameter oParameter) {
		try {
			Processor oProcessor = new ProcessorRepository().getProcessor(oParameter.getProcessorID());

			if (oProcessor == null) {
				WasdiLog.errorLog("LocalDockerDriver.prepareContainerImage: processor not found");
				return null;
			}

			DockerUtils oDockerUtils = new DockerUtils(oProcessor, oParameter, PathsConfig.getProcessorFolder(oProcessor.getName()), m_oDockerEngine.m_sDockerRegistry, m_oEngine.m_oProcessWorkspaceLogger);
			String sImageName = oDockerUtils.build();

			if (Utils.isNullOrEmpty(sImageName)) {
				WasdiLog.errorLog("LocalDockerDriver.prepareContainerImage: build returned an empty image name");
				return null;
			}

			// Registry upload still goes through the engine's existing, unchanged push logic
			String sPushedImageAddress = m_oDockerEngine.pushImageInRegisters(oProcessor);

			if (Utils.isNullOrEmpty(sPushedImageAddress)) {
				WasdiLog.errorLog("LocalDockerDriver.prepareContainerImage: impossible to push the image");
				return null;
			}

			return sPushedImageAddress;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LocalDockerDriver.prepareContainerImage: exception", oEx);
			return null;
		}
	}

	@Override
	public String run(ProcessorParameter oParameter, String sTag, String sParams) {
		try {
			Processor oProcessor = new ProcessorRepository().getProcessor(oParameter.getProcessorID());

			if (oProcessor == null) {
				WasdiLog.errorLog("LocalDockerDriver.run: processor not found");
				return null;
			}

			DockerUtils oDockerUtils = new DockerUtils(oProcessor, oParameter, PathsConfig.getProcessorFolder(oProcessor.getName()), m_oDockerEngine.m_sDockerRegistry, m_oEngine.m_oProcessWorkspaceLogger);

			// sTag/sParams are not needed here: local Docker resolves the image from Processor+version,
			// and env vars are already staged on WasdiConfig.Current.dockers by the caller.
			m_sContainerName = m_oDockerEngine.startContainerAndGetName(oDockerUtils, oProcessor, oParameter, false, WasdiConfig.Current.dockers.removeDockersAfterShellExec, false);

			return m_sContainerName;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LocalDockerDriver.run: exception", oEx);
			return null;
		}
	}

	@Override
	public String waitForCompletion(ProcessorParameter oParameter) {
		try {
			Processor oProcessor = new ProcessorRepository().getProcessor(oParameter.getProcessorID());
			ProcessWorkspace oProcessWorkspace = new ProcessWorkspaceRepository().getProcessByProcessObjId(oParameter.getProcessObjId());

			return m_oDockerEngine.waitForApplicationToFinish(oProcessor, oParameter.getProcessObjId(), oProcessWorkspace.getStatus(), oProcessWorkspace, m_sContainerName);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LocalDockerDriver.waitForCompletion: exception", oEx);
			return ProcessStatus.ERROR.name();
		}
	}

	@Override
	public String getStatus(String sProcessWorkspaceId) {
		try {
			ProcessWorkspace oProcessWorkspace = new ProcessWorkspaceRepository().getProcessByProcessObjId(sProcessWorkspaceId);

			if (oProcessWorkspace == null) {
				return ProcessStatus.ERROR.name();
			}

			return oProcessWorkspace.getStatus();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LocalDockerDriver.getStatus: exception", oEx);
			return ProcessStatus.ERROR.name();
		}
	}

	@Override
	public boolean stop(String sProcessWorkspaceId) {
		try {
			ProcessWorkspace oProcessWorkspace = new ProcessWorkspaceRepository().getProcessByProcessObjId(sProcessWorkspaceId);

			if (oProcessWorkspace == null) {
				WasdiLog.warnLog("LocalDockerDriver.stop: process workspace not found " + sProcessWorkspaceId);
				return false;
			}

			String sProcessorName = oProcessWorkspace.getProductName();
			Processor oProcessor = new ProcessorRepository().getProcessorByName(sProcessorName);

			// DockerUtils.stop() needs no ProcessorParameter: that is only used by start() for mount paths
			DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorName);
			oDockerUtils.setProcessWorkspaceLogger(m_oEngine.m_oProcessWorkspaceLogger);

			return oDockerUtils.stop(oProcessor);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LocalDockerDriver.stop: exception", oEx);
			return false;
		}
	}
}
