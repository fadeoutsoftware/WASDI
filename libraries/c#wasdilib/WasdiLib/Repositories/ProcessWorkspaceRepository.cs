using System.Text;
using System.Web;
using Microsoft.Extensions.Logging;

using WasdiLib.Extensions;
using WasdiLib.Helpers;
using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal class ProcessWorkspaceRepository : IProcessWorkspaceRepository
    {

        private const string PROCESS_WORKSPACES_BY_WORKSPACE_ID_PATH = "process/byws";
        private const string PROCESS_WORKSPACES_BY_PROCESS_ID_PATH = "process/byid";
        private const string PROCESSES_STATUS_BY_IDS_PATH = "process/statusbyid";
        private const string PROCESSES_UPDATE_PATH = "process/updatebyid";
        private const string PROCESSES_UPDATE_PAYLOAD_PATH = "process/setpayload";
        private const string PROCESS_PAYLOAD_PATH = "process/payload";
        private const string PROCESS_SETSUBPID_PATH = "process/setsubpid";

        private readonly ILogger<ProcessWorkspaceRepository> _logger;

        private readonly HttpClient _wasdiHttpClient;

        public ProcessWorkspaceRepository(ILogger<ProcessWorkspaceRepository> logger, IHttpClientFactory httpClientFactory)
        {
            _logger = logger;

            _wasdiHttpClient = httpClientFactory.CreateClient("WasdiApi");
        }

        public async Task<List<ProcessWorkspace>> GetProcessWorkspacesByWorkspaceId(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId)
        {
            _logger.LogDebug("GetProcessWorkspacesByWorkspaceId()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("workspace", sWorkspaceId);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            query = "?" + query;

            string url = sWorkspaceBaseUrl + PROCESS_WORKSPACES_BY_WORKSPACE_ID_PATH + query;

            var response = await _wasdiHttpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<List<ProcessWorkspace>>();
        }

        public async Task<List<ProcessWorkspace>> GetProcessWorkspacesByWorkspace(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId,
            int iStartIndex, int? iEndIndex, string sStatus, string sOperationType, string sNamePattern)
        {
            _logger.LogDebug("GetProcessWorkspacesByWorkspace()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("workspace", sWorkspaceId);
            parameters.Add("startindex", iStartIndex.ToString());

            if (null != iEndIndex && iEndIndex > iStartIndex)
                parameters.Add("endindex", iEndIndex.Value.ToString());

            if (!String.IsNullOrEmpty(sStatus))
                parameters.Add("status", sStatus);

            if (!String.IsNullOrEmpty(sOperationType))
                parameters.Add("operationType", sOperationType);

            if (!String.IsNullOrEmpty(sNamePattern))
                parameters.Add("namePattern", sNamePattern);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            query = "?" + query;

            string url = sWorkspaceBaseUrl + PROCESS_WORKSPACES_BY_WORKSPACE_ID_PATH + query;

            var response = await _wasdiHttpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<List<ProcessWorkspace>>();
        }

        public async Task<ProcessWorkspace> GetProcessWorkspaceByProcessId(string sWorkspaceBaseUrl, string sSessionId, string sProcessId)
        {
            _logger.LogDebug("GetProcessWorkspacesByProcessId()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("procws", sProcessId);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            query = "?" + query;

            string url = sWorkspaceBaseUrl + PROCESS_WORKSPACES_BY_PROCESS_ID_PATH + query;

            var response = await _wasdiHttpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<ProcessWorkspace>();
        }

        public async Task<ProcessWorkspace> SetSubPid(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, int iSubPid)
        {
            _logger.LogDebug("SetSubPid()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("procws", sProcessId);
            parameters.Add("subpid", iSubPid.ToString());

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            query = "?" + query;

            string url = sWorkspaceBaseUrl + PROCESS_SETSUBPID_PATH + query;

            var response = await _wasdiHttpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<ProcessWorkspace>();
        }

        public async Task<string> GetProcessesStatus(string sWorkspaceBaseUrl, string sSessionId, List<string> asIds)
        {
            _logger.LogDebug("GetProcessesStatus()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            var json = SerializationHelper.ToJson(asIds);

            var requestPayload = new StringContent(json, Encoding.UTF8, "application/json");

            string url = sWorkspaceBaseUrl + PROCESSES_STATUS_BY_IDS_PATH;
            var response = await _wasdiHttpClient.PostAsync(url, requestPayload);

            var data = string.Empty;

            if (response.IsSuccessStatusCode)
                data = await response.ConvertResponse<string>();

            return data;
        }

        public async Task<string> GetProcessPayload(string sWorkspaceBaseUrl, string sSessionId, string sProcessObjId)
        {
            _logger.LogDebug("GetProcessPayload()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("procws", sProcessObjId);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            query = "?" + query;

            string url = sWorkspaceBaseUrl + PROCESS_PAYLOAD_PATH + query;

            var response = await _wasdiHttpClient.GetAsync(url);

            var data = string.Empty;

            if (response.IsSuccessStatusCode)
                data = await response.ConvertResponse<string>();

            if (data != null)
                data = HttpUtility.UrlDecode(data);

            return data;
        }

        public async Task<ProcessWorkspace> UpdateProcessStatus(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sStatus, int iPerc)
        {
            _logger.LogDebug("UpdateProcessStatus()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("procws", sProcessId);
            parameters.Add("status", sStatus);

            if (iPerc >= 0 && iPerc <= 100)
                parameters.Add("perc", iPerc.ToString());

            parameters.Add("sendrabbit", "1");

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            query = "?" + query;

            string url = sWorkspaceBaseUrl + PROCESSES_UPDATE_PATH + query;

            var response = await _wasdiHttpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<ProcessWorkspace>();
        }

        public async Task<ProcessWorkspace> UpdateProcessPayload(string sWorkspaceBaseUrl, string sSessionId, string sProcessId, string sData)
        {
            _logger.LogDebug("UpdateProcessPayload()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("procws", sProcessId);
            parameters.Add("payload", sData);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            query = "?" + query;

            string url = sWorkspaceBaseUrl + PROCESSES_UPDATE_PAYLOAD_PATH + query;

            var response = await _wasdiHttpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<ProcessWorkspace>();
        }

    }
}
