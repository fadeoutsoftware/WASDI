using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IWorkflowRepository
    {
        Task<List<Workflow>> GetWorkflows(string sBaseUrl, string sSessionId);

        Task<PrimitiveResult> CreateWorkflow(string sBaseUrl, string sSessionId, string sWorkspace, string? sParentId, Workflow oWorkflow);

    }
}
