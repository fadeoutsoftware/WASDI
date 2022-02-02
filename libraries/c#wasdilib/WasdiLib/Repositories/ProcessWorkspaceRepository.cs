using System.Text;

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
        private const string PROCESSES_STATUS_BY_IDS_PATH = "/process/statusbyid";
        private const string PROCESSES_UPDATE_PATH = "/process/updatebyid";
        private const string PROCESSES_UPDATE_PAYLOAD_PATH = "/process/setpayload";

        private readonly ILogger<ProcessWorkspaceRepository> _logger;

        private readonly HttpClient _wasdiHttpClient;

        public ProcessWorkspaceRepository(ILogger<ProcessWorkspaceRepository> logger, IHttpClientFactory httpClientFactory)
        {
            _logger = logger;

            _wasdiHttpClient = httpClientFactory.CreateClient("WasdiApi");
        }

        public async Task<List<ProcessWorkspace>> GetProcessWorkspacesByWorkspaceId(string sSessionId, string workspaceId)
        {
            _logger.LogDebug("GetProcessWorkspacesByWorkspaceId()");

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("workspace", workspaceId);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
            {
                query = "?" + query;
            }

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            var response = await _wasdiHttpClient.GetAsync(PROCESS_WORKSPACES_BY_WORKSPACE_ID_PATH + query);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<List<ProcessWorkspace>>();
        }

        public async Task<ProcessWorkspace> GetProcessWorkspaceByProcessId(string sSessionId, string processId, string sWorkspaceBaseUrl)
        {
            _logger.LogDebug("GetProcessWorkspacesByProcessId()");

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("procws", processId);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
            {
                query = "?" + query;
            }

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            string url = sWorkspaceBaseUrl + PROCESS_WORKSPACES_BY_PROCESS_ID_PATH + query;
            var response = await _wasdiHttpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<ProcessWorkspace>();
        }

        public async Task<string> GetProcessesStatus(string sSessionId, List<string> asIds, string sWorkspaceBaseUrl)
        {
            _logger.LogDebug("GetProcessesStatus()");

            var json = SerializationHelper.ToJson(asIds);

            var requestPayload = new StringContent(json, Encoding.UTF8, "application/json");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            string url = sWorkspaceBaseUrl + PROCESSES_STATUS_BY_IDS_PATH;
            var response = await _wasdiHttpClient.PostAsync(url, requestPayload);

            var data = string.Empty;

            if (response.IsSuccessStatusCode)
                data = await response.ConvertResponse<string>();

            return data;
        }

        public async Task<ProcessWorkspace> UpdateProcessStatus(string sSessionId, string processId, string sStatus,
            int iPerc, string sWorkspaceBaseUrl)
        {
            _logger.LogDebug("UpdateProcessStatus()");

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("procws", processId);
            parameters.Add("status", sStatus);

            if (iPerc >= 0 && iPerc <= 100)
                parameters.Add("perc", iPerc.ToString());

            parameters.Add("sendrabbit", "1");

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
            {
                query = "?" + query;
            }

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            string url = sWorkspaceBaseUrl + PROCESSES_UPDATE_PATH + query;
            var response = await _wasdiHttpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<ProcessWorkspace>();
        }

        public async Task<ProcessWorkspace> UpdateProcessPayload(string sSessionId, string processId, string sData,
            string sWorkspaceBaseUrl)
        {
            _logger.LogDebug("UpdateProcessPayload()");

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("procws", processId);
            parameters.Add("payload", sData);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            if (!String.IsNullOrEmpty(query))
            {
                query = "?" + query;
            }

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            string url = sWorkspaceBaseUrl + PROCESSES_UPDATE_PAYLOAD_PATH + query;
            var response = await _wasdiHttpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<ProcessWorkspace>();
        }

    }
}
