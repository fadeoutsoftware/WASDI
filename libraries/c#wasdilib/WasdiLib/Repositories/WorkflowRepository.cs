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
        private const string WORKFLOWS_CREATE_PATH = "workflows/run";

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

            var response = await _wasdiHttpClient.GetAsync(sBaseUrl + WORKFLOWS_BY_USER_PATH);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<List<Workflow>>();
        }

        public async Task<PrimitiveResult> CreateWorkflow(string sBaseUrl, string sSessionId, string sWorkspace, string? sParentId, Workflow oWorkflow)
        {
            _logger.LogDebug("CreateWorkflow()");

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

            string sQueryParam = "?workspace=" + sWorkspace;
            if (sParentId != null)
            {
                sQueryParam += "&parent=" + sParentId;
            }


            var response = await _wasdiHttpClient.PostAsync(sBaseUrl + WORKFLOWS_CREATE_PATH + sQueryParam, requestPayload);
            response.EnsureSuccessStatusCode();

            return await response.ConvertResponse<PrimitiveResult>();
        }

    }
}
