package org.geoserver.wpasdi;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;

@DescribeProcess(title="WASDIProcessStatus", description="Get the status of a WASDI Process. Every WPS asych available operation returns a processId. This can be used to get the status of the process using this API")
public class WASDIProcessStatus implements GeoServerProcess{
	
	@DescribeResult(name="ProcessStatus", description="Status of the specifed Process. Can be WAITING, RUNNING, STOPPED, DONE, ERROR, NOTFOUND")
	public String execute(@DescribeParameter(name="processId", description="ProcessId got from a previous call") String sProcessId) {
		String sStatus = "ERROR";
		
		try {
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessWS = oProcessWorkspaceRepository.GetProcessByProcessObjId(sProcessId);
			
			if (oProcessWS != null) {
				sStatus = oProcessWS.getStatus();
			}
			else {
				sStatus = "NOTFOUND";
			}
		}
		catch (Exception e) {
			System.out.println("WPASDI.WASDIProcessStatus: Error " + e.getMessage());
			e.printStackTrace();
		}
		
		return sStatus;
	}
}
