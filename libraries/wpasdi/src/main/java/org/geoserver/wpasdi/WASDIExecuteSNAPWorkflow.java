package org.geoserver.wpasdi;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.parameters.ApplyOrbitParameter;
import wasdi.shared.parameters.CalibratorParameter;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.parameters.GraphSetting;
import wasdi.shared.parameters.MultilookingParameter;
import wasdi.shared.parameters.NDVIParameter;
import wasdi.shared.parameters.OperatorParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;

@DescribeProcess(title="WASDIExecuteSNAPWorkflow", description="WASDI Execute SNAP Workflow XML")
public class WASDIExecuteSNAPWorkflow implements GeoServerProcess {
	@DescribeResult(name="WasdiProcessId", description="Unique identifier of the WASDI process created. Use this ID to ask for the status")
	public String execute(@DescribeParameter(name="EOImageInput", description="Input EO Image, alredy imported in WASDI") String sEOInputFile,
		@DescribeParameter(name="EOImageOutput", description="Name of the Output Node") String sEOImageOutput, 
		@DescribeParameter(name="Workflow", description="WASDI Uploaded Workflow Id") String sWorkflowId) throws IOException  {
		
		String sUserId = "wps";
		String sProcessId = "-1";
		String sSerializationPath = "";
		
		GraphSetting oSettings = new GraphSetting();		
		String graphXml;
		
		SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
		SnapWorkflow oWF = oSnapWorkflowRepository.GetSnapWorkflow(sWorkflowId);
		
		if (oWF == null) return "-1";
		
		FileInputStream fileInputStream = new FileInputStream(oWF.getFilePath());
		
		graphXml = IOUtils.toString(fileInputStream, Charset.defaultCharset().name());
		oSettings.setGraphXml(graphXml);
				
		try {
			//Update process list
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcess = new ProcessWorkspace();
			
			String sProcessObjId = Utils.GetRandomName();

			String sPath = sSerializationPath + oProcess.getProcessObjId();
			
			OperatorParameter oParameter = GetParameter(LauncherOperations.GRAPH); 
			oParameter.setSourceProductName(sEOInputFile);
			oParameter.setDestinationProductName(sEOImageOutput);
			oParameter.setWorkspace("wps");
			oParameter.setUserId(sUserId);
			oParameter.setExchange("wps");
			oParameter.setProcessObjId(sProcessObjId);
			
			if (oSettings != null) oParameter.setSettings(oSettings);	
			
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

			try
			{
				oProcess.setOperationDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
				oProcess.setOperationType(LauncherOperations.GRAPH.toString());
				oProcess.setProductName(sEOInputFile);
				oProcess.setWorkspaceId("wps");
				oProcess.setUserId(sUserId);
				oProcess.setProcessObjId(sProcessObjId);
				oProcess.setStatus(ProcessStatus.CREATED.name());
				sProcessId = oRepository.InsertProcessWorkspace(oProcess);
			}
			catch(Exception oEx){
				System.out.println("SnapOperations.ExecuteOperation: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
				return sProcessId;
			}

		} catch (IOException e) {
			e.printStackTrace();
			return sProcessId;
		} catch (Exception e) {
			e.printStackTrace();
			return sProcessId;
		}

		return sProcessId;
	}
	
	private OperatorParameter GetParameter(LauncherOperations op)
	{
		switch (op) {
		case APPLYORBIT:
			return new ApplyOrbitParameter();
		case CALIBRATE:
			return new CalibratorParameter();
		case MULTILOOKING:
			return new MultilookingParameter();
		case TERRAIN:
			return new RangeDopplerGeocodingParameter();
		case NDVI:
			return new NDVIParameter();
			
		case GRAPH:
			return new GraphParameter();
		default:
			return null;
			
		}
	}
}