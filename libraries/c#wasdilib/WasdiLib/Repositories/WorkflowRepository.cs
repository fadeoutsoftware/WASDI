using System.Text;

using Microsoft.Extensions.Logging;

using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;

using WasdiLib.Extensions;
using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal class WorkflowRepository : IWorkflowRepository
    {

        private const string WORKFLOWS_BY_USER_PATH = "workflows/getbyuser";
        private const string WORKFLOWS_RUN_PATH = "workflows/run";

        private readonly ILogger<WorkflowRepository> _logger;

        private readonly HttpClient _wasdiHttpClient;

        public WorkflowRepository(ILogger<WorkflowRepository> logger, IHttpClientFactory httpClientFactory)
        {
            _logger = logger;

            _wasdiHttpClient = httpClientFactory.CreateClient("WasdiApi");
        }

        public async Task<List<Workflow>> GetWorkflows(string sBaseUrl, string sSessionId)
        {
            _logger.LogDebug("GetWorkflows()");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);

            string url = sBaseUrl + WORKFLOWS_BY_USER_PATH;

            var response = await _wasdiHttpClient.GetAsync(url);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<List<Workflow>>();
        }

        public async Task<PrimitiveResult> RunWorkflow(string sBaseUrl, string sSessionId, string sWorkspaceId, string? sParentId, Workflow oWorkflow)
        {
            _logger.LogDebug("RunWorkflow()");

            var json = JsonConvert.SerializeObject(oWorkflow,
                new JsonSerializerSettings
                {
                    ContractResolver = new CamelCasePropertyNamesContractResolver()
                }
            );

            var requestPayload = new StringContent(json, Encoding.UTF8, "application/json");

            _wasdiHttpClient.DefaultRequestHeaders.Clear();
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("Content-Type", "application/json");
            _wasdiHttpClient.DefaultRequestHeaders.TryAddWithoutValidation("x-session-token", sSessionId);


            Dictionary<string, string> parameters = new Dictionary<string, string>();
            parameters.Add("workspace", sWorkspaceId);

            if (sParentId != null)
                parameters.Add("parent", sParentId);

            var formUrlEncodedContent = new FormUrlEncodedContent(parameters);
            string query = formUrlEncodedContent.ReadAsStringAsync().Result;
            query = "?" + query;

            string url = sBaseUrl + WORKFLOWS_RUN_PATH + query;

            var response = await _wasdiHttpClient.PostAsync(url, requestPayload);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<PrimitiveResult>();
        }

    }
}
