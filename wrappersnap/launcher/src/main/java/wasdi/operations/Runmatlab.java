package wasdi.operations;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.MATLABProcParameters;
import wasdi.shared.utils.log.WasdiLog;

public class Runmatlab extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {

		WasdiLog.debugLog("Runmatlab.executeOperation");
        
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}        

        try {
        	
        	MATLABProcParameters oParameter = (MATLABProcParameters) oParam; 
        	
            String sBasePath = WasdiConfig.Current.paths.downloadRootPath;

            if (!sBasePath.endsWith("/")) sBasePath += "/";

            String sRunPath = sBasePath + "processors/" + oParameter.getProcessorName() + "/run_" + oParameter.getProcessorName() + ".sh";

            String sMatlabRunTimePath = WasdiConfig.Current.paths.matlabRuntimePath;
            String sConfigFilePath = sBasePath + "processors/" + oParameter.getProcessorName() + "/config.properties";

            String asCmd[] = new String[]{sRunPath, sMatlabRunTimePath, sConfigFilePath};

            WasdiLog.debugLog("Runmatlab.executeOperation: shell exec " + Arrays.toString(asCmd));
            ProcessBuilder oProcBuilder = new ProcessBuilder(asCmd);

            oProcBuilder.directory(new File(sBasePath + "processors/" + oParameter.getProcessorName()));
            Process oProc = oProcBuilder.start();

            BufferedReader oInput = new BufferedReader(new InputStreamReader(oProc.getInputStream()));

            String sLine;
            while ((sLine = oInput.readLine()) != null) {
                WasdiLog.debugLog("Runmatlab.executeOperation: script stdout: " + sLine);
            }

            WasdiLog.debugLog("Runmatlab.executeOperation: waiting for the process to exit");

            if (oProc.waitFor() == 0) {
                // ok
                WasdiLog.debugLog("Runmatlab.executeOperation: process done with code 0");
                
                return true;
            } else {
                // error
                WasdiLog.debugLog("Runmatlab.executeOperation: process done with code != 0");
                
                return false;
            }

        } catch (Exception oEx) {
            WasdiLog.errorLog("Runmatlab.executeOperation: exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));

            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.RUNMATLAB.name(), oParam.getWorkspace(), sError, oParam.getExchange());

        } 
        
		return false;
	}

}
