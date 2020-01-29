package org.geoserver.wpasdi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.business.Processor;
import wasdi.shared.data.ProcessorRepository;

@DescribeProcess(title="WASDIGetAvailableProcessors", description="WASDI get list of available processors")
public class WASDIGetAvailableProcessors implements GeoServerProcess {
	
	public static ObjectMapper s_oMapper = new ObjectMapper();
	
	@DescribeResult(name="Processes", description="List of processors ids encoded in a JSON String")
	public String execute(@DescribeParameter(name="UserName", description="Name of the user") String sUserName) throws IOException  {
		
		ArrayList<WASDIProcess> aoProcesses = new ArrayList<>();
		
		try {
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			List<Processor> aoAvailableProcessors = oProcessorRepository.getDeployedProcessors();
			
			for (int iProcs = 0; iProcs<aoAvailableProcessors.size(); iProcs++) {
				
				WASDIProcess oProcess = new WASDIProcess(aoAvailableProcessors.get(iProcs).getProcessorId(), 
						aoAvailableProcessors.get(iProcs).getName(), 
						aoAvailableProcessors.get(iProcs).getDescription(), 
						aoAvailableProcessors.get(iProcs).getUserId());
				
				aoProcesses.add(oProcess);
			}
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
		}
		
		String sReturn = s_oMapper.writeValueAsString(aoProcesses);
		
		return sReturn;
	}
	
	public static final class WASDIProcess {
		String id;
		String name;
		String description;
		String creator;
		
		public WASDIProcess(String sId, String sName, String sDescription, String sCreator) {
			id =sId;
			name = sName;
			description = sDescription;
			creator = sCreator;
		}
		
		
		public String getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public String getDescription() {
			return description;
		}
		public String getCreator() {
			return creator;
		}

	}
}
