package wasdi.operations;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;

import wasdi.ConfigReader;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.MATLABProcParameters;

public class Runmatlab extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {

		m_oLocalLogger.debug("Runmatlab.executeOperation");
        
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}        

        try {
        	
        	MATLABProcParameters oParameter = (MATLABProcParameters) oParam; 

            if (oProcessWorkspace != null) {

                oProcessWorkspace.setStatus(ProcessStatus.RUNNING.name());
                oProcessWorkspace.setProgressPerc(0);
                // update the process
                m_oProcessWorkspaceRepository.updateProcess(oProcessWorkspace);
                // send update process message
                if (m_oSendToRabbit != null && !m_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
                    m_oLocalLogger.debug("Runmatlab.executeOperation: Error sending rabbitmq message to update process list");
                }
            }

            String sBasePath = ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH");

            if (!sBasePath.endsWith("/")) sBasePath += "/";

            String sRunPath = sBasePath + "processors/" + oParameter.getProcessorName() + "/run_" + oParameter.getProcessorName() + ".sh";

            String sMatlabRunTimePath = ConfigReader.getPropValue("MATLAB_RUNTIME_PATH", "/usr/local/MATLAB/MATLAB_Runtime/v95");
            String sConfigFilePath = sBasePath + "processors/" + oParameter.getProcessorName() + "/config.properties";

            String asCmd[] = new String[]{sRunPath, sMatlabRunTimePath, sConfigFilePath};

            m_oLocalLogger.debug("Runmatlab.executeOperation: shell exec " + Arrays.toString(asCmd));
            ProcessBuilder oProcBuilder = new ProcessBuilder(asCmd);

            oProcBuilder.directory(new File(sBasePath + "processors/" + oParameter.getProcessorName()));
            Process oProc = oProcBuilder.start();

            BufferedReader oInput = new BufferedReader(new InputStreamReader(oProc.getInputStream()));

            String sLine;
            while ((sLine = oInput.readLine()) != null) {
                m_oLocalLogger.debug("Runmatlab.executeOperation: script stdout: " + sLine);
            }

            m_oLocalLogger.debug("Runmatlab.executeOperation: waiting for the process to exit");

            if (oProc.waitFor() == 0) {
                // ok
                m_oLocalLogger.debug("Runmatlab.executeOperation: process done with code 0");
                if (oProcessWorkspace != null)
                    oProcessWorkspace.setStatus(ProcessStatus.DONE.name());
                
                return true;
            } else {
                // error
                m_oLocalLogger.debug("Runmatlab.executeOperation: process done with code != 0");
                if (oProcessWorkspace != null)
                    oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
                
                return false;

            }

        } catch (Exception oEx) {
            m_oLocalLogger.error("Runmatlab.executeOperation: exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
            if (oProcessWorkspace != null)
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            if (m_oSendToRabbit != null)
                m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.RUNMATLAB.name(), oParam.getWorkspace(), sError, oParam.getExchange());

        } 
        
		return false;
	}

}
