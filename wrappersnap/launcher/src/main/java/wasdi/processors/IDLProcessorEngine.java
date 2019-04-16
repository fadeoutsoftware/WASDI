package wasdi.processors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;

public class IDLProcessorEngine extends WasdiProcessorEngine{
	
    /**
     * Object Mapper
     */
    public static ObjectMapper s_oMapper = new ObjectMapper();

    static  {
        s_oMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

	
	public IDLProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath) {
		super(sWorkingRootPath,sDockerTemplatePath);
	}

	@Override
	public boolean DeployProcessor(ProcessorParameter oParameter) {		
		//oParameter.getProcessorID()

		return true;
	}

	@Override
	public boolean run(ProcessorParameter oParameter) {
		
		LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: start");
		
		if (oParameter == null) return false;
		
		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessWorkspace oProcessWorkspace = null;		
		
		try {
			
			// Get My Own Process Workspace
			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProcessObjId(oParameter.getProcessObjId());
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			// First Check if processor exists
			String sProcessorName = oParameter.getName();
			//String sProcessorId = oParameter.getProcessorID();
			
			// Set the processor path
			String sDownloadRootPath = m_sWorkingRootPath;
			if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
			
			String sProcessorFolder = sDownloadRootPath + "processors/" + sProcessorName + "/" ;
			String sRunPath = sProcessorFolder + "run_" + sProcessorName + ".sh";
			// Create the file
			File oProcessorScriptFile = new File(sRunPath);
			
			LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: check if launch script exists");
			
			// Check it
			if (oProcessorScriptFile.exists()==false) {
				LauncherMain.s_oLogger.debug("IDLProcessorEngine.run the script to run Processor [" + sProcessorName + "] does not exists in path " + oProcessorScriptFile.getPath());
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}
			
			// Get The workspace name for parameters
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			List<Workspace> aoWorkspaces = oWorkspaceRepository.GetWorkspaceByUser(oParameter.getUserId());
			
			String sWorkspaceName = "";
			for (int iWorkspaces = 0; iWorkspaces<aoWorkspaces.size(); iWorkspaces++) {
				if (aoWorkspaces.get(iWorkspaces).getWorkspaceId().equals(oParameter.getWorkspace())) {
					sWorkspaceName = aoWorkspaces.get(iWorkspaces).getName();
					break;
				}
			}
			
			// Write Param and Config file
			String sConfigFile = sProcessorFolder + "config.properties";
			String sParamFile = sProcessorFolder + "params.txt";
			
			
			File oParameterFile = new File(sParamFile);
			File oConfigFile = new File (sConfigFile);
			
			BufferedWriter oConfigWriter = new BufferedWriter(new FileWriter(oConfigFile));
			
			if(null!= oConfigWriter) {
				LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: Creating config.properties file");

				oConfigWriter.write("BASEPATH=" + m_sWorkingRootPath);
				oConfigWriter.newLine();
				oConfigWriter.write("USER=" + oParameter.getUserId());
				oConfigWriter.newLine();
				oConfigWriter.write("WORKSPACE=" + sWorkspaceName);
				oConfigWriter.newLine();
				oConfigWriter.write("SESSIONID="+oParameter.getSessionID());
				oConfigWriter.newLine();
				oConfigWriter.write("ISONSERVER=1");
				oConfigWriter.newLine();
				oConfigWriter.write("DOWNLOADACTIVE=0");
				oConfigWriter.newLine();
				oConfigWriter.write("UPLOADACTIVE=0");
				oConfigWriter.newLine();
				oConfigWriter.write("VERBOSE=0");
				oConfigWriter.newLine();
				oConfigWriter.write("PARAMETERSFILEPATH=" + sParamFile);
				oConfigWriter.newLine();
				oConfigWriter.write("MYPROCID="+oParameter.getProcessObjId());
				oConfigWriter.newLine();				
				oConfigWriter.flush();
				oConfigWriter.close();
			}			
			
			
			BufferedWriter oParamWriter = new BufferedWriter(new FileWriter(oParameterFile));
			
			if(oParamWriter != null) {
				
				LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: Creating parameters file " + sParamFile);
				
				// Get the JSON
				String sJson = oParameter.getJson();
				
				// URL Decode
				try {
				    sJson = java.net.URLDecoder.decode(sJson, StandardCharsets.UTF_8.name());
				} catch (UnsupportedEncodingException e) {
					LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: Exception decoding JSON " + e.toString());
				}
				
				// Get the JSON as a Map
				Map<String, Object> aoParametersMap = s_oMapper.readValue(sJson, new TypeReference<Map<String,Object>>(){});
				
				// Write KEY=VALUE in the file
				for (String sKey : aoParametersMap.keySet()) {
					oParamWriter.write(sKey+"="+aoParametersMap.get(sKey).toString());
					oParamWriter.newLine();
				}
				
				// Flush and Close
				oParamWriter.flush();
				oParamWriter.close();
			}
			
			// Cmq for the shell exex
			String asCmd[] = new String[] {
					sRunPath
			};
			
			LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: shell exec " + Arrays.toString(asCmd));
			ProcessBuilder oProcBuilder = new ProcessBuilder(asCmd);
			Process oProc = oProcBuilder.start();
			
			BufferedReader oInput = new BufferedReader(new InputStreamReader(oProc.getInputStream()));
			
            String sLine;
            while((sLine=oInput.readLine()) != null) {
            	LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: envi stdout: " + sLine);
            }
            
            LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: waiting for the process to exit");
			
			if (oProc.waitFor() == 0) {
				// ok
				LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: process done with code 0");
				if (oProcessWorkspace != null) {
					oProcessWorkspace.setOperationEndDate(Utils.GetFormatDate(new Date()));
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
				}
			}
			else {
				// errore
				LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: process done with code != 0");
				if (oProcessWorkspace != null) {
					oProcessWorkspace.setOperationEndDate(Utils.GetFormatDate(new Date()));
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
			}			
			
		}
		catch (Exception oEx) {
			//String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
			//if (LauncherMain.s_oSendToRabbit!=null) LauncherMain.s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace,sError,sExchange);			
			LauncherMain.s_oLogger.error("IDLProcessorEngine.run Exception", oEx);
			try {
				if (oProcessWorkspace != null) oProcessWorkspace.setOperationEndDate(Utils.GetFormatDate(new Date()));
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			} catch (JsonProcessingException e) {
				LauncherMain.s_oLogger.error("IDLProcessorEngine.run Exception", e);
			}
			return false;
		}
		
		return true;
	}

}
