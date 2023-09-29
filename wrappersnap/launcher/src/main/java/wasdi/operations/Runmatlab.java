package wasdi.operations;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.MATLABProcParameters;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;

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

            String sRunPath = PathsConfig.getProcessorFolder(oParameter.getProcessorName()) + "run_" + oParameter.getProcessorName() + ".sh";

            String sMatlabRunTimePath = WasdiConfig.Current.paths.matlabRuntimePath;
            String sConfigFilePath = PathsConfig.getProcessorFolder(oParameter.getProcessorName()) + "config.properties";
            
            ArrayList<String> asCmd = new ArrayList<>();
            asCmd.add(sRunPath);
            asCmd.add(sMatlabRunTimePath);
            asCmd.add(sConfigFilePath);

            WasdiLog.debugLog("Runmatlab.executeOperation: shell exec");
            
            ShellExecReturn oReturn = RunTimeUtils.shellExec(asCmd, true, true, false, true);
            
            if (oReturn.getOperationReturn() == 0) {
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
