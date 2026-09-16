package wasdi.processors.drivers;

import java.util.List;

import wasdi.LauncherMain;
import wasdi.asynch.PushDockerImagesThread;
import wasdi.processors.DockerProcessorEngine;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.docker.DockerUtils;
import wasdi.shared.utils.docker.containersViewModels.ContainerInfo;
import wasdi.shared.utils.docker.containersViewModels.constants.ContainerStates;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;

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
	public String buildImage(ProcessorParameter oParameter) {
		try {
			Processor oProcessor = new ProcessorRepository().getProcessor(oParameter.getProcessorID());

			if (oProcessor == null) {
				WasdiLog.errorLog("LocalDockerDriver.prepareContainerImage: processor not found");
				return null;
			}

			DockerUtils oDockerUtils = new DockerUtils(oProcessor, oParameter, PathsConfig.getProcessorFolder(oProcessor.getName()), m_oDockerEngine.getDockerRegistry(), m_oEngine.geProcessWorkspaceLogger());
			String sImageName = oDockerUtils.build();

			if (Utils.isNullOrEmpty(sImageName)) {
				WasdiLog.errorLog("LocalDockerDriver.prepareContainerImage: build returned an empty image name");
				return null;
			}

			// Registry upload is not every engine's concern (plain local engines never push): it stays
			// a separate step, called explicitly by the build-once engines via pushImageInRegisters.
			return sImageName;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LocalDockerDriver.prepareContainerImage: exception", oEx);
			return null;
		}
	}

	@Override
	public String pushImage(ProcessorParameter oParameter, String sImageName) {
		try {
			Processor oProcessor = new ProcessorRepository().getProcessor(oParameter.getProcessorID());

			if (oProcessor == null) {
				WasdiLog.errorLog("LocalDockerDriver.pushImage: processor not found");
				return null;
			}

			List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();

			DockerUtils oDockerUtils = new DockerUtils(oProcessor, oParameter, PathsConfig.getProcessorFolder(oProcessor.getName()));
			oDockerUtils.setProcessWorkspaceLogger(m_oEngine.geProcessWorkspaceLogger());

			// Here we keep track of how many registers we tried
			int iAvailableRegisters = 0;
			// Here we save the address of the image
			String sPushedImageAddress = "";

			// For each register: ordered by priority
			for (; iAvailableRegisters < aoRegisters.size(); iAvailableRegisters++) {

				DockerRegistryConfig oDockerRegistryConfig = aoRegisters.get(iAvailableRegisters);

				WasdiLog.debugLog("LocalDockerDriver.pushImage: try to push to " + oDockerRegistryConfig.id);

				sPushedImageAddress = loginAndPush(oDockerUtils, oDockerRegistryConfig, sImageName);

				if (!Utils.isNullOrEmpty(sPushedImageAddress)) {
					WasdiLog.debugLog("LocalDockerDriver.pushImage: image pushed");
					break;
				}
			}

			// Did we use all the available options?
			if (iAvailableRegisters < aoRegisters.size()) {

				PushDockerImagesThread oPushDockerImagesThread = new PushDockerImagesThread();
				oPushDockerImagesThread.setProcessor(oProcessor);

				for (int iOtherRegisters = 0; iOtherRegisters < aoRegisters.size(); iOtherRegisters++) {
					oPushDockerImagesThread.getRegisters().add(aoRegisters.get(iOtherRegisters));
				}

				WasdiLog.debugLog("LocalDockerDriver.pushImage: starting thread to push on other registries");
				oPushDockerImagesThread.start();
			}

			return Utils.isNullOrEmpty(sPushedImageAddress) ? null : sPushedImageAddress;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LocalDockerDriver.pushImage: exception", oEx);
			return null;
		}
	}

	/**
	 * Logs into a single registry and pushes the given image name to it.
	 */
	private String loginAndPush(DockerUtils oDockerUtils, DockerRegistryConfig oDockerRegistryConfig, String sImageName) {
		try {
			String sToken = oDockerUtils.loginInRegistry(oDockerRegistryConfig);

			if (Utils.isNullOrEmpty(sToken)) {
				WasdiLog.debugLog("LocalDockerDriver.loginAndPush: error logging in, return false.");
				return "";
			}

			boolean bPushed = oDockerUtils.push(sImageName, sToken);

			if (!bPushed) {
				WasdiLog.debugLog("LocalDockerDriver.loginAndPush: error in push, return false.");
				return "";
			}

			return sImageName;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("LocalDockerDriver.loginAndPush: Exception " + oEx.toString());
		}

		return "";
	}

	@Override
	public String run(ProcessorParameter oParameter, String sTag, String sParams) {
		try {
			Processor oProcessor = new ProcessorRepository().getProcessor(oParameter.getProcessorID());

			if (oProcessor == null) {
				WasdiLog.errorLog("LocalDockerDriver.run: processor not found");
				return null;
			}

			DockerUtils oDockerUtils = new DockerUtils(oProcessor, oParameter, PathsConfig.getProcessorFolder(oProcessor.getName()), m_oDockerEngine.getDockerRegistry(), m_oEngine.geProcessWorkspaceLogger());

			// sTag/sParams are not needed here: local Docker resolves the image from Processor+version,
			// and env vars are already staged on WasdiConfig.Current.dockers by the caller.
			// One-shot containers are always started fresh: never reused, never reconstructed.
			m_sContainerName = oDockerUtils.start("", oProcessor.getPort(), WasdiConfig.Current.dockers.removeDockersAfterShellExec, false);

			if (Utils.isNullOrEmpty(m_sContainerName)) {
				WasdiLog.errorLog("LocalDockerDriver.run: Impossible to start the application docker");
				m_oEngine.geProcessWorkspaceLogger().log("There was an error starting the application.");
				m_oEngine.geProcessWorkspaceLogger().log("Usually this can happen for these reasons:");
				m_oEngine.geProcessWorkspaceLogger().log("1-There was a problem in the last build of the App (usually due to missing or conflicted packages");
				m_oEngine.geProcessWorkspaceLogger().log("2-There was a problem contacting the Docker Regsitry");
				m_oEngine.geProcessWorkspaceLogger().log("3-The docker image is not available");
				m_oEngine.geProcessWorkspaceLogger().log("If a build is ongoing we can try again in few minutes.");
				m_oEngine.geProcessWorkspaceLogger().log("Or please contact our Discord support channel ( https://discord.gg/JYuNhPaZbE ) providing the Process Workspace Id: " + oParameter.getProcessObjId());
				return null;
			}

			oParameter.setContainerName(m_sContainerName);

			waitForContainerToStart(oDockerUtils, oProcessor);

			return m_sContainerName;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LocalDockerDriver.run: exception", oEx);
			return null;
		}
	}

	/**
	 * Polls until the container reaches RUNNING/EXITED/DEAD, confirming it actually launched.
	 */
	private void waitForContainerToStart(DockerUtils oDockerUtils, Processor oProcessor) {
		try {
			WasdiLog.debugLog("LocalDockerDriver.waitForContainerToStart: wait to let docker start");

			Integer iNumberOfAttemptsToPingTheServer = WasdiConfig.Current.dockers.numberOfAttemptsToPingTheServer;
			Integer iMillisBetweenAttmpts = WasdiConfig.Current.dockers.millisBetweenAttmpts;

			for (int i = 0; i < iNumberOfAttemptsToPingTheServer; i++) {

				ContainerInfo oContainer = oDockerUtils.getContainerInfoByImageName(oProcessor.getName(), oProcessor.getVersion());

				if (oContainer.State.equals(ContainerStates.RUNNING) || oContainer.State.equals(ContainerStates.EXITED) || oContainer.State.equals(ContainerStates.DEAD)) {
					return;
				}

				Thread.sleep(iMillisBetweenAttmpts);
			}

			WasdiLog.debugLog("LocalDockerDriver.waitForContainerToStart: attempts finished.. probably did not started!");
		}
		catch (InterruptedException oEx) {
			Thread.currentThread().interrupt();
			WasdiLog.errorLog("LocalDockerDriver.waitForContainerToStart: current thread was interrupted ", oEx);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LocalDockerDriver.waitForContainerToStart: exception ", oEx);
		}
	}

	@Override
	public String waitForCompletion(ProcessorParameter oParameter) {
		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

		try {
			Processor oProcessor = new ProcessorRepository().getProcessor(oParameter.getProcessorID());
			ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oParameter.getProcessObjId());
			String sProcId = oParameter.getProcessObjId();
			String sStatus = oProcessWorkspace.getStatus();

			WasdiLog.debugLog("LocalDockerDriver.waitForCompletion: wait for the processor to finish");

			if (sProcId.equals("ERROR")) {
				oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
				return ProcessStatus.ERROR.name();
			}

			long lTimeSpentMs = 0;
			long iThreadSleepMs = 2000;

			try {
				long iThreadSleep = Long.parseLong(WasdiConfig.Current.scheduler.processingThreadSleepingTimeMS);
				if (iThreadSleep > 0) {
					iThreadSleepMs = iThreadSleep;
				}
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("LocalDockerDriver.waitForCompletion: Could not read processingThreadSleepingTimeMS config: " + oEx);
			}

			int iSometimesCheck = 30;
			int iCycleCount = 0;
			boolean bForcedError = false;

			// Wait for the process to finish, while checking timeout
			while (!(sStatus.equals("DONE") || sStatus.equals("STOPPED") || sStatus.equals("ERROR"))) {

				try {
					Thread.sleep(iThreadSleepMs);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}

				oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());
				sStatus = oProcessWorkspace.getStatus();

				if (sStatus.equals(ProcessStatus.RUNNING.name())) {
					lTimeSpentMs += iThreadSleepMs;
				}

				iCycleCount++;

				if (iCycleCount == iSometimesCheck) {
					iCycleCount = 0;

					// The container name is always known here: confirm it is still alive
					if (!RunTimeUtils.isProcessStillAllive(m_sContainerName, false)) {
						WasdiLog.warnLog("LocalDockerDriver.waitForCompletion: Process " + oProcessWorkspace.getProcessObjId() + " has Name " + m_sContainerName + ", but looks not existing. We stop here");
						bForcedError = true;
					}

					if (bForcedError) {
						LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
						sStatus = ProcessStatus.ERROR.name();
						break;
					}
				}

				if (oProcessor.getTimeoutMs() > 0) {
					if (lTimeSpentMs > oProcessor.getTimeoutMs()) {
						WasdiLog.debugLog("LocalDockerDriver.waitForCompletion: Timeout of Processor with ProcId " + oProcessWorkspace.getProcessObjId() + " Time spent [ms] " + lTimeSpentMs);
						LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.STOPPED, 100);
						bForcedError = true;
						sStatus = ProcessStatus.STOPPED.name();
						break;
					}
				}
			}

			// The process finished: alone or forced?
			if (!bForcedError) {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.valueOf(oProcessWorkspace.getStatus()), oProcessWorkspace.getProgressPerc());
			}

			WasdiLog.debugLog("LocalDockerDriver.waitForCompletion: processor done");

			return sStatus;
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
			oDockerUtils.setProcessWorkspaceLogger(m_oEngine.geProcessWorkspaceLogger());

			return oDockerUtils.stop(oProcessor);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("LocalDockerDriver.stop: exception", oEx);
			return false;
		}
	}
}
