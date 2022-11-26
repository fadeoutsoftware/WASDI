package wasdi.processors;

import java.io.File;

import org.apache.commons.io.FileUtils;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.payloads.DeployProcessorPayload;
import wasdi.shared.utils.EndMessageProvider;

public class EoepcaProcessorEngine extends WasdiProcessorEngine {
	
	public EoepcaProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser)  {
		super(sWorkingRootPath,sDockerTemplatePath, sTomcatUser);

		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "eoepca";
		
	}	
	
	public EoepcaProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "eoepca";
		
	}	

	@Override
	public boolean deploy(ProcessorParameter oParameter) {
		return deploy(oParameter, true);
	}
	
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
        LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.DeployProcessor: start");

        if (oParameter == null) return false;

        ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
        ProcessorRepository oProcessorRepository = new ProcessorRepository();
        ProcessWorkspace oProcessWorkspace = null;


        String sProcessorName = oParameter.getName();
        String sProcessorId = oParameter.getProcessorID();

        try {

            processWorkspaceLog("Start Deploy of " + sProcessorName + " Type " + oParameter.getProcessorType());

            oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
            oProcessWorkspace = m_oProcessWorkspace;

            if (bFirstDeploy) {
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
                processWorkspaceLog("This is a first deploy of this app");
            }

            // First Check if processor exists
            Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

            String sProcessorFolder = getProcessorFolder(sProcessorName);

            // Create the file
            File oProcessorZipFile = new File(sProcessorFolder + sProcessorId + ".zip");

            LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.DeployProcessor: check processor exists");

            // Check it
            if (oProcessorZipFile.exists() == false) {
                LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.DeployProcessor the Processor [" + sProcessorName + "] does not exists in path " + oProcessorZipFile.getPath());

                processWorkspaceLog("Cannot find the processor file... something went wrong");
                processWorkspaceLog(new EndMessageProvider().getBad());

                if (bFirstDeploy) {
                    oProcessorRepository.deleteProcessor(sProcessorId);
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
                return false;
            }

            if (bFirstDeploy)
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 2);
            
            LauncherMain.s_oLogger.error("EoepcaProcessorEngine.DeployProcessor: unzip processor");

            // Unzip the processor (and check for entry point myProcessor.py)
            if (!unzipProcessor(sProcessorFolder, sProcessorId + ".zip", oParameter.getProcessObjId())) {
                LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.DeployProcessor error unzipping the Processor [" + sProcessorName + "]");

                processWorkspaceLog("Error unzipping the processor");
                processWorkspaceLog(new EndMessageProvider().getBad());

                if (bFirstDeploy) {
                    oProcessorRepository.deleteProcessor(sProcessorId);
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
                return false;
            }

            //onAfterUnzipProcessor(sProcessorFolder);

            if (bFirstDeploy)
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 20);
            LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.DeployProcessor: copy container image template");

            // Copy Docker template files in the processor folder
            File oDockerTemplateFolder = new File(m_sDockerTemplatePath);
            File oProcessorFolder = new File(sProcessorFolder);

            FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);

            if (bFirstDeploy)
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 25);

            // Generate the image
            LauncherMain.s_oLogger.debug("EoepcaProcessorEngine.DeployProcessor: building image");
            //onAfterCopyTemplate(sProcessorFolder);

            processWorkspaceLog("Start building Image");

            // Create Docker Util and deploy the docker
            DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sWorkingRootPath, m_sTomcatUser);
            oDockerUtils.deploy();

            //onAfterDeploy(sProcessorFolder);

            processWorkspaceLog("Image done, start the docker");

            if (bFirstDeploy)
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 70);

            // Run the container: find the port and reconstruct the environment
            int iProcessorPort = oProcessorRepository.getNextProcessorPort();
            if (!bFirstDeploy) {
                iProcessorPort = oProcessor.getPort();
            }

            oDockerUtils.run(iProcessorPort);

            processWorkspaceLog("Application started");

            if (bFirstDeploy) {
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 90);
                oProcessor.setPort(iProcessorPort);
                oProcessorRepository.updateProcessor(oProcessor);
            }
            else {
            	waitForApplicationToStart(oParameter);
            	reconstructEnvironment(oParameter, iProcessorPort);
            }

            try {
                DeployProcessorPayload oDeployPayload = new DeployProcessorPayload();
                oDeployPayload.setProcessorName(sProcessorName);
                oDeployPayload.setType(oParameter.getProcessorType());
                oProcessWorkspace.setPayload(LauncherMain.s_oMapper.writeValueAsString(oDeployPayload));
            } catch (Exception oPayloadException) {
                LauncherMain.s_oLogger.error("EoepcaProcessorEngine.DeployProcessor Exception creating payload ", oPayloadException);
            }

            if (bFirstDeploy)
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
            
            
            processWorkspaceLog(new EndMessageProvider().getGood());

        } catch (Exception oEx) {

            processWorkspaceLog("There was an error... sorry...");
            processWorkspaceLog(new EndMessageProvider().getBad());

            LauncherMain.s_oLogger.error("EoepcaProcessorEngine.DeployProcessor Exception", oEx);
            try {
                if (bFirstDeploy) {
                    try {
                        oProcessorRepository.deleteProcessor(sProcessorId);
                    } catch (Exception oInnerEx) {
                        LauncherMain.s_oLogger.error("EoepcaProcessorEngine.DeployProcessor Exception", oInnerEx);
                    }

                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
            } catch (Exception e) {
                LauncherMain.s_oLogger.error("EoepcaProcessorEngine.DeployProcessor Exception", e);
            }
            return false;
        }

        return true;
	}

	@Override
	public boolean run(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean redeploy(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean libraryUpdate(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean refreshPackagesInfo(ProcessorParameter oParameter) {
		// TODO Auto-generated method stub
		return false;
	}

}
