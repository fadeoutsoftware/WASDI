using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IWorkflowRepository
    {
        Task<List<Workflow>> GetWorkflows(string sBaseUrl, string sSessionId);

        Task<PrimitiveResult> RunWorkflow(string sBaseUrl, string sSessionId, string sWorkspaceId, string? sParentId, Workflow oWorkflow);

    }
}
