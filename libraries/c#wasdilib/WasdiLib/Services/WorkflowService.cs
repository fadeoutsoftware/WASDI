using Microsoft.Extensions.Logging;

using WasdiLib.Models;
using WasdiLib.Repositories;

namespace WasdiLib.Services
{
    internal class WorkflowService : IWorkflowService
    {

        private readonly ILogger<WorkflowService> _logger;
        private readonly IWorkflowRepository _repository;

        public WorkflowService(ILogger<WorkflowService> logger, IWorkflowRepository repository)
        {
            _logger = logger;
            _repository = repository;
        }

        public List<Workflow> GetWorkflows(string sBaseUrl, string sSessionId)
        {
            _logger.LogDebug("GetWorkflows()");

            return _repository.GetWorkflows(sBaseUrl, sSessionId).GetAwaiter().GetResult();
        }

        public PrimitiveResult CreateWorkflow(string sBaseUrl, string sSessionId, string sWorkspace, string? sParentId, Workflow oWorkflow)
        {
            _logger.LogDebug("CreateWorkflow()");

            return _repository.CreateWorkflow(sBaseUrl, sSessionId, sWorkspace, sParentId, oWorkflow).GetAwaiter().GetResult();
        }

    }
}
