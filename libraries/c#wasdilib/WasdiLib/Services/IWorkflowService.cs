using WasdiLib.Models;

namespace WasdiLib.Services
{
    internal interface IWorkflowService
    {
        List<Workflow> GetWorkflows(string sBaseUrl, string sSessionId);

        PrimitiveResult RunWorkflow(string sBaseUrl, string sSessionId, string sWorkspaceId, string? sParentId, Workflow oWorkflow);

    }
}
