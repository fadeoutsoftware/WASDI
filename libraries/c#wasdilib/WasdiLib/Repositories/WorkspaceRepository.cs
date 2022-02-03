using Microsoft.Extensions.Logging;

using WasdiLib.Extensions;
using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal class WorkspaceRepository : IWorkspaceRepository
    {

        private const string WORKSPACES_BY_USER_PATH = "ws/byuser";
        private const string WORKSPACES_CREATE_PATH = "ws/create";
        private const string WORKSPACES_DELETE_PATH = "ws/delete";
        private const string WORKSPACE_BY_WS_ID_PATH = "ws/getws";

        private readonly ILogger<WorkspaceRepository> _logger;

        private readonly HttpClient _wasdiHttpClient;

        public WorkspaceRepository(ILogger<WorkspaceRepository> logger, IHttpClientFactory httpClientFactory)
        {
            _logger = logger;

            _wasdiHttpClient = httpClientFactory.CreateClient("WasdiApi");
        }

        public async Task<List<Workspace>> GetWorkspaces(string sBaseUrl, string sSessionId)
        {
            _logger.LogDebug("GetWorkspaces()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            var response = await _wasdiHttpClient.GetAsync(sBaseUrl + WORKSPACES_BY_USER_PATH);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<List<Workspace>>();
        }

        public async Task<WorkspaceEditorViewModel> GetWorkspace(string sBaseUrl, string sSessionId, string sWorkspaceId)
        {
            _logger.LogDebug("GetWorkspace({0})", sWorkspaceId);

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("workspace", sWorkspaceId);

            var content = new FormUrlEncodedContent(parameters);
            string query = content.ReadAsStringAsync().Result;
            if (!string.IsNullOrEmpty(query))
                query = "?" + query;

            var response = await _wasdiHttpClient.GetAsync(sBaseUrl + WORKSPACE_BY_WS_ID_PATH + query);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<WorkspaceEditorViewModel>();
        }

        public async Task<PrimitiveResult> CreateWorkspace(string sBaseUrl, string sSessionId, string sWorkspaceName, string sNodeCode)
        {
            _logger.LogDebug("CreateWorkspace({0}, {1})", sWorkspaceName, sNodeCode);

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            if (!string.IsNullOrEmpty(sWorkspaceName))
                parameters.Add("name", sWorkspaceName);

            if (!string.IsNullOrEmpty(sNodeCode))
                parameters.Add("node", sNodeCode);

            var content = new FormUrlEncodedContent(parameters);

            string query = content.ReadAsStringAsync().Result;
            if (!string.IsNullOrEmpty(query))
                query = "?" + query;

            var response = await _wasdiHttpClient.GetAsync(sBaseUrl + WORKSPACES_CREATE_PATH + query);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<PrimitiveResult>();
        }

        public async Task<string> DeleteWorkspace(string sBaseUrl, string sSessionId, string sWorkspaceId)
        {
            _logger.LogDebug("DeleteWorkspace({0})", sWorkspaceId);

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("workspace", sWorkspaceId);
            parameters.Add("deletelayer", "true");
            parameters.Add("deletefile", "true");

            var content = new FormUrlEncodedContent(parameters);
            string query = content.ReadAsStringAsync().Result;

            if (!string.IsNullOrEmpty(query))
                query = "?" + query;


            var response = await _wasdiHttpClient.DeleteAsync(sBaseUrl + WORKSPACES_DELETE_PATH + query);

            var data = string.Empty;

            if (!response.IsSuccessStatusCode)
            {
                data = await response.Content.ReadAsStringAsync();
                _logger.LogError(data);
            }

            return data;
        }

    }
}
