package org.geoserver.wpasdi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.DeployProcessorParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.RunningProcessorViewModel;

@DescribeProcess(title="WASDIExecuteProcessor", description="WASDI Execute a user-configured processor")

public class WASDIExecuteProcessor implements GeoServerProcess {
	
	public static ObjectMapper s_oMapper = new ObjectMapper();
	
	
	@DescribeResult(name="WasdiProcessId", description="Unique identifier of the WASDI process created. Use this ID to ask for the status")
	public String execute(@DescribeParameter(name="ProcessorName", description="Name of the processor uploaded in WASDI") String sProcessorName,
		@DescribeParameter(name="encodedJSON", description="URI Encoded JSON Parameter that will be sent to the Processor") String sEncodedJson
		) throws IOException  {
		
		RunningProcessorViewModel oRunning = new RunningProcessorViewModel();
		String sUserId ="wps";
		String sSerializationPath = "/usr/lib/wasdi/params/";
		
		try {
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToRun = oProcessorRepository.GetProcessorByName(sProcessorName);

			// Schedule the process to run the processor
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
			
			try
			{
				
				String sProcessObjId = Utils.GetRandomName();
				
				String sPath = sSerializationPath + oProcessWorkspace.getProcessObjId();

				DeployProcessorParameter oDeployPocessorParameter = new DeployProcessorParameter();
				oDeployPocessorParameter.setName(sProcessorName);
				oDeployPocessorParameter.setProcessorID(oProcessorToRun.getProcessorId());
				oDeployPocessorParameter.setWorkspace("wps");
				oDeployPocessorParameter.setUserId(sUserId);
				oDeployPocessorParameter.setExchange("wps");
				oDeployPocessorParameter.setProcessObjId(sProcessObjId);
				oDeployPocessorParameter.setJson(sEncodedJson);
				oDeployPocessorParameter.setProcessorType(oProcessorToRun.getType());
				
				SerializationUtils.serializeObjectToXML(sPath, oDeployPocessorParameter);

				oProcessWorkspace.setOperationDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
				oProcessWorkspace.setOperationType(LauncherOperations.RUNPROCESSOR.name());
				oProcessWorkspace.setProductName(sProcessorName);
				oProcessWorkspace.setWorkspaceId("wps");
				oProcessWorkspace.setUserId(sUserId);
				oProcessWorkspace.setProcessObjId(sProcessObjId);
				oProcessWorkspace.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcessWorkspace);
								
				oRunning.setJsonEncodedResult("");
				oRunning.setName(sProcessorName);
				oRunning.setProcessingIdentifier(oProcessWorkspace.getProcessObjId());
				oRunning.setProcessorId(oProcessorToRun.getProcessorId());
				oRunning.setStatus("CREATED");
				//Wasdi.DebugLog("ProcessorsResource.run: done"); 
			}
			catch(Exception oEx){
				System.out.println("ProcessorsResource.run: Error scheduling the run process " + oEx.getMessage());
				oEx.printStackTrace();
				oRunning.setStatus(ProcessStatus.ERROR.toString());
				
				String sReturn = s_oMapper.writeValueAsString(oRunning);
				
				return sReturn;
			}
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			oRunning.setStatus(ProcessStatus.ERROR.toString());
			String sReturn = s_oMapper.writeValueAsString(oRunning);
			return sReturn;
		}
		
		
		String sReturn = s_oMapper.writeValueAsString(oRunning);
		return sReturn;
		
	}
}
