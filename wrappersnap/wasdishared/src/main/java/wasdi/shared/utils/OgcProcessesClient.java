package wasdi.shared.utils;

import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.ogcprocesses.Conformance;
import wasdi.shared.viewmodels.ogcprocesses.Execute;
import wasdi.shared.viewmodels.ogcprocesses.JobList;
import wasdi.shared.viewmodels.ogcprocesses.LandingPage;
import wasdi.shared.viewmodels.ogcprocesses.ProcessList;
import wasdi.shared.viewmodels.ogcprocesses.Results;
import wasdi.shared.viewmodels.ogcprocesses.StatusInfo;

/**
 * OGC Processes API Client
 * 
 * The class implements the calls to the OGC Processes API standard.
 * 
 * @author p.campanella
 *
 */
public class OgcProcessesClient {
	
	private String m_sBaseUrl;
	
	/**
	 * Default constructor
	 */
	public OgcProcessesClient() {
	}
	
	/**
	 * Constructor with the base url to use
	 * @param sBaseUrl
	 */
	public OgcProcessesClient(String sBaseUrl) {
		setBaseUrl(sBaseUrl);
	}
	
	public String getBaseUrl() {
		return m_sBaseUrl;
	}

	public void setBaseUrl(String sBaseUrl) {
		
		if (!sBaseUrl.endsWith("/")) sBaseUrl += "/";
		
		this.m_sBaseUrl = sBaseUrl;
	}
	
	
	/**
	 * Gets the landing page of the server.
	 * Can also be used as a test to see if it is alive
	 * @return LandingPage View Model
	 */
	public LandingPage getLandingPage() {
		try {
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(m_sBaseUrl); 
			String sResponse = oHttpCallResponse.getResponseBody();
			
			if (Utils.isNullOrEmpty(sResponse)) {
				WasdiLog.debugLog("OgcProcessesClient.getLandingPage: empty response, return null");
				return null;
			}
			
			LandingPage oLandingPage = MongoRepository.s_oMapper.readValue(sResponse,LandingPage.class);
			
			return oLandingPage;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("OgcProcessesClient.getLandingPage: Exception " + oEx.toString());
			return null;
		}
	}
	
	/**
	 * Get the conformance declaration of the server
	 * @return Conformance View Model
	 */
	public Conformance getConformance() {
		try {
			String sUrl = m_sBaseUrl + "conformance";
			
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl); 
			String sResponse = oHttpCallResponse.getResponseBody();
			
			if (Utils.isNullOrEmpty(sResponse)) {
				WasdiLog.debugLog("OgcProcessesClient.getConformance: empty response, return null");
				return null;
			}
			
			Conformance oConformance = MongoRepository.s_oMapper.readValue(sResponse, Conformance.class);
			
			return oConformance;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("OgcProcessesClient.getConformance: Exception " + oEx.toString());
			return null;
		}
	}

	/**
	 * Get the list of processes available in the server
	 * @return ProcessList View Model 
	 */
	public ProcessList getProcesses() {
		try {
			String sUrl = m_sBaseUrl + "processes";
			
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl); 
			String sResponse = oHttpCallResponse.getResponseBody();
			
			if (Utils.isNullOrEmpty(sResponse)) {
				WasdiLog.debugLog("OgcProcessesClient.getProcesses: empty response, return null");
				return null;
			}
			
			ProcessList oProcessList = MongoRepository.s_oMapper.readValue(sResponse, ProcessList.class);
			
			return oProcessList;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("OgcProcessesClient.getProcesses: Exception " + oEx.toString());
			return null;
		}
	}
	
	/**
	 * Get the description of a single process
	 * @return ProcessList View Model 
	 */
	public wasdi.shared.viewmodels.ogcprocesses.Process getProcessDescription(String sProcessId) {
		try {
			String sUrl = m_sBaseUrl + "processes/"+sProcessId;
			
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl); 
			String sResponse = oHttpCallResponse.getResponseBody();
			
			if (Utils.isNullOrEmpty(sResponse)) {
				WasdiLog.debugLog("OgcProcessesClient.getProcessDescription: empty response, return null");
				return null;
			}
			
			wasdi.shared.viewmodels.ogcprocesses.Process oProcess = MongoRepository.s_oMapper.readValue(sResponse, wasdi.shared.viewmodels.ogcprocesses.Process.class);
			
			return oProcess;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("OgcProcessesClient.getProcessDescription: Exception " + oEx.toString());
			return null;
		}
	}
	
	/**
	 * Asynch execution of a Process
	 * @param sProcessId Id of the process to start
	 * @param oExecute Input parameters for the processor
	 * @return StatusInfo View Model
	 */
	public StatusInfo executProcess(String sProcessId, Execute oExecute) {
		try {
			String sUrl = m_sBaseUrl + "processes/"+sProcessId+"/execution";
			
			String sPayload = MongoRepository.s_oMapper.writeValueAsString(oExecute);
			
			HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(sUrl, sPayload, null); 
			String sResponse = oHttpCallResponse.getResponseBody();
			
			if (Utils.isNullOrEmpty(sResponse)) {
				WasdiLog.debugLog("OgcProcessesClient.executProcess: empty response, return null");
				return null;
			}
			
			StatusInfo oProcessList = MongoRepository.s_oMapper.readValue(sResponse, StatusInfo.class);
			
			return oProcessList;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("OgcProcessesClient.executProcess: Exception " + oEx.toString());
			return null;
		}
	}
	
	/**
	 * Get the list of jobs
	 * @return
	 */
	public JobList getJobs() {
		try {
			String sUrl = m_sBaseUrl + "jobs";
			
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl); 
			String sResponse = oHttpCallResponse.getResponseBody();
			
			if (Utils.isNullOrEmpty(sResponse)) {
				WasdiLog.debugLog("OgcProcessesClient.getJobs: empty response, return null");
				return null;
			}
			
			JobList oJobList = MongoRepository.s_oMapper.readValue(sResponse, JobList.class);
			
			return oJobList;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("OgcProcessesClient.getJobs: Exception " + oEx.toString());
			return null;
		}
	}

	/**
	 * Get the status of a job
	 * @param sJobId Id of the job
	 * @return Status Info View Model
	 */
	public StatusInfo getStatus(String sJobId) {
		try {
			String sUrl = m_sBaseUrl + "jobs/"+sJobId;
			
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl); 
			String sResponse = oHttpCallResponse.getResponseBody();
			
			if (Utils.isNullOrEmpty(sResponse)) {
				WasdiLog.debugLog("OgcProcessesClient.getStatus: empty response, return null");
				return null;
			}
			
			StatusInfo oStatusInfo = MongoRepository.s_oMapper.readValue(sResponse, StatusInfo.class);
			
			return oStatusInfo;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("OgcProcessesClient.getStatus: Exception " + oEx.toString());
			return null;
		}
	}	
	
	/**
	 * Stops the execution of a Job and removes it from the job list
	 * @param sJobId Id of the job to dismiss
	 * @return StatusInfo View Model
	 */
	public StatusInfo dismiss(String sJobId) {
		try {
			String sUrl = m_sBaseUrl + "jobs/"+sJobId;
			
			String sResponse = HttpUtils.httpDelete(sUrl, null);
			
			if (Utils.isNullOrEmpty(sResponse)) {
				WasdiLog.debugLog("OgcProcessesClient.dismiss: empty response, return null");
				return null;
			}
			
			StatusInfo oStatusInfo = MongoRepository.s_oMapper.readValue(sResponse, StatusInfo.class);
			
			return oStatusInfo;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("OgcProcessesClient.dismiss: Exception " + oEx.toString());
			return null;
		}
	}
	
	/**
	 * Get the results of a Job
	 * @param sJobId Job Id
	 * @return Results View Model
	 */
	public Results getResults(String sJobId) {
		try {
			String sUrl = m_sBaseUrl + "jobs/"+sJobId;
			
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl); 
			String sResponse = oHttpCallResponse.getResponseBody();
			
			if (Utils.isNullOrEmpty(sResponse)) {
				WasdiLog.debugLog("OgcProcessesClient.getResults: empty response, return null");
				return null;
			}
			
			Results oResults = MongoRepository.s_oMapper.readValue(sResponse, Results.class);
			
			return oResults;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("OgcProcessesClient.getResults: Exception " + oEx.toString());
			return null;
		}
	}	
		
}
