package wasdi.operations;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;

import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
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
        	
            String sBasePath = WasdiConfig.s_oConfig.paths.DownloadRootPath;

            if (!sBasePath.endsWith("/")) sBasePath += "/";

            String sRunPath = sBasePath + "processors/" + oParameter.getProcessorName() + "/run_" + oParameter.getProcessorName() + ".sh";

            String sMatlabRunTimePath = WasdiConfig.s_oConfig.paths.MATLAB_RUNTIME_PATH;
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
                
                return true;
            } else {
                // error
                m_oLocalLogger.debug("Runmatlab.executeOperation: process done with code != 0");
                
                return false;
            }

        } catch (Exception oEx) {
            m_oLocalLogger.error("Runmatlab.executeOperation: exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.RUNMATLAB.name(), oParam.getWorkspace(), sError, oParam.getExchange());

        } 
        
		return false;
	}

}
