package org.geoserver.wpasdi;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.parameters.DownloadFileParameter;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;


@DescribeProcess(title="ImportEOImage", description="Imports a new EO Image in Wasdi. Based on the full name of the image, will search the available WASDI providers and try to import the image for further processing")
public class WASDIImportEOImage implements GeoServerProcess{
	
	@DescribeResult(name="WasdiProcessId", description="Unique identifier of the WASDI process created. Use this ID to ask for the status")
	public String execute(@DescribeParameter(name="EOImageLink", description="Full link of the EO Image to Import") String sEOFileLink,
			@DescribeParameter(name="RESTCallback", description="Optional Address of a Web Callback that WASDI can call when the process is finished. WASDI will call <address>?processId=[processId]") String sWebCallbackAddress) {
		
		String sProcessObjId = "-1";
		
		try {	
			String sUserId = "wps";
			String sWorkspaceId = "wps";
			
			// TODO: HARD CODED TO MOVE TO CONFIG OR TO READ FROM EXISTING CONFIG
			String sSerializationPath = "/usr/lib/wasdi/params/";
			String sDownloadUser = "contrariwise";
			String sDownloadPassword="nientepasswordpersonali";
	
			//Update process list
			ProcessWorkspace oProcess = null;
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			
			sProcessObjId = Utils.GetRandomName();
			
			DownloadFileParameter oParameter = new DownloadFileParameter();
			oParameter.setQueue("");
			oParameter.setUrl(sEOFileLink);
			oParameter.setWorkspace("wps");
			oParameter.setUserId(sUserId);
			oParameter.setExchange(sWorkspaceId);
			oParameter.setBoundingBox("");
			oParameter.setDownloadUser(sDownloadUser);
			oParameter.setDownloadPassword(sDownloadPassword);
			//set the process object Id to params
			oParameter.setProcessObjId(sProcessObjId);

			String sPath = sSerializationPath + sProcessObjId;
			SerializationUtils.serializeObjectToXML(sPath, oParameter);

			try
			{
				oProcess = new ProcessWorkspace();
				oProcess.setOperationDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
				oProcess.setOperationType(LauncherOperations.DOWNLOAD.name());
				oProcess.setProductName(sEOFileLink);
				oProcess.setWorkspaceId(sWorkspaceId);
				oProcess.setUserId(sUserId);
				oProcess.setProcessObjId(sProcessObjId);
				oProcess.setStatus(ProcessStatus.CREATED.name());
				oRepository.InsertProcessWorkspace(oProcess);
			}
			catch(Exception oEx){
				System.out.println("WPASDI.ImportEOImage: Error updating process list " + oEx.getMessage());
				oEx.printStackTrace();
			}
			
		}
		catch (Exception e) {
			System.out.println("WPASDI.ImportEOImage: Error updating process list " + e.getMessage());
			e.printStackTrace();
		}
		return sProcessObjId;
	}
}
