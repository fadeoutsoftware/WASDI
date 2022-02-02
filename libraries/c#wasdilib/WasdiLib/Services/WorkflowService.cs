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

        public List<Workflow> GetWorkflows(string sSessionId)
        {
            _logger.LogDebug("GetWorkflows()");

            return _repository.GetWorkflows(sSessionId).GetAwaiter().GetResult();
        }

        public WasdiResponse CreateWorkflow(string sSessionId, Workflow workflow)
        {
            _logger.LogDebug("CreateWorkflow()");

            return _repository.CreateWorkflow(sSessionId, workflow).GetAwaiter().GetResult();
        }

    }
}
